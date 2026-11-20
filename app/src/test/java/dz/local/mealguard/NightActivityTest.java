package dz.local.mealguard;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class NightActivityTest {
 private View find(View v,String s){if(v instanceof TextView&&((TextView)v).getText().toString().equals(s))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),s);if(f!=null)return f;}return null;}
 @Test public void darkModeColorsCardsAndWindowAndKeepsConsentOff(){Context context=RuntimeEnvironment.getApplication();Vault v=new Vault(context);v.clear();v.appearance(Appearance.DARK);try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();TextView title=(TextView)find(a.getWindow().getDecorView(),"وجباتي");assertEquals(new Appearance.Palette(true).ink,title.getCurrentTextColor());View card=(View)find(a.getWindow().getDecorView(),"خطوات الإعداد").getParent();assertEquals(new Appearance.Palette(true).surface,((GradientDrawable)card.getBackground()).getColor().getDefaultColor());assertFalse(v.enabled());assertEquals(0,a.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);}finally{v.clear();}}
 @Test public void lightModeCanBeForcedAndModePickerPersistsChoice(){Vault v=new Vault(RuntimeEnvironment.getApplication());v.clear();v.appearance(Appearance.LIGHT);try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();View root=a.getWindow().getDecorView();assertEquals(new Appearance.Palette(false).ink,((TextView)find(root,"وجباتي")).getCurrentTextColor());find(root,"الإعدادات").performClick();find(root,"المظهر").performClick();android.app.AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d);assertEquals(3,d.getListView().getAdapter().getCount());d.getListView().performItemClick(d.getListView().getChildAt(2),2,2);assertEquals(Appearance.DARK,v.appearance());assertFalse(v.enabled());}finally{v.clear();}}
 @Test @Config(sdk=35,qualifiers="night") public void automaticModeUsesNightResources(){Vault v=new Vault(RuntimeEnvironment.getApplication());v.clear();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){assertEquals(new Appearance.Palette(true).ink,((TextView)find(c.get().getWindow().getDecorView(),"وجباتي")).getCurrentTextColor());}finally{v.clear();}}
 @Test @Config(sdk=35,qualifiers="night") public void explicitLightOverridesNightResources(){Vault v=new Vault(RuntimeEnvironment.getApplication());v.clear();v.appearance(Appearance.LIGHT);try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){assertEquals(new Appearance.Palette(false).ink,((TextView)find(c.get().getWindow().getDecorView(),"وجباتي")).getCurrentTextColor());}finally{v.clear();}}
 @Test public void savedSettingsPageRestoresAcrossThemeRecreation(){Vault v=new Vault(RuntimeEnvironment.getApplication());v.clear();android.os.Bundle saved=new android.os.Bundle();try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){find(c.get().getWindow().getDecorView(),"الإعدادات").performClick();c.saveInstanceState(saved);}v.appearance(Appearance.DARK);try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).create(saved).start().resume().visible()){View root=c.get().getWindow().getDecorView();assertTrue(find(root,"مدة التحقق").isShown());assertFalse(find(root,"بدء الإعداد").isShown());assertFalse(v.enabled());}finally{v.clear();}}

}
