package dz.local.mealguard;
final class AboutInfo {
 static final String NAME="TFYCHE AHMED REDOUANE";
 static final String BIO="طالب في ESGEN";
 static final String INSTAGRAM="https://www.instagram.com/hamado___mc/";
 static final String LINKEDIN="https://www.linkedin.com/in/ahmed-redouane-tfyeche-167588336/";
 static boolean allowed(String url){return INSTAGRAM.equals(url)||LINKEDIN.equals(url);}
}
