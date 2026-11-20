package dz.local.mealguard;
/** Explicit light/dark palettes, with system mode as the default. */
final class Appearance {
 static final int SYSTEM=0,LIGHT=1,DARK=2;
 static boolean valid(int mode){return mode>=SYSTEM&&mode<=DARK;}
 static boolean dark(int mode,int systemUiMode){return mode==DARK||(mode==SYSTEM&&(systemUiMode&0x30)==0x20);}
 static String label(int mode){return mode==DARK?"داكن":mode==LIGHT?"فاتح":"حسب النظام";}
 static final class Palette {
  final int background,surface,ink,muted,accent,primary,segment,selected,warningBg,warningInk,danger,hint,chevron,line,successBg,successInk,dangerBg;
  Palette(boolean dark){
   background=dark?0xff000000:0xffF2F2F7;surface=dark?0xff1C1C1E:0xffFFFFFF;
   ink=dark?0xffF5F5F7:0xff1C1C1E;muted=dark?0xffA1A1AA:0xff6C6C73;
   accent=dark?0xff64A8FF:0xff0064D8;primary=dark?0xff0A63D0:0xff0066DD;
   segment=dark?0xff2C2C2E:0xffE3E3E8;selected=dark?0xff636366:0xffFFFFFF;
   warningBg=dark?0xff342812:0xffFFF6E2;warningInk=dark?0xffFFD27D:0xff875600;
   danger=dark?0xffFF8279:0xffAD362B;hint=muted;chevron=dark?0xff8E8E93:0xff8E8E93;
   line=dark?0xff48484A:0xffC7C7CC;successBg=dark?0xff12301C:0xffE4F5E9;successInk=dark?0xff7EE29A:0xff17662B;dangerBg=dark?0xff3A1A17:0xffFBE9E7;
  }
 }
}
