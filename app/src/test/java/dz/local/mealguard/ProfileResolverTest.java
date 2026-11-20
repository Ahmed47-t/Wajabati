package dz.local.mealguard;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
public class ProfileResolverTest {
 @Test public void academicYearFormats()throws Exception{assertEquals(42,ProfileResolver.year(new JSONObject().put("id",42)));assertEquals(42,ProfileResolver.year(new JSONObject().put("idAnneeAcademique",42)));assertEquals(42,ProfileResolver.year("42"));}
 @Test public void regionVariantsUseActualIds()throws Exception{for(String k:new String[]{"id","idWilaya","wilayaId","refCodeWilaya","wilaya_id"})assertEquals(16,ProfileResolver.region(new JSONObject().put(k,16)));assertEquals(16,ProfileResolver.region(new JSONObject().put("wilaya",new JSONObject().put("id",16))));assertEquals(16,ProfileResolver.region(16));}
 @Test public void ambiguousRegionNeverGuessed()throws Exception{assertThrows(ServiceApi.Failure.class,()->ProfileResolver.region(new JSONArray().put(16).put(31)));assertThrows(ServiceApi.Failure.class,()->ProfileResolver.region(0));}
 @Test public void currentResidenceNotOtherAcademicYear()throws Exception{JSONArray a=new JSONArray().put(new JSONObject().put("idAnneeAcademique",41).put("idResidence",90)).put(new JSONObject().put("anneeAcademiqueId",42).put("idResidence",20));assertEquals(20,ProfileResolver.residence(a,42));}
 @Test public void duplicatedSameResidenceResolvedButDifferentOnesRefused()throws Exception{JSONObject a=new JSONObject().put("idAnneeAcademique",42).put("idResidance",10);assertEquals(10,ProfileResolver.residence(new JSONArray().put(a).put(a),42));JSONArray conflict=new JSONArray().put(a).put(new JSONObject().put("idAnneeAcademique",42).put("idResidance",20));assertThrows(ServiceApi.Failure.class,()->ProfileResolver.residence(conflict,42));}
 @Test public void noZeroOrMissingResidenceFallback()throws Exception{assertThrows(ServiceApi.Failure.class,()->ProfileResolver.residence(new JSONArray(),42));}
 @Test public void currentRegistrationObjectAndArray()throws Exception{assertEquals(7,ProfileResolver.dia(new JSONObject().put("id",7),42));assertEquals(7,ProfileResolver.dia(new JSONArray().put(new JSONObject().put("id",7).put("idAnneeAcademique",42)),42));}
 @Test public void ambiguousRegistrationRefused()throws Exception{JSONArray a=new JSONArray().put(new JSONObject().put("id",7).put("idAnneeAcademique",42)).put(new JSONObject().put("id",8).put("idAnneeAcademique",42));assertThrows(ServiceApi.Failure.class,()->ProfileResolver.dia(a,42));}
}
