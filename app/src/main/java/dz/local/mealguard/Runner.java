package dz.local.mealguard;
import android.content.Context;
import org.json.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

final class Runner {
 static final ReentrantLock LOCK=new ReentrantLock();
 static class Snapshot {JSONArray depots,rows;List<Engine.Meal> plan;List<Replacement.Conflict> conflicts=Collections.emptyList();}
 static Snapshot inspect(JSONObject cfg)throws Exception {
  ServiceApi api=new ServiceApi();api.login(cfg);Snapshot s=new Snapshot();s.depots=api.depots();s.rows=api.reservations();return s;
 }
 static Snapshot preview(JSONObject cfg)throws Exception {
  Snapshot s=inspect(cfg);if(AccountSettings.configured(cfg)){Replacement.Review r=Replacement.inspect(cfg,s.depots,s.rows,Engine.today());s.plan=r.plan;s.conflicts=r.conflicts;}return s;
 }
 static String replace(Context context,JSONObject approvedAccount,java.time.LocalDate approvedDay,List<Replacement.Conflict> approved,BooleanSupplier stop)throws Exception{
  if(!LOCK.tryLock())throw new Engine.Problem("هناك عملية قيد التنفيذ.");
  try{Vault v=new Vault(context);JSONObject cfg=v.account();if(!Engine.today().equals(approvedDay)||!AccountSettings.sameChoices(cfg,approvedAccount)||!cfg.optString("student").equals(approvedAccount.optString("student"))||!cfg.optString("password").equals(approvedAccount.optString("password")))throw new Engine.Problem("تغيّرت الإعدادات أو اليوم؛ أعد المعاينة.");
   Automation.disengage(context,v,true);ServiceApi api=new ServiceApi();api.login(cfg);
   Engine.Journal journal=new Engine.Journal(){public JSONObject load()throws Exception{return v.read(AccountSettings.journalKey(cfg));}public void save(JSONObject j)throws Exception{v.write(AccountSettings.journalKey(cfg),j);}};
   String result=Replacement.apply(api,journal,cfg,approved,stop);v.status(result);return result;
  }finally{LOCK.unlock();}
 }
 static String book(Context c,boolean automatic,BooleanSupplier cancelled)throws Exception {
  if(!LOCK.tryLock())throw new Engine.Problem("هناك فحص قيد التنفيذ.");
  try{
   Vault v=new Vault(c);if(automatic&&!v.enabled())return "الأتمتة متوقفة.";
   JSONObject cfg=v.account();ServiceApi api=new ServiceApi();api.login(cfg);
   List<Engine.Meal> plan=Engine.plan(cfg,api.depots(),api.reservations(),Engine.today());
   Engine.Journal journal=new Engine.Journal(){public JSONObject load()throws Exception{return v.read(AccountSettings.journalKey(cfg));}public void save(JSONObject j)throws Exception{v.write(AccountSettings.journalKey(cfg),j);}};
   String result=Engine.apply(api,journal,plan,()->cancelled.getAsBoolean()||(automatic&&!v.enabled()));v.success(result);return result;
  }finally{LOCK.unlock();}
 }
 static String safe(Exception e){
  if(e instanceof ServiceApi.Failure||e instanceof Engine.Problem)return e.getMessage();
  if(e instanceof java.security.GeneralSecurityException)return "تعذّر فتح التخزين المحمي. قد يلزم حذف البيانات وإعادة إعداد الحساب.";
  return "تعذّرت العملية ("+e.getClass().getSimpleName()+"). أعد الفحص أو استخدم Webetu.";
 }
}
