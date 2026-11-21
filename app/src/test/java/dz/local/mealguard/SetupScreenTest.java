package dz.local.mealguard;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.lang.reflect.*;
import static org.junit.Assert.*;

/** Why the step moved: this marks the screen layout the user sent in — plan, then a long text, then two buttons. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class SetupScreenTest {
 @After public void clean(){new Vault(RuntimeEnvironment.getApplication()).clear();}
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
 private View root(MainActivity a){return a.getWindow().getDecorView();}
 private Object get(MainActivity a,String n)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(a);}
 private void set(MainActivity a,String n,Object v)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);f.set(a,v);}
 @Test public void thePlanStaysFirstAndTheBatteryRowComesAfterItNotOverIt()throws Exception{
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
   set(a,"cfg",new JSONObject().put("student","20230001").put("password","secret").put("meal_schema",2).put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10).put("breakfast_name","مطعم الصباح").put("lunch_name","مطعم الغداء").put("dinner_name","مطعم الصباح"));
   Shadows.shadowOf(a.getSystemService(android.os.PowerManager.class)).setIgnoringBatteryOptimizations(a.getPackageName(),false);
   Method setStep=MainActivity.class.getDeclaredMethod("setStep",int.class);setStep.setAccessible(true);setStep.invoke(a,2);
   Shadows.shadowOf(Looper.getMainLooper()).idle();
   LinearLayout rev=(LinearLayout)find(root(a),"خطة الأيام الثلاثة").getParent();
   LinearLayout plan=(LinearLayout)get(a,"planRows"),row=(LinearLayout)((View)get(a,"batteryNote")).getParent().getParent();
   int planAt=rev.indexOfChild(plan);
   assertNotNull("the last step is inside the review card",row);
   int batteryAt=rev.indexOfChild(row);
   assertTrue("the plan comes before the last step",planAt>=0&&batteryAt>planAt);
   assertTrue("and the last step is not the only thing left",rev.getChildCount()>3);
  }
 }
}
