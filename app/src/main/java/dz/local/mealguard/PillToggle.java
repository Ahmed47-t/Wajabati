package dz.local.mealguard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;

/** Original pill switch drawing. No Apple assets or fonts are bundled. */
public final class PillToggle extends CompoundButton {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private ValueAnimator animation;
 private float progress=0f;
 private boolean ready=false;
 private int offColor=0xffE5E5EA,onColor=0xff34C759;
 public PillToggle(Context c){super(c);setButtonDrawable(null);setBackgroundColor(Color.TRANSPARENT);setClickable(true);setFocusable(true);setContentDescription(c.getString(R.string.auto_booking));boolean night=Appearance.dark(new Vault(c).appearance(),c.getResources().getConfiguration().uiMode);offColor=night?0xff39393D:0xffE5E5EA;onColor=night?0xff30D158:0xff34C759;ready=true;}
 private float dp(float v){return getResources().getDisplayMetrics().density*v;}
 @Override protected void onMeasure(int w,int h){setMeasuredDimension(resolveSize((int)dp(64),w),resolveSize((int)dp(48),h));}
 @Override public void setChecked(boolean checked){
  super.setChecked(checked);float target=checked?1f:0f;
  if(!ready||!isLaidOut()){progress=target;invalidate();return;}
  if(animation!=null)animation.cancel();animation=ValueAnimator.ofFloat(progress,target);animation.setDuration(180);animation.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});animation.start();
 }
 @Override protected void onDraw(Canvas canvas){
  float x=(getWidth()-dp(52))/2f,y=(getHeight()-dp(32))/2f;
  paint.setColor(mix(offColor,onColor,progress));paint.setAlpha(isEnabled()?255:110);canvas.drawRoundRect(x,y,x+dp(52),y+dp(32),dp(16),dp(16),paint);
  if(progress<1f){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.5f));paint.setColor(0xff8E8E93);paint.setAlpha((int)((isEnabled()?255:110)*(1f-progress)));float h=dp(.75f);canvas.drawRoundRect(x+h,y+h,x+dp(52)-h,y+dp(32)-h,dp(16),dp(16),paint);paint.setStyle(Paint.Style.FILL);}
  float cx=x+dp(16)+dp(20)*progress;paint.setColor(Color.argb(20,0,0,0));canvas.drawCircle(cx,y+dp(17),dp(14.4f),paint);paint.setColor(Color.WHITE);paint.setAlpha(isEnabled()?255:190);canvas.drawCircle(cx,y+dp(16),dp(13.5f),paint);paint.setAlpha(255);
 }
 private int mix(int a,int b,float f){return Color.rgb((int)(Color.red(a)+(Color.red(b)-Color.red(a))*f),(int)(Color.green(a)+(Color.green(b)-Color.green(a))*f),(int)(Color.blue(a)+(Color.blue(b)-Color.blue(a))*f));}
 @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Switch");info.setCheckable(true);info.setChecked(isChecked());}
 @Override protected void onDetachedFromWindow(){if(animation!=null)animation.cancel();super.onDetachedFromWindow();}
}
