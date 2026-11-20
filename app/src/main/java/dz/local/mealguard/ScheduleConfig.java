package dz.local.mealguard;
/** Minutes are local user preferences, never sent as an API booking-window override. */
final class ScheduleConfig {
 static final int MIN=15,MAX=2880,DEFAULT=60;
 static final int[] PRESETS={15,30,60,180,360,1440,2880};
 static long staleAfterMillis(int minutes){return Math.max(24*3600000L,minutes*60000L+3600000L);}
 static boolean valid(int m){return m>=MIN&&m<=MAX;}
 static int parse(String raw){
  String s=raw.trim();if(s.isEmpty())throw new IllegalArgumentException("أدخل مدة من 15 إلى 2880 دقيقة.");
  long value=0;for(int i=0;i<s.length();i++){int n=Character.digit(s.charAt(i),10);if(n<0)throw new IllegalArgumentException("استخدم عددًا صحيحًا من الدقائق.");value=value*10+n;if(value>MAX)throw new IllegalArgumentException("الحد الأقصى 2880 دقيقة.");}
  if(!valid((int)value))throw new IllegalArgumentException("الحد الأدنى 15 دقيقة.");return (int)value;
 }
 static String label(int m){if(m==60)return "كل ساعة";if(m==120)return "كل ساعتين";if(m%60==0){int h=m/60;return "كل "+h+(h<=10?" ساعات":" ساعة");}return "كل "+m+" دقيقة";}
}
