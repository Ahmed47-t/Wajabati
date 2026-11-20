package dz.local.mealguard;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.android.controller.ActivityController;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;

/** Optional visual check: renders real screens to PNG when MEALGUARD_SHOTS points to a folder. Skipped otherwise. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenshotTest {
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
 private Object get(MainActivity a,String n)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(a);}
 private Object call(MainActivity a,String n,Class<?>[] t,Object... args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(a,args);}
 private void shot(MainActivity a,String name)throws Exception{
  Shadows.shadowOf(Looper.getMainLooper()).idle();View v=a.findViewById(android.R.id.content);int w=v.getWidth(),h=v.getHeight();
  v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);
  Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));
  try(OutputStream o=new FileOutputStream(new File(System.getenv("MEALGUARD_SHOTS"),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,o);}
 }
 private JSONObject account()throws Exception{return new JSONObject().put("student","20230001").put("password","x").put("meal_schema",2).put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10).put("breakfast_name","مطعم الإقامة الجامعية").put("lunch_name","المطعم المركزي").put("dinner_name","مطعم الإقامة الجامعية");}
 private JSONArray items()throws Exception{JSONArray a=new JSONArray();java.time.LocalDate d=Engine.today();String[] n={"","مطعم الإقامة الجامعية","المطعم المركزي","مطعم الإقامة الجامعية"};int[] st={1,1,1,1,2,0,0,0,0};for(int i=0;i<9;i++)a.put(new JSONObject().put("day",d.plusDays(i/3).toString()).put("meal",i%3+1).put("name",n[i%3+1]).put("state",st[i]));return a;}
 private void draw(View v,String name)throws Exception{Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);b.eraseColor(0xff808080);v.draw(new Canvas(b));try(OutputStream o=new FileOutputStream(new File(System.getenv("MEALGUARD_SHOTS"),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,o);}}
 private void run(String prefix,String home,String setup,String login,String settings,String meals)throws Exception{
  Assume.assumeNotNull(System.getenv("MEALGUARD_SHOTS"));
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();View root=a.getWindow().getDecorView();
   shot(a,prefix+"1-home-fresh");
   find(root,setup).performClick();find(root,login).performClick();{Field pf=MainActivity.class.getDeclaredField("password");pf.setAccessible(true);((android.widget.EditText)pf.get(a)).setText("MotDePasse123");}shot(a,prefix+"2-login-error");
   a.onBackPressed();find(root,settings).performClick();shot(a,prefix+"3-settings");
   call(a,"lockUi",new Class<?>[]{boolean.class},true);((TextView)get(a,"busyText")).setText("…");shot(a,prefix+"4-settings-busy");call(a,"lockUi",new Class<?>[]{boolean.class},false);
   Field cfg=MainActivity.class.getDeclaredField("cfg");cfg.setAccessible(true);cfg.set(a,account());find(root,home).performClick();call(a,"update",new Class<?>[0]);
   call(a,"renderSteps",new Class<?>[]{int.class},1);shot(a,prefix+"5-home-status");
   find(root,meals).performClick();Field bl=MainActivity.class.getDeclaredField("bookLoading");bl.setAccessible(true);bl.set(a,true);call(a,"lockUi",new Class<?>[]{boolean.class},true);call(a,"renderBookings",new Class<?>[0]);shot(a,prefix+"6-bookings-loading");call(a,"lockUi",new Class<?>[]{boolean.class},false);
   call(a,"renderDays",new Class<?>[]{LinearLayout.class,JSONArray.class},get(a,"bookDays"),items());shot(a,prefix+"6b-bookings");
   {Vault vv=new Vault(RuntimeEnvironment.getApplication());vv.accountVerified(true);vv.success("ok");Shadows.shadowOf(a.getSystemService(android.os.PowerManager.class)).setIgnoringBatteryOptimizations(a.getPackageName(),true);call(a,"show",new Class<?>[]{int.class},0);call(a,"update",new Class<?>[0]);shot(a,prefix+"6c-home-complete");vv.invalidateTest();vv.accountVerified(false);vv.setupSeen(false);Shadows.shadowOf(a.getSystemService(android.os.PowerManager.class)).setIgnoringBatteryOptimizations(a.getPackageName(),false);}call(a,"show",new Class<?>[]{int.class},2);
   call(a,"setStep",new Class<?>[]{int.class},1);ChoiceField[] fields=(ChoiceField[])get(a,"meals");fields[2].setItems(java.util.Arrays.asList("—","مطعم الإقامة الجامعية","المطعم المركزي","مطعم كلية العلوم"));fields[2].setSelection(2);shot(a,prefix+"7-step-meals");
   fields[2].performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();View sheet=fields[2].sheet().getWindow().getDecorView();if(sheet.getWidth()>0)draw(sheet,prefix+"8-choice-sheet");fields[2].sheet().dismiss();
   call(a,"setStep",new Class<?>[]{int.class},2);call(a,"renderPlan",new Class<?>[]{LinearLayout.class,JSONArray.class,java.time.LocalDate.class},get(a,"planRows"),items(),null);((View)get(a,"replaceButton")).setVisibility(View.VISIBLE);((View)get(a,"bookButton")).setVisibility(View.GONE);shot(a,prefix+"9-review");
  }
 }
 private void ar(String prefix)throws Exception{new Vault(RuntimeEnvironment.getApplication()).clear();run(prefix,"الرئيسية","بدء الإعداد","تسجيل الدخول والمتابعة","الإعدادات","الحجوزات");}
 @Test @Config(qualifiers="w411dp-h1500dp-xhdpi") public void light()throws Exception{ar("light-");}
 @Test @Config(qualifiers="w411dp-h1500dp-night-xhdpi") public void dark()throws Exception{ar("dark-");}
 @Test @Config(qualifiers="w360dp-h2200dp-xhdpi",fontScale=1.6f) public void largeFont()throws Exception{ar("large-");}
 @Test @Config(qualifiers="w411dp-h1500dp-xhdpi") public void french()throws Exception{Vault v=new Vault(RuntimeEnvironment.getApplication());v.clear();v.language("fr");try{run("fr-","Accueil","Commencer la configuration","Se connecter et continuer","Réglages","Réservations");}finally{v.clear();}}
}
