package dz.local.mealguard;
import org.json.*;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;
/** Explicit manual cancel-then-reserve only. No automatic cancellation, no write retries. */
final class Replacement {
 interface Gateway extends Engine.Gateway {JSONArray depots()throws Exception;void cancel(long reservationId)throws Exception;}
 static final class Conflict {
  final Engine.Meal target;final long oldId;final String oldArabic,oldFrench;
  Conflict(Engine.Meal meal,JSONObject old)throws Exception{target=meal;oldId=old.getLong("id");oldArabic=old.optString("depot_ar");oldFrench=old.optString("depot_fr");}
  String oldName(){return oldArabic.isEmpty()?oldFrench:oldArabic;}
  String describe(){return target.label()+"\nالحالي: "+oldName()+" (#"+oldId+")\nالجديد: "+Engine.name(target.depot);}
 }
 static final class Review {final List<Engine.Meal> plan=new ArrayList<>();final List<Conflict> conflicts=new ArrayList<>();}
 static JSONObject row(JSONArray rows,Engine.Meal target)throws Exception{
  JSONObject found=null;
  for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(!target.day.toString().equals(r.optString("date_reserve")))continue;if(Engine.mealType(r)!=target.type)continue;if(found!=null)throw new Engine.Problem(target.label()+": أكثر من حجز لنفس الوجبة؛ راجع Webetu.");found=r;}
  if(found!=null){if(found.optLong("id")<=0)throw new Engine.Problem("حجز دون رقم موثوق؛ لا يمكن استبداله.");if(found.has("status")&&!found.isNull("status")&&!Arrays.asList("confirmed","reserved","active","true","1").contains(Engine.norm(String.valueOf(found.get("status")))))throw new Engine.Problem(target.label()+": حالة الحجز غير مؤكدة؛ راجع Webetu قبل الاستبدال.");}
  return found;
 }
 static Review inspect(JSONObject cfg,JSONArray depots,JSONArray rows,LocalDate today)throws Exception{
  Review result=new Review();for(Engine.Meal target:Engine.plan(cfg,depots,new JSONArray(),today)){
   JSONObject r=row(rows,target);long existing=0;
   if(r!=null){try{existing=Engine.matching(new JSONArray().put(r),target);}catch(Engine.Problem mismatch){if(r.optString("depot_ar").trim().isEmpty()&&r.optString("depot_fr").trim().isEmpty())throw new Engine.Problem(target.label()+": اسم المطعم الحالي غير متاح؛ لا استبدال دون عرضه.");result.conflicts.add(new Conflict(target,r));}}
   result.plan.add(new Engine.Meal(target.day,target.type,target.depot,existing));
  }return result;
 }
 static boolean same(List<Conflict> a,List<Conflict> b)throws Exception{if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++){Conflict x=a.get(i),y=b.get(i);if(!x.target.key().equals(y.target.key())||x.oldId!=y.oldId||x.target.depot.getLong("id")!=y.target.depot.getLong("id")||!x.oldArabic.equals(y.oldArabic)||!x.oldFrench.equals(y.oldFrench))return false;}return true;}
 static String describe(List<Conflict> conflicts){StringBuilder s=new StringBuilder();for(Conflict c:conflicts){if(s.length()>0)s.append("\n\n");s.append(c.describe());}return s.toString();}
 private static void current(Engine.Meal target,BooleanSupplier stop)throws Exception{if(stop.getAsBoolean())throw new Engine.Problem("توقف الاستبدال قبل الطلب التالي. راجع الحجوزات قبل المتابعة.");LocalDate today=Engine.today();if(!target.day.isAfter(today)||target.day.isAfter(today.plusDays(3)))throw new Engine.Problem("تغيّر اليوم؛ راجع الخطة. لم يُسمح بطلب خارج نافذة الحجز.");}
 static String apply(Gateway api,Engine.Journal store,JSONObject cfg,List<Conflict> approved,BooleanSupplier stop)throws Exception{
  if(approved.isEmpty())throw new Engine.Problem("لا توجد حجوزات لاستبدالها.");
  Review fresh=inspect(cfg,api.depots(),api.reservations(),Engine.today());if(!same(approved,fresh.conflicts))throw new Engine.Problem("تغيّرت الحجوزات أو المطاعم منذ المعاينة. راجع الخطة وأكّدها مجددًا.");
  JSONObject journal=store.load();for(Conflict c:approved)if(journal.has(c.target.key()))throw new Engine.Problem(c.target.label()+": توجد محاولة معلّقة؛ تحقّق من Webetu قبل إزالة القفل. لن نكرر الإلغاء أو الحجز.");
  int replaced=0;
  for(Conflict c:approved){
   current(c.target,stop);
   // Refresh target availability before sacrificing the current reservation.
   boolean available=false;for(Engine.Meal m:Engine.plan(cfg,api.depots(),new JSONArray(),Engine.today()))if(m.key().equals(c.target.key())&&m.depot.getLong("id")==c.target.depot.getLong("id"))available=true;
   if(!available)throw new Engine.Problem("المطعم الجديد غير متاح؛ لم نلغ الحجز الحالي.");
   JSONObject before=row(api.reservations(),c.target);if(before==null||!same(Collections.singletonList(c),Collections.singletonList(new Conflict(c.target,before))))throw new Engine.Problem("تغيّر الحجز الحالي؛ أعد المعاينة. لم نرسل الإلغاء.");
   current(c.target,stop);
   JSONObject entry=new JSONObject().put("state","replacement_cancel_pending").put("old_id",c.oldId).put("depot",c.target.depot.getLong("id"));journal.put(c.target.key(),entry);store.save(journal);
   current(c.target,stop);Exception cancelError=null;try{api.cancel(c.oldId);}catch(Exception e){cancelError=e;}
   JSONObject after;try{after=row(api.reservations(),c.target);}catch(Exception e){throw new Engine.Problem(c.target.label()+": نتيجة الإلغاء غير واضحة. حُفظ القفل ولم نرسل حجزًا جديدًا. افحص Webetu.");}
   if(after!=null){boolean targetConfirmed=false;try{targetConfirmed=Engine.matching(new JSONArray().put(after),c.target)>0;}catch(Engine.Problem mismatch){/* Keep the lock, never create beside a conflicting row. */}if(after.getLong("id")!=c.oldId&&targetConfirmed){journal.remove(c.target.key());store.save(journal);replaced++;continue;}throw new Engine.Problem(c.target.label()+": لم يُثبت زوال الحجز القديم؛ لم نرسل حجزًا جديدًا. افحص Webetu قبل إزالة القفل."+(cancelError instanceof ServiceApi.Failure?"\n"+cancelError.getMessage():""));}
   try{
    entry.put("state","replacement_create_pending");store.save(journal);
    current(c.target,stop);
    // A concurrent official-app booking must not lead to a duplicate POST.
    JSONArray latest=api.reservations();JSONObject concurrent=row(latest,c.target);if(concurrent!=null){if(Engine.matching(latest,c.target)>0){journal.remove(c.target.key());store.save(journal);replaced++;continue;}throw new Engine.Problem("ظهر حجز آخر بعد الإلغاء.");}
    Engine.plan(cfg,api.depots(),new JSONArray(),Engine.today()); // revalidate offerings after cancellation
    current(c.target,stop);try{api.reserve(c.target);}catch(Exception uncertain){/* read back even after a lost response; never replay */}
    if(Engine.matching(api.reservations(),c.target)<=0)throw new Engine.Problem("لم يظهر الحجز الجديد.");
    journal.remove(c.target.key());store.save(journal);replaced++;
   }catch(Exception e){throw new Engine.Problem(c.target.label()+": أُلغي الحجز القديم، لكن لم يتأكد إكمال البديل. قد تبقى الوجبة دون حجز. حُفظ قفل لمنع التكرار؛ افحص Webetu الآن.");}
  }
  try{JSONArray finalRows=api.reservations();for(Conflict c:approved)if(Engine.matching(finalRows,c.target)<=0)throw new Engine.Problem("تأكيد نهائي ناقص.");}catch(Exception e){throw new Engine.Problem("نُفّذت محاولات الاستبدال، لكن تعذّر التحقق النهائي. قد تكون بعض الحجوزات تغيّرت؛ راجع Webetu الآن قبل تفعيل الأتمتة.");}
  return "تم تأكيد استبدال "+replaced+" حجز. راجع الخطة ثم أكّد باقي الوجبات قبل تفعيل الأتمتة.";
 }
}
