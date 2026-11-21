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

/**
 * Pins the layout of the review step after 0.14.0: the plan keeps the first place it had in the screenshot the
 * owner sent, and the battery setting sits under it as the last step instead of the long explanation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class SetupScreenTest {
 @After public void clean(){new Vault(RuntimeEnvironment.getApplication()).clear();}
 private View root(MainActivity a){return a.getWindow().getDecorView();}
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
 private Object get(MainActivity a,String n)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(a);}
 private void set(MainActivity a,String n,Object v)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);f.set(a,v);}

 @Test public void thePlanStaysFirstAndTheBatteryRowComesAfterIt()throws Exception{
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){MainActivity a=c.get();
   set(a,"cfg",new JSONObject().put("student","20230001").put("password","secret").put("meal_schema",2)
     .put("breakfast_id",10).put("lunch_id",20).put("dinner_id",10)
     .put("breakfast_name","مطعم الصباح").put("lunch_name","مطعم الغداء").put("dinner_name","مطعم الصباح"));
   Shadows.shadowOf(a.getSystemService(android.os.PowerManager.class)).setIgnoringBatteryOptimizations(a.getPackageName(),false);
   Method setStep=MainActivity.class.getDeclaredMethod("setStep",int.class);setStep.setAccessible(true);setStep.invoke(a,2);
   Shadows.shadowOf(Looper.getMainLooper()).idle();
   View title=find(root(a),"خطة الأيام الثلاثة");
   assertNotNull("the review step still names the plan",title);
   LinearLayout card=(LinearLayout)title.getParent();
   LinearLayout plan=(LinearLayout)get(a,"planRows");
   View row=(View)get(a,"batteryNote");
   while(row!=null&&row.getParent()!=card)row=(View)row.getParent();
   assertNotNull("the last step is drawn inside the review card",row);
   int planAt=card.indexOfChild(plan),batteryAt=card.indexOfChild(row);
   assertTrue("the plan comes before the last step",planAt>=0&&batteryAt>planAt);
   assertTrue("the card keeps its other content, not only the last step",card.getChildCount()>3);
  }
 }
}
