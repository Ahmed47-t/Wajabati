package dz.local.mealguard;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import android.app.AlertDialog;
import android.os.Looper;
import android.view.*;
import android.widget.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class ActivityTest {
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
 private boolean contains(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),text))return true;return false;}
 private boolean visible(View v,String text){if(v.getVisibility()!=View.VISIBLE)return false;if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(visible(((ViewGroup)v).getChildAt(i),text))return true;return false;}
 private <T extends View>T type(View v,Class<T> cls){if(cls.isInstance(v))return cls.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T f=type(((ViewGroup)v).getChildAt(i),cls);if(f!=null)return f;}return null;}
 @Test public void freshUserSeesOneSetupActionAndOfflineWarning(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){View r=c.get().getWindow().getDecorView();assertFalse(new Vault(c.get()).enabled());assertTrue(visible(r,"بدء الإعداد"));assertTrue(visible(r,"لا يعمل الحجز التلقائي إذا كان الهاتف مغلقًا أو غير متصل بالإنترنت."));assertFalse(visible(r,"تسجيل الدخول والمتابعة"));}}
 @Test public void noUniversityRestaurantOrTestBadgeHardcodedInUi(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){View r=c.get().getWindow().getDecorView();for(String s:new String[]{"القليعة","1000 سرير","800 مقعد","نسخة اختبار","بلا نسيان"})assertFalse(contains(r,s));}}
 @Test public void setupOpensStepOneWithoutBooking(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){View r=c.get().getWindow().getDecorView();find(r,"بدء الإعداد").performClick();assertTrue(visible(r,"1 من 4"));assertTrue(visible(r,"تسجيل الدخول والمتابعة"));assertFalse(visible(r,"تأكيد وحجز الناقص"));assertFalse(new Vault(c.get()).enabled());}}
 @Test public void mealEditingCannotSkipUnverifiedAccount(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){View r=c.get().getWindow().getDecorView();find(r,"الإعدادات").performClick();find(r,"الغداء").performClick();assertTrue(visible(r,"1 من 4"));assertFalse(visible(r,"مراجعة الخطة"));}}
 @Test public void allThreeMealControlsExistIndependently(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){View r=c.get().getWindow().getDecorView();assertTrue(contains(r,"فطور الصباح"));assertTrue(contains(r,"الغداء"));assertTrue(contains(r,"العشاء"));}}
 @Test public void aboutRetainsHonestScopeAndOfflineLimits(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){View r=c.get().getWindow().getDecorView();find(r,"الإعدادات").performClick();find(r,"حول التطبيق").performClick();View d=ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView();assertTrue(contains(d,"0.12.0"));assertTrue(contains(d,"التوافق مع كل الجامعات لم يُختبر"));assertTrue(contains(d,"لا يعمل الحجز التلقائي"));}}
 @Test public void customDurationValidationKeepsDialogOpenThenSavesWithoutEnabling(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){View r=c.get().getWindow().getDecorView();find(r,"الإعدادات").performClick();find(r,"مدة التحقق").performClick();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();ChoiceField sp=type(d.getWindow().getDecorView(),ChoiceField.class);sp.setSelection(ScheduleConfig.PRESETS.length);Shadows.shadowOf(Looper.getMainLooper()).idle();EditText e=type(d.getWindow().getDecorView(),EditText.class);e.setText("١٠");d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertTrue(d.isShowing());assertEquals(60,new Vault(c.get()).intervalMinutes());e.setText("٩٠");d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertFalse(d.isShowing());assertEquals(90,new Vault(c.get()).intervalMinutes());assertFalse(new Vault(c.get()).enabled());}}
 @Test public void aboutShowsOwnerBioAndSocialActionsOnlyOpenOnTap(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
  MainActivity a=c.get();View r=a.getWindow().getDecorView();find(r,"الإعدادات").performClick();find(r,"حول التطبيق").performClick();View d=ShadowAlertDialog.getLatestAlertDialog().getWindow().getDecorView();assertTrue(contains(d,"TFYCHE AHMED REDOUANE"));assertTrue(contains(d,"طالب في ESGEN"));assertNull(Shadows.shadowOf(a).getNextStartedActivity());find(d,"Instagram").performClick();android.content.Intent first=Shadows.shadowOf(a).getNextStartedActivity();assertEquals(android.content.Intent.ACTION_VIEW,first.getAction());assertEquals("https://www.instagram.com/hamado___mc/",first.getDataString());find(d,"LinkedIn").performClick();assertEquals("https://www.linkedin.com/in/ahmed-redouane-tfyeche-167588336/",Shadows.shadowOf(a).getNextStartedActivity().getDataString());}}
 @Test public void fortyEightHourPresetSavesWithoutTurningOnAutomation(){try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
  View r=c.get().getWindow().getDecorView();find(r,"الإعدادات").performClick();find(r,"مدة التحقق").performClick();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();ChoiceField sp=type(d.getWindow().getDecorView(),ChoiceField.class);int index=-1;for(int n=0;n<ScheduleConfig.PRESETS.length;n++)if(ScheduleConfig.PRESETS[n]==2880)index=n;assertTrue(index>=0);sp.setSelection(index);Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(visible(d.getWindow().getDecorView(),"المدد الطويلة"));d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertEquals(2880,new Vault(c.get()).intervalMinutes());assertFalse(new Vault(c.get()).enabled());}}

}
