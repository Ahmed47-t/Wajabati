package dz.local.mealguard;
import org.json.*;
import java.util.*;
/** Account-derived restaurant choices. No university, residence or restaurant is hard-coded. */
final class AccountSettings {
 static final String[] IDS={"","breakfast_id","lunch_id","dinner_id"};
 static final String[] NAMES={"","breakfast_name","lunch_name","dinner_name"};
 static final String[] OFFERS={"","breakfast","lunch","dinner"};
 static JSONObject migrate(JSONObject source)throws Exception{
  JSONObject c=new JSONObject(source.toString());
  if(c.optLong("dorm_id")>0){
   if(!c.has("breakfast_id"))c.put("breakfast_id",c.getLong("dorm_id"));
   if(!c.has("dinner_id"))c.put("dinner_id",c.getLong("dorm_id"));
   if(!c.has("breakfast_name")&&c.has("dorm_name"))c.put("breakfast_name",c.getString("dorm_name"));
   if(!c.has("dinner_name")&&c.has("dorm_name"))c.put("dinner_name",c.getString("dorm_name"));
  }
  c.remove("dorm_id");c.remove("dorm_name");c.put("meal_schema",2);return c;
 }
 static String journalKey(JSONObject c)throws Exception {
  byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(c.getString("student").getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder s=new StringBuilder("journal_");for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
 }
 static boolean configured(JSONObject c){for(int m=1;m<=3;m++)if(c.optLong(IDS[m])<=0)return false;return true;}
 static List<JSONObject> options(JSONArray depots,int meal)throws Exception{
  List<JSONObject> out=new ArrayList<>();Set<Long> ids=new HashSet<>();
  for(int i=0;i<depots.length();i++){
   JSONObject d=depots.getJSONObject(i);long id=d.optLong("id");
   if(id<=0||!ids.add(id))throw new Engine.Problem("قائمة مطاعم غير صالحة. أعد الفحص.");
   if(Boolean.TRUE.equals(d.opt(OFFERS[meal])))out.add(d);
  }return out;
 }
 static JSONObject select(JSONObject source,JSONObject[] chosen)throws Exception{
  JSONObject c=new JSONObject(source.toString());
  for(int m=1;m<=3;m++){
   JSONObject d=chosen[m];if(d==null||d.optLong("id")<=0)throw new Engine.Problem("اختر مطعم "+Engine.LABEL[m]+".");
   if(!Boolean.TRUE.equals(d.opt(OFFERS[m])))throw new Engine.Problem("المطعم لا يعرض "+Engine.LABEL[m]+".");
   c.put(IDS[m],d.getLong("id")).put(NAMES[m],Engine.name(d));
  }c.put("meal_schema",2);return c;
 }
 static boolean sameChoices(JSONObject a,JSONObject b){for(int m=1;m<=3;m++)if(a.optLong(IDS[m])!=b.optLong(IDS[m]))return false;try{WeeklySchedule.rules(a);WeeklySchedule.rules(b);for(int day=1;day<=7;day++)for(int meal=1;meal<=3;meal++)if(WeeklySchedule.restaurant(a,day,meal)!=WeeklySchedule.restaurant(b,day,meal))return false;}catch(Exception e){return false;}return true;}
}
