package dz.local.mealguard;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.lang.reflect.*;
import java.util.*;

/** Behaviour added in 0.8.0 from the UX audit: Back handling, visible busy/disabled states, flexible heights, button roles, trust text, booking status on Home. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class UxTest {
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
 private boolean visible(View v,String text){if(v.getVisibility()!=View.VISIBLE)return false;if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(visible(((ViewGroup)v).getChildAt(i),text))return true;return false;}
 private void all(View v,List<View> out){out.add(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)all(((ViewGroup)v).getChildAt(i),out);}
 private View root(MainActivity a){return a.getWindow().getDecorView();}
 private Object get(MainActivity a,String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
 private void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private Vault clean(){Vault v=new Vault(RuntimeEnvironment.getApplication());v.clear();return v;}

 @Test public void systemBackLeavesWizardAndSettingsWithoutClosingTheApp(){Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  find(root(a),"بدء الإعداد").performClick();idle();assertTrue(visible(root(a),"1 من 4"));
  a.onBackPressed();idle();assertFalse(a.isFinishing());assertTrue(visible(root(a),"بدء الإعداد"));assertFalse(visible(root(a),"1 من 4"));
  find(root(a),"الإعدادات").performClick();idle();assertTrue(visible(root(a),"حول التطبيق"));
  a.onBackPressed();idle();assertFalse(a.isFinishing());assertFalse(visible(root(a),"حول التطبيق"));
  a.onBackPressed();idle();assertTrue(a.isFinishing());
 }finally{v.clear();}}

 @Test public void wizardOpenedFromSettingsReturnsToSettings(){Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  find(root(a),"الإعدادات").performClick();idle();find(root(a),"رقم التسجيل").performClick();idle();assertTrue(visible(root(a),"1 من 4"));
  a.onBackPressed();idle();assertFalse(a.isFinishing());assertTrue(visible(root(a),"حول التطبيق"));
 }finally{v.clear();}}

 @Test public void noButtonOrFieldHasAFixedHeight(){Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){List<View> views=new ArrayList<>();all(root(c.get()).findViewById(android.R.id.content),views);int checked=0;
  for(View x:views)if(x instanceof Button&&!(x instanceof CompoundButton)||x instanceof EditText||x instanceof Spinner){assertEquals(String.valueOf(x),ViewGroup.LayoutParams.WRAP_CONTENT,x.getLayoutParams().height);assertTrue(x.getMinimumHeight()>0);checked++;}
  assertTrue(checked>20);
 }finally{v.clear();}}

 @Test public void chevronOnlyOnNavigationRowsAndDeleteIsDestructive(){Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  for(String navRow:new String[]{"رقم التسجيل","كلمة المرور","فطور الصباح","الغداء","العشاء","الاستثناءات الأسبوعية","مدة التحقق","إشعارات نتيجة الحجز","المظهر","حول التطبيق"})assertNotNull(navRow,((Button)find(root(a),navRow)).getCompoundDrawablesRelative()[2]);
  for(String action:new String[]{"فحص الاتصال","إعادة جدولة الفحص","حذف البيانات","رجوع","تحديث المعاينة"})assertNull(action,((Button)find(root(a),action)).getCompoundDrawablesRelative()[2]);
  boolean dark=Appearance.dark(v.appearance(),a.getResources().getConfiguration().uiMode);assertEquals(new Appearance.Palette(dark).danger,((Button)find(root(a),"حذف البيانات")).getCurrentTextColor());
  assertNull(find(root(a),"الوضع الليلي"));
  assertTrue(String.valueOf(find(root(a),"المظهر").getContentDescription()).contains("حسب النظام"));
 }finally{v.clear();}}

 /** 0.11.0: the owner asked to drop the long explanation from the login step; the same facts stay one tap away under "الخصوصية والاستخدام" and on the Home checklist. */
 @Test public void loginStepIsShortAndKeepsThePrivacyDetailsOneTapAway(){Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  assertTrue(visible(root(a),"مشفّرًا على هاتفك"));assertTrue(visible(root(a),"لا يُرسل أي حجز قبل أن تراجع الخطة"));
  find(root(a),"بدء الإعداد").performClick();idle();assertFalse(visible(root(a),"قبل إدخال كلمة المرور"));
  find(root(a),"الخصوصية والاستخدام").performClick();idle();android.app.AlertDialog d=(android.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(d);assertTrue(d.isShowing());assertTrue(visible(d.getWindow().getDecorView(),"دون خادم وسيط"));
 }finally{v.clear();}}

 @Test public void emptyLoginShowsPersistentErrorNextToTheAction()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();find(root(a),"بدء الإعداد").performClick();idle();
  find(root(a),"تسجيل الدخول والمتابعة").performClick();idle();
  TextView note=((TextView[])get(a,"pageNote"))[0];assertEquals(View.VISIBLE,note.getVisibility());assertTrue(note.getText().toString().contains("أدخل رقم التسجيل"));
  ViewGroup card=(ViewGroup)note.getParent();assertEquals(card.indexOfChild(note)+1,card.indexOfChild(find(root(a),"تسجيل الدخول والمتابعة")));
  assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,note.getAccessibilityLiveRegion());
 }finally{v.clear();}}

 @Test public void busyStateIsVisibleOnEveryPageAndDimsDisabledButtons()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  Method lock=MainActivity.class.getDeclaredMethod("lockUi",boolean.class);lock.setAccessible(true);View bar=(View)get(a,"busyBar");assertFalse(bar.isShown());
  find(root(a),"الإعدادات").performClick();idle();lock.invoke(a,true);
  assertEquals(View.VISIBLE,bar.getVisibility());Button about=(Button)find(root(a),"حول التطبيق");assertFalse(about.isEnabled());assertTrue(about.getAlpha()<0.6f);
  ViewParent p=bar.getParent();boolean insideScroll=false;while(p!=null){insideScroll|=p instanceof ScrollView;p=p.getParent();}assertFalse("busy bar must stay pinned, not scroll away",insideScroll);
  lock.invoke(a,false);assertEquals(View.GONE,bar.getVisibility());assertTrue(about.isEnabled());assertEquals(1f,about.getAlpha(),0.001f);
 }finally{v.clear();}}

 @Test public void homeShowsPerMealBookingStatusRowsWithTextChips()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  JSONObject account=new JSONObject().put("student","20230001").put("password","secret").put("meal_schema",2).put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10).put("breakfast_name","مطعم الصباح").put("lunch_name","مطعم الغداء").put("dinner_name","مطعم الصباح");
  Field cfg=MainActivity.class.getDeclaredField("cfg");cfg.setAccessible(true);cfg.set(a,account);Method update=MainActivity.class.getDeclaredMethod("update");update.setAccessible(true);update.invoke(a);
  assertTrue(visible(root(a),"وجبات اليوم"));assertTrue(visible(root(a),"لم تُقرأ حالة الحجز بعد"));assertTrue(visible(root(a),"مطعم الغداء"));assertNull("no status chip without an actual reading",find(root(a),"محجوز"));find(root(a),"الحجوزات").performClick();idle();assertNull(find(root(a),"غير محجوز"));
  String today=Engine.today().toString(),old=Engine.today().minusDays(2).toString();
  JSONArray items=new JSONArray().put(new JSONObject().put("day",old).put("meal",1).put("name","مطعم قديم").put("state",1)).put(new JSONObject().put("day",today).put("meal",2).put("name","مطعم الغداء").put("state",1)).put(new JSONObject().put("day",today).put("meal",3).put("name","مطعم العشاء").put("state",0)).put(new JSONObject().put("day",Engine.today().plusDays(1).toString()).put("meal",2).put("name","مطعم آخر").put("state",2));
  Method render=MainActivity.class.getDeclaredMethod("renderPlan",LinearLayout.class,JSONArray.class,java.time.LocalDate.class);render.setAccessible(true);LinearLayout rows=(LinearLayout)get(a,"bookDays");
  assertEquals(3,render.invoke(a,rows,items,Engine.today()));
  assertTrue(visible(root(a),"مطعم العشاء"));assertNotNull(find(rows,"محجوز"));assertNotNull(find(rows,"غير محجوز"));assertNotNull(find(rows,"يحتاج استبدالًا"));assertTrue(visible(rows,"اليوم"));assertTrue(visible(rows,"غدًا"));
  assertFalse("past days are not shown",visible(rows,"مطعم قديم"));
  for(int i=0;i<rows.getChildCount();i++){View r=rows.getChildAt(i);if(r instanceof LinearLayout)assertNotNull(r.getContentDescription());}
  assertEquals(4,render.invoke(a,rows,items,null));
 }finally{v.clear();}}
 private boolean inScroll(View v){ViewParent p=v.getParent();while(p!=null){if(p instanceof ScrollView)return true;p=p.getParent();}return false;}
 private void arabic(View v,List<String> out){if(v.getVisibility()!=View.VISIBLE)return;if(v instanceof TextView&&((TextView)v).getText().toString().matches("(?s).*[\\u0600-\\u06FF].*"))out.add(((TextView)v).getText().toString());if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)arabic(((ViewGroup)v).getChildAt(i),out);}

 @Test  public void bottomNavigationHasThreePinnedDestinations()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  for(String tab:new String[]{"الرئيسية","الحجوزات","الإعدادات"}){View t=find(root(a),tab);assertNotNull(tab,t);assertTrue(t.isShown());assertFalse(tab,inScroll(t));assertTrue(t.getMinimumHeight()>=48);}
  assertTrue(find(root(a),"الرئيسية").isSelected());
  assertEquals("three destinations, no notifications tab",3,((LinearLayout)get(a,"tabs")).getChildCount());find(root(a),"الحجوزات").performClick();idle();assertTrue(find(root(a),"الحجوزات").isSelected());assertFalse(find(root(a),"الرئيسية").isSelected());assertTrue(visible(root(a),"لم تُحدَّد المطاعم بعد"));
  a.onBackPressed();idle();assertFalse(a.isFinishing());assertTrue(find(root(a),"الرئيسية").isSelected());
  find(root(a),"بدء الإعداد").performClick();idle();assertFalse("navigation hidden inside the setup",find(root(a),"الرئيسية").isShown());
 }finally{v.clear();}}

 @Test public void primaryWizardActionIsPinnedOutsideTheScrollArea(){Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();find(root(a),"بدء الإعداد").performClick();idle();
  View login=find(root(a),"تسجيل الدخول والمتابعة");assertTrue(login.isShown());assertFalse(inScroll(login));assertFalse(find(root(a),"مراجعة الخطة").isShown());assertFalse(find(root(a),"تأكيد وحجز الناقص").isShown());
 }finally{v.clear();}}

 @Test public void weeklyScheduleIsAScreenWithSevenDaysAndUsualRestaurants()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  JSONObject account=new JSONObject().put("student","20230001").put("password","secret").put("meal_schema",2).put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10).put("breakfast_name","مطعم الصباح").put("lunch_name","مطعم الغداء").put("dinner_name","مطعم الصباح");
  Field cfg=MainActivity.class.getDeclaredField("cfg");cfg.setAccessible(true);cfg.set(a,account);find(root(a),"الإعدادات").performClick();idle();assertTrue(visible(root(a),"مطعم الغداء"));{Method show=MainActivity.class.getDeclaredMethod("show",int.class);show.setAccessible(true);show.invoke(a,2);Method st=MainActivity.class.getDeclaredMethod("setStep",int.class);st.setAccessible(true);st.invoke(a,1);Method up=MainActivity.class.getDeclaredMethod("update");up.setAccessible(true);up.invoke(a);}idle();
  assertTrue("weekly exceptions live in step 2",visible(root(a),"الاستثناءات الأسبوعية"));assertTrue(visible(root(a),"2 من 4"));for(int d=1;d<=7;d++){View row=find(root(a),WeeklySchedule.DAYS[d]);assertTrue(row instanceof Button);assertTrue(row.isShown());assertTrue(String.valueOf(row.getContentDescription()).contains("المطاعم المعتادة"));}
  Method update=MainActivity.class.getDeclaredMethod("update");update.setAccessible(true);int before=((List<?>)get(a,"actions")).size();update.invoke(a);update.invoke(a);assertEquals("re-rendering must not leak buttons",before,((List<?>)get(a,"actions")).size());
 }finally{v.clear();}}

 @Test public void choiceFieldOpensASheetAndReportsTheChosenRow(){Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  ChoiceField f=new ChoiceField(a,new Appearance.Palette(false),"الغداء");int[] told={-1};f.setListener(i->told[0]=i);f.setItems(Arrays.asList("اختر المطعم","مطعم أ","مطعم ب"));
  assertEquals(0,f.getSelectedItemPosition());assertEquals("اختر المطعم",f.getText().toString());f.setSelection(9);assertEquals(0,f.getSelectedItemPosition());
  f.performClick();idle();assertTrue(f.sheetShowing());List<View> rows=f.sheetRows();assertEquals(3,rows.size());for(View r:rows)assertTrue(r.getMinimumHeight()>0);assertTrue(rows.get(0).isSelected());
  rows.get(2).performClick();idle();assertFalse(f.sheetShowing());assertEquals(2,f.getSelectedItemPosition());assertEquals(2,told[0]);assertEquals("مطعم ب",f.getText().toString());assertTrue(String.valueOf(f.getContentDescription()).contains("الغداء"));
  f.setEnabled(false);f.performClick();idle();assertFalse(f.sheetShowing());assertTrue(f.getAlpha()<0.6f);
 }finally{v.clear();}}

 @Test public void frenchIsOptInAndTranslatesTheWholeFirstScreens(){Vault v=clean();try{
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){assertNotNull("Arabic stays the default whatever the phone language",find(root(c.get()),"بدء الإعداد"));assertNull(find(root(c.get()),"Accueil"));}
  v.language("fr");
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();View content=a.findViewById(android.R.id.content);
   assertNotNull(find(root(a),"Accueil"));assertNotNull(find(root(a),"Commencer la configuration"));assertTrue(visible(root(a),"La réservation automatique ne fonctionne pas"));
   List<String> left=new ArrayList<>();arabic(content,left);assertTrue("untranslated on Home: "+left,left.isEmpty());
   find(root(a),"Réglages").performClick();idle();arabic(content,left);assertTrue("untranslated in Settings: "+left,left.isEmpty());assertNotNull(find(root(a),"Langue"));
   find(root(a),"Réservations").performClick();idle();arabic(content,left);assertTrue("untranslated in Bookings: "+left,left.isEmpty());
   find(root(a),"Accueil").performClick();idle();find(root(a),"Commencer la configuration").performClick();idle();arabic(content,left);assertTrue("untranslated in Login: "+left,left.isEmpty());assertTrue(visible(root(a),"1 sur 4"));
   assertEquals(View.LAYOUT_DIRECTION_LTR,find(root(a),"Se connecter et continuer").getLayoutDirection());
  }
 }finally{v.clear();}}

 private JSONObject sample()throws Exception{return new JSONObject().put("student","20230001").put("password","secret").put("meal_schema",2).put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10).put("breakfast_name","مطعم الصباح").put("lunch_name","مطعم الغداء").put("dinner_name","مطعم الصباح");}
 private Object call(MainActivity a,String name,Class<?>[] types,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(a,args);}

 @Test public void homeShowsSetupProgressAndThreeStepChecklist()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  assertTrue(visible(root(a),"خطوات الإعداد"));assertTrue(visible(root(a),"اكتمال الإعداد 0/4"));assertTrue(visible(root(a),"0%"));
  for(String step:new String[]{"إدخال رقم التسجيل وكلمة المرور","اختيار مطعم لكل وجبة","عرض الحجوزات وتأكيدها"})assertTrue(step,visible(root(a),step));
  LinearLayout rows=(LinearLayout)get(a,"stepRows");List<View> steps=new ArrayList<>();for(int i=0;i<rows.getChildCount();i++)if(rows.getChildAt(i) instanceof LinearLayout)steps.add(rows.getChildAt(i));assertEquals(4,steps.size());assertTrue(visible(root(a),"السماح بالعمل في الخلفية (البطارية)"));
  for(View s:steps){assertTrue(s.getMinimumHeight()>0);assertNotNull(s.getContentDescription());}assertTrue(String.valueOf(steps.get(0).getContentDescription()).contains("الخطوة الحالية"));
  steps.get(1).performClick();idle();assertFalse("a later step cannot be opened before the account",visible(root(a),"1 من 4"));
  steps.get(0).performClick();idle();assertTrue(visible(root(a),"الخطوة 1 من 4"));assertEquals(1,((ProgressBar)get(a,"wizardBar")).getProgress());
  call(a,"show",new Class<?>[]{int.class},0);call(a,"renderSteps",new Class<?>[]{int.class},2);assertNotNull(find(rows,"✓"));assertNotNull(find(rows,"3"));assertNull(find(rows,"1"));
 }finally{v.clear();}}

 @Test public void wizardHasCloseButtonPasswordToggleAndTruthfulStorageNote()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  find(root(a),"بدء الإعداد").performClick();idle();assertFalse(visible(root(a),"Android Keystore"));assertFalse(visible(root(a),"قبل إدخال كلمة المرور"));assertFalse(visible(root(a),"اترك كلمة المرور"));assertTrue(find(root(a),"الخصوصية والاستخدام").isShown());
  List<View> views=new ArrayList<>();all(root(a),views);View eye=null,close=null;EditText pass=null;for(View x:views){if("إظهار كلمة المرور".contentEquals(String.valueOf(x.getContentDescription())))eye=x;if("إغلاق الإعداد".contentEquals(String.valueOf(x.getContentDescription())))close=x;if(x instanceof EditText&&(((EditText)x).getInputType()&android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)==android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)pass=(EditText)x;}
  assertNotNull(eye);assertNotNull(close);assertNotNull(pass);assertTrue(eye.getLayoutParams().width>=48&&close.getLayoutParams().width>=48);
  assertSame("eye and field share one box",eye.getParent(),pass.getParent());assertTrue(((LinearLayout)eye.getParent()).indexOfChild(eye)>((LinearLayout)eye.getParent()).indexOfChild(pass));assertNull(pass.getBackground());assertEquals(1f,((LinearLayout.LayoutParams)pass.getLayoutParams()).weight,0f);
  eye.performClick();assertFalse(pass.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);assertEquals("إخفاء كلمة المرور",String.valueOf(eye.getContentDescription()));
  eye.performClick();assertTrue(pass.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);assertEquals("إظهار كلمة المرور",String.valueOf(eye.getContentDescription()));
  close.performClick();idle();assertFalse(a.isFinishing());assertTrue(visible(root(a),"خطوات الإعداد"));assertFalse(visible(root(a),"الخطوة 1 من 4"));
 }finally{v.clear();}}

 @Test public void settingsRowsShowCurrentValuesAndNeverThePassword()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  find(root(a),"الإعدادات").performClick();idle();assertTrue("a",visible(root(a),"لم يُضبط بعد"));assertTrue("b",visible(root(a),"غير محفوظة"));assertTrue("c",visible(root(a),"مطاعم الوجبات الثلاث"));
  Field cfg=MainActivity.class.getDeclaredField("cfg");cfg.setAccessible(true);cfg.set(a,sample());call(a,"update",new Class<?>[0]);
  assertTrue(visible(root(a),"20230001"));assertTrue(visible(root(a),"••••••••"));assertFalse(visible(root(a),"secret"));assertTrue(visible(root(a),"مطعم الغداء"));assertTrue(visible(root(a),"بلا استثناءات"));
  List<View> views=new ArrayList<>();all(root(a),views);boolean lunch=false;for(View x:views){assertFalse(String.valueOf(x.getContentDescription()).contains("secret"));if(x instanceof Button&&"الغداء".contentEquals(((Button)x).getText())&&String.valueOf(x.getContentDescription()).contains("مطعم الغداء"))lunch=true;}assertTrue(lunch);
 }finally{v.clear();}}

 @Test public void bookingsLoadingShowsReadOnlyBannerAndSkeleton()throws Exception{Vault v=clean();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
  Field cfg=MainActivity.class.getDeclaredField("cfg");cfg.setAccessible(true);cfg.set(a,sample());find(root(a),"الحجوزات").performClick();idle();
  assertFalse(visible(root(a),"الأيام الثلاثة القادمة"));assertFalse(visible(root(a),"آخر قراءة"));assertFalse(visible(root(a),"حسب آخر معاينة"));assertTrue(visible(root(a),"لم تُقرأ الحجوزات بعد"));assertFalse(visible(root(a),"جارٍ قراءة حجوزاتك"));assertTrue(find(root(a),"تحديث من Webetu").isShown());
  Field loading=MainActivity.class.getDeclaredField("bookLoading");loading.setAccessible(true);loading.set(a,true);call(a,"lockUi",new Class<?>[]{boolean.class},true);call(a,"renderBookings",new Class<?>[0]);
  assertTrue(visible(root(a),"جارٍ قراءة حجوزاتك"));assertTrue(visible(root(a),"لا يُرسل أي حجز في هذه الخطوة"));assertEquals(2,((LinearLayout)get(a,"bookDays")).getChildCount());assertFalse(find(root(a),"تحديث من Webetu").isShown());
  call(a,"lockUi",new Class<?>[]{boolean.class},false);assertFalse(visible(root(a),"جارٍ قراءة حجوزاتك"));assertEquals(0,((LinearLayout)get(a,"bookDays")).getChildCount());
 }finally{v.clear();}}

 @Test public void batteryIsTheFourthStepAndCompletedCardShowsOnlyOnce()throws Exception{Vault v=clean();try{v.accountVerified(true);v.success("ok");
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();Field cfg=MainActivity.class.getDeclaredField("cfg");cfg.setAccessible(true);cfg.set(a,sample());call(a,"update",new Class<?>[0]);
   assertTrue(visible(root(a),"تبقّى إعداد البطارية"));assertTrue(visible(root(a),"75%"));assertTrue(visible(root(a),"اكتمال الإعداد 3/4"));assertNotNull(find(root(a),"التالي: إعداد البطارية"));assertFalse(v.setupSeen());
   find(root(a),"التالي: إعداد البطارية").performClick();idle();{android.content.Intent asked=Shadows.shadowOf(a).getNextStartedActivity();assertEquals(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,asked.getAction());assertEquals("package:"+a.getPackageName(),String.valueOf(asked.getData()));}
   Shadows.shadowOf(a.getSystemService(android.os.PowerManager.class)).setIgnoringBatteryOptimizations(a.getPackageName(),true);call(a,"update",new Class<?>[0]);
   assertTrue(visible(root(a),"إعدادك مكتمل"));assertTrue(visible(root(a),"100%"));assertFalse(visible(root(a),"خطوات الإعداد"));assertTrue(v.setupSeen());
   find(root(a),"الإعدادات").performClick();idle();find(root(a),"الرئيسية").performClick();idle();assertFalse("the completed card is shown once",visible(root(a),"إعدادك مكتمل"));assertTrue(visible(root(a),"وجبات اليوم"));
   Shadows.shadowOf(a.getSystemService(android.os.PowerManager.class)).setIgnoringBatteryOptimizations(a.getPackageName(),false);call(a,"update",new Class<?>[0]);assertTrue("an undone step brings the card back",visible(root(a),"تبقّى إعداد البطارية"));
  }}finally{v.clear();}}

 @Test public void bookingAndActivationNoLongerOpenAConfirmationDialog()throws Exception{String src=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/dz/local/mealguard/MainActivity.java")),java.nio.charset.StandardCharsets.UTF_8);
  String book=src.substring(src.indexOf(" private void confirmBooking("));book=book.substring(0,book.indexOf("\n"));String enable=src.substring(src.indexOf(" private void enable(boolean"));enable=enable.substring(0,enable.indexOf("\n"));String replace=src.substring(src.indexOf(" private void confirmReplacement("));replace=replace.substring(0,replace.indexOf("\n"));
  assertFalse(book.contains("AlertDialog"));assertFalse(enable.contains("AlertDialog"));assertTrue("booking still requires a matching same-day preview",book.contains("sameChoices")&&book.contains("previewDay"));assertTrue("replacing existing reservations still asks",replace.contains("AlertDialog"));
 }
}
