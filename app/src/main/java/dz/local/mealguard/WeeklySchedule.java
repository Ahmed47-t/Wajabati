package dz.local.mealguard;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
/** ISO weekdays: Monday=1 .. Sunday=7. Rules replace restaurants, never the booking window. */
final class WeeklySchedule {
 static final String KEY="weekly_overrides";
 static final String[] DAYS={"","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت","الأحد"};
 static JSONObject rules(JSONObject cfg)throws Exception{
  if(!cfg.has(KEY))return new JSONObject();
  if(!(cfg.opt(KEY) instanceof JSONObject))throw new Engine.Problem("جدول الاستثناءات غير صالح. راجع الإعدادات.");
  JSONObject all=cfg.getJSONObject(KEY);
  for(Iterator<String> ds=all.keys();ds.hasNext();){String d=ds.next();if(!d.matches("[1-7]")||!(all.opt(d) instanceof JSONObject))throw new Engine.Problem("يوم استثناء غير صالح.");JSONObject row=all.getJSONObject(d);
   for(Iterator<String> ms=row.keys();ms.hasNext();){String m=ms.next();if(!m.matches("[1-3]")||!(row.opt(m) instanceof JSONObject))throw new Engine.Problem("وجبة استثناء غير صالحة.");JSONObject jr=row.getJSONObject(m);if(Boolean.TRUE.equals(jr.opt("skip")))continue;Object id=jr.opt("id");if(!(id instanceof Number)||((Number)id).longValue()<=0||((Number)id).doubleValue()!=((Number)id).longValue())throw new Engine.Problem("مطعم استثناء غير صالح.");}
  }return all;
 }
 /** Effective restaurant id for a day+meal. -1 = skipped by weekly exception, 0 = not configured (skipped), otherwise the restaurant id. */
 static long restaurant(JSONObject cfg,int day,int meal)throws Exception{JSONObject row=rules(cfg).optJSONObject(Integer.toString(day));JSONObject rule=row==null?null:row.optJSONObject(Integer.toString(meal));if(rule!=null&&Boolean.TRUE.equals(rule.opt("skip")))return -1;return rule==null?cfg.optLong(AccountSettings.IDS[meal],0):rule.getLong("id");}
 static long restaurant(JSONObject cfg,LocalDate day,int meal)throws Exception{return restaurant(cfg,day.getDayOfWeek().getValue(),meal);}
 static JSONObject setDay(JSONObject cfg,int day,JSONObject[] selected,JSONArray offered)throws Exception{
  if(day<1||day>7)throw new Engine.Problem("يوم غير صالح.");
  JSONObject next=new JSONObject(cfg.toString()),all=new JSONObject(rules(cfg).toString()),row=new JSONObject();
  for(int meal=1;meal<=3;meal++)if(selected[meal]!=null){JSONObject s=selected[meal];if(Boolean.TRUE.equals(s.opt("skip"))){row.put(Integer.toString(meal),new JSONObject().put("skip",true));continue;}long id=s.getLong("id");JSONObject match=null;for(JSONObject d:AccountSettings.options(offered,meal))if(d.getLong("id")==id)match=d;if(match==null)throw new Engine.Problem("مطعم الاستثناء غير متاح لـ"+Engine.LABEL[meal]+". حدّث القائمة أو اختر المطعم المعتاد.");row.put(Integer.toString(meal),new JSONObject().put("id",id).put("name",Engine.name(match)));}
  if(row.length()==0)all.remove(Integer.toString(day));else all.put(Integer.toString(day),row);
  if(all.length()==0)next.remove(KEY);else next.put(KEY,all);return next;
 }
 static String daySummary(JSONObject cfg,int day){try{JSONObject row=rules(cfg).optJSONObject(Integer.toString(day));if(row==null||row.length()==0)return "المطاعم المعتادة";StringBuilder s=new StringBuilder();for(int meal=1;meal<=3;meal++){JSONObject rule=row.optJSONObject(Integer.toString(meal));if(rule!=null){if(s.length()>0)s.append(" · ");if(Boolean.TRUE.equals(rule.opt("skip")))s.append(Engine.LABEL[meal]).append(": غير مُختارة");else s.append(Engine.LABEL[meal]).append(": ").append(rule.optString("name","مطعم #"+rule.optLong("id")));}}return s.toString();}catch(Exception e){return "جدول غير صالح — راجع الإعدادات";}}
}
