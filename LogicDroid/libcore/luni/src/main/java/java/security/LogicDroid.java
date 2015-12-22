package java.security;

public final class LogicDroid
{
	public static native int initializeMonitor(int[] UID);
	public static native int modifyStaticVariable(int policyID, int UID, boolean value, int rel);
	public static native int checkEvent(int policyID, int caller, int target);
	public static native String getRelationName(int ID);
	public static native boolean isMonitorPresent();
	public static native void registerMonitor(String inputString, String[] relations, int policyId, int callRelationID);
	public static native void unregisterMonitor();
}
