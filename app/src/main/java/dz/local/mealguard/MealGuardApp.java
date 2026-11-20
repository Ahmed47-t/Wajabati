package dz.local.mealguard;
import android.app.Application;
import android.net.*;
/** Callback is best effort while process exists. WorkManager remains the process-independent scheduler. */
public final class MealGuardApp extends Application {
 private boolean wasReady=false;
 @Override public void onCreate(){
  super.onCreate();Automation.channels(this);Vault v=new Vault(this);
  try{v.account();}catch(Exception e){v.enable(false);v.status(Runner.safe(e));}
  if(v.enabled())Automation.start(this);
  ConnectivityManager cm=getSystemService(ConnectivityManager.class);if(cm==null)return;
  try{cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
   @Override public void onCapabilitiesChanged(Network n,NetworkCapabilities caps){changed();}
   @Override public void onLost(Network n){changed();}
   private synchronized void changed(){
    boolean ready=NetworkState.ready(MealGuardApp.this),returned=ready&&!wasReady;wasReady=ready;
    if(new Vault(MealGuardApp.this).enabled()){
     Automation.showStatus(MealGuardApp.this,ready?"الحجز التلقائي مفعّل.":"بانتظار الاتصال بالإنترنت.");
     if(returned)Automation.enqueueCheck(MealGuardApp.this);
    }
   }
  });}catch(SecurityException ignored){}
 }
}
