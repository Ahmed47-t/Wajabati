package dz.local.mealguard;
import android.app.AlertDialog;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/**
 * 0.14.0 — the owner asked for the last step to be the battery setting, in place of the long text that used to
 * fill the review step. These tests pin the new shape: battery lives inside the review step, activation unlocks
 * only after the exemption, and nothing else in the review step became a dead end.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class BatteryStepTest {
 private Vault vault;
 @Before public void init(){vault=new Vault(RuntimeEnvironment.getApplication());vault.clear();}
 @After public void clean(){vault.clear();}
 private View root(MainActivity a){return a.getWindow().getDecorView();}
 private void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
 private boolean visible(View v,String text){if(v.getVisibility()!=View.VISIBLE)return false;if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(visible(((ViewGroup)v).getChildAt(i),text))return true;return false;}
 private Object get(MainActivity a,String n)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(a);}
 private void set(MainActivity a,String n,Object v)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);f.set(a,v);}
 private Object call(MainActivity a,String n,Class<?>[] t,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(a,args);}
 private void batteryFree(MainActivity a,boolean free){Shadows.shadowOf(a.getSystemService(android.os.PowerManager.class)).setIgnoringBatteryOptimizations(a.getPackageName(),free);}
 private JSONObject account()throws Exception{return new JSONObject().put("student","20230001").put("password","secret").put("meal_schema",2).put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10).put("breakfast_name","مطعم الصباح").put("lunch_name","مطعم الغداء").put("dinner_name","مطعم الصباح");}
 /** Verified account, chosen restaurants, and a same-day confirmed booking: everything but the battery. */
 private void ready(MainActivity a)throws Exception{vault.accountVerified(true);vault.success("تم تأكيد 9 وجبات. حجوزات جديدة: 9.");set(a,"cfg",account());call(a,"update",new Class<?>[0]);}
 private JSONArray plan()throws Exception{JSONArray a=new JSONArray();java.time.LocalDate d=Engine.today();for(int i=1;i<=9;i++)a.put(new JSONObject().put("day",d.plusDays((i-1)/3+1).toString()).put("meal",(i-1)%3+1).put("name",i%3==2?"مطعم الغداء":"مطعم الصباح").put("state",1));return a;}

 /** The screenshot this release answers: the review step used to hold the plan and a long explanation. */
 @Test public void lastStepShowsTheBatterySettingInsideTheReviewInsteadOfLongText(){
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   call(a,"setStep",new Class<?>[]{int.class},2);
   assertTrue(visible(root(a),"خطة الأيام الثلاثة"));
   assertTrue("the battery step is offered in the review step",visible(root(a),"استثناء التطبيق من توفير البطارية"));
   assertFalse("no system prompt without a tap",tooManyIntents(a));
   View row=find(root(a),"استثناء التطبيق من توفير البطارية");
   assertNotNull(row.getContentDescription());
   assertTrue(String.valueOf(row.getContentDescription()).contains("اضغط للسماح"));
  }
 }

 @Test public void batteryRowOpensTheSystemPromptAndThenReturnsToTheSamePlan(){
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   call(a,"setStep",new Class<?>[]{int.class},2);
   find(root(a),"استثناء التطبيق من توفير البطارية").performClick();
   android.content.Intent asked=Shadows.shadowOf(a).getNextStartedActivity();
   assertNotNull("the row asks the system for the exemption",asked);
   assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,asked.getAction());
   assertEquals("package:"+a.getPackageName(),String.valueOf(asked.getData()));
   assertEquals("and comes back to the same review step",2,(int)(Integer)get(a,"step"));
   assertNull("returning does not ask the system twice",Shadows.shadowOf(a).getNextStartedActivity());
  }
 }

 @Test public void activationIsOnlyOfferedOnceTheExemptionIsGranted(){
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   call(a,"setStep",new Class<?>[]{int.class},2);call(a,"confirmBooking",new Class<?>[0]);idle();
   View activate=(View)get(a,"activateButton");
   assertFalse("the exemption is still missing",activate.isShown());assertTrue("a same-day confirmation is what unlocks activation",visible(root(a),"المتابعة دون أتمتة"));
   batteryFree(a,true);call(a,"update",new Class<?>[0]);
   assertTrue(activate.isShown());assertEquals("تفعيل الحجز التلقائي",((Button)activate).getText().toString());
  }
 }

 @Test public void tappingActivateWithoutTheExemptionAsksForItAndTurnsNothingOn(){
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   call(a,"enable",new Class<?>[]{boolean.class},false);
   assertFalse("consent stays off",vault.enabled());
   android.content.Intent asked=Shadows.shadowOf(a).getNextStartedActivity();
   assertNotNull(asked);assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,asked.getAction());
   assertTrue(visible(root(a),"اسمح بالعمل في الخلفية أولًا"));
  }
 }

 @Test public void withTheExemptionActivationTurnsTheAutomationOn(){
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);batteryFree(a,true);
   call(a,"enable",new Class<?>[]{boolean.class},true);
   assertTrue(vault.enabled());
   assertTrue("activation still returns to the Home screen",visible(root(a),"خطوات الإعداد")||visible(root(a),"إعدادك مكتمل")||visible(root(a),"وجبات اليوم"));
  }
 }

 @Test public void homeChecklistMarksTheBatteryStepWithATextStatusNotColourOnly()throws Exception{
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   LinearLayout steps=(LinearLayout)get(a,"stepRows");assertEquals(4,steps.getChildCount());
   String last=String.valueOf(steps.getChildAt(3).getContentDescription());
   assertTrue(last.contains("استثناء التطبيق من توفير البطارية"));
   assertTrue("a pending step says so in words",last.contains("الخطوة الحالية")||last.contains("لاحقة"));
   batteryFree(a,true);call(a,"update",new Class<?>[0]);
   assertTrue(String.valueOf(steps.getChildAt(3).getContentDescription()).contains("مكتملة"));
   assertTrue(visible(root(a),"إعدادك مكتمل"));
  }
 }

 @Test public void theFourthChecklistRowOpensTheReviewStepWithTheBatterySetting()throws Exception{
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   LinearLayout steps=(LinearLayout)get(a,"stepRows");
   steps.getChildAt(3).performClick();idle();
   assertEquals("the wizard opens on the review step",2,(int)(Integer)get(a,"step"));
   assertTrue(visible(root(a),"خطة الأيام الثلاثة"));
   assertTrue(visible(root(a),"استثناء التطبيق من توفير البطارية"));
  }
 }

 @Test public void conflictsHideTheBatteryRowAndRenameTheCard(){
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   call(a,"planCard",new Class<?>[]{boolean.class},true);
   assertFalse("nothing else competes with the consent screen",((View)get(a,"batteryNote")).isShown());
   assertTrue(visible(root(a),"حجوزات تحتاج قرارك"));
   call(a,"planCard",new Class<?>[]{boolean.class},false);
   assertTrue(visible(root(a),"خطة الأيام الثلاثة"));
  }
 }

 /** The old flow left the user on a dead-end "review" screen; the plan must always be visible there. */
 @Test public void reviewStillShowsEveryMealSoTheBatteryRowIsNeverTheOnlyThingOnScreen()throws Exception{
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();ready(a);
   Method render=MainActivity.class.getDeclaredMethod("renderPlan",LinearLayout.class,JSONArray.class,java.time.LocalDate.class);render.setAccessible(true);
   LinearLayout rows=(LinearLayout)get(a,"planRows");assertEquals(9,render.invoke(a,rows,plan(),null));
   call(a,"setStep",new Class<?>[]{int.class},2);
   assertNotNull(find(root(a),"مطعم الغداء"));assertNotNull(find(root(a),"محجوز"));
  }
 }

 private boolean tooManyIntents(MainActivity a){return Shadows.shadowOf(a).getNextStartedActivity()!=null;}
}
