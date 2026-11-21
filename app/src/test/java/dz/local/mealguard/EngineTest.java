package dz.local.mealguard;
import org.junit.*;
import static org.junit.Assert.*;
import org.json.*;
import java.time.*;
import java.util.*;

public class EngineTest {
 JSONObject cfg;JSONArray depots;
 static final String[] FR={"","Petit-déjeuner","Déjeuner","Dîner"};
 @Before public void setup()throws Exception{
  cfg=new JSONObject().put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10);
  depots=new JSONArray().put(new JSONObject().put("id",10).put("nameAR","الإقامة الجامعية 1000 سرير القليعة").put("nameFR","Dorm").put("breakfast",true).put("dinner",true).put("lunch",true)).put(new JSONObject().put("id",20).put("nameAR","المطعم المركزي 800 مقعد القليعة").put("nameFR","Main").put("breakfast",false).put("lunch",true).put("dinner",false));
 }
 JSONArray rows(List<Engine.Meal> plan)throws Exception{JSONArray a=new JSONArray();for(Engine.Meal m:plan)a.put(row(m));return a;}
 JSONObject row(Engine.Meal m)throws Exception{return new JSONObject().put("id",Long.parseLong(m.day.toString().replace("-",""))*10+m.type).put("date_reserve",m.day.toString()).put("mealtype_fr",FR[m.type]).put("depot_fr",m.depot.getString("nameFR"));}
 List<Engine.Meal> plan()throws Exception{return Engine.plan(cfg,depots,new JSONArray(),Engine.today());}
 static class Memory implements Engine.Journal{JSONObject j=new JSONObject();public JSONObject load()throws Exception{return new JSONObject(j.toString());}public void save(JSONObject value)throws Exception{j=new JSONObject(value.toString());}}
 class Fake implements Engine.Gateway {
  JSONArray data=new JSONArray();int writes=0,reads=0,failReadAt=-1;boolean accept=true,loseResponse=false;final Map<String,Integer> attempts=new HashMap<>();
  public JSONArray reservations()throws Exception{reads++;if(reads==failReadAt)throw new Exception("network");return data;}
  public void reserve(Engine.Meal m)throws Exception{writes++;attempts.merge(m.key(),1,Integer::sum);if(accept)data.put(row(m));if(loseResponse)throw new Exception("timeout");}
 }
 @Test public void nineMealsOnlyFutureThreeDays()throws Exception{List<Engine.Meal> p=plan();assertEquals(9,p.size());assertEquals(Engine.today().plusDays(1),p.get(0).day);assertEquals(Engine.today().plusDays(3),p.get(8).day);}
 @Test public void lunchAlwaysUses800IncludingFridaySaturday()throws Exception{for(int weekday=0;weekday<7;weekday++){List<Engine.Meal> p=Engine.plan(cfg,depots,new JSONArray(),LocalDate.of(2026,11,1).plusDays(weekday));for(Engine.Meal m:p)assertEquals(m.type==2?20:10,m.depot.getLong("id"));}}
 @Test public void planDoesNotWrite()throws Exception{Fake f=new Fake();Engine.plan(cfg,depots,f.reservations(),Engine.today());assertEquals(0,f.writes);}
 @Test public void noMissingRestaurantGuessing()throws Exception{cfg.put("lunch_id",999);assertThrows(Engine.Problem.class,()->plan());}
 @Test public void unavailableMealStopsPlan()throws Exception{depots.getJSONObject(1).put("lunch",false);assertThrows(Engine.Problem.class,()->plan());}
 @Test public void stringTrueNotTreatedAsExplicitOffer()throws Exception{depots.getJSONObject(1).put("lunch","true");assertThrows(Engine.Problem.class,()->plan());}
 @Test public void duplicatesInRestaurantListStopPlan()throws Exception{depots.put(depots.getJSONObject(0));assertThrows(Engine.Problem.class,()->plan());}
 @Test public void existingReservationsRecognized()throws Exception{List<Engine.Meal> p=plan();List<Engine.Meal> existing=Engine.plan(cfg,depots,rows(p),Engine.today());for(Engine.Meal m:existing)assertTrue(m.existingId>0);}
 @Test public void differentRestaurantBlocksEntirePlan()throws Exception{JSONObject r=row(plan().get(8));r.put("depot_fr","Other");assertThrows(Engine.Problem.class,()->Engine.plan(cfg,depots,new JSONArray().put(r),Engine.today()));}
 @Test public void unknownMealNeverGuessed()throws Exception{JSONObject r=row(plan().get(0));r.put("mealtype_fr","???");assertThrows(Engine.Problem.class,()->Engine.plan(cfg,depots,new JSONArray().put(r),Engine.today()));}
 @Test public void duplicateReservationsBlockPlan()throws Exception{JSONObject r=row(plan().get(0));assertThrows(Engine.Problem.class,()->Engine.plan(cfg,depots,new JSONArray().put(r).put(r),Engine.today()));}
 @Test public void arabicLabelsRecognized()throws Exception{for(int n=1;n<=3;n++){JSONObject r=new JSONObject().put("mealtype_ar",Engine.LABEL[n]);assertEquals(n,Engine.mealType(r));}}
 @Test public void allConfirmedNeedsNoWrites()throws Exception{Fake f=new Fake();List<Engine.Meal> p=plan();f.data=rows(p);List<Engine.Meal> q=Engine.plan(cfg,depots,f.data,Engine.today());Engine.apply(f,new Memory(),q,()->false);assertEquals(0,f.writes);}
 @Test public void allMissingConfirmedAfterNineWrites()throws Exception{Fake f=new Fake();Memory s=new Memory();String result=Engine.apply(f,s,plan(),()->false);assertEquals(9,f.writes);assertEquals(0,s.j.length());assertTrue(result.contains("9"));}
 @Test public void phoneBookingAfterPreviewDoesNotDuplicate()throws Exception{Fake f=new Fake();List<Engine.Meal> p=plan();f.data=rows(p);Engine.apply(f,new Memory(),p,()->false);assertEquals(0,f.writes);}
 @Test public void lostResponseButVisibleReservationIsSuccess()throws Exception{Fake f=new Fake();f.loseResponse=true;Engine.apply(f,new Memory(),plan(),()->false);assertEquals(9,f.writes);}
 @Test public void uncertainWriteIsNeverAutomaticallyResent()throws Exception{
  Fake f=new Fake();f.accept=false;f.loseResponse=true;Memory s=new Memory();List<Engine.Meal> p=plan();
  assertThrows(Engine.Problem.class,()->Engine.apply(f,s,p,()->false));
  assertEquals(1,f.writes);assertEquals(1,f.attempts.get(p.get(0).key()).intValue());assertTrue(s.j.has(p.get(0).key()));
  f.accept=true;f.loseResponse=false;
  assertThrows(Engine.Problem.class,()->Engine.apply(f,s,p,()->false));
  assertEquals(9,f.writes);assertEquals(1,f.attempts.get(p.get(0).key()).intValue());assertTrue(s.j.has(p.get(0).key()));
 }
 @Test public void pendingMealBlocksOnlyItsOwnWrite()throws Exception{
  Fake f=new Fake();Memory s=new Memory();List<Engine.Meal> p=plan();
  s.j.put(p.get(4).key(),new JSONObject().put("state","pending"));
  Engine.Problem e=assertThrows(Engine.Problem.class,()->Engine.apply(f,s,p,()->false));
  assertEquals(8,f.writes);assertTrue(s.j.has(p.get(4).key()));
  assertTrue(e.getMessage().contains("المحاولات المعلّقة"));assertTrue(e.getMessage().contains(p.get(4).label()));
 }
 @Test public void visiblePendingIsReconciled()throws Exception{Fake f=new Fake();Memory s=new Memory();List<Engine.Meal> p=plan();f.data=rows(p);s.j.put(p.get(0).key(),new JSONObject().put("state","pending"));List<Engine.Meal> q=Engine.plan(cfg,depots,f.data,Engine.today());Engine.apply(f,s,q,()->false);assertEquals(0,f.writes);assertEquals(0,s.j.length());}
 @Test public void failedVerificationKeepsPending()throws Exception{Fake f=new Fake();f.failReadAt=2;Memory s=new Memory();List<Engine.Meal> p=plan();assertThrows(Engine.Problem.class,()->Engine.apply(f,s,p,()->false));assertEquals(1,f.writes);assertTrue(s.j.has(p.get(0).key()));}
 @Test public void disappearedExistingReservationIsNotRebooked()throws Exception{Fake f=new Fake();List<Engine.Meal> p=Engine.plan(cfg,depots,rows(plan()),Engine.today());assertThrows(Engine.Problem.class,()->Engine.apply(f,new Memory(),p,()->false));assertEquals(0,f.writes);}
 @Test public void cancellationStopsBeforeWrite()throws Exception{Fake f=new Fake();assertThrows(Engine.Problem.class,()->Engine.apply(f,new Memory(),plan(),()->true));assertEquals(0,f.writes);}
 @Test public void storageFailureStopsBeforeWrite()throws Exception{Fake f=new Fake();Engine.Journal s=new Engine.Journal(){public JSONObject load(){return new JSONObject();}public void save(JSONObject j)throws Exception{throw new Exception("disk full");}};assertThrows(Exception.class,()->Engine.apply(f,s,plan(),()->false));assertEquals(0,f.writes);}
 @Test public void outsideWindowNeverSubmitted()throws Exception{Fake f=new Fake();List<Engine.Meal> p=Collections.singletonList(new Engine.Meal(Engine.today().plusDays(4),1,depots.getJSONObject(0),0));assertThrows(Engine.Problem.class,()->Engine.apply(f,new Memory(),p,()->false));assertEquals(0,f.writes);}
 @Test public void todayNeverSubmitted()throws Exception{Fake f=new Fake();List<Engine.Meal> p=Collections.singletonList(new Engine.Meal(Engine.today(),1,depots.getJSONObject(0),0));assertThrows(Engine.Problem.class,()->Engine.apply(f,new Memory(),p,()->false));assertEquals(0,f.writes);}
 @Test public void hmacIsDeterministic()throws Exception{assertEquals("d3ea7424a34a29d68f133c5c7cbec755439c298c59b8564ec0a1d9aad261dbac",ServiceApi.hmac("test-key","1000|nonce|{}"));assertEquals(64,ServiceApi.hmac("test-key","1000|nonce|{}").length());}
 @Test public void cancelledReservationIsNotReportedConfirmed()throws Exception{JSONObject r=row(plan().get(0));r.put("status","cancelled");assertThrows(Engine.Problem.class,()->Engine.plan(cfg,depots,new JSONArray().put(r),Engine.today()));}
 @Test public void unknownStatusFailsClosed()throws Exception{JSONObject r=row(plan().get(0));r.put("status","new-state");assertThrows(Engine.Problem.class,()->Engine.plan(cfg,depots,new JSONArray().put(r),Engine.today()));}
 @Test public void explicitConfirmedStatusAccepted()throws Exception{JSONObject r=row(plan().get(0));r.put("status","CONFIRMED");assertTrue(Engine.plan(cfg,depots,new JSONArray().put(r),Engine.today()).get(0).existingId>0);}

 @Test public void fullyConfirmedPlanUsesOneFinalReadInsteadOfNineRepeatedReads()throws Exception{
  Fake f=new Fake();List<Engine.Meal> p=plan();f.data=rows(p);List<Engine.Meal> q=Engine.plan(cfg,depots,f.data,Engine.today());Engine.apply(f,new Memory(),q,()->false);assertEquals(1,f.reads);assertEquals(0,f.writes);
 }
 @Test public void mixedPlanChecksMissingExistingBeforeAnyNewWrite()throws Exception{
  Fake f=new Fake();List<Engine.Meal> p=plan();JSONArray initial=new JSONArray().put(row(p.get(8)));List<Engine.Meal> q=Engine.plan(cfg,depots,initial,Engine.today());assertThrows(Engine.Problem.class,()->Engine.apply(f,new Memory(),q,()->false));assertEquals(0,f.writes);
 }
 @Test public void mixedPlanBooksOnlyMissingAndRetainsFinalVerification()throws Exception{
  Fake f=new Fake();List<Engine.Meal> p=plan();f.data=new JSONArray().put(row(p.get(0)));List<Engine.Meal> q=Engine.plan(cfg,depots,f.data,Engine.today());Memory m=new Memory();Engine.apply(f,m,q,()->false);assertEquals(8,f.writes);assertEquals(9,f.data.length());assertEquals(0,m.j.length());assertEquals(18,f.reads);
 }

 @Test public void threeIndependentRestaurantsAcrossAllWeekdays()throws Exception{
  depots.put(new JSONObject().put("id",30).put("nameAR","مطعم ثالث للاختبار").put("nameFR","Third").put("breakfast",false).put("lunch",false).put("dinner",true));cfg.put("dinner_id",30);
  for(int day=0;day<7;day++){List<Engine.Meal> p=Engine.plan(cfg,depots,new JSONArray(),Engine.today().plusDays(day));for(Engine.Meal m:p)assertEquals(m.type==1?10:m.type==2?20:30,m.depot.getLong("id"));}
 }
 @Test public void legacyPartialSelectionBooksOnlyExplicitMeals()throws Exception{
  JSONObject old=new JSONObject().put("dorm_id",10).put("lunch_id",20);List<Engine.Meal> p=Engine.plan(old,depots,new JSONArray(),Engine.today());assertEquals(3,p.size());for(Engine.Meal m:p)assertEquals(2,m.type);
 }
 @Test public void optionalMealsPlanOnlyConfiguredOnes()throws Exception{
  cfg.remove("breakfast_id");cfg.remove("dinner_id");List<Engine.Meal> p=plan();assertEquals(3,p.size());for(Engine.Meal m:p)assertEquals(2,m.type);
 }
 @Test public void weeklySkipOmitsOnlyThatDay()throws Exception{
  LocalDate day=Engine.today().plusDays(1);
  JSONObject slot=new JSONObject().put("skip",true);
  JSONObject withSkip=WeeklySchedule.setDay(cfg,day.getDayOfWeek().getValue(),new JSONObject[]{null,null,slot,null},depots);
  List<Engine.Meal> p=Engine.plan(withSkip,depots,new JSONArray(),Engine.today());
  assertEquals(8,p.size());for(Engine.Meal m:p)assertFalse(m.type==2&&m.day.equals(day));
  assertEquals(10,WeeklySchedule.restaurant(withSkip,day.getDayOfWeek().getValue(),1));// breakfast stays configured
  assertEquals(-1,WeeklySchedule.restaurant(withSkip,day.getDayOfWeek().getValue(),2));// lunch skipped
 }
 @Test public void weeklyExceptionCanEnableUnconfiguredMealForThatDayOnly()throws Exception{
  cfg.remove("dinner_id");
  JSONObject depot=new JSONObject().put("id",30).put("nameFR","Third").put("dinner",true);depots.put(depot);
  LocalDate day=Engine.today().plusDays(1);
  JSONObject override=new JSONObject().put("id",30).put("name","Third");
  JSONObject withRule=WeeklySchedule.setDay(cfg,day.getDayOfWeek().getValue(),new JSONObject[]{null,null,null,override},depots);
  List<Engine.Meal> p=Engine.plan(withRule,depots,new JSONArray(),Engine.today());
  int dinners=0;for(Engine.Meal m:p)if(m.type==3){dinners++;assertEquals(day,m.day);assertEquals(30,m.depot.getLong("id"));}
  assertEquals(1,dinners);
  List<Engine.Meal> base=Engine.plan(cfg,depots,new JSONArray(),Engine.today());for(Engine.Meal m:base)assertNotEquals(3,m.type);
 }
 @Test public void sameChoicesDetectsSkipDifference()throws Exception{
  LocalDate day=Engine.today().plusDays(1);
  JSONObject skipRule=WeeklySchedule.setDay(cfg,day.getDayOfWeek().getValue(),new JSONObject[]{null,null,new JSONObject().put("skip",true),null},depots);
  assertFalse(AccountSettings.sameChoices(cfg,skipRule));
  assertTrue(AccountSettings.sameChoices(cfg,new JSONObject(cfg.toString())));
 }
 @Test public void unconfiguredSetupIsRejectedByPlan()throws Exception{
  assertThrows(Engine.Problem.class,()->Engine.plan(new JSONObject().put("student","x"),depots,new JSONArray(),Engine.today()));
 }

 @Test public void outOfWindowLocksAreExpired()throws Exception{
  Fake f=new Fake();Memory s=new Memory();List<Engine.Meal> p=plan();
  s.j.put("2020-01-01:2",new JSONObject().put("state","pending"));
  s.j.put(p.get(0).key(),new JSONObject().put("state","pending"));
  Engine.Problem e=assertThrows(Engine.Problem.class,()->Engine.apply(f,s,p,()->false));
  assertFalse(s.j.has("2020-01-01:2"));assertTrue(s.j.has(p.get(0).key()));assertEquals(8,f.writes);
  assertTrue(e.getMessage().contains("المحاولات المعلّقة"));
 }
 @Test public void displayNameStripsOverlayAndTatweel()throws Exception{
  assertEquals("الإقامة الجامعية",Engine.name(new JSONObject().put("nameAR","الإ\u0336قـامـة الجا\u0336معية")));
  assertEquals("الإقامة 1000",Engine.name(new JSONObject().put("nameAR","الإقـامـة 1000")));
  assertEquals("Cafe X",Engine.name(new JSONObject().put("nameFR","Cafe X")));
  assertEquals("مطعم دون اسم",Engine.name(new JSONObject()));
 }
 @Test public void failedPendingCarriesReasonAndTime()throws Exception{
  Fake f=new Fake();f.failReadAt=2;Memory s=new Memory();List<Engine.Meal> p=plan();
  assertThrows(Engine.Problem.class,()->Engine.apply(f,s,p,()->false));
  JSONObject e=s.j.getJSONObject(p.get(0).key());
  assertTrue(e.optLong("at")>0);assertFalse(e.optString("reason").isEmpty());
 }
}
