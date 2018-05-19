#include "emulator.h"

int main( int argc, const char* argv[] ) { 
  printf("Monitor emulator\n");

  int UID[10] = {5000, 5001, 5001, 5003, 5004, 5005, 5006, 5007, 5008, 5009 };

  sys_LogicDroid_initializeMonitor(UID, 10);
  
  char inputString[] ="2|1|2|4|1|2|0|0|0|1|1|2|2|9|7|16|0|0|0|0|0|0|1|1|1|0|2|0|0|0|3|1|1|0|4|1|0|0|5|1|1|0|6|2|0|0|7|1|1|0|8|3|1|1|1|3|0|1|2|2|0|1|3|3|0|1|4|2|1|1|5|0|1|1|6|3|1|1|0|0|009|05|2|2|2|1|0|1|1|2|1|0|1|1|5|1|4|5|0|10|01|3|3|3|1|0|1|1|1|2|2|1|0|1|1|2|1|1|1|2|4|1|3|4|6|00|3|2|2|1|0|1|1|3|1|0|1|2|1|1|3|1|2|3|02|2|3|2|1|0|1|1|2|1|0|1|1|2|1|0|1|1|4|1|0|1|2|03|1|2|1|1|0|1|1|0|3|0|3|4|03|1|2|1|1|0|1|1|0|3|0|5|6|06|1|2|1|1|0|2|1|0|0|4|3|0|7|8|01|1|5|1|1|0|2|1|0|0|1|1|1|0|1|1|0|1|1|0|6|0|1|2|3|5|7|00|1|2|1|0|0|1|1|0|3|0|0|1";

  int policyId = 9;
  int relationSize = 4;
  int callRelationId = 3;
  char *relationsDataArray[10] = {"trans","system","trusted","call","","","","","",""};

  sys_LogicDroid_registerMonitor(inputString, relationsDataArray, relationSize, policyId, callRelationId);

  sys_LogicDroid_checkChain(policyId, 5000, 1103);
  sys_LogicDroid_checkChain(policyId, 1103, 1100);

  sys_LogicDroid_unregisterMonitor();
}
