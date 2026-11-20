package dz.local.mealguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppearanceTest {
 @Test public void explicitModesOverrideSystem(){assertTrue(Appearance.dark(Appearance.DARK,0x10));assertFalse(Appearance.dark(Appearance.LIGHT,0x20));}
 @Test public void systemModeFollowsNightFlag(){assertTrue(Appearance.dark(Appearance.SYSTEM,0x20));assertFalse(Appearance.dark(Appearance.SYSTEM,0x10));}
 @Test public void validatesModesAndSocialUrls(){assertTrue(Appearance.valid(0));assertTrue(Appearance.valid(2));assertFalse(Appearance.valid(3));assertTrue(AboutInfo.allowed(AboutInfo.INSTAGRAM));assertTrue(AboutInfo.allowed(AboutInfo.LINKEDIN));assertFalse(AboutInfo.allowed("https://example.com"));}
 private double channel(int n){double c=n/255.0;return c<=0.04045?c/12.92:Math.pow((c+0.055)/1.055,2.4);}
 private double lum(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
 private double contrast(int a,int b){double x=lum(a),y=lum(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 @Test public void primaryTextAndWarningsHaveReadableContrast(){for(boolean dark:new boolean[]{false,true}){Appearance.Palette p=new Appearance.Palette(dark);assertTrue(contrast(p.ink,p.background)>=4.5);assertTrue(contrast(p.muted,p.surface)>=4.5);assertTrue(contrast(0xffffffff,p.primary)>=4.5);assertTrue(contrast(p.warningInk,p.warningBg)>=4.5);}}
}
