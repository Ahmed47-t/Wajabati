package dz.local.mealguard;
import org.json.*;
import java.util.*;
/** Resolve only real identifiers supplied by the authenticated service. Ambiguity fails closed. */
final class ProfileResolver {
 static long positive(Object v){try{long n=Long.parseLong(String.valueOf(v));return n>0?n:0;}catch(Exception ignored){return 0;}}
 static long field(JSONObject j,String...keys){for(String k:keys){long n=positive(j.opt(k));if(n>0)return n;}return 0;}
 static long year(Object data)throws Exception{long id=data instanceof JSONObject?field((JSONObject)data,"id","idAnneeAcademique","anneeAcademiqueId"):positive(data);if(id<=0)throw new ServiceApi.Failure("تعذّر تحديد السنة الجامعية.").at(ServiceApi.Stage.YEAR);return id;}
 static long region(Object data)throws Exception{
  long n=0;if(data instanceof JSONObject){JSONObject j=(JSONObject)data;n=field(j,"id","idWilaya","wilayaId","refCodeWilaya","id_wilaya","wilaya_id","refWilayaId");if(n==0)for(String k:new String[]{"wilaya","refWilaya"}){JSONObject sub=j.optJSONObject(k);if(sub!=null)n=field(sub,"id");if(n>0)break;}}
  else if(data instanceof JSONArray){JSONArray a=(JSONArray)data;if(a.length()==1)return region(a.get(0));}
  else n=positive(data);
  if(n<=0)throw new ServiceApi.Failure("تعذّر تحديد ولاية الحجز من بيانات الحساب.").at(ServiceApi.Stage.REGION);return n;
 }
 static long residence(Object data,long year)throws Exception{
  JSONArray a=data instanceof JSONArray?(JSONArray)data:data instanceof JSONObject?new JSONArray().put(data):new JSONArray();Set<Long> ids=new HashSet<>();
  for(int i=0;i<a.length();i++){
   JSONObject r=a.getJSONObject(i);long y=field(r,"idAnneeAcademique","anneeAcademiqueId");
   if(y==year){long id=field(r,"idResidance","idResidence","residenceId","id_residence","id_residance");if(id>0)ids.add(id);}
  }
  if(ids.size()!=1)throw new ServiceApi.Failure("لم تُحدد إقامة واحدة للسنة الحالية. راجع بيانات الإيواء في Webetu؛ لن نختَر معرّفًا افتراضيًا.").at(ServiceApi.Stage.HOUSING);return ids.iterator().next();
 }
 static long dia(Object data,long year)throws Exception{
  if(data instanceof JSONObject){JSONObject j=(JSONObject)data;long y=field(j,"idAnneeAcademique","anneeAcademiqueId");long id=field(j,"id");if(id>0&&(y==0||y==year))return id;}
  else if(data instanceof JSONArray){Set<Long> ids=new HashSet<>();JSONArray a=(JSONArray)data;for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);if(field(j,"idAnneeAcademique","anneeAcademiqueId")==year){long id=field(j,"id");if(id>0)ids.add(id);}}if(ids.size()==1)return ids.iterator().next();}
  throw new ServiceApi.Failure("تعذّر تحديد تسجيل واحد للسنة الحالية.").at(ServiceApi.Stage.DIA);
 }
}
