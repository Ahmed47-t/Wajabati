package dz.local.mealguard;
import android.content.Context;
import android.net.*;
final class NetworkState {
 static boolean ready(Context c){
  ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);
  if(cm==null)return false;Network active=cm.getActiveNetwork();if(active==null)return false;
  NetworkCapabilities n=cm.getNetworkCapabilities(active);
  return n!=null&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
 }
}
