#define LOG_TAG "LogicDroid"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "JniException.h"

#include <unistd.h>

// LogicDroid - Kernel System Call
#include <sys/syscall.h>

#include <string.h>
#include <stdlib.h>

#define RELATION_MAX_LENGTH 30

static jint LogicDroid_checkEvent(JNIEnv*, jobject, jint policyID, jint caller, jint target)
{
	//return (jboolean)syscall(376, caller, target);
	return (jint)syscall(361, policyID, caller, target);
}

static jint LogicDroid_initializeMonitor(JNIEnv* mEnv, jobject, jintArray UID) {
	jsize len = mEnv->GetArrayLength(UID);
	jint *bufferUID = mEnv->GetIntArrayElements(UID, 0);
	int argUID[len];
	int i;
	for (i = 0; i < len; i++)
	{
		argUID[i] = (int)bufferUID[i];
	}
	//syscall(377, argUID, len);
	int policyID = syscall(362, argUID, len);
	mEnv->ReleaseIntArrayElements(UID, bufferUID, 0);
        return (jint)policyID;
}

// In this case, we know that there will only be one variable
static jint LogicDroid_modifyStaticVariable(JNIEnv*, jobject, jint policyID, jint UID, jboolean value, jint rel) {
	//syscall(378, rel, (int)UID, (char)value);
	return (jint)syscall(363, policyID, rel, (int)UID, (char)value);
}

static jstring LogicDroid_getRelationName(JNIEnv* mEnv, jobject, jint ID)
{
	char temp[25];
	int res = syscall(364, ID, temp);
	if (res > 0) return mEnv->NewStringUTF(temp);
	return mEnv->NewStringUTF("OUTSIDE-BOUND");
}

static jboolean LogicDroid_isMonitorPresent(JNIEnv*, jobject) {
	return (jboolean)(syscall(365) == 1);
}

static void LogicDroid_registerMonitor(JNIEnv* mEnv, jobject, jstring inputString, jobjectArray relations, jint policyID, jint callRelationID) {

	const char* inputStringToCharArray = mEnv->GetStringUTFChars(inputString, 0);

	int relationSize = mEnv->GetArrayLength(relations);
	char * relationsToCharArray[relationSize];

	for (int i = 0; i < relationSize; i++) {
		jstring relationString = (jstring)mEnv->GetObjectArrayElement(relations, i);
		const char* relationStringToCharArray = mEnv->GetStringUTFChars(relationString, 0);
		relationsToCharArray[i] = new char[RELATION_MAX_LENGTH];
		strcpy(relationsToCharArray[i], relationStringToCharArray);
		mEnv->ReleaseStringUTFChars(relationString, relationStringToCharArray);
		mEnv->DeleteLocalRef(relationString);
	}
	
	syscall(366, inputStringToCharArray, relationsToCharArray, relationSize, policyID, callRelationID);

	mEnv->ReleaseStringUTFChars(inputString, inputStringToCharArray);

	for (int i = 0; i < relationSize; i++)
		delete[] relationsToCharArray[i];
}

static void LogicDroid_unregisterMonitor(JNIEnv*, jobject) {
	syscall(367);
}

static JNINativeMethod gMethods[] = {
	NATIVE_METHOD(LogicDroid, initializeMonitor, "([I)I"),
	NATIVE_METHOD(LogicDroid, modifyStaticVariable, "(IIZI)I"),
	NATIVE_METHOD(LogicDroid, checkEvent, "(III)I"),
	NATIVE_METHOD(LogicDroid, getRelationName, "(I)Ljava/lang/String;"),
	NATIVE_METHOD(LogicDroid, isMonitorPresent, "()Z"),
	NATIVE_METHOD(LogicDroid, registerMonitor, "(Ljava/lang/String;[Ljava/lang/String;II)V"),
	NATIVE_METHOD(LogicDroid, unregisterMonitor, "()V")
};

void register_java_security_LogicDroid(JNIEnv* env) {
    jniRegisterNativeMethods(env, "java/security/LogicDroid", gMethods, NELEM(gMethods));
}
