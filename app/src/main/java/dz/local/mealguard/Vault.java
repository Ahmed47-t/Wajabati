package dz.local.mealguard;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Device-bound AES/GCM. No plaintext fallback, account backup, analytics or credential logging. */
public final class Vault {
 private final SharedPreferences prefs;
 public Vault(Context c) { prefs=c.getSharedPreferences("mealguard_private",Context.MODE_PRIVATE); }
 private SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
  if(!ks.containsAlias("mealguard-aes-v1")) {
   KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
   g.init(new KeyGenParameterSpec.Builder("mealguard-aes-v1",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());g.generateKey();
  }
  return (SecretKey)ks.getKey("mealguard-aes-v1",null);
 }
 public synchronized JSONObject read(String name) throws Exception {
  String raw=prefs.getString(name,null);if(raw==null)return new JSONObject();
  JSONObject box=new JSONObject(raw);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
  c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(box.getString("iv"),Base64.NO_WRAP)));
  c.updateAAD(name.getBytes(StandardCharsets.UTF_8));
  return new JSONObject(new String(c.doFinal(Base64.decode(box.getString("data"),Base64.NO_WRAP)),StandardCharsets.UTF_8));
 }
 public synchronized void write(String name,JSONObject value) throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());c.updateAAD(name.getBytes(StandardCharsets.UTF_8));
  JSONObject box=new JSONObject().put("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)).put("data",Base64.encodeToString(c.doFinal(value.toString().getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP));
  if(!prefs.edit().putString(name,box.toString()).commit())throw new Exception("تعذّر حفظ البيانات محليًا؛ لم يُسمح بإرسال حجز جديد.");
 }
 public synchronized JSONObject account()throws Exception {
  JSONObject c=read("account");
  if(c.has("student")&&c.optInt("meal_schema")!=2){
   enable(false);invalidateTest();JSONObject next=AccountSettings.migrate(c);write("account",next);
   status("تحدّثت إعدادات الوجبات. راجعها قبل التفعيل.");c=next;
  }
  if(c.has("student")&&prefs.contains("journal")){
   String key=AccountSettings.journalKey(c);JSONObject old=read("journal"),scoped=read(key);java.util.Iterator<String> it=old.keys();while(it.hasNext()){String k=it.next();if(!scoped.has(k))scoped.put(k,old.get(k));}
   write(key,scoped);if(!prefs.edit().remove("journal").commit())throw new Exception("تعذّر نقل سجل المحاولات.");
  }return c;
 }
 public boolean accountVerified(){return prefs.getBoolean("account_verified",false);}
 public void accountVerified(boolean value){prefs.edit().putBoolean("account_verified",value).commit();}
 public int intervalMinutes(){int m=prefs.getInt("interval_minutes",ScheduleConfig.DEFAULT);return ScheduleConfig.valid(m)?m:ScheduleConfig.DEFAULT;}
 public void intervalMinutes(int m){if(!ScheduleConfig.valid(m))throw new IllegalArgumentException("مدة غير صالحة.");if(!prefs.edit().putInt("interval_minutes",m).commit())throw new IllegalStateException("تعذّر حفظ المدة.");}
 /** UI language: "ar" (default) or "fr". Chosen in the app, independent of the phone language. */
 public String language(){return "fr".equals(prefs.getString("language","ar"))?"fr":"ar";}
 public void language(String code){prefs.edit().putString("language","fr".equals(code)?"fr":"ar").commit();}
 public int appearance(){int n=prefs.getInt("appearance",Appearance.SYSTEM);return Appearance.valid(n)?n:Appearance.SYSTEM;}
 public void appearance(int n){if(!Appearance.valid(n))throw new IllegalArgumentException("مظهر غير صالح.");if(!prefs.edit().putInt("appearance",n).commit())throw new IllegalStateException("تعذّر حفظ المظهر.");}
 public boolean enabled(){return prefs.getBoolean("enabled",false);}
 public void enable(boolean v){prefs.edit().putBoolean("enabled",v).commit();}
 public long lastAttempt(){return prefs.getLong("last_attempt",0);}
 public long lastFailure(){return prefs.getLong("last_failure",0);}
 public void attempt(){prefs.edit().putLong("last_attempt",System.currentTimeMillis()).commit();}
 public void failure(String s){prefs.edit().putLong("last_failure",System.currentTimeMillis()).putString("status",s).commit();}
 public long lastSuccess(){return prefs.getLong("last_success",0);}
 public void success(String text){prefs.edit().putLong("last_success",System.currentTimeMillis()).putLong("last_failure",0).putString("status",text).putBoolean("tested",true).commit();}
 public boolean setupSeen(){return prefs.getBoolean("setup_seen",false);}
 public void setupSeen(boolean value){prefs.edit().putBoolean("setup_seen",value).apply();}
 public boolean tested(){return prefs.getBoolean("tested",false);}
 public void invalidateTest(){prefs.edit().putBoolean("tested",false).putLong("last_success",0).commit();}
 public void status(String s){prefs.edit().putString("status",s).commit();}
 static final String NO_STATUS="لم يتم الفحص بعد.";
 public String status(){return prefs.getString("status",NO_STATUS);}
 public void clear(){prefs.edit().clear().commit();}
}
