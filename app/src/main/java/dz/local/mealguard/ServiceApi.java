package dz.local.mealguard;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Compatibility adapter for observed Webetu requests; not an official supported SDK.
 * The signature constant is shared app protocol material, never a user's credential.
 * Default system TLS validation, fixed HTTPS origins, no redirects and no write retries.
 */
public class ServiceApi implements Replacement.Gateway {
 static final String WEB="https://api-webetu.mesrs.dz/api", ONOU="https://gs-api.onou.dz/api";
 private static final String SIGNING="pUzHUW2WX54uCzhO8JC2eQ6g1Ol21upw";
 private String token, mealToken; private JSONObject context, credentials;
 private final Map<String,Boolean> useMealForRead=new HashMap<>();
 enum Stage {
  WEB_LOGIN("S01", "تسجيل الدخول إلى حساب الطالب"), YEAR("S02", "قراءة السنة الجامعية"), DIA("S03", "قراءة التسجيل الجامعي"),
  HOUSING("S04", "قراءة الإقامة"), REGION("S05", "قراءة ولاية الحجز"), MEAL_LOGIN("S06", "فتح جلسة الإطعام"),
  DEPOTS("S07", "قراءة المطاعم"), RESERVATIONS("S08", "قراءة الحجوزات"), RESERVE("S09", "إرسال الحجز"), CANCEL("S10", "إلغاء الحجز للاستبدال"), UNKNOWN("S00", "الاتصال");
  final String code,label; Stage(String c,String l){code=c;label=l;}
 }
 static class Failure extends Exception {
  final int http;final Stage stage;private final String detail;
  Failure(String s){this(s,0,null);} Failure(String s,int status,Stage st){super(s+(st==null?"":" ["+st.code+": "+st.label+"]"));detail=s;http=status;stage=st;}
  static Failure http(int code){return new Failure(code==401?"تعذّرت المصادقة (HTTP 401).":code==403?"رفضت الخدمة الوصول (HTTP 403).":"الخدمة أعادت HTTP "+code+". لا تأكيد دون قراءة الحجوزات.",code,null);}
  Failure at(Stage st){return stage==null?new Failure(detail,http,st):this;}
 }
 static Stage stageFor(String base,String path){
  if(path.equals("/authentication/v1/"))return Stage.WEB_LOGIN;
  if(path.equals("/infos/AnneeAcademiqueEncours"))return Stage.YEAR;
  if(path.endsWith("/dia"))return Stage.DIA;
  if(path.endsWith("/demandesHebregement"))return Stage.HOUSING;
  if(path.startsWith("/infos/wilayaInscription/"))return Stage.REGION;
  if(path.equals("/loginpwebetu"))return Stage.MEAL_LOGIN;
  if(path.equals("/getdepotres"))return Stage.DEPOTS;
  if(path.equals("/meal-reservations/student"))return Stage.RESERVATIONS;
  if(path.equals("/reservemeal"))return Stage.RESERVE;
  if(path.matches("/reservemeal/[1-9][0-9]*"))return Stage.CANCEL;
  return Stage.UNKNOWN;
 }
 static String normalizeToken(String value)throws Failure {
  String t=value.trim();while(t.regionMatches(true,0,"Bearer ",0,7))t=t.substring(7).trim();
  if(t.isEmpty()||t.equalsIgnoreCase("Bearer")||t.indexOf('\n')>=0||t.indexOf('\r')>=0)throw new Failure("رمز جلسة غير صالح؛ لا يُعرض أو يُسجّل.");return t;
 }
 private Object request(String base,String path,String method,JSONObject body,JSONObject query,String auth,boolean sign)throws Exception {
  try{return send(base,path,method,body,query,auth,sign);}catch(Failure f){throw f.at(stageFor(base,path));}
 }
 // Protected transport seam enables fake-response tests without making real requests.
 static String hmac(String key,String value)throws Exception{
  Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
  StringBuilder s=new StringBuilder();for(byte b:m.doFinal(value.getBytes(StandardCharsets.UTF_8)))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
 }
 protected Object send(String base,String path,String method,JSONObject body,JSONObject query,String auth,boolean sign)throws Exception
 {
  if(!(WEB.equals(base)||ONOU.equals(base))||!path.startsWith("/")||path.contains("?")||path.contains(".."))throw new Failure("مسار اتصال غير مسموح.");
  StringBuilder url=new StringBuilder(base).append(path);
  if(query!=null){url.append('?');Iterator<String> it=query.keys();while(it.hasNext()){String k=it.next();url.append(URLEncoder.encode(k,"UTF-8")).append('=').append(URLEncoder.encode(String.valueOf(query.get(k)),"UTF-8")).append('&');}}
  String raw=body==null?"":body.toString(); HttpsURLConnection c=null;
  try{
   c=(HttpsURLConnection)new URL(url.toString()).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestMethod(method);c.setUseCaches(false);
   c.setRequestProperty("User-Agent","MealGuard/0.14.0 Android");c.setRequestProperty("Accept","application/json");
   if(auth!=null)c.setRequestProperty("authorization",auth);
   if(sign){String ts=Long.toString(System.currentTimeMillis()/1000),nonce=UUID.randomUUID().toString();c.setRequestProperty("X-Timestamp",ts);c.setRequestProperty("X-Nonce",nonce);c.setRequestProperty("X-Signature",hmac(SIGNING,ts+"|"+nonce+"|"+raw));}
   if(body!=null){byte[] bytes=raw.getBytes(StandardCharsets.UTF_8);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setDoOutput(true);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
   int code=c.getResponseCode();if(code<200||code>=300)throw Failure.http(code);
   ByteArrayOutputStream buf=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(buf.size()+n>4*1024*1024)throw new Failure("حجم استجابة غير متوقع؛ توقف آمن.");buf.write(b,0,n);}}
   if(method.equals("DELETE"))return null; // 2xx alone is not proof: replacement always reads history.
   String text=buf.toString("UTF-8").trim();if(path.equals("/reservemeal")&&text.equals("Reserved"))return text;
   try{return new JSONTokener(text).nextValue();}catch(JSONException ex){throw new Failure("تغيّر تنسيق استجابة الخدمة؛ يلزم تحديث التطبيق.");}
  }catch(javax.net.ssl.SSLException e){throw new Failure("تعذّر التحقق من شهادة الاتصال. استخدم Webetu مؤقتًا.");}
  catch(IOException e){throw new Failure("تعذّر الاتصال بالخدمة أو انتهت المهلة.");}
  finally{if(c!=null)c.disconnect();}
 }
 private JSONObject obj(Object value)throws Exception{if(!(value instanceof JSONObject))throw new Failure("استجابة غير متوقعة؛ توقف آمن دون تخمين.");return (JSONObject)value;}
 private Object web(String path)throws Exception{return request(WEB,path,"GET",null,null,token,false);}
 private Object onou(String path,String method,JSONObject body,JSONObject query,String auth)throws Exception{return request(ONOU,path,method,body,query,auth,true);}
 private JSONObject ctx()throws Exception{return new JSONObject(context.toString());}
 public void login(JSONObject cfg)throws Exception {
  credentials=new JSONObject(cfg.toString());loginOnce();
 }
 private void loginOnce()throws Exception {
  JSONObject cfg=credentials;token=null;mealToken=null;context=null;useMealForRead.clear();
  JSONObject auth=obj(request(WEB,"/authentication/v1/","POST",new JSONObject().put("username",cfg.getString("student")).put("password",cfg.getString("password")),null,null,false));
  token=normalizeToken(auth.getString("token"));String uuid=auth.getString("uuid");if(token.isEmpty()||uuid.isEmpty())throw new Failure("لم تُرجع الخدمة جلسة صالحة.");
  String account=URLEncoder.encode(uuid,"UTF-8").replace("+","%20");long y=ProfileResolver.year(web("/infos/AnneeAcademiqueEncours"));
  long dia=ProfileResolver.dia(web("/infos/bac/"+account+"/anneeAcademique/"+y+"/dia"),y);
  long residence=ProfileResolver.residence(web("/infos/bac/"+account+"/demandesHebregement"),y);
  long wilaya=ProfileResolver.region(web("/infos/wilayaInscription/"+dia));
  context=new JSONObject().put("uuid",uuid).put("wilaya",wilaya).put("residence",residence).put("token",token);
  JSONObject meals=obj(onou("/loginpwebetu","POST",context,null,token));mealToken=normalizeToken(meals.getString("token"));if(mealToken.isEmpty())throw new Failure("لم تُرجع خدمة الإطعام جلسة صالحة.");
 }
 interface Read<T> { T get()throws Exception; }
 private <T> T readRecovering(Read<T> op)throws Exception {
  try{return op.get();}catch(Failure f){
   if(f.http!=401||credentials==null)throw f;
   // One fresh authenticated session per failed read operation; never for booking POSTs.
   loginOnce();return op.get();
  }
 }
 private Object readOnou(String path,JSONObject query)throws Exception {
  boolean preferMeal=Boolean.TRUE.equals(useMealForRead.get(path));
  String primary=preferMeal?mealToken:token,alternate=preferMeal?token:mealToken;
  try{return onou(path,"GET",null,query,"Bearer "+primary);}
  catch(Failure f){
   if(f.http!=401||alternate==null||alternate.equals(primary))throw f;
   // Both tokens were issued to this authenticated account. Alternate only for read-only GET.
   Object result=onou(path,"GET",null,query,"Bearer "+alternate);useMealForRead.put(path,!preferMeal);return result;
  }
 }
 public JSONArray depots()throws Exception {return readRecovering(()->obj(readOnou("/getdepotres",ctx())).getJSONArray("depots"));}
 public JSONArray reservations()throws Exception {return readRecovering(this::reservationsOnce);}
 private JSONArray reservationsOnce()throws Exception {
  JSONArray rows=new JSONArray();Set<Long> ids=new HashSet<>();
  for(int page=1;page<=100;page++){
   JSONObject q=ctx().put("page",page);JSONObject data=obj(readOnou("/meal-reservations/student",q));JSONArray chunk=data.getJSONArray("data");JSONObject meta=data.optJSONObject("meta");
   if(meta!=null&&meta.optInt("current_page",page)!=page)throw new Failure("ترقيم الحجوزات غير متوقع؛ توقف لمنع التكرار.");
   for(int i=0;i<chunk.length();i++){JSONObject row=chunk.getJSONObject(i);long id=row.optLong("id");if(id<=0||!ids.add(id))throw new Failure("قائمة حجوزات غير مكتملة أو مكررة؛ توقف آمن.");rows.put(row);}
   JSONObject links=data.optJSONObject("links");boolean next=links!=null&&!links.isNull("next")&&!links.optString("next").isEmpty();
   if(!next){if(meta!=null&&meta.has("total")&&meta.getInt("total")!=rows.length())throw new Failure("تغيّرت قائمة الحجوزات أثناء القراءة؛ أعد الفحص.");return rows;}
  }throw new Failure("تجاوز سجل الحجوزات الحد الآمن للقراءة.");
 }
 public void cancel(long reservationId)throws Exception {if(reservationId<=0)throw new Failure("رقم حجز غير صالح.");onou("/reservemeal/"+reservationId,"DELETE",ctx(),null,"Bearer "+mealToken);}
 public void reserve(Engine.Meal meal)throws Exception {
  JSONObject detail=new JSONObject().put("date_reserve",meal.day.toString()).put("menu_type",meal.type).put("idDepot",meal.depot.getLong("id"));
  onou("/reservemeal","POST",ctx().put("details",new JSONArray().put(detail.toString())),null,"Bearer "+mealToken);
 }
}
