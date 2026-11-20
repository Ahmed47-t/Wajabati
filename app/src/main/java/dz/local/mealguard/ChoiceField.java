package dz.local.mealguard;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Single-choice field that opens a bottom sheet with large rows. Replaces the stock Spinner; keeps its two-method contract (setSelection / getSelectedItemPosition). */
public final class ChoiceField extends Button {
 interface Listener { void selected(int index); }
 private final Appearance.Palette palette;
 private final String title;
 private List<String> items=new ArrayList<>();
 private int selected=-1;
 private Listener listener;
 private Dialog sheet;

 ChoiceField(Context c,Appearance.Palette p,String title){
  super(c);palette=p;this.title=title;
  setAllCaps(false);setTextSize(16);setTextColor(p.ink);setStateListAnimator(null);setElevation(0);setGravity(Gravity.START|Gravity.CENTER_VERTICAL);setTextDirection(c.getResources().getConfiguration().getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_LTR);setCompoundDrawablePadding(dp(8));
  setMinHeight(dp(52));setMinimumHeight(dp(52));setPadding(dp(12),dp(8),dp(12),dp(8));
  GradientDrawable shape=new GradientDrawable();shape.setColor(p.surface);shape.setCornerRadius(dp(10));shape.setStroke(Math.max(2,dp(1)),p.chevron);
  setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(25,128,128,128)),shape,null));
  setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,new ChevronDrawable(getResources().getDisplayMetrics().density,p.muted,true),null);
  setOnClickListener(v->open());
 }
 private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
 void setItems(List<String> names){items=new ArrayList<>(names);selected=items.isEmpty()?-1:0;refresh();}
 void setListener(Listener l){listener=l;}
 /** Same contract as Spinner: out-of-range positions are ignored. The listener is told synchronously. */
 public void setSelection(int index){if(index<0||index>=items.size())return;selected=index;refresh();if(listener!=null)listener.selected(index);}
 public int getSelectedItemPosition(){return selected;}
 public int getCount(){return items.size();}
 Dialog sheet(){return sheet;}
 boolean sheetShowing(){return sheet!=null&&sheet.isShowing();}
 private void refresh(){String value=selected>=0?items.get(selected):"";setText(value);setContentDescription(title.isEmpty()?value:title+": "+value);}
 @Override public void setEnabled(boolean enabled){super.setEnabled(enabled);setAlpha(enabled?1f:.45f);}
 @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Spinner");}

 void open(){
  if(items.isEmpty()||!isEnabled()||sheetShowing())return;
  Context c=getContext();Dialog d=new Dialog(c);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setLayoutDirection(getLayoutDirection());box.setPadding(dp(16),dp(10),dp(16),dp(14));
  GradientDrawable top=new GradientDrawable();top.setColor(palette.surface);float r=dp(22);top.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});box.setBackground(top);
  View grip=new View(c);GradientDrawable g=new GradientDrawable();g.setColor(palette.line);g.setCornerRadius(dp(3));grip.setBackground(g);LinearLayout.LayoutParams gl=new LinearLayout.LayoutParams(dp(40),dp(5));gl.gravity=Gravity.CENTER_HORIZONTAL;gl.setMargins(0,0,0,dp(8));box.addView(grip,gl);
  if(!title.isEmpty()){TextView t=new TextView(c);t.setText(title);t.setTextSize(18);t.setTypeface(null,Typeface.BOLD);t.setTextColor(palette.ink);t.setPadding(dp(6),dp(6),dp(6),dp(8));if(android.os.Build.VERSION.SDK_INT>=28)t.setAccessibilityHeading(true);box.addView(t);}
  LinearLayout list=new LinearLayout(c);list.setOrientation(LinearLayout.VERTICAL);
  for(int i=0;i<items.size();i++){
   final int index=i;boolean on=i==selected;LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(52));row.setPadding(dp(10),dp(8),dp(10),dp(8));
   GradientDrawable rb=new GradientDrawable();rb.setColor(on?palette.background:palette.surface);rb.setCornerRadius(dp(12));row.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(25,128,128,128)),rb,null));
   TextView name=new TextView(c);name.setText(items.get(i));name.setTextSize(16);name.setTextColor(palette.ink);if(on)name.setTypeface(null,Typeface.BOLD);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
   TextView mark=new TextView(c);mark.setText(on?"✓":"");mark.setTextSize(18);mark.setTypeface(null,Typeface.BOLD);mark.setTextColor(palette.accent);mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(mark,new LinearLayout.LayoutParams(dp(28),-2));
   row.setFocusable(true);row.setClickable(true);row.setSelected(on);row.setContentDescription(items.get(i));row.setOnClickListener(v->{d.dismiss();setSelection(index);});
   list.addView(row,new LinearLayout.LayoutParams(-1,-2));
  }
  ScrollView scroll=new ScrollView(c);scroll.addView(list);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  Button cancel=new Button(c);cancel.setText(c.getString(R.string.common_cancel));cancel.setAllCaps(false);cancel.setTextSize(16);cancel.setTextColor(palette.accent);cancel.setStateListAnimator(null);cancel.setMinHeight(dp(50));
  GradientDrawable cb=new GradientDrawable();cb.setColor(palette.surface);cb.setCornerRadius(dp(12));cb.setStroke(Math.max(2,dp(1)),palette.line);cancel.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(25,128,128,128)),cb,null));
  LinearLayout.LayoutParams cl=new LinearLayout.LayoutParams(-1,-2);cl.setMargins(0,dp(10),0,0);box.addView(cancel,cl);cancel.setOnClickListener(v->d.dismiss());
  d.setContentView(box);Window w=d.getWindow();
  if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);w.setWindowAnimations(android.R.style.Animation_InputMethod);}
  sheet=d;d.show();
 }
 List<View> sheetRows(){List<View> out=new ArrayList<>();if(sheet==null)return out;collect(sheet.getWindow().getDecorView(),out);return out;}
 private void collect(View v,List<View> out){if(v instanceof LinearLayout&&v.isClickable()&&v.getContentDescription()!=null)out.add(v);if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++)collect(((android.view.ViewGroup)v).getChildAt(i),out);}
 @Override protected void onDetachedFromWindow(){if(sheetShowing())sheet.dismiss();super.onDetachedFromWindow();}
}
