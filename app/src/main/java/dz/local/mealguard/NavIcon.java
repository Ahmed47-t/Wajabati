package dz.local.mealguard;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Original line icons for the bottom navigation, drawn on a 24-unit grid. No third-party assets. */
final class NavIcon extends Drawable {
 static final int HOME=0,MEALS=1,SETTINGS=2,CALENDAR=3,USER=4,LOCK=5,CUP=6,BOWL=7,MOON=8,BELL=9,CLOCK=10,LOOK=11,GLOBE=12,INFO=13,EYE=14,CLOSE=15,EYE_OFF=16;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int kind,size;
 NavIcon(float density,int kind,int color){this.kind=kind;size=(int)(24*density);p.setColor(color);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 void color(int c){p.setColor(c);invalidateSelf();}
 @Override public void draw(Canvas c){
  Rect r=getBounds();float u=Math.min(r.width(),r.height())/24f;c.save();c.translate(r.left+(r.width()-24*u)/2f,r.top+(r.height()-24*u)/2f);c.scale(u,u);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.9f);Path a=new Path();
  if(kind==HOME){a.moveTo(3.5f,11.5f);a.lineTo(12,4);a.lineTo(20.5f,11.5f);a.moveTo(6,10);a.lineTo(6,20);a.lineTo(18,20);a.lineTo(18,10);a.moveTo(10,20);a.lineTo(10,14.5f);a.lineTo(14,14.5f);a.lineTo(14,20);c.drawPath(a,p);}
  else if(kind==MEALS){a.moveTo(5.5f,4);a.lineTo(5.5f,9);a.quadTo(5.5f,11,8,11);a.quadTo(10.5f,11,10.5f,9);a.lineTo(10.5f,4);a.moveTo(8,4);a.lineTo(8,20);a.moveTo(16,20);a.lineTo(16,4);a.quadTo(19.5f,7,19.5f,13);a.lineTo(16,13);c.drawPath(a,p);}
  else if(kind==CALENDAR){c.drawRoundRect(4,6,20,20,2.5f,2.5f,p);c.drawLine(4,10.5f,20,10.5f,p);c.drawLine(8,3.8f,8,7,p);c.drawLine(16,3.8f,16,7,p);p.setStyle(Paint.Style.FILL);c.drawCircle(8.5f,14.5f,1.1f,p);c.drawCircle(12,14.5f,1.1f,p);c.drawCircle(15.5f,14.5f,1.1f,p);}
  else if(kind==USER){c.drawCircle(12,8.5f,3.6f,p);a.moveTo(5,20);a.quadTo(5,14,12,14);a.quadTo(19,14,19,20);c.drawPath(a,p);}
  else if(kind==LOCK){c.drawRoundRect(6,11,18,20,2.2f,2.2f,p);a.moveTo(8.5f,11);a.lineTo(8.5f,8.5f);a.quadTo(8.5f,4.5f,12,4.5f);a.quadTo(15.5f,4.5f,15.5f,8.5f);a.lineTo(15.5f,11);c.drawPath(a,p);}
  else if(kind==CUP){a.moveTo(5,9.5f);a.lineTo(16,9.5f);a.lineTo(16,14);a.quadTo(16,19.5f,10.5f,19.5f);a.quadTo(5,19.5f,5,14);a.close();a.moveTo(16,11);a.quadTo(20,11,20,13.3f);a.quadTo(20,15.6f,16,15.6f);a.moveTo(8.5f,4);a.lineTo(8.5f,6.5f);a.moveTo(12.5f,4);a.lineTo(12.5f,6.5f);c.drawPath(a,p);}
  else if(kind==BOWL){a.moveTo(4,11.5f);a.lineTo(20,11.5f);a.quadTo(20,19.5f,12,19.5f);a.quadTo(4,19.5f,4,11.5f);a.close();a.moveTo(9,5);a.lineTo(9,8);a.moveTo(12.5f,4);a.lineTo(12.5f,8);a.moveTo(16,5);a.lineTo(16,8);c.drawPath(a,p);}
  else if(kind==MOON){a.moveTo(19,14.5f);a.quadTo(14,16,10.6f,12.6f);a.quadTo(7.6f,9.2f,10,5);a.quadTo(4.2f,7,5,13.2f);a.quadTo(6.6f,19.6f,13,19.5f);a.quadTo(17.6f,19,19,14.5f);a.close();c.drawPath(a,p);}
  else if(kind==BELL){a.moveTo(6,17);a.lineTo(18,17);a.lineTo(16.5f,14.5f);a.lineTo(16.5f,10.5f);a.quadTo(16.5f,6,12,6);a.quadTo(7.5f,6,7.5f,10.5f);a.lineTo(7.5f,14.5f);a.close();a.moveTo(10.5f,19.8f);a.lineTo(13.5f,19.8f);c.drawPath(a,p);}
  else if(kind==CLOCK){c.drawCircle(12,12,8,p);a.moveTo(12,7.5f);a.lineTo(12,12);a.lineTo(15,14);c.drawPath(a,p);}
  else if(kind==LOOK){c.drawCircle(12,12,8,p);p.setStyle(Paint.Style.FILL);c.drawArc(new RectF(4,4,20,20),90,180,true,p);}
  else if(kind==GLOBE){c.drawCircle(12,12,8,p);c.drawLine(4,12,20,12,p);c.drawOval(new RectF(8.5f,4,15.5f,20),p);}
  else if(kind==INFO){c.drawCircle(12,12,8,p);c.drawLine(12,11.2f,12,16,p);p.setStyle(Paint.Style.FILL);c.drawCircle(12,8,1.2f,p);}
  else if(kind==EYE){a.moveTo(3,12);a.quadTo(12,4.5f,21,12);a.quadTo(12,19.5f,3,12);a.close();c.drawPath(a,p);c.drawCircle(12,12,2.7f,p);}
  else if(kind==EYE_OFF){a.moveTo(3,12);a.quadTo(12,4.5f,21,12);a.quadTo(12,19.5f,3,12);a.close();c.drawPath(a,p);c.drawCircle(12,12,2.7f,p);c.drawLine(5,4.5f,19,19.5f,p);}
  else if(kind==CLOSE){c.drawLine(6,6,18,18,p);c.drawLine(18,6,6,18,p);}
  else{for(int i=0;i<3;i++){float y=7+i*5;c.drawLine(4,y,20,y,p);}p.setStyle(Paint.Style.FILL);float[] x={9,15,8};for(int i=0;i<3;i++){c.drawCircle(x[i],7+i*5,2.4f,p);}}
  c.restore();
 }
 @Override public int getIntrinsicWidth(){return size;}
 @Override public int getIntrinsicHeight(){return size;}
 @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
