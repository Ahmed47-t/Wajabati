package dz.local.mealguard;

import org.json.*;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Conservative reservation state machine. Preview is read-only. No cancellation endpoints. */
public final class Engine {
 public static final ZoneId ZONE=ZoneId.of("Africa/Algiers");
 public static final String[] LABEL={"","فطور الصباح","الغداء","العشاء"};
 public interface Gateway { JSONArray reservations()throws Exception; void reserve(Meal m)throws Exception; }
 public interface Journal { JSONObject load()throws Exception; void save(JSONObject j)throws Exception; }
 public static class Problem extends Exception {public Problem(String s){super(s);}}
 public static final class Meal {
  public final LocalDate day;public final int type;public final JSONObject depot;public final long existingId;
  public Meal(LocalDate d,int t,JSONObject p,long id){day=d;type=t;depot=p;existingId=id;}
  public String key(){return day+":"+type;}
  public String label(){return day+" • "+LABEL[type];}
 }
 public static LocalDate today(){return LocalDate.now(ZONE);}
 public static String name(JSONObject d){String s=clean(d.optString("nameAR"));if(!s.isEmpty())return s;s=clean(d.optString("nameFR"));return s.isEmpty()?"مطعم دون اسم":s;}
 static String norm(String s){return Normalizer.normalize(s,Normalizer.Form.NFKD).replaceAll("\\p{M}","").replace('ـ',' ').toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
 static String clean(String s){return s==null?"":s.replaceAll("\\p{M}","").replace("ـ","").trim().replaceAll("\\s+"," ");}
 static int mealType(JSONObject r)throws Problem {
  for(String k:new String[]{"mealtype_fr","mealtype_ar"}){
   String s=norm(r.optString(k));
   if(Arrays.asList("petit-dejeuner","petit dejeuner","breakfast","فطور","فطور الصباح","الافطار").contains(s))return 1;
   if(Arrays.asList("dejeuner","lunch","غداء","الغداء").contains(s))return 2;
   if(Arrays.asList("diner","dinner","عشاء","العشاء").contains(s))return 3;
  }throw new Problem("نوع وجبة غير معروف في الحجوزات؛ توقف لمنع حجز مكرر.");
 }
 static long matching(JSONArray rows,Meal m)throws Exception {
  JSONObject found=null;
  for(int i=0;i<rows.length();i++){
   JSONObject r=rows.getJSONObject(i);if(!m.day.toString().equals(r.optString("date_reserve")))continue;
   if(mealType(r)!=m.type)continue;if(found!=null)throw new Problem("يوجد أكثر من حجز لنفس الوجبة. راجع Webetu الرسمي.");found=r;
  }
  if(found==null)return 0;
  Set<String> names=new HashSet<>();for(String k:new String[]{"nameAR","nameFR","nameEN"}){String n=norm(m.depot.optString(k));if(!n.isEmpty())names.add(n);}
  boolean same=false;for(String k:new String[]{"depot_ar","depot_fr"}){String n=norm(found.optString(k));if(!n.isEmpty()&&names.contains(n))same=true;}
  if(!same)throw new Problem(m.label()+": حجز موجود في مطعم مختلف أو اسم غير مطابق. لن نلغيه أو نستبدله.");
  if(found.has("status")&&!found.isNull("status")){
   String state=norm(String.valueOf(found.get("status")));
   if(!Arrays.asList("confirmed","reserved","active","true","1").contains(state))throw new Problem(m.label()+": حالة الحجز غير مؤكدة أو غير معروفة. راجع Webetu الرسمي.");
  }
  long id=found.optLong("id");if(id<=0)throw new Problem("حجز دون رقم تأكيد؛ توقف آمن.");return id;
 }
 public static List<Meal> plan(JSONObject cfg,JSONArray depots,JSONArray rows,LocalDate today)throws Exception {
  Map<Long,JSONObject> offered=new HashMap<>();for(int i=0;i<depots.length();i++){JSONObject d=depots.getJSONObject(i);long id=d.getLong("id");if(offered.put(id,d)!=null)throw new Problem("معرّف مطعم مكرر؛ توقف آمن.");}
  if(!AccountSettings.configured(cfg))throw new Problem("اختر مطعمًا لكل وجبة أولًا.");
  WeeklySchedule.rules(cfg);
  List<Meal> plan=new ArrayList<>();
  for(int ahead=1;ahead<=3;ahead++)for(int type=1;type<=3;type++){
   JSONObject d=offered.get(WeeklySchedule.restaurant(cfg,today.plusDays(ahead),type));if(d==null)throw new Problem(today.plusDays(ahead)+" • "+LABEL[type]+": المطعم المختار أو مطعم الاستثناء غير متاح لحسابك الآن. لن نستخدم بديلًا تلقائيًا.");
   String field=type==1?"breakfast":type==2?"lunch":"dinner";
   if(!Boolean.TRUE.equals(d.opt(field)))throw new Problem("المطعم المختار لا يعرض "+LABEL[type]+". لن نغيّر المطعم تلقائيًا.");
   Meal m=new Meal(today.plusDays(ahead),type,d,0);plan.add(new Meal(m.day,type,d,matching(rows,m)));
  }return plan;
 }
 static void expireStaleLocks(Journal store)throws Exception{
  JSONObject j=store.load();LocalDate floor=today().plusDays(1);boolean changed=false;
  for(Iterator<String> it=j.keys();it.hasNext();){String k=it.next();int cut=k.indexOf(':');if(cut<=0)continue;
   try{if(LocalDate.parse(k.substring(0,cut)).isBefore(floor)){it.remove();changed=true;}}catch(Exception ignored){}}
  if(changed)store.save(j);
 }
 public static String apply(Gateway api,Journal store,List<Meal> plan,BooleanSupplier cancelled)throws Exception {
  expireStaleLocks(store);
  JSONObject j=store.load();
  // ب1: a pending lock covers only its own meal; a confirmed row drops its stale lock.
  List<Meal> actionable=new ArrayList<>(),locked=new ArrayList<>();
  for(Meal m:plan){
   boolean hasLock=j.has(m.key());
   if(m.existingId>0){if(hasLock){j.remove(m.key());store.save(j);}continue;}
   if(hasLock)locked.add(m);else actionable.add(m);
  }
  boolean hasExisting=false,hasMissing=false;
  for(Meal m:plan)if(m.existingId>0)hasExisting=true;
  for(Meal m:actionable)hasMissing=true;
  if(hasExisting&&hasMissing){
   if(cancelled.getAsBoolean())throw new Problem("توقف التنفيذ قبل إرسال طلب جديد.");
   JSONArray fresh=api.reservations();
   // Validate the entire mixed plan before its first write, including existing reservations.
   for(Meal m:plan){long id=matching(fresh,m);if(m.existingId>0&&id<=0)throw new Problem(m.label()+": اختفى حجز كان مؤكدًا. توقف آمن دون إعادة حجز تلقائية.");}
  }
  int added=0;
  for(Meal m:actionable){
   if(cancelled.getAsBoolean())throw new Problem("توقف التنفيذ قبل إرسال طلب جديد.");
   LocalDate now=today();if(!m.day.isAfter(now)||m.day.isAfter(now.plusDays(3)))throw new Problem("تغيّر اليوم أثناء الفحص؛ اعرض خطة حديثة قبل المتابعة.");
   // Confirmed rows need only the final full read, not nine identical history downloads.
   long id=matching(api.reservations(),m);
   if(id>0){j.remove(m.key());store.save(j);continue;}
   j.put(m.key(),new JSONObject().put("state","pending").put("depot",m.depot.getLong("id")));store.save(j);
   Exception writeFailure=null;try{api.reserve(m);}catch(Exception e){writeFailure=e;/* A lost response is not proof of failure. Reconcile below. */}
   long verified;
   try{verified=matching(api.reservations(),m);}catch(Exception e){
    j.put(m.key(),new JSONObject().put("state","pending").put("depot",m.depot.getLong("id")).put("at",System.currentTimeMillis()).put("reason","تعذّر تأكيد النتيجة"));store.save(j);
    throw new Problem(m.label()+": تعذّر تأكيد النتيجة. حُفظت المحاولة كمعلّقة ولن تُرسل مرة أخرى تلقائيًا."+(e instanceof ServiceApi.Failure?"\n"+e.getMessage():""));}
   if(verified<=0){
    j.put(m.key(),new JSONObject().put("state","pending").put("depot",m.depot.getLong("id")).put("at",System.currentTimeMillis()).put("reason",writeFailure==null?"لم يظهر الحجز بعد الإرسال":"الاستجابة غير واضحة ولم يظهر الحجز"));store.save(j);
    throw new Problem(m.label()+": لم يظهر حجز مؤكّد. أوقفنا الإرسال لتجنّب التكرار؛ افحص التطبيق الرسمي."+(writeFailure instanceof ServiceApi.Failure?"\n"+writeFailure.getMessage():""));}
   j.remove(m.key());store.save(j);added++;
  }
  // A final complete read is required before reporting full coverage. Locked meals are skipped: they are expected to be absent.
  Set<String> held=new HashSet<>();for(Meal m:locked)held.add(m.key());
  JSONArray finalRows=api.reservations();
  for(Meal m:plan){if(held.contains(m.key()))continue;if(matching(finalRows,m)<=0)throw new Problem("لم يكتمل التحقق النهائي لكل الوجبات. راجع قائمة الحجوزات.");}
  if(!locked.isEmpty()){
   StringBuilder s=new StringBuilder();
   for(Meal m:locked){JSONObject e=j.optJSONObject(m.key());s.append("\n").append(m.label());if(e!=null&&e.optLong("at")>0)s.append(" • ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(e.optLong("at"))));if(e!=null&&!e.optString("reason").isEmpty())s.append(": ").append(e.optString("reason"));}
   throw new Problem("تم حجز الوجبات غير المقفلة ("+added+" حجوزات جديدة)، لكن "+locked.size()+" وجبة لديها محاولة سابقة غير مؤكدة ولن تُرسل تلقائيًا:"+s+"\nالحل: الإعدادات ← الأدوات ← «المحاولات المعلّقة» بعد التحقق من Webetu.");
  }
  return "تم تأكيد "+plan.size()+" وجبات. حجوزات جديدة: "+added+".";
 }
}
