package dz.local.mealguard;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Small original disclosure indicator for RTL grouped lists. */
final class ChevronDrawable extends Drawable {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final int width,height;private final boolean down;private boolean right;
 /** Points right, for left-to-right layouts. */
 ChevronDrawable right(boolean on){right=on;return this;}
 ChevronDrawable(float density){this(density,Color.rgb(142,142,147));}
 ChevronDrawable(float density,int color){this(density,color,false);}
 ChevronDrawable(float density,int color,boolean down){this.down=down;width=(int)(12*density);height=(int)(18*density);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*density);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
 @Override public void draw(Canvas c){Rect r=getBounds();if(down){float y=r.centerY()-r.height()*.12f,x=r.centerX();Path d=new Path();d.moveTo(x-r.width()*.38f,y);d.lineTo(x,y+r.height()*.25f);d.lineTo(x+r.width()*.38f,y);c.drawPath(d,p);return;}float x=r.left+r.width()*.65f,y=r.centerY();if(right){c.save();c.scale(-1,1,r.centerX(),r.centerY());}Path path=new Path();path.moveTo(x,y-r.height()*.25f);path.lineTo(r.left+r.width()*.3f,y);path.lineTo(x,y+r.height()*.25f);c.drawPath(path,p);if(right)c.restore();}
 @Override public int getIntrinsicWidth(){return width;}
 @Override public int getIntrinsicHeight(){return height;}
 @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
