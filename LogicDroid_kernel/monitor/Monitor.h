#ifndef LOGICDROID_MONITOR_H_INCLUDED
#define LOGICDROID_MONITOR_H_INCLUDED

typedef enum 
{
  OUT_OF_BOUND = -3,
  NO_MONITOR = -2,
  POLICY_MISMATCH = -1,
  OK = 0,
  VIOLATION = 1
} LOGICDROID_RETURN_VALUE;

int LogicDroid_checkEvent(int rel, int *UID, int varCount, long timestamp);
int LogicDroid_renewMonitorVariable(int* UID, int varCount, char value, int rel);
int LogicDroid_initializeMonitor(int *UID, int appCount);
void LogicDroid_registerMonitor(char *inputString, char **relationsDataArray, int relationDataSize, int policyID, int callRelationID);
void LogicDroid_unregisterMonitor(void);
int LogicDroid_getCallRelationID(void);
int LogicDroid_getInternetUID(void);

//***********************************************************************************************************************************************//

typedef struct tHistory {
  char ***propositions;
  char **atoms;
  int **time_tag;
  long timestamp;
} History;

#define MAX_APPLICATION 100000
#define RELATION_MAX_LENGTH 30

#define ROOT_UID 0
#define INTERNET_UID 1100
#define SMS_UID 1101
#define LOCATION_UID 1102
#define CONTACT_UID 1103
#define HANGUP_UID 1104
#define CALL_PRIVILEGED_UID 1105

int LogicDroid_Module_initializeMonitor(int *UID, int appCount);

History* History_Constructor(long timestamp);
void History_Reset(History *h);
void History_insertEvent(History *h, int rel, int idx);
char History_Process(History *next, History *prev);
void History_Dispose(History *h);

//***********************************************************************************************************************************************//
// [POLICY DATA STRUCTURE DECLARATION]
//***********************************************************************************************************************************************//
// (*) The data structure of policy including:
//     - Resource Allocation Table
//     - Dependency Table
//***********************************************************************************************************************************************//

// Resource Allocation Table

typedef struct tAtom{
  //For Atom : type 0 -> static | 1 -> dynamic
  int type;
  int value;
} Atom;

typedef struct tResourceAllocationRecord{
  //type 0 -> allocate | 1 -> mapping
  int type;
  int firstIndex;
  int secondIndex;
  int value;
} ResourceAllocationRecord;

// Dependency Table

typedef struct tVariable{								// This data structure is used to present the Variable (i.e: constant : 0,1,... - variable: x,y,z,...)
  //For Variable : type = 0 -> constant | 1 -> variable
  int type;
  int value;
} Variable;

typedef struct tVariableIndex{								// This data structure is used to present the Variable Index (i.e: [x * app_num + y] or [0], ...)
  int varLength;
  Variable *var;
} VariableIndex;

typedef struct tDependencyTableRecord{							// This is the parameters to pass to policy execution functions
  int id;										// This is the function pointer index : 0 -> 10 : exist_function to since_function
  int freeVarNo;									// Number of kfree variables
  int varIdxLength;
  VariableIndex * varIdx;
  int fixIdxLength;
  int *fixIdx;
} DependencyTableRecord;

int read_policy(char *inputString);
int power(int number);
int freeVarCal(int freeVarNo, int loopNo, VariableIndex varIdx);
void policy_data_cleanup(void);
int read_policy_data(int *currentPos, char *inputString);

#endif
