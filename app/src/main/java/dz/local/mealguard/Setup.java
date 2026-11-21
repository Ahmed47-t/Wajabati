package dz.local.mealguard;

/**
 * The four setup steps as one pure decision, so the screen and the tests never disagree about what is missing.
 * Steps 1–3 are account, meals and review; step 4 is the battery exemption that keeps the periodic check alive.
 * Battery is a prerequisite for turning the automation on, never a condition for keeping it saved.
 */
final class Setup {
 static final int ACCOUNT=0,MEALS=1,REVIEW=2,BATTERY=3,STEPS=4;
 private Setup(){}
 /** Steps 1–3 only (0–3); battery is reported separately because it lives in the system settings. */
 static int done(boolean verified,boolean configured,boolean tested){return !verified?0:!configured?1:!tested?2:3;}
 /** A finished setup means the three app steps and the battery exemption. */
 static boolean complete(int done,boolean batteryFree){return done>=3&&batteryFree;}
 /** 0–4, used by the progress bar and the "x/4" label. */
 static int progress(int done,boolean batteryFree){return Math.min(STEPS,Math.max(0,done)+(batteryFree?1:0));}
 /** The first step the user still has to do, or {@link #STEPS} when everything is finished. */
 static int next(int done,boolean batteryFree){return complete(done,batteryFree)?STEPS:!batteryFree&&done>=3?BATTERY:Math.min(done,2);}
 /**
  * The last review step offers one action that depends on what was found: a replacement consent when an existing
  * booking has to be cancelled first, otherwise the single booking/activation action.
  */
 static boolean needsReplacement(boolean hasConflicts){return hasConflicts;}
 /** Activation needs the battery exemption; without it the button tells the user to do that step instead. */
 static boolean canActivate(int done,boolean batteryFree){return done>=3&&batteryFree;}
}
