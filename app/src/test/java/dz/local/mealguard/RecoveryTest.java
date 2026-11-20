package dz.local.mealguard;
import android.app.*;
import android.content.*;
import android.net.*;
import androidx.work.*;
import androidx.work.testing.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.concurrent.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class RecoveryTest {
 Context c;Vault v;
 @Before public void init(){c=RuntimeEnvironment.getApplication();v=new Vault(c);v.clear();}
 @Test public void offlineWorkerDoesNotDisableAutomationOrAttemptAuthentication(){
  v.enable(true);Automation.BookingWorker worker=TestWorkerBuilder.from(c,Automation.BookingWorker.class,Executors.newSingleThreadExecutor()).build();
  assertFalse(NetworkState.ready(c));assertEquals(ListenableWorker.Result.retry(),worker.doWork());assertTrue(v.enabled());assertEquals(0,v.lastAttempt());assertEquals(0,v.lastFailure());
 }
 @Test public void failureDoesNotChangeConsentOrSuccessfulCoverageTime(){v.enable(true);v.success("ok");long t=v.lastSuccess();v.failure("HTTP 401 [S08]");assertTrue(v.enabled());assertEquals(t,v.lastSuccess());assertTrue(v.lastFailure()>0);}
 @Test public void nextSuccessClearsFailureMarker(){v.failure("HTTP 401 [S08]");v.success("ok");assertEquals(0,v.lastFailure());}
 @Test public void showingStatusIsNotAToggle(){v.enable(true);Automation.showStatus(c,"بانتظار إنترنت");assertTrue(v.enabled());v.enable(false);Automation.showStatus(c,"لا تفعيل ضمني");assertFalse(v.enabled());}
}
