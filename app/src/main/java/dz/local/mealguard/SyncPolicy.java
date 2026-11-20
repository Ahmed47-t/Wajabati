package dz.local.mealguard;
import java.time.*;
/** Pure scheduling rule: a failed attempt must not be suppressed by an older successful sync. */
final class SyncPolicy {
 static boolean due(long success,long failure,long now){
  return due(success,failure,now,ScheduleConfig.DEFAULT);
 }
 static boolean due(long success,long failure,long now,int minutes){
  if(!ScheduleConfig.valid(minutes))return true;
  if(success<=0||failure>=success&&failure>0||now<success)return true;
  boolean sameDay=Instant.ofEpochMilli(success).atZone(Engine.ZONE).toLocalDate().equals(Instant.ofEpochMilli(now).atZone(Engine.ZONE).toLocalDate());
  return !sameDay||now-success>=minutes*60000L;
 }
}
