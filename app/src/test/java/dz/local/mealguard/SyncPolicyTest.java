package dz.local.mealguard;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.*;
public class SyncPolicyTest {
 long at(String s){return LocalDateTime.parse(s).atZone(Engine.ZONE).toInstant().toEpochMilli();}
 @Test public void neverSucceededIsDue(){assertTrue(SyncPolicy.due(0,0,at("2026-11-08T12:00:00")));}
 @Test public void recentSuccessSkipsUnneededReads(){long s=at("2026-11-08T12:00:00");assertFalse(SyncPolicy.due(s,0,s+30*60000));}
 @Test public void laterFailureIsNotHiddenByRecentSuccess(){long s=at("2026-11-08T12:00:00");assertTrue(SyncPolicy.due(s,s+60000,s+10*60000));}
 @Test public void newDayIsDueEvenBeforeOneHour(){assertTrue(SyncPolicy.due(at("2026-11-08T23:50:00"),0,at("2026-11-09T00:01:00")));}
 @Test public void oneHourReachedIsDue(){long s=at("2026-11-08T06:00:00");assertTrue(SyncPolicy.due(s,0,s+3600000));}
 @Test public void justBeforeOneHourIsNotDue(){long s=at("2026-11-08T06:00:00");assertFalse(SyncPolicy.due(s,0,s+3599999));}
 @Test public void twoHoursNeverWaitsForOldSixHourRule(){long s=at("2026-11-08T06:00:00");assertTrue(SyncPolicy.due(s,0,s+2*3600000));}
 @Test public void olderFailureDoesNotOverrideLaterSuccess(){long s=at("2026-11-08T06:00:00");assertFalse(SyncPolicy.due(s,s-60000,s+20*60000));}
 @Test public void backwardClockDoesNotSuppressWorkForever(){long s=at("2026-11-08T06:00:00");assertTrue(SyncPolicy.due(s,0,s-60000));}
}
