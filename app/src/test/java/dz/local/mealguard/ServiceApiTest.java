package dz.local.mealguard;
import org.junit.*;
import static org.junit.Assert.*;
import org.json.*;
import java.util.*;

/** No real account, real endpoint, socket, or stored student credential is used. */
public class ServiceApiTest {
 static class Fake extends ServiceApi {
  int logins=0,readCalls=0,writes=0;
  boolean prefixed=false,rejectLogin=false,readAlways401=false,rejectWebRead=false,rejectReadOnce=false,forbidden=false,network=false,rejectWrite=false;
  final List<String> readAuth=new ArrayList<>();final List<JSONObject> readQueries=new ArrayList<>();
  @Override protected Object send(String base,String path,String method,JSONObject body,JSONObject query,String auth,boolean sign)throws Exception {
   if(path.equals("/authentication/v1/")){logins++;if(rejectLogin)throw Failure.http(401);return new JSONObject().put("token",(prefixed?"Bearer ":"")+"web"+logins).put("uuid","dummy-uuid");}
   if(path.equals("/infos/AnneeAcademiqueEncours"))return new JSONObject().put("id",42);
   if(path.endsWith("/dia"))return new JSONObject().put("id",7);
   if(path.endsWith("/demandesHebregement"))return new JSONArray().put(new JSONObject().put("idAnneeAcademique",42).put("idResidance",10));
   if(path.startsWith("/infos/wilayaInscription/"))return 16;
   if(path.equals("/loginpwebetu")){assertEquals("web"+logins,body.getString("token"));assertEquals("web"+logins,auth);return new JSONObject().put("token",(prefixed?"Bearer ":"")+"meal"+logins);}
   if(path.equals("/reservemeal")){writes++;if(rejectWrite)throw Failure.http(401);return "Reserved";}
   if(path.equals("/getdepotres")||path.equals("/meal-reservations/student")){
    readCalls++;readAuth.add(auth);readQueries.add(new JSONObject(query.toString()));
    if(forbidden)throw Failure.http(403);
    if(network)throw new Failure("تعذر الاتصال.");
    if(readAlways401||rejectWebRead&&auth.startsWith("Bearer web")||rejectReadOnce&&logins==1)throw Failure.http(401);
    assertFalse(auth.contains("Bearer Bearer"));
    if(path.equals("/getdepotres"))return new JSONObject().put("depots",new JSONArray());
    return new JSONObject().put("data",new JSONArray()).put("meta",new JSONObject().put("current_page",1).put("total",0)).put("links",new JSONObject().put("next",JSONObject.NULL));
   }throw new AssertionError("unexpected fake path");
  }
 }
 JSONObject cfg()throws Exception{return new JSONObject().put("student","not-real-account").put("password","not-real-password");}
 Engine.Meal meal()throws Exception{return new Engine.Meal(Engine.today().plusDays(1),1,new JSONObject().put("id",10),0);}
 @Test public void login401HasPreciseStageWithoutSensitiveValues()throws Exception{
  Fake f=new Fake();f.rejectLogin=true;ServiceApi.Failure e=assertThrows(ServiceApi.Failure.class,()->f.login(cfg()));assertEquals(ServiceApi.Stage.WEB_LOGIN,e.stage);assertEquals(401,e.http);assertTrue(e.getMessage().contains("S01"));assertFalse(e.getMessage().contains("not-real-account"));assertFalse(e.getMessage().contains("not-real-password"));assertEquals(1,f.logins);
 }
 @Test public void prefixedTokensAreNotDoublePrefixed()throws Exception{Fake f=new Fake();f.prefixed=true;f.login(cfg());f.depots();assertEquals("Bearer web1",f.readAuth.get(0));}
 @Test public void repeatedPrefixNormalized()throws Exception{assertEquals("token",ServiceApi.normalizeToken(" Bearer bearer token "));}
 @Test public void newlineTokenRejected(){assertThrows(ServiceApi.Failure.class,()->ServiceApi.normalizeToken("a\nb"));}
 @Test public void blankTokenRejected(){assertThrows(ServiceApi.Failure.class,()->ServiceApi.normalizeToken("Bearer "));}
 @Test public void readCanUseOwnMealSessionAfterWebSessionRejected()throws Exception{
  Fake f=new Fake();f.rejectWebRead=true;f.login(cfg());f.depots();assertEquals(Arrays.asList("Bearer web1","Bearer meal1"),f.readAuth);assertEquals(1,f.logins);assertEquals(0,f.writes);
 }
 @Test public void chosenReadSessionIsRememberedForSameEndpoint()throws Exception{
  Fake f=new Fake();f.rejectWebRead=true;f.login(cfg());f.depots();f.depots();assertEquals(Arrays.asList("Bearer web1","Bearer meal1","Bearer meal1"),f.readAuth);
 }
 @Test public void oneRefreshRecoversExpiredReadAndUpdatesQueryContext()throws Exception{
  Fake f=new Fake();f.rejectReadOnce=true;f.login(cfg());f.reservations();assertEquals(2,f.logins);assertEquals(3,f.readCalls);assertEquals("web2",f.readQueries.get(2).getString("token"));assertEquals(0,f.writes);
 }
 @Test public void repeatedRead401IsBoundedAndReportsReadStage()throws Exception{
  Fake f=new Fake();f.readAlways401=true;f.login(cfg());ServiceApi.Failure e=assertThrows(ServiceApi.Failure.class,f::reservations);assertEquals(2,f.logins);assertEquals(4,f.readCalls);assertEquals(ServiceApi.Stage.RESERVATIONS,e.stage);assertTrue(e.getMessage().contains("S08"));assertEquals(0,f.writes);
 }
 @Test public void read403NeverTriggersTokenFallbackOrRelogin()throws Exception{
  Fake f=new Fake();f.forbidden=true;f.login(cfg());ServiceApi.Failure e=assertThrows(ServiceApi.Failure.class,f::depots);assertEquals(403,e.http);assertEquals(1,f.logins);assertEquals(1,f.readCalls);assertEquals(ServiceApi.Stage.DEPOTS,e.stage);
 }
 @Test public void networkReadErrorDoesNotTriggerAuthLoop()throws Exception{
  Fake f=new Fake();f.network=true;f.login(cfg());assertThrows(ServiceApi.Failure.class,f::depots);assertEquals(1,f.logins);assertEquals(1,f.readCalls);
 }
 @Test public void reservation401NeverAutomaticallyResendsWriteOrRelogsIn()throws Exception{
  Fake f=new Fake();f.rejectWrite=true;f.login(cfg());ServiceApi.Failure e=assertThrows(ServiceApi.Failure.class,()->f.reserve(meal()));assertEquals(1,f.writes);assertEquals(1,f.logins);assertEquals(ServiceApi.Stage.RESERVE,e.stage);assertTrue(e.getMessage().contains("S09"));
 }
 @Test public void invalidLoginDuringReadRefreshStopsAtLoginStage()throws Exception{
  Fake f=new Fake();f.login(cfg());f.readAlways401=true;f.rejectLogin=true;ServiceApi.Failure e=assertThrows(ServiceApi.Failure.class,f::depots);assertEquals(ServiceApi.Stage.WEB_LOGIN,e.stage);assertEquals(2,f.logins);assertEquals(2,f.readCalls);
 }
 @Test public void diagnosticsNeverIncludeAccountPath(){ServiceApi.Failure e=ServiceApi.Failure.http(401).at(ServiceApi.stageFor(ServiceApi.WEB,"/infos/bac/SECRET_UUID/demandesHebregement"));assertEquals(ServiceApi.Stage.HOUSING,e.stage);assertFalse(e.getMessage().contains("SECRET_UUID"));assertFalse(e.getMessage().contains("https://"));}
 static class DeleteFake extends Fake {int deletes;String cancelAuth;JSONObject cancelBody;boolean rejectCancel;@Override protected Object send(String base,String path,String method,JSONObject body,JSONObject query,String auth,boolean sign)throws Exception{if(path.equals("/reservemeal/77")){deletes++;assertEquals("DELETE",method);assertEquals(ONOU,base);assertTrue(sign);cancelAuth=auth;cancelBody=new JSONObject(body.toString());if(rejectCancel)throw Failure.http(401);return null;}return super.send(base,path,method,body,query,auth,sign);}}
 @Test public void deletionUsesMealTokenContextAndSignedExactReservationEndpoint()throws Exception{DeleteFake f=new DeleteFake();f.login(cfg());f.cancel(77);assertEquals(1,f.deletes);assertEquals("Bearer meal1",f.cancelAuth);assertEquals("web1",f.cancelBody.getString("token"));assertEquals("dummy-uuid",f.cancelBody.getString("uuid"));assertEquals(0,f.writes);}
 @Test public void deletion401NeverRetriesRefreshesOrFallsBack()throws Exception{DeleteFake f=new DeleteFake();f.login(cfg());f.rejectCancel=true;ServiceApi.Failure e=assertThrows(ServiceApi.Failure.class,()->f.cancel(77));assertEquals(ServiceApi.Stage.CANCEL,e.stage);assertEquals(1,f.logins);assertEquals(1,f.deletes);assertTrue(e.getMessage().contains("S10"));}
 @Test public void invalidDeletionIdDoesNotSend()throws Exception{DeleteFake f=new DeleteFake();f.login(cfg());assertThrows(ServiceApi.Failure.class,()->f.cancel(0));assertEquals(0,f.deletes);}

}
