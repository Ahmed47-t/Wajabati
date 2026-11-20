package dz.local.mealguard;
import android.app.*;
import android.content.*;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public final class Automation {
 static final String CHANNEL="mealguard_status",STATE_CHANNEL="mealguard_schedule";
 static final String ALERT="تنبيه: ";
 public static void channels(Context c){
  NotificationManager nm=c.getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel(CHANNEL,"نتائج الحجز",NotificationManager.IMPORTANCE_DEFAULT));
  nm.createNotificationChannel(new NotificationChannel(STATE_CHANNEL,"حالة الحجز",NotificationManager.IMPORTANCE_LOW));
 }
 private static Notification.Builder base(Context c,String channel,String text){
  Intent open=new Intent(c,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_notification).setContentTitle("وجباتي").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setContentIntent(pi);
 }
 public static void notify(Context c,String text,int id){
  channels(c);try{c.getSystemService(NotificationManager.class).notify(id,base(c,CHANNEL,text).setAutoCancel(true).build());}catch(SecurityException ignored){}
 }
 public static void showStatus(Context c,String text){
  channels(c);if(!new Vault(c).enabled())return;
  try{c.getSystemService(NotificationManager.class).notify(20,base(c,STATE_CHANNEL,text).setOnlyAlertOnce(true).setOngoing(true).setAutoCancel(false).build());}catch(SecurityException ignored){}
 }
 public static void start(Context c){
  Constraints online=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
  PeriodicWorkRequest sync=new PeriodicWorkRequest.Builder(BookingWorker.class,new Vault(c).intervalMinutes(),TimeUnit.MINUTES).setConstraints(online).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES).build();
  WorkManager.getInstance(c).enqueueUniquePeriodicWork("mealguard-booking",ExistingPeriodicWorkPolicy.UPDATE,sync);
  PeriodicWorkRequest guard=new PeriodicWorkRequest.Builder(WatchdogWorker.class,1,TimeUnit.HOURS).build();WorkManager.getInstance(c).enqueueUniquePeriodicWork("mealguard-watchdog",ExistingPeriodicWorkPolicy.UPDATE,guard);
  showStatus(c,"الحجز التلقائي مفعّل.");
 }
 public static void enqueueCheck(Context c){
  if(!new Vault(c).enabled())return;
  OneTimeWorkRequest check=new OneTimeWorkRequest.Builder(BookingWorker.class).setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setInitialDelay(5,TimeUnit.SECONDS).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.MINUTES).build();
  WorkManager.getInstance(c).enqueueUniqueWork("mealguard-online-recheck",ExistingWorkPolicy.KEEP,check);
 }
 /** Every UI path that turns the automation off: stop the schedule, then invalidate the test state when the caller needs it. */
 static void disengage(Context c,Vault v,boolean invalidateTest){v.enable(false);stop(c);if(invalidateTest)v.invalidateTest();}
 public static void stop(Context c){
  WorkManager.getInstance(c).cancelUniqueWork("mealguard-booking");WorkManager.getInstance(c).cancelUniqueWork("mealguard-watchdog");WorkManager.getInstance(c).cancelUniqueWork("mealguard-online-recheck");
  NotificationManager nm=c.getSystemService(NotificationManager.class);nm.cancel(20);nm.cancel(11);nm.cancel(12);
 }
 public static class BookingWorker extends Worker {
  public BookingWorker(Context c,WorkerParameters p){super(c,p);}
  @Override public Result doWork(){
   Context c=getApplicationContext();Vault v=new Vault(c);if(!v.enabled())return Result.success();
   if(!NetworkState.ready(c)){Automation.showStatus(c,"بانتظار الاتصال بالإنترنت.");return Result.retry();}
   if(!SyncPolicy.due(v.lastSuccess(),v.lastFailure(),System.currentTimeMillis(),v.intervalMinutes())){Automation.showStatus(c,"الحجوزات محدثة.");return Result.success();}
   // Read-only diagnostics can overlap, but automatic writes never run concurrently.
   if(Runner.LOCK.isLocked())return Result.retry();
   v.attempt();Automation.showStatus(c,"جارٍ فحص الحجوزات…");
   try{
    String result=Runner.book(c,true,this::isStopped);
    if(v.enabled()){c.getSystemService(NotificationManager.class).cancel(11);c.getSystemService(NotificationManager.class).cancel(12);Automation.notify(c,result,10);Automation.showStatus(c,"حجوزات الأيام الثلاثة مؤكدة.");}
    return Result.success();
   }catch(Exception e){
    if(isStopped()||!v.enabled())return Result.success();
    String msg=Runner.safe(e);v.failure(msg);Automation.showStatus(c,"تعذّر الفحص. ستُعاد المحاولة.");Automation.notify(c,ALERT+msg,11);return Result.retry();
   }
  }
 }
 public static class WatchdogWorker extends Worker {
  public WatchdogWorker(Context c,WorkerParameters p){super(c,p);}
  @Override public Result doWork(){Vault v=new Vault(getApplicationContext());if(v.enabled()&&System.currentTimeMillis()-v.lastSuccess()>ScheduleConfig.staleAfterMillis(v.intervalMinutes()))Automation.notify(getApplicationContext(),"تأخر تأكيد الحجوزات عن الموعد المتوقع. راجع Webetu.",12);return Result.success();}
 }
}
