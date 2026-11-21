package dz.local.mealguard;
import org.junit.Test;
import static org.junit.Assert.*;

/** 0.14.0: the last setup step (battery) is a real prerequisite for the automation, so its rules are unit-tested. */
public class SetupTest {
 @Test public void appStepsAdvanceOnlyWhenThePreviousOneIsReallyDone(){
  assertEquals(0,Setup.done(false,false,false));
  assertEquals(1,Setup.done(true,false,false));
  assertEquals(2,Setup.done(true,true,false));
  assertEquals(3,Setup.done(true,true,true));
  assertEquals("a verified account is useless without restaurants",1,Setup.done(true,false,true));
  assertEquals("no review without a verified account",0,Setup.done(false,true,true));
 }
 @Test public void completionNeedsTheBatteryExemptionToo(){
  assertFalse(Setup.complete(3,false));assertTrue(Setup.complete(3,true));
  assertFalse("battery alone is not setup",Setup.complete(2,true));
 }
 @Test public void progressCountsFourStepsAndNeverOverflows(){
  assertEquals(0,Setup.progress(0,false));assertEquals(2,Setup.progress(1,true));assertEquals(3,Setup.progress(3,false));
  assertEquals(4,Setup.progress(3,true));assertEquals(4,Setup.progress(9,true));
 }
 @Test public void nextStepIsTheFirstThingStillMissing(){
  assertEquals(Setup.ACCOUNT,Setup.next(0,false));assertEquals(Setup.MEALS,Setup.next(1,false));
  assertEquals(Setup.REVIEW,Setup.next(2,false));assertEquals("a finished review still asks for the battery",Setup.BATTERY,Setup.next(3,false));
  assertEquals(Setup.BATTERY,Setup.next(2,true));assertEquals(Setup.STEPS,Setup.next(3,true));
 }
 @Test public void activationIsBlockedWithoutTheExemption(){
  assertFalse(Setup.canActivate(3,false));assertTrue(Setup.canActivate(3,true));
  assertFalse(Setup.canActivate(2,true));
 }
 @Test public void conflictsReplaceThePrimaryActionInsteadOfStackingTwoOfThem(){
  assertTrue(Setup.needsReplacement(true));assertFalse(Setup.needsReplacement(false));
 }
}
