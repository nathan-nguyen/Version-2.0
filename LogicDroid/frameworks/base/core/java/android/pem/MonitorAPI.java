package android.pem;

import java.security.LogicDroid;
import android.content.Context;
import android.Manifest;
import android.os.Binder;
import android.content.pm.PackageManager;

public class MonitorAPI{

  private final Context mContext;
  private final int uid;
  private final String perm;

  public MonitorAPI(Context mContext) {
    this.mContext = mContext;    
    this.perm = Manifest.permission.CHANGE_POLICY_MONITOR;
    this.uid = Binder.getCallingUid();
  }

  public void registerMonitor(String inputString, String[] relations, int policyID, int callRelationID){
    if (mContext.checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED)
        throw new SecurityException("caller uid " + uid + " lacks " + perm);
    LogicDroid.registerMonitor(inputString, relations, policyID, callRelationID);
  }

  public void unregisterMonitor(){
    if (mContext.checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED)
        throw new SecurityException("caller uid " + uid + " lacks " + perm);
    LogicDroid.unregisterMonitor();
  }
}
