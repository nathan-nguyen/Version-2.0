#include "Monitor.h"
#include<linux/linkage.h> 	// To handle system call interface
#include<linux/kernel.h>
#include<linux/slab.h>		// For memory allocation in kernel
#include<linux/uaccess.h>
#include<linux/time.h> 		// For do_gettimeofday
#include<linux/mutex.h>

static char notInitialized = 1;

static int LogicDroid_CallRelationID = -1;
static int LogicDroid_INTERNET_UID = -1;
static int* localUID = NULL;
static int localAppCount = 0;

// Note
static char** relations;
static int relationSize = 0;
static int kernel_policyID = 0;

static int mapping[MAX_APPLICATION];
static int app_num;
static int currentHist = 0;
DEFINE_MUTEX(lock);

int LogicDroid_getRelationName(int ID, char __user *relationName);

int LogicDroid_getCallRelationID(void) {
  return LogicDroid_CallRelationID;
}

int LogicDroid_getInternetUID(void) {
  return LogicDroid_INTERNET_UID;
}

History **hist = NULL;

// #######################################################################################
// #                            Interface to System Calls                                #
// #######################################################################################
asmlinkage int sys_LogicDroid_checkChain(int policyID, int caller, int target) {
  int UID[2] = {caller, target};
  int result;

  struct timeval tv;
  do_gettimeofday(&tv);

  if (notInitialized)
    return NO_MONITOR;

  if (kernel_policyID != policyID)
    return POLICY_MISMATCH;

  result = LogicDroid_checkEvent(LogicDroid_CallRelationID, UID, 2, (tv.tv_sec));

  return result;
}

asmlinkage int sys_LogicDroid_initializeMonitor(int __user *UID, int count) {
  return LogicDroid_initializeMonitor(UID, count);
}

asmlinkage int sys_LogicDroid_modifyStaticVariable(int policyID, int rel, int UID, char value) {
  if (notInitialized)
    return NO_MONITOR;
  if (kernel_policyID != policyID)
    return POLICY_MISMATCH;
  return LogicDroid_renewMonitorVariable(&UID, 1, value, rel);
}

asmlinkage int sys_LogicDroid_getRelationName(int ID, char __user *relationName) {
  return LogicDroid_getRelationName(ID, relationName);
}

asmlinkage int sys_LogicDroid_isMonitorPresent(void) {
  return !notInitialized;
}

asmlinkage void sys_LogicDroid_registerMonitor(char __user *inputString, char __user **relationsDataArray, int relationsDataSize, int policyID, int callRelationID) {
  LogicDroid_registerMonitor(inputString, relationsDataArray, relationsDataSize, policyID, callRelationID);
}

asmlinkage void sys_LogicDroid_unregisterMonitor(void) {
  LogicDroid_unregisterMonitor();
}
//***********************************************************************************************************************************************//

int LogicDroid_renewMonitorVariable(int *UID, int varCount, char value, int rel) {
  int varIdx = 0;
  int mul = 1;
  int i = 0;

  printk("\nRenew Monitor Variable\n");

  mutex_lock(&lock);
  for (i = varCount - 1; i >= 0; mul *= app_num, i--) {
    varIdx += mapping[UID[i]] * mul;
  }
  hist[currentHist]->atoms[rel][varIdx] = value;
  mutex_unlock(&lock);

  return 0;
} 

int LogicDroid_initializeMonitor(int __user *UID, int appCount) {

  // Copy the UID's
  kfree(localUID);
  localAppCount = appCount;
  localUID = (int*) kmalloc(sizeof(int) * localAppCount, GFP_KERNEL);
  copy_from_user(localUID, UID, sizeof(int) * localAppCount);

  if (notInitialized)
    return NO_MONITOR;

  return LogicDroid_Module_initializeMonitor(UID, appCount);
}

int LogicDroid_checkEvent(int rel, int *UID, int varCount, long timestamp) {
  int varIdx = 0;
  int mul = 1;
  int i = 0;
  char result;

  // Block the direct call from /net/socket.c
  if (notInitialized)
    return NO_MONITOR;

  mutex_lock(&lock);
  History_Reset(hist[!currentHist]);

  currentHist = !currentHist;

  hist[currentHist]->timestamp = timestamp;

  // Update the current dynamic atom
  for (i = varCount - 1; i >= 0; mul *= app_num, i--)
    varIdx += mapping[UID[i]] * mul;

  History_insertEvent(hist[currentHist], rel, varIdx);

  result = History_Process(hist[currentHist], hist[!currentHist]);

  if (varCount == 2)
    printk("\nCall( %d , %d) - Result : %d\n", UID[0], UID[1], result);

  // Revert the history before the event occurred if the event causes a violation of policy
  if (result)
    currentHist = !currentHist;

  mutex_unlock(&lock);

  return result;
}

int LogicDroid_getRelationName(int ID, char __user *relationName) {
  if (ID < 0 || ID >= relationSize)
    return OUT_OF_BOUND;

  if (notInitialized)
    return NO_MONITOR;

  strcpy(relationName, relations[ID]);

  return 1;
}

void LogicDroid_registerMonitor(char __user *inputString, char __user **relationsDataArray, int relationsDataSize, int policyID, int callRelationID) {
  int i;

  // TODO: Consider the memory allocation of inputString
  // 1. Copy to new String
  // 2. Keep the inputString
  // - inputString points to local variable therefore it will be free after finishing executing the function
  // - inputString is not a global variable, it might not need to copy to new string.

  notInitialized = 0;

  printk("\nReset Monitor\n");

  printk(KERN_INFO "\n\nDetaching the policy from the monitor stub in kernel\n");

  // Free relations
  for (i = 0; i < relationSize; i++)
    kfree(relations[i]);
  kfree(relations);

  printk("\ninputString : %s\n", inputString);
  printk(KERN_INFO "Attaching the policy %d\n", policyID);

  printk("\ncallRelationID = %d\n", callRelationID);

  relationSize = relationsDataSize;
  relations = (char**) kmalloc(sizeof(char*) * relationSize, GFP_KERNEL);

  for (i = 0; i < relationSize; i++) {
    relations[i] = (char*) kmalloc(sizeof(char*) * RELATION_MAX_LENGTH, GFP_KERNEL);
    strcpy(relations[i], relationsDataArray[i]);
    printk("\nrelations[%d] = %s\n", i, relations[i]);
  }

  kernel_policyID = policyID;

  if (hist != NULL) {
    History_Dispose(hist[0]);
    History_Dispose(hist[1]);
    hist[0] = NULL;
    hist[1] = NULL;
  }

  policy_data_cleanup();
  read_policy(inputString);

  LogicDroid_CallRelationID = callRelationID;
  LogicDroid_INTERNET_UID = INTERNET_UID;

  if (localAppCount > 0)
    LogicDroid_Module_initializeMonitor(localUID, localAppCount);
}

void LogicDroid_unregisterMonitor() {
  printk("\nUnregister Monitor\n");
  notInitialized = 1;
}
//***********************************************************************************************************************************************//

static int num_temp;
static int time_tag_size;

static char **staticAtoms;
static int numberOfStaticAtoms = 0;							// Number of static atoms
int numberOfAtoms;									// Number of all atoms (static + dynamic)
int numberOfFormulas;									// Number of formulas

int * secondIndexSize;

int numberOfResourceAllocationRecords;							// Number of Resource Allocation Record
ResourceAllocationRecord * resourceAllocationTable;					// Resource Allocation Table is an array of Resource Allocation Records
Atom * atomList;									// List of all atoms

static int dependencyTableRecordNo = 0;							// Number of records in dependency table
DependencyTableRecord * dependencyTable;

// Function pointer (used in History_Process)
int (*functionPointer[11])(History * next, History * prev, int recordIdx);

//***********************************************************************************************************************************************//
// [MAIN MONITOR FUNCTIONS]
//***********************************************************************************************************************************************//
// (*) int LogicDroid_Module_renewMonitorVariable(int *UID, int varCount, char value, int rel)
// (*) int LogicDroid_Module_initializeMonitor(int *UID, int appCount)
// (*) int LogicDroid_Module_checkEvent(int rel, int *UID, int varCount, long timestamp)
// (*) History* History_Constructor(long timestamp)
// (*) void History_Reset(History *h)
// (*) void History_insertEvent(History *h, int rel, int idx)
// (*) void History_Dispose(History *h)
// (*) char History_Process(History *next, History *prev)
//***********************************************************************************************************************************************//

// Initialize the Monitor
int LogicDroid_Module_initializeMonitor(int *UID, int appCount) {
  int appIdx = 8;
  int i, j, k;

  mutex_lock(&lock);
  printk("\n\nInitializing Monitor for %d applications\n", appCount);

  mapping[ROOT_UID] = 0;
  mapping[INTERNET_UID] = 1;
  mapping[SMS_UID] = 2;
  mapping[LOCATION_UID] = 3;
  mapping[CONTACT_UID] = 4;
  mapping[HANGUP_UID] = 5;
  mapping[CALL_PRIVILEGED_UID] = 6;
  mapping[IMEI_UID] = 7;
  app_num = appCount + 8;

  staticAtoms = (char**) kmalloc(sizeof(char*) * numberOfStaticAtoms, GFP_KERNEL);
  for (i = 0; i < numberOfStaticAtoms; i++) {
    //TODO: static atoms size app_num ?
    staticAtoms[i] = (char*) kmalloc(sizeof(char) * app_num, GFP_KERNEL);
    memset(staticAtoms[i], 0, sizeof(char) * app_num);
  }

  for (i = 0; i < appCount; i++)
    mapping[UID[i]] = appIdx++;

  // For the fix UID
  for (i = 0; i < dependencyTableRecordNo; i++){
    for (j = 0; j < dependencyTable[i].varIdxLength; j++){
      for (k = 0; k < dependencyTable[i].varIdx[j].varLength; k++){
	if (dependencyTable[i].varIdx[j].var[k].type == 0 && dependencyTable[i].varIdx[j].var[k].value >= 1000){
	  dependencyTable[i].varIdx[j].var[k].value = mapping[dependencyTable[i].varIdx[j].var[k].value];
	}
      }
    }
  }

  if (hist == NULL) {
    hist = (History**) kmalloc(sizeof(History*) * 2, GFP_KERNEL);
    hist[0] = NULL;
    hist[1] = NULL;
  }

  History_Dispose(hist[0]);
  History_Dispose(hist[1]);

  hist[0] = History_Constructor(0);
  hist[1] = History_Constructor(0);

  History_Reset(hist[0]);
  History_Reset(hist[1]);

  currentHist = 0;
  mutex_unlock(&lock);

  return kernel_policyID;
}

// Initialize the history
History* History_Constructor(long timestamp) {
  int i;

  History *retVal = (History*) kmalloc(sizeof(History), GFP_KERNEL);

  retVal->atoms = (char**) kmalloc(sizeof(char*) * numberOfAtoms, GFP_KERNEL);

  for (i = 0; i < numberOfAtoms; i++) {
    if (atomList[i].type == 0)
      retVal->atoms[i] = staticAtoms[atomList[i].value];
    else
      retVal->atoms[i] = (char*) kmalloc(sizeof(char) * power(atomList[i].value), GFP_KERNEL);
  }

  retVal->propositions = (char***) kmalloc(sizeof(char**) * numberOfFormulas, GFP_KERNEL);

  for (i = 0; i < numberOfFormulas; i++) {
    retVal->propositions[i] = (char**) kmalloc(sizeof(char*) * secondIndexSize[i], GFP_KERNEL);
  }

  for (i = 0; i < numberOfResourceAllocationRecords; i++) {
    if (resourceAllocationTable[i].type == 0)
      retVal->propositions[resourceAllocationTable[i].firstIndex][resourceAllocationTable[i].secondIndex] = (char*) kmalloc(sizeof(char) * power(resourceAllocationTable[i].value), GFP_KERNEL);
    else
      retVal->propositions[resourceAllocationTable[i].firstIndex][resourceAllocationTable[i].secondIndex] = retVal->atoms[resourceAllocationTable[i].value];
  }

  if (num_temp > 0) {
    retVal->time_tag = (int**) kmalloc(sizeof(int*) * num_temp, GFP_KERNEL);
    for (i = 0; i < num_temp; i++)
      retVal->time_tag[i] = (int*) kmalloc(sizeof(int) * power(time_tag_size), GFP_KERNEL) ;
  }
  retVal->timestamp = timestamp;

  return retVal;
}

void History_Reset(History *h) {
  int i;

  for (i = 0; i < numberOfAtoms; i++){
    if (atomList[i].type == 1)
      memset(h->atoms[i], 0, sizeof(char) * power(atomList[i].value));
  }

  for (i = 0; i < numberOfResourceAllocationRecords; i++) {
    if (resourceAllocationTable[i].type == 0)
      memset(h->propositions[resourceAllocationTable[i].firstIndex][resourceAllocationTable[i].secondIndex], 0, power(resourceAllocationTable[i].value));
  }

  for (i = 0 ; i < num_temp; i++)
    memset(h->time_tag[i], 0, sizeof(int) * power(time_tag_size));
}

void History_insertEvent(History *h, int rel, int idx) {
  h->atoms[rel][idx] = 1;
}

// Additional function to clean up garbage
void History_Dispose(History *h) {
  int i,j;

  if (h == NULL) return;

  for (i = 0; i < numberOfResourceAllocationRecords; i++) {
    if (resourceAllocationTable[i].type == 1)
      h->propositions[resourceAllocationTable[i].firstIndex][resourceAllocationTable[i].secondIndex] = NULL;
  }

  // Clean propositions
  for (i = 0; i < numberOfFormulas; i++) {
    for (j = 0; j < secondIndexSize[i]; j++)
      kfree(h->propositions[i][j]);
    kfree(h->propositions[i]);
  }
  kfree(h->propositions);

  for (i = 0; i < numberOfAtoms; i++){
    if (atomList[i].type == 0)
      h->atoms[i] = NULL;
  }

  // Clean atoms
  for (i = 0; i < numberOfAtoms; i++) {
    kfree(h->atoms[i]);
  }
  kfree(h->atoms);

  if (num_temp > 0) {
    // Clean temporal metric
    for (i = 0; i < num_temp; i++)
      kfree(h->time_tag[i]);
    kfree(h->time_tag);
  }

  // Finally kfree the history reference
  kfree(h);
}

// This function is used to execute the policy
char History_Process(History *next, History *prev) {
  int recordIdx;

  for (recordIdx = 0; recordIdx < dependencyTableRecordNo; recordIdx++) { 
    (*functionPointer[dependencyTable[recordIdx].id])(next, prev, recordIdx);
  }
  return next->propositions[0][0][0];
}

void policy_data_cleanup(void) {
  int i, j;

  for (i = 0; i < numberOfStaticAtoms; i++) {
    kfree(staticAtoms[i]);
  }
  kfree(staticAtoms);

  //kfree Dependency Table and Resource Allocation Table

  for (i = 0; i < dependencyTableRecordNo; i++) {
    kfree(dependencyTable[i].fixIdx);  
    for (j = 0 ; j < dependencyTable[i].varIdxLength; j ++)
      kfree(dependencyTable[i].varIdx[j].var);
    kfree(dependencyTable[i].varIdx);
  }
  kfree(dependencyTable);

  kfree(resourceAllocationTable);
  kfree(secondIndexSize);
  kfree(atomList);
}

//***********************************************************************************************************************************************//
// [POLICY EXECUTION FUNCTIONS]
//***********************************************************************************************************************************************//
// (*) int exist_function(History *next, History *prev, int recordIdx)
// (*) int and_function(History *next, History *prev, int recordIdx)
// (*) int or_function(History *next, History *prev, int recordIdx)
// (*) inr not_function(History *next, History *prev, int recordIdx)
// (*) int forall_function(History *next, History *prev, int recordIdx)
// (*) int diamond_dot_metric_function(History *next, History *prev, int recordIdx)
// (*) int diamond_dot_function(History *next, History *prev, int recordIdx)
// (*) int diamond_metric_function(History *next, History *prev, int recordIdx)
// (*) int diamond_function(History *next, History *prev, int recordIdx)
// (*) int since_metric_function(History *next, History *prev, int recordIdx)
// (*) int since_function(History *next, History *prev, int recordIdx)
// --
// (*) The above functions are used inside History_Process()
//***********************************************************************************************************************************************//

// EXIST - ID = 0 - Need optimization
int exist_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo, currIdx, subIdx, mainLoopsNo, i, firstCalIdx;

  if (freeVarNo < 1)
    return 0;

  policyNo = dependencyTable[recordIdx].fixIdx[0];
  currIdx  = dependencyTable[recordIdx].fixIdx[1];
  subIdx   = dependencyTable[recordIdx].fixIdx[2];
  mainLoopsNo = power(freeVarNo);

  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] || next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])];

    //if (next->propositions[policyNo][currIdx][firstCalIdx])
    //  return 0;
  }
  return 0;
}

// AND - ID = 1
int and_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo = dependencyTable[recordIdx].fixIdx[0];
  int currIdx  = dependencyTable[recordIdx].fixIdx[1];
  int mainLoopsNo = power(freeVarNo);
  int i, j, subIdx, firstCalIdx;

  for (i = 0; i < mainLoopsNo; i++)
    next->propositions[policyNo][currIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0])] = next->propositions[policyNo][dependencyTable[recordIdx].fixIdx[2]][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])];

  for (i = 2; i < dependencyTable[recordIdx].varIdxLength; i++){
    subIdx = dependencyTable[recordIdx].fixIdx[i+1];
    for (j = 0; j < mainLoopsNo; j++){
      firstCalIdx = freeVarCal(freeVarNo, j, dependencyTable[recordIdx].varIdx[0]);
      next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] && next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, j, dependencyTable[recordIdx].varIdx[i])];
    }
  }
  return 0;
}

// OR - ID = 2
int or_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo = dependencyTable[recordIdx].fixIdx[0];
  int currIdx  = dependencyTable[recordIdx].fixIdx[1];
  int mainLoopsNo = power(freeVarNo);
  int i, j, subIdx, firstCalIdx;
  for (i = 0; i < mainLoopsNo; i++)
    next->propositions[policyNo][currIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0])] = next->propositions[policyNo][dependencyTable[recordIdx].fixIdx[2]][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])];
  for (i = 2; i < dependencyTable[recordIdx].varIdxLength; i++){
    subIdx = dependencyTable[recordIdx].fixIdx[i+1];
    for (j = 0; j < mainLoopsNo; j++){
      firstCalIdx = freeVarCal(freeVarNo, j, dependencyTable[recordIdx].varIdx[0]);
      next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] || next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, j, dependencyTable[recordIdx].varIdx[i])];
    }
  }
  return 0;
}

// NOT - ID = 3
int not_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo = dependencyTable[recordIdx].fixIdx[0];
  int currIdx  = dependencyTable[recordIdx].fixIdx[1];
  int subIdx   = dependencyTable[recordIdx].fixIdx[2];
  int mainLoopsNo = power(freeVarNo);
  int i;
  for (i = 0; i < mainLoopsNo; i ++)
    next->propositions[policyNo][currIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0])] = !next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])];
  return 0;
}

// FOR ALL - ID = 4 - Need Optimization
int forall_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo, currIdx, subIdx, mainLoopsNo, i, firstCalIdx;
  if (freeVarNo < 1)
    return 0;
  policyNo = dependencyTable[recordIdx].fixIdx[0];
  currIdx  = dependencyTable[recordIdx].fixIdx[1];
  subIdx   = dependencyTable[recordIdx].fixIdx[2];
  mainLoopsNo = power(freeVarNo);

  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] && next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])];
    //if (!next->propositions[policyNo][currIdx][firstCalIdx])
    //  return 0;
  }
  return 0;
}

// DIAMOND DOT with METRIC - ID = 5
int diamond_dot_metric_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo    = dependencyTable[recordIdx].fixIdx[0];
  int currIdx     = dependencyTable[recordIdx].fixIdx[1];
  int subIdx      = dependencyTable[recordIdx].fixIdx[2];
  int timetagIdx  = dependencyTable[recordIdx].fixIdx[3];
  int metric 	  = dependencyTable[recordIdx].fixIdx[4];
  int mainLoopsNo = power(freeVarNo);
  int i,firstCalIdx;
  int delta = next->timestamp - prev->timestamp;
  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    next->propositions[policyNo][currIdx][firstCalIdx] = 0;
    next->time_tag[timetagIdx][firstCalIdx] = 0;
    if (delta <= metric) {
      next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])];
      next->time_tag[timetagIdx][firstCalIdx] = (int) delta;
      if (!next->propositions[policyNo][currIdx][firstCalIdx] && prev->time_tag[timetagIdx][firstCalIdx] + delta <= metric) {
        next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][currIdx][firstCalIdx];
        if (next->propositions[policyNo][currIdx][firstCalIdx])
          next->time_tag[timetagIdx][firstCalIdx] += prev->time_tag[timetagIdx][firstCalIdx];
        }
    }
  }
  return 0;
}

// DIAMOND DOT without METRIC - ID = 6
int diamond_dot_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo    = dependencyTable[recordIdx].fixIdx[0];
  int currIdx     = dependencyTable[recordIdx].fixIdx[1];
  int subIdx      = dependencyTable[recordIdx].fixIdx[2];
  int mainLoopsNo = power(freeVarNo);
  int i,firstCalIdx;
  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])] || prev->propositions[policyNo][currIdx][firstCalIdx];
  }
  return 0;
}

// DIAMOND with METRIC - ID = 7
int diamond_metric_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo    = dependencyTable[recordIdx].fixIdx[0];
  int currIdx     = dependencyTable[recordIdx].fixIdx[1];
  int subIdx      = dependencyTable[recordIdx].fixIdx[2];
  int timetagIdx  = dependencyTable[recordIdx].fixIdx[3];
  int metric 	  = dependencyTable[recordIdx].fixIdx[4];
  int mainLoopsNo = power(freeVarNo);
  int i,firstCalIdx;
  int delta = next->timestamp - prev->timestamp;
  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])];
    next->time_tag[timetagIdx][firstCalIdx] = 0;
    if (!next->propositions[policyNo][currIdx][firstCalIdx] && prev->time_tag[timetagIdx][firstCalIdx] + delta <= metric) {
      next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][currIdx][firstCalIdx];
      next->time_tag[timetagIdx][firstCalIdx] = delta + prev->time_tag[timetagIdx][firstCalIdx];
    }
  }
  return 0;
}

// DIAMOND without METRIC - ID = 8
int diamond_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo    = dependencyTable[recordIdx].fixIdx[0];
  int currIdx     = dependencyTable[recordIdx].fixIdx[1];
  int subIdx      = dependencyTable[recordIdx].fixIdx[2];
  int mainLoopsNo = power(freeVarNo);
  int i,firstCalIdx;
  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])] || prev->propositions[policyNo][currIdx][firstCalIdx];
  }
  return 0;
}

// SINCE with METRIC - ID = 9
int since_metric_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo	   = dependencyTable[recordIdx].fixIdx[0];
  int currIdx	   = dependencyTable[recordIdx].fixIdx[1];
  int firstSubIdx  = dependencyTable[recordIdx].fixIdx[2];
  int timetagIdx   = dependencyTable[recordIdx].fixIdx[3];
  int metric	   = dependencyTable[recordIdx].fixIdx[4];
  int secondSubIdx = dependencyTable[recordIdx].fixIdx[5];
  int mainLoopsNo = power(freeVarNo);
  int i, firstCalIdx, thirdCalIdx;
  int delta = next->timestamp - prev->timestamp;
  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    thirdCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[2]);
    next->time_tag[timetagIdx][firstCalIdx] = 0;
    if (delta <= metric) {
      next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][secondSubIdx][thirdCalIdx] || (next->propositions[policyNo][firstSubIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])] && prev->propositions[policyNo][currIdx][firstCalIdx]);
      if (!next->propositions[policyNo][secondSubIdx][thirdCalIdx])
        next->time_tag[timetagIdx][firstCalIdx] = delta + prev->time_tag[timetagIdx][firstCalIdx];
    }
    else
      next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][secondSubIdx][thirdCalIdx];
  }
  return 0;
}

// SINCE without METRIC - ID = 10
int since_function(History *next, History *prev, int recordIdx){
  int freeVarNo = dependencyTable[recordIdx].freeVarNo;
  int policyNo     = dependencyTable[recordIdx].fixIdx[0];
  int currIdx      = dependencyTable[recordIdx].fixIdx[1];
  int firstSubIdx  = dependencyTable[recordIdx].fixIdx[2];
  int secondSubIdx = dependencyTable[recordIdx].fixIdx[3];
  int mainLoopsNo = power(freeVarNo);
  int i,firstCalIdx;
  for (i = 0; i < mainLoopsNo; i ++){
    firstCalIdx = freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[0]);
    next->propositions[policyNo][currIdx][firstCalIdx] = (next->propositions[policyNo][firstSubIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[1])] && prev->propositions[policyNo][currIdx][firstCalIdx]) || next->propositions[policyNo][secondSubIdx][freeVarCal(freeVarNo, i, dependencyTable[recordIdx].varIdx[2])];
  }
  return 0;
}

//***********************************************************************************************************************************************//
// (*) int freeVarCal(int freeVarNo, int loopNo, VariableIndex varIdx)
// (*) int power(int number)
// --
// (*) These two functions are mathematics functions used inside policy execution functions
//***********************************************************************************************************************************************//

// Calculate the value of firstIndex, secondIndex
int freeVarCal(int freeVarNo, int loopNo, VariableIndex varIdx){
  int i;
  int value = 0;
  int kfreeVarCurrValue[freeVarNo]; 
  for (i = freeVarNo - 1; i >= 0; i--) {
    kfreeVarCurrValue[i] = loopNo % app_num;
    loopNo = loopNo/app_num;
  }  
  for (i = 0; i < varIdx.varLength; i++) {
    if (varIdx.var[i].type)
      value = value * app_num + kfreeVarCurrValue[varIdx.var[i].value];
    else
      value = value * app_num + varIdx.var[i].value;
  }
  return value;
}

// Calculate the value of pow(app_num, number)
int power(int number){
  int result = 1;
  int i;

  if (number < 0)
    return 0;

  for (i = 0; i < number; i++)
    result *= app_num;

  return result;
}

//***********************************************************************************************************************************************//
// [READ POLICY FUNCION]
//***********************************************************************************************************************************************//
// (*) This function is used to read the string from policy file and initialize variables for policy execution
//***********************************************************************************************************************************************//

int read_policy(char *inputString){
  // [START READING THE POLICY]

  int i, j, k, varIdxLength, fixIdxLength, varLength;

  int currentPos = 0;

  // Start reading the resources allocation table

  numberOfStaticAtoms = read_policy_data(&currentPos, inputString);
  num_temp = read_policy_data(&currentPos, inputString);
  time_tag_size = read_policy_data(&currentPos, inputString);
  numberOfAtoms = read_policy_data(&currentPos, inputString);

  atomList = (Atom*) kmalloc(sizeof(Atom) * numberOfAtoms, GFP_KERNEL);
  for (i = 0; i < numberOfAtoms; i++) {
    atomList[i].type = read_policy_data(&currentPos, inputString);
    // atomList[i].value is the varCount for dynamic atom
    atomList[i].value = read_policy_data(&currentPos, inputString);
  }

  numberOfFormulas = read_policy_data(&currentPos, inputString);
  secondIndexSize = (int*) kmalloc (sizeof(int) * numberOfFormulas, GFP_KERNEL);

  for (i = 0; i < numberOfFormulas; i++)
    secondIndexSize[i] = read_policy_data(&currentPos, inputString);

  numberOfResourceAllocationRecords = read_policy_data(&currentPos, inputString);
  resourceAllocationTable = (ResourceAllocationRecord*) kmalloc(sizeof(ResourceAllocationRecord)* numberOfResourceAllocationRecords, GFP_KERNEL);
  
  for (i = 0; i < numberOfResourceAllocationRecords; i++) {
    resourceAllocationTable[i].type = read_policy_data(&currentPos, inputString);
    resourceAllocationTable[i].firstIndex = read_policy_data(&currentPos, inputString);
    resourceAllocationTable[i].secondIndex = read_policy_data(&currentPos, inputString);
    // resourceAllocationTable[i].value is the varCount for allocation type
    // and is atom index for mapping type
    resourceAllocationTable[i].value = read_policy_data(&currentPos, inputString);
  }

  // Start reading the dependency table

  dependencyTableRecordNo = read_policy_data(&currentPos, inputString);
  dependencyTable = (DependencyTableRecord*) kmalloc (sizeof(DependencyTableRecord) * dependencyTableRecordNo, GFP_KERNEL);
  for (i = 0; i < dependencyTableRecordNo; i++){
    dependencyTable[i].id = read_policy_data(&currentPos, inputString);
    dependencyTable[i].freeVarNo = read_policy_data(&currentPos, inputString);
    varIdxLength = read_policy_data(&currentPos, inputString);
    dependencyTable[i].varIdxLength = varIdxLength;
    dependencyTable[i].varIdx = (VariableIndex*) kmalloc (sizeof(VariableIndex) * varIdxLength, GFP_KERNEL);
    for (j = 0; j < varIdxLength; j++){
      varLength = read_policy_data(&currentPos, inputString);
      dependencyTable[i].varIdx[j].varLength = varLength;
      dependencyTable[i].varIdx[j].var = (Variable*) kmalloc(sizeof(Variable)* varLength, GFP_KERNEL);
      for (k = 0; k < varLength; k++){
        dependencyTable[i].varIdx[j].var[k].type = read_policy_data(&currentPos, inputString);
        dependencyTable[i].varIdx[j].var[k].value = read_policy_data(&currentPos, inputString);
      }
    }
    fixIdxLength = read_policy_data(&currentPos, inputString);
    dependencyTable[i].fixIdxLength = fixIdxLength;
    dependencyTable[i].fixIdx = (int*) kmalloc (sizeof(int) * fixIdxLength, GFP_KERNEL);
    for (j = 0; j < fixIdxLength; j++)
      dependencyTable[i].fixIdx[j] = read_policy_data(&currentPos, inputString);
  }


  // Function pointer using in History_Process
  functionPointer[0]  = exist_function;
  functionPointer[1]  = and_function;
  functionPointer[2]  = or_function;
  functionPointer[3]  = not_function;
  functionPointer[4]  = forall_function;
  functionPointer[5]  = diamond_dot_metric_function;
  functionPointer[6]  = diamond_dot_function;
  functionPointer[7]  = diamond_metric_function;
  functionPointer[8]  = diamond_function;
  functionPointer[9]  = since_metric_function;
  functionPointer[10] = since_function;
  return 0;
}

int read_policy_data(int *currentPos, char *inputString) {
  int result = 0;

  if (inputString[*currentPos] == '|')
    (*currentPos)++;

  if (inputString[*currentPos] == '\0' || inputString[*currentPos] < 48 || inputString[*currentPos] > 57)
    return -1;

  while (inputString[*currentPos] != '\0' && inputString[*currentPos] != '|') {
    result = result * 10 + inputString[*currentPos] - 48 ;
    (*currentPos)++;
  }

  return result;
}
