/*
    PolicyMonitoring is a program to generate monitor module in LogicDroid specified in XML
    Copyright (C) 2012-2013  Hendra Gunadi

    This file is part of PolicyMonitoring

    PolicyMonitoring is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

// The new monitoring targets to create a kernel space monitor

import java.io.*;
import java.util.*;

public class Monitoring {
	private static String[] sizeArr = new String[11];
	private static String[] indent = new String[10];
	private static int startVirtualUID = 1100;
	private static int idx;
	private static int timetag_idx;
	
	static class C_UID // this class is to address the problem of mapping using standard C which doesn't have class mapping
	{
		public int virtualUID;
		public int tempUID;
		public int localUID;
		
		public C_UID()
		{
			virtualUID = -1;
			tempUID = -1;
			localUID = -1;
		}
		
		public C_UID(int virtualUID, int tempUID, int localUID)
		{
			this.virtualUID = virtualUID;
			this.tempUID = tempUID;
			this.localUID = localUID;
		}
	}
	
	// for the purpose of initialising atoms
	private static HashMap<String, Integer> mapping;
	private static ArrayList<String> policyObjects;
	private static int policyObjectCount;
	private static HashMap<String, C_UID> ObjectMapping; // to map an explicit object to a temp UID
	
	public static int getCUID_localUID(String var)
	{
		return ObjectMapping.get(var).localUID;
	}
	
	public static void set_virtual_UID(Policy policy_list) {
		policyObjects = new ArrayList<String>();
		policyObjectCount = 0;//policy_list.objectCount;
		/* maintain consistency of virtual UID
		 * for (int i = 0; i < policy_list.objectCount; i++)
		{
			policyObjects.add(policy_list.objects.get(i));
		}*/
		/*
		 * These needs to be updated every time, at the moment we only need to hook with INTERNET and SMS
		 * 06 May 2013 : added object of LOCATION and CONTACT to address GPS location and the state whether something accessed contact database
		 * It's because to test on the sink of SMS and Internet, we already place a fixed reference which needs to exists
		 */
		policyObjects.add("internet");
		policyObjectCount++;
		policyObjects.add("sms");
		policyObjectCount++;
		policyObjects.add("location");
		policyObjectCount++;
		policyObjects.add("contact");
		policyObjectCount++;
		policyObjects.add("hangup");
		policyObjectCount++;
		policyObjects.add("call_privileged");
		policyObjectCount++;
		for (int i = 0; i < policy_list.objectCount; i++) {
			// Object with fix localAppUID should not be added
			try{
				Integer.parseInt(policy_list.objects.get(i));
				continue;
			}
			catch (NumberFormatException e){
				// Not object:fixLocalAppUID
			}
			if (!policyObjects.contains(policy_list.objects.get(i).toLowerCase())) 
			{
				policyObjects.add(policy_list.objects.get(i));
				policyObjectCount++;
			}
		}
		
		ObjectMapping = new HashMap<String, C_UID>();
		for (int i = 0; i < policyObjectCount; i++)
		{
			ObjectMapping.put(policyObjects.get(i), new C_UID(i + startVirtualUID, i + 3, i + 1)); 
			// map objects to an application ID : virtual UID starting from 1100
		}
	}

	public static void generate_kernel_monitor(Policy policy_list, String fileName)
	{
		// Assume that we are given the root formula as the first policy in the list
		// the file name is going to be appended with .java later
		StringBuilder indentSb = new StringBuilder();
		
		/* 
		 * sizeArr is a pre-initialized size indices for the arrays, so for example :
		 *   - sizeArr[0] = "app_num";
		 *   - sizeArr[1] = "app_num * app_num";
		 *   - sizeArr[2] = "app_num * app_num * app_num";
		 *   
		 * This is due to the observation that all the variables depends on the domain which in this context is the number of applications
		 */
		StringBuilder sizeSb = new StringBuilder("app_num");
		sizeArr[0] = "";
		for (int i = 0; i < indent.length; i++)
		{
			indent[i] = indentSb.toString();
			sizeArr[i + 1] = sizeSb.toString();
			indentSb.append("  ");
			sizeSb.append(" * app_num");
		}
		
		try
		{
			FileOutputStream stream = new FileOutputStream(fileName + ".c");
			PrintStream ps = new PrintStream(stream);
			
			int num_temp = policy_list.tempCount;
			ArrayList<Integer> metric = policy_list.metrics;
			
			//String permissionString = ""; // TODO : have to know how to get the permission list from the android
			
			mapping = new HashMap<String, Integer>();
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				mapping.put(policy_list.relations.get(i), i);
			}
			
			// This metricStr is the metric for each temporal operators
			StringBuilder metricStr = new StringBuilder();
			if (metric.size() > 0) metricStr.append(metric.get(0));
			for (int i = 1; i < metric.size(); i++)
			{
				metricStr.append(", " + metric.get(i));
			}
			
			//ps.println(indent[0] + "#include \"Monitor.h\"");
			ps.println(indent[0] + "#include <stdio.h>");
			ps.println(indent[0] + "#include <stdlib.h>");
			ps.println(indent[0] + "#include <string.h>");
			ps.println();
			ps.println(indent[0] + "#define MAX_APPLICATION 100000");
			ps.println();
			ps.println(indent[0] + "typedef struct tHistory {");
			ps.println(indent[1] + "char ***propositions;");
			ps.println(indent[1] + "char **atoms;");
			ps.println(indent[1] + "int **time_tag;");
			ps.println(indent[1] + "long timestamp;");
			ps.println(indent[0] + "} History;");
			ps.println();
			ps.println(indent[0] + "#define ROOT_UID 0");
			// fixed mappings for static object from real UID to tempUID e.g. : case 1100 : return 3;
			for (int i = 0; i < policyObjectCount; i++)
			{
				C_UID temp = ObjectMapping.get(policyObjects.get(i));
				ps.println(indent[0] + "#define " + policyObjects.get(i).toUpperCase() + "_UID " + temp.virtualUID + "");
			}
			ps.println();
			ps.println(indent[0] + "static char notInitialized = 1;");
			ps.println(indent[0] + "static int mapping[MAX_APPLICATION];");
			
			// Handles static variables declaration
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[0] + "static char *" + policy_list.relations.get(i) + ";");
				}
			}
			ps.println(indent[0] + "static char** relations;");
			ps.println(indent[0] + "static int relationSize = 0;");
			ps.println(indent[0] + "static int app_num;");
			ps.println(indent[0] + "static int module_policyID = " + policy_list.ID + ";");
			// ##########################################################
			// #                   See change log c                     #
			// ##########################################################			
			//ps.println(indent[0] + "static int LogicDroid_CallRelationID = -1;");
			//ps.println(indent[0] + "static int LogicDroid_INTERNET_UID = -1;");
			// ##########################################################
			ps.println();
			if (num_temp > 0)
			{
				ps.println(indent[0] + "static int num_temp = " + num_temp + ";");
				ps.println(indent[0] + "static long metric[" + num_temp + "] = {" + metricStr.toString() + "};");
				ps.println();
			}
			ps.println(indent[0] + "static int currentHist = 0;");
			ps.println();
			
			// ##########################################################
			// #                   See change log a                     #
			// ##########################################################
			//ps.println(indent[0] + "int map_UID_to_tempUID(int UID);");
			//ps.println();
			// ##########################################################
			
			ps.println(indent[0] + "int LogicDroid_Module_renewMonitorVariable(int* UID, int varCount, char value, int rel);");
			ps.println(indent[0] + "int LogicDroid_Module_initializeMonitor(int *UID, int appCount);");
			ps.println(indent[0] + "int LogicDroid_Module_checkEvent(int rel, int *UID, int varCount, long timestamp);");
			// ##########################################################
			// #                   See change log c                     #
			// ##########################################################
			//ps.println(indent[0] + "int LogicDroid_getCallRelationID() {return LogicDroid_CallRelationID;}");
			//ps.println(indent[0] + "int LogicDroid_getInternetUID() {return LogicDroid_INTERNET_UID;}");
			ps.println();
			// ##########################################################
			ps.println();
			ps.println(indent[0] + "History* History_Constructor(long timestamp);");
			ps.println(indent[0] + "void History_Reset(History *h);");
			ps.println(indent[0] + "void History_insertEvent(History *h, int rel, int idx);");
			ps.println(indent[0] + "char History_Process(History *next, History *prev);");
			ps.println(indent[0] + "void History_Dispose(History *h);");
			ps.println();

			ps.println();
			
			ps.println(indent[0] + "History **hist = NULL;");
			ps.println();

			
			ps.println(indent[0] + "int LogicDroid_Module_renewMonitorVariable(int *UID, int varCount, char value, int rel) {");
			ps.println(indent[1] + "int varIdx = 0;");
			ps.println(indent[1] + "int mul = 1;");
			ps.println(indent[1] + "int i = 0;");
			ps.println();			
			ps.println();
			ps.println(indent[1] + "if (notInitialized) {");
			ps.println(indent[2] + "return 0;");
			ps.println(indent[1] + "}");
			ps.println();
			ps.println(indent[1] + "for (i = varCount - 1; i >= 0; mul *= app_num, i--) {");

			ps.println(indent[2] + "varIdx += mapping[UID[i]] * mul;");
	
			ps.println(indent[1] + "}");

			ps.println(indent[1] + "hist[currentHist]->atoms[rel][varIdx] = value;");
			ps.println(indent[1] + "return 0;");
			ps.println(indent[0] + "}");
			
			ps.println();
			
			ps.println(indent[0] + "int LogicDroid_Module_initializeMonitor(int *UID, int appCount) {");
			ps.println(indent[1] + "int appIdx = " + (policyObjectCount + 1) + ";");
			ps.println(indent[1] + "int i;");
			ps.println();
			ps.println(indent[1] + "printf(\"Initializing Monitor for %d applications\\n\", appCount);");
			ps.println();
			ps.println(indent[1] + "mapping[0] = 0;");
			
			// ##########################################################
			// #                   See change log a                     #
			// ##########################################################
			// Obsolete : put fixed mappings for static objects from tempUID to local UID e.g. : mapping[3] = 1
			//            This is different from mapping from real UID to tempUID
			for (int i = 0; i < policyObjectCount; i++)
			{
				C_UID temp = ObjectMapping.get(policyObjects.get(i));
				ps.println(indent[1] + "mapping[" + policyObjects.get(i).toUpperCase() + "_UID] = " + temp.localUID + ";");
			}
			// ##########################################################
			
			ps.println(indent[1] + "app_num = appCount + " + (policyObjectCount + 1) + ";");
			ps.println();
			// Free static variable
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					ps.println(indent[1] + "free(" + policy_list.relations.get(i) + ");");
				}
			}
			ps.println();

			// Allocate space for all static variable
			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					Print.numberOfStaticAtoms ++;
					ps.println(indent[1] + policy_list.relations.get(i) + " = (char*) malloc(sizeof(char) * " + 
							sizeArr[policy_list.relationsCard.get(i)] + ");");
					ps.println(indent[1] + "memset(" + policy_list.relations.get(i) + ", 0, sizeof(char) * app_num);");
				}
			}

			ps.println();
			ps.println(indent[1] + "for (i = 0; i < appCount; i++) {");

			ps.println(indent[2] + "mapping[UID[i]] = appIdx++;");

			ps.println(indent[1] + "}");
			ps.println();

			{
				ps.println(indent[1] + "if (hist == NULL) {");
				ps.println(indent[2] + "hist = (History**) malloc(sizeof(History*) * 2);");
				ps.println(indent[2] + "hist[0] = NULL;");
				ps.println(indent[2] + "hist[1] = NULL;");
				ps.println(indent[1] + "}");
			}
			ps.println();
			ps.println(indent[1] + "History_Dispose(hist[0]);");
			ps.println(indent[1] + "History_Dispose(hist[1]);");
			ps.println(indent[1] + "hist[0] = History_Constructor(0);");
			ps.println(indent[1] + "hist[1] = History_Constructor(0);");
			ps.println(indent[1] + "History_Reset(hist[0]);");
			ps.println(indent[1] + "History_Reset(hist[1]);");
			ps.println();

			int callRelationID = -1;
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relations.get(i).equalsIgnoreCase("call") || policy_list.relations.get(i).equalsIgnoreCase("calls"))
				{
					callRelationID = i;
					break;
				}
			}

			ps.println(indent[1] + "currentHist = 0;");
			ps.println(indent[1] + "notInitialized = 0;");
			ps.println(indent[1] + "return module_policyID;");
			ps.println(indent[0] + "}");
			
			ps.println();
			ps.println(indent[0] + "int LogicDroid_Module_checkEvent(int rel, int *UID, int varCount, long timestamp) {");
			ps.println(indent[1] + "int varIdx = 0;");
			ps.println(indent[1] + "int mul = 1;");
			ps.println(indent[1] + "int i = 0;");
			ps.println(indent[1] + "char result;");
			ps.println(indent[1] + "if (notInitialized) {");
			ps.println(indent[2] + "return 0;");
			ps.println(indent[1] + "}");
			ps.println();

			ps.println(indent[1] + "History_Reset(hist[!currentHist]);");
			ps.println(indent[1] + "currentHist = !currentHist;");
			ps.println(indent[1] + "hist[currentHist]->timestamp = timestamp;");
			ps.println();		
			ps.println(indent[1] + "//if (UID != NULL) {");	
			ps.println(indent[2] + "for (i = varCount - 1; i >= 0; mul *= app_num, i--) {");
			
			ps.println(indent[3] + "varIdx += mapping[UID[i]] * mul;");
		
			ps.println(indent[2] + "}");

			ps.println(indent[2] + "History_insertEvent(hist[currentHist], rel, varIdx);");
			ps.println(indent[1] + "//}");
			ps.println();
			ps.println(indent[1] + "result = History_Process(hist[currentHist], hist[!currentHist]);");

			ps.println(indent[1] + "if (result) {");

			ps.println(indent[2] + "currentHist = !currentHist;");
			ps.println(indent[1] + "}");
			ps.println();
			ps.println(indent[1] + "return result;");
			ps.println(indent[0] + "}");
			ps.println();
			

			ps.println(indent[0] + "History* History_Constructor(long timestamp) {");
			ps.println(indent[1] + "History *retVal = (History*) malloc(sizeof(History));");
			ps.println();
			ps.println(indent[1] + "retVal->atoms = (char**) malloc(sizeof(char*) * " + policy_list.relationCount + ");");

			Print.numberOfAtoms = policy_list.relationCount;
			Print.atomDeclaration += policy_list.relationCount;
			int atomCount = 0;

			StringBuilder memsetSb = new StringBuilder();

			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.DYNAMIC)) {	
					Print.atomDeclaration += "|1|" + policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)));

					ps.println(indent[1] + "retVal->atoms[" + i + "] = (char*) malloc(sizeof(char) * " + 
							sizeArr[policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)))] + "); // " + policy_list.relations.get(i));					
					memsetSb.append(indent[1] + "memset(h->atoms[" + i + "], 0, sizeof(char) * " + 
							sizeArr[policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)))] + ");\n");
				}
				else if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					Print.atomDeclaration += "|0|" + (atomCount++) ;

					ps.println(indent[1] + "retVal->atoms[" + i + "] = " + policy_list.relations.get(i) + "; // " + policy_list.relations.get(i));
				}
			}


			memsetSb.append("\n");
			ps.println(indent[1] + "retVal->propositions = (char***) malloc(sizeof(char**) * " + policy_list.formulaCount + ");");

			Print.formulaDeclaration += policy_list.formulaCount;

			// Traverse all the formula for initialising the space
			StringBuilder spaceSb = new StringBuilder();
			idx = 0;
			ArrayList<ArrayList<Integer>> atomsIdx = new ArrayList<ArrayList<Integer>>(); // This is used for history disposal, as we need to know which propositions referring to the static data
			atomsIdx.add(new ArrayList<Integer>());
			traverse_space(policy_list.formulas.get(0), spaceSb, memsetSb, indent[1], 0, atomsIdx.get(0));
			// idx (a "global" variable) resulting from traverse_space will tell us how many sub formulas are there

			ps.println(indent[1] + "retVal->propositions[0] = (char**) malloc(sizeof(char*) * " + idx + ");");

			Print.formulaDeclaration += "|" + idx;

			ArrayList<Integer> propositionIdx = new ArrayList<Integer>();
			propositionIdx.add(idx);

			for (int i = 1; i < policy_list.formulaCount; i++) {
				idx = 0;
				atomsIdx.add(new ArrayList<Integer>());
				traverse_space(policy_list.formulas.get(i), spaceSb, memsetSb, indent[1], i, atomsIdx.get(i));
				ps.println(indent[1] + "retVal->propositions[" + i + "] = (char**) malloc(sizeof(char*) * " + idx + ");");

				Print.formulaDeclaration += "|" + idx;

				ps.println(indent[1] + "retVal->propositions[" + i + "][0] = retVal->atoms[" + mapping.get(policy_list.target_recursive.get(i)) + "];");

				Print.updateResourceAllocation ("|1|" + i + "|0|" + mapping.get(policy_list.target_recursive.get(i)));
				propositionIdx.add(idx);
				atomsIdx.get(i).add(mapping.get(policy_list.target_recursive.get(i)));
			}
			ps.println();
			ps.println(spaceSb.toString());
			// End Traverse
			
			if (num_temp > 0)
			{
				ps.println(indent[1] + "retVal->time_tag = (int**) malloc(sizeof(int*) * num_temp);");
				// Traverse all the formula for temporal operator
				idx = 0;
				spaceSb = new StringBuilder();
				memsetSb.append("\n");
				for (int i = policy_list.formulaCount - 1; i >= 0; i--)
				{
					traverse_timetag(policy_list.formulas.get(i), spaceSb, memsetSb, indent[1]);
				}
				ps.println(spaceSb.toString());
				// End Traverse
			}
			ps.println(indent[1] + "retVal->timestamp = timestamp;");
			ps.println();
			ps.println(indent[1] + "return retVal;");
			ps.println(indent[0] + "}");
			
			ps.println(indent[0] + "void History_Reset(History *h) {");
			ps.print(memsetSb.toString());
			ps.println(indent[0] + "}");
			
			ps.println();
			ps.println(indent[0] + "void History_insertEvent(History *h, int rel, int idx) {");
			ps.println(indent[1] + "h->atoms[rel][idx] = 1;");
			ps.println(indent[0] + "}");
			
			ps.println();						
			
			ps.println(indent[0] + "// Additional function to clean up garbage");
			ps.println(indent[0] + "void History_Dispose(History *h) {");
			ps.println(indent[1] + "int i;");
			ps.println();
			ps.println(indent[1] + "if (h == NULL) return;");
			ps.println();
			ps.println(indent[1] + "// Don't remove the actual data aliased by the variables");
			for (int i = 0; i < atomsIdx.size(); i++)
			{
				ArrayList<Integer> temp = atomsIdx.get(i);
				for (int j = 0; j < temp.size(); j++)
				{
					ps.println(indent[1] + "h->propositions[" + i + "][" + temp.get(j) + "] = NULL;");
				}
			}
			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					ps.println(indent[1] + "h->atoms[" + i + "] = NULL;");
				}
			}
			ps.println();
			ps.println(indent[1] + "// clean propositions");
			for (int i = 0; i < policy_list.formulaCount; i++)
			{
				ps.println(indent[1] + "for (i = 0; i < " +  propositionIdx.get(i) + "; i++) {");
				ps.println(indent[2] + "free(h->propositions[" + i + "][i]);");
				ps.println(indent[1] + "}");
				ps.println(indent[1] + "free(h->propositions[" + i + "]);");
			}
			ps.println(indent[1] + "free(h->propositions);");
			ps.println();
			ps.println(indent[1] + "// Clean atoms");
			ps.println(indent[1] + "for (i = 0; i < " + policy_list.relationCount + "; i++) {");
			ps.println(indent[2] + "free(h->atoms[i]);");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "free(h->atoms);");
			ps.println();
			if (num_temp > 0)
			{
				ps.println(indent[1] + "// Clean temporal metric");
				ps.println(indent[1] + "for (i = 0; i < num_temp; i++) {");
				ps.println(indent[2] + "free(h->time_tag[i]);");
				ps.println(indent[1] + "}");
				ps.println(indent[1] + "free(h->time_tag);");
				ps.println();
			}
			ps.println(indent[1] + "// finally free the history reference");
			ps.println(indent[1] + "free(h);");
			ps.println(indent[0] + "}");

			ps.println();
			ps.println(indent[0] + "void cleanup_monitor_module(void) {");
			//ps.println(indent[1] + "int i;");
			ps.println(indent[1] + "printf(\"Detaching the policy from the monitor stub in kernel\\n\");");
			//ps.println(indent[1] + "for (i = 0; i < " + policy_list.relationCount + "; i++) free(relations[i]);");
			ps.println(indent[1] + "free(relations);");
			ps.println(indent[1] + "History_Dispose(hist[0]);");
			ps.println(indent[1] + "History_Dispose(hist[1]);");
			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					ps.println(indent[1] + "free(" + policy_list.relations.get(i) + ");");
				}
			}
			ps.println(indent[0] + "}");
			ps.println();












			


			ps.println("//***********************************************************************************************************************************************//");
			ps.println();			

			ps.println("typedef struct tVariable{");
			ps.println(indent[1] + "//type = 0 -> constant");
			ps.println(indent[1] + "//type = 1 -> variable");
			ps.println(indent[1] + "int type;");
			ps.println(indent[1] + "int value;");
			ps.println("} Variable;");
			ps.println();

			ps.println("typedef struct tVariableIndex{");
			ps.println(indent[1] + "int varLength;");
			ps.println(indent[1] + "Variable *var;");
			ps.println("} VariableIndex;");
			ps.println();

			ps.println("typedef struct tDependancyTableRecord{");
			ps.println(indent[1] + "int id;");
			ps.println(indent[1] + "int freeVarNo;");
			ps.println(indent[1] + "int varIdxLength;");
			ps.println(indent[1] + "VariableIndex * varIdx;");
			ps.println(indent[1] + "int fixIdxLength;");
			ps.println(indent[1] + "int *fixIdx;");
			ps.println("} DependancyTableRecord;");
			ps.println();

			ps.println("int dependancyTableRecordNo;								// No of records in dependancy table");
			ps.println("DependancyTableRecord * dependancyTable;");
			ps.println();
			
			ps.println(indent[0] + "int read_dependancy_table(){");
			ps.println(indent[1] + "char inputString[] =\"1|00|1|2|1|0|0|1|1|0|3|0|0|1\";");

			ps.println(indent[1] + "int i, j, k, varIdxLength, fixIdxLength, varLength;");
			ps.println(indent[1] + "dependancyTableRecordNo = atoi(strtok(inputString,\"|\"));");
			ps.println(indent[1] + "dependancyTable = (DependancyTableRecord*) malloc (sizeof(DependancyTableRecord) * dependancyTableRecordNo);");
			ps.println(indent[1] + "for (i = 0; i < dependancyTableRecordNo; i++){");
			ps.println(indent[2] + "dependancyTable[i].id = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[2] + "dependancyTable[i].freeVarNo = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[2] + "varIdxLength = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[2] + "dependancyTable[i].varIdxLength = varIdxLength;");
			ps.println(indent[2] + "dependancyTable[i].varIdx = (VariableIndex*) malloc (sizeof(VariableIndex) * varIdxLength);");
			ps.println(indent[2] + "for (j = 0; j < varIdxLength; j++){");
			ps.println(indent[3] + "varLength = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[3] + "dependancyTable[i].varIdx[j].varLength = varLength;");
			ps.println(indent[3] + "dependancyTable[i].varIdx[j].var = (Variable*) malloc(sizeof(Variable)* varLength);");

			ps.println(indent[3] + "for (k = 0; k < varLength; k++){");
			ps.println(indent[4] + "dependancyTable[i].varIdx[j].var[k].type = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[4] + "dependancyTable[i].varIdx[j].var[k].value = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[3] + "}");
			ps.println(indent[2] + "}");
			ps.println(indent[2] + "fixIdxLength = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[2] + "dependancyTable[i].fixIdxLength = fixIdxLength;");
			ps.println(indent[2] + "dependancyTable[i].fixIdx = (int*) malloc (sizeof(int) * fixIdxLength);");
			ps.println(indent[2] + "for (j = 0; j < fixIdxLength; j++)");
			ps.println(indent[3] + "dependancyTable[i].fixIdx[j] = atoi(strtok (NULL, \"|\"));");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "return 0;");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println("//***********************************************************************************************************************************************//");
			ps.println();

			ps.println("int exist_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "if (freeVarNo < 1)");
			ps.println(indent[2] + "return 0;");
			ps.println(indent[1] + "int policyNo = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx  = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int subIdx   = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i,firstCalIdx;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] || next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])];");
			ps.println(indent[2] + "if (next->propositions[policyNo][currIdx][firstCalIdx])");
			ps.println(indent[3] + "return 0;");
			ps.println(indent[1] + "}");
			ps.println("}");
			ps.println();

			ps.println("int and_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx  = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i, j, subIdx, firstCalIdx;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i++)");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0])] = next->propositions[policyNo][dependancyTable[recordIdx].fixIdx[2]][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])];");
			ps.println(indent[1] + "for (i = 2; i < dependancyTable[recordIdx].varIdxLength; i++){");
			ps.println(indent[2] + "subIdx = dependancyTable[recordIdx].fixIdx[i+1];");
			ps.println(indent[2] + "for (j = 0; j < mainLoopsNo; j++){");
			ps.println(indent[3] + "firstCalIdx = freeVarCal(freeVarNo, j, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[3] + "next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] && next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, j, dependancyTable[recordIdx].varIdx[i])];");
			ps.println(indent[2] + "}");
			ps.println(indent[1] + "}");
			ps.println("}");
			ps.println();

			ps.println(indent[0] + "int or_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx  = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i, j, subIdx, firstCalIdx;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i++)");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0])] = next->propositions[policyNo][dependancyTable[recordIdx].fixIdx[2]][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])];");
			ps.println(indent[1] + "for (i = 2; i < dependancyTable[recordIdx].varIdxLength; i++){");
			ps.println(indent[2] + "subIdx = dependancyTable[recordIdx].fixIdx[i+1];");
			ps.println(indent[2] + "for (j = 0; j < mainLoopsNo; j++){");
			ps.println(indent[3] + "firstCalIdx = freeVarCal(freeVarNo, j, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[3] + "next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] || next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, j, dependancyTable[recordIdx].varIdx[i])];");
			ps.println(indent[2] + "}");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int not_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx  = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int subIdx   = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++)");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0])] = !next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])];");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int forall_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "if (freeVarNo < 1)");
			ps.println(indent[2] + "return 0;");
			ps.println(indent[1] + "int policyNo = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx  = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int subIdx   = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i,firstCalIdx;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][currIdx][firstCalIdx] && next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])];");
			ps.println(indent[2] + "if (!next->propositions[policyNo][currIdx][firstCalIdx])");
			ps.println(indent[3] + "return 0;");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int diamond_dot_metric_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo    = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx     = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int subIdx      = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int timetagIdx  = dependancyTable[recordIdx].fixIdx[3];");
			ps.println(indent[1] + "int metric 	  = dependancyTable[recordIdx].fixIdx[4];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i,firstCalIdx;");
			ps.println(indent[1] + "int delta = next->timestamp - prev->timestamp;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][firstCalIdx] = 0;");
			ps.println(indent[2] + "next->time_tag[timetagIdx][firstCalIdx] = 0;");
			ps.println(indent[2] + "if (delta <= metric) {");
			ps.println(indent[3] + "next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])];");
			ps.println(indent[3] + "next->time_tag[timetagIdx][firstCalIdx] = (int) delta;");
			ps.println(indent[3] + "if (!next->propositions[policyNo][currIdx][firstCalIdx] && prev->time_tag[timetagIdx][firstCalIdx] + delta <= metric) {");
			ps.println(indent[4] + "next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][currIdx][firstCalIdx];");
			ps.println(indent[4] + "if (next->propositions[policyNo][currIdx][firstCalIdx])");
			ps.println(indent[5] + "next->time_tag[timetagIdx][firstCalIdx] += prev->time_tag[timetagIdx][firstCalIdx];");
			ps.println(indent[4] + "}");
			ps.println(indent[2] + "}");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int diamond_dot_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo    = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx     = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int subIdx      = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i,firstCalIdx;");

			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])] || prev->propositions[policyNo][currIdx][firstCalIdx];");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int diamond_metric_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo    = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx     = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int subIdx      = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int timetagIdx  = dependancyTable[recordIdx].fixIdx[3];");
			ps.println(indent[1] + "int metric 	  = dependancyTable[recordIdx].fixIdx[4];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i,firstCalIdx;");
			ps.println(indent[1] + "int delta = next->timestamp - prev->timestamp;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])];");
			ps.println(indent[2] + "next->time_tag[timetagIdx][firstCalIdx] = 0;");
			ps.println(indent[2] + "if (!next->propositions[policyNo][currIdx][firstCalIdx] && prev->time_tag[timetagIdx][firstCalIdx] + delta <= metric) {");
			ps.println(indent[3] + "next->propositions[policyNo][currIdx][firstCalIdx] = prev->propositions[policyNo][currIdx][firstCalIdx];");
			ps.println(indent[3] + "next->time_tag[timetagIdx][firstCalIdx] = delta + prev->time_tag[timetagIdx][firstCalIdx];");
			ps.println(indent[2] + "}");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int diamond_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo    = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx     = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int subIdx      = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i,firstCalIdx;");

			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][subIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])] || prev->propositions[policyNo][currIdx][firstCalIdx];");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int since_metric_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo	   = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx	   = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int firstSubIdx  = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int timetagIdx   = dependancyTable[recordIdx].fixIdx[3];");
			ps.println(indent[1] + "int metric	   = dependancyTable[recordIdx].fixIdx[4];");
			ps.println(indent[1] + "int secondSubIdx = dependancyTable[recordIdx].fixIdx[5];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i, firstCalIdx, thirdCalIdx;");

			ps.println(indent[1] + "int delta = next->timestamp - prev->timestamp;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "thirdCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[2]);");
			ps.println(indent[2] + "next->time_tag[timetagIdx][firstCalIdx] = 0;");
			ps.println(indent[2] + "if (delta <= metric) {");
			ps.println(indent[3] + "next->propositions[policyNo][currIdx][firstCalIdx] = (next->propositions[policyNo][firstSubIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])] || next->propositions[policyNo][secondSubIdx][thirdCalIdx]) && (next->propositions[policyNo][secondSubIdx][thirdCalIdx] || prev->propositions[policyNo][currIdx][firstCalIdx]);");
			ps.println(indent[3] + "if (!next->propositions[policyNo][secondSubIdx][thirdCalIdx])");
			ps.println(indent[4] + "next->time_tag[timetagIdx][firstCalIdx] = delta + prev->time_tag[timetagIdx][firstCalIdx];");
			ps.println(indent[2] + "}");
			ps.println(indent[2] + "else");
			ps.println(indent[3] + "next->propositions[policyNo][currIdx][firstCalIdx] = next->propositions[policyNo][secondSubIdx][thirdCalIdx];");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int since_function(History *next, History *prev, int recordIdx){");
			ps.println(indent[1] + "int freeVarNo = dependancyTable[recordIdx].freeVarNo;");
			ps.println(indent[1] + "int policyNo     = dependancyTable[recordIdx].fixIdx[0];");
			ps.println(indent[1] + "int currIdx      = dependancyTable[recordIdx].fixIdx[1];");
			ps.println(indent[1] + "int firstSubIdx  = dependancyTable[recordIdx].fixIdx[2];");
			ps.println(indent[1] + "int secondSubIdx = dependancyTable[recordIdx].fixIdx[3];");
			ps.println(indent[1] + "int mainLoopsNo = power(freeVarNo);");
			ps.println(indent[1] + "int i,firstCalIdx;");
			ps.println(indent[1] + "for (i = 0; i < mainLoopsNo; i ++){");
			ps.println(indent[2] + "firstCalIdx = freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[0]);");
			ps.println(indent[2] + "next->propositions[policyNo][currIdx][firstCalIdx] = (next->propositions[policyNo][firstSubIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[1])] && prev->propositions[policyNo][currIdx][firstCalIdx]) || next->propositions[policyNo][secondSubIdx][freeVarCal(freeVarNo, i, dependancyTable[recordIdx].varIdx[2])];");
			ps.println(indent[1] + "}");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println(indent[0] + "int (*functionPointer[11])(History * next, History * prev, int recordIdx);");
			ps.println();

			ps.println("//***********************************************************************************************************************************************//");
			ps.println();

			ps.println("// Calculate the value of firstIndex, secondIndex");
			ps.println("int freeVarCal(int freeVarNo, int loopNo, VariableIndex varIdx){");
			ps.println(indent[1] + "int i;");
			ps.println(indent[1] + "int value = 0;");
			ps.println(indent[1] + "int freeVarCurrValue[freeVarNo]; ");
			ps.println(indent[1] + "for (i = freeVarNo - 1; i >= 0; i--) {");
			ps.println(indent[2] + "freeVarCurrValue[i] = loopNo % app_num;");
			ps.println(indent[2] + "loopNo = loopNo/app_num;");
			ps.println(indent[1] + "}  ");
			ps.println(indent[1] + "for (i = 0; i < varIdx.varLength; i++) {");
			ps.println(indent[2] + "if (varIdx.var[i].type)");
			ps.println(indent[3] + "value = value * app_num + freeVarCurrValue[varIdx.var[i].value];");
			ps.println(indent[2] + "else");
			ps.println(indent[3] + "value = value * app_num + varIdx.var[i].value;");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "return value;");
			ps.println("}");
			ps.println();


			ps.println(indent[0] + "// Calculate the value of pow(app_num, number)");
			ps.println(indent[0] + "int power(int number){");
			ps.println(indent[1] + "int result = 1;");
			ps.println(indent[1] + "int i;");
			ps.println();
			ps.println(indent[1] + "if (number < 0)");
			ps.println(indent[2] + "return 0;");
			ps.println();
			ps.println(indent[1] + "for (i = 0; i < number; i++)");
			ps.println(indent[2] + "result *= app_num;");
			ps.println();
			ps.println(indent[1] + "return result;");
			ps.println(indent[0] + "}");
			ps.println();

			ps.println("//***********************************************************************************************************************************************//");
			ps.println();
			ps.println("int main() {");
			ps.println(indent[1] + "functionPointer[0]  = exist_function;");
			ps.println(indent[1] + "functionPointer[1]  = and_function;");
			ps.println(indent[1] + "functionPointer[2]  = or_function;");
			ps.println(indent[1] + "functionPointer[3]  = not_function;");
			ps.println(indent[1] + "functionPointer[4]  = forall_function;");
			ps.println(indent[1] + "functionPointer[5]  = diamond_dot_metric_function;");
			ps.println(indent[1] + "functionPointer[6]  = diamond_dot_function;");
			ps.println(indent[1] + "functionPointer[7]  = diamond_metric_function;");
			ps.println(indent[1] + "functionPointer[8]  = diamond_function;");
			ps.println(indent[1] + "functionPointer[9]  = since_metric_function;");
			ps.println(indent[1] + "functionPointer[10] = since_function;");
			ps.println(indent[1] + "read_dependancy_table();");
			ps.println();
			ps.println(indent[1] + "int appId[] = {1106, 1107, 1108, 1109, 1110};");
			ps.println(indent[1] + "LogicDroid_Module_initializeMonitor(appId, 5);");
			ps.println();
			ps.println(indent[1] + "int i, choice,rel,varCount,value;");
			ps.println(indent[1] + "long timestamp;");
			ps.println(indent[1] + "int *UID;");
			ps.println();
			ps.println(indent[1] + "/*");
			ps.println(indent[1] + "for (i = 0; i < 7; i++){");
			ps.println(indent[2] + "system[i] = 1;");
			ps.println(indent[2] + "trusted[i] = 1;");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "*/");
			ps.println();
			ps.println(indent[1] + "while (1) {");
			ps.println(indent[2] + "printf(\"\\n\\nAppId: 0 1100 1101 1102 1103 1104 1105 1106 1107 1108 1109 1110\");");
			ps.println(indent[2] + "printf(\"\\n\\n1. CheckEvent(int rel, int *UID, int varCount, long timestamp)\");");
			ps.println(indent[2] + "printf(\"\\n2. RenewMonitorVariable(int *UID, int varCount, char value, int rel)\");");
			ps.println(indent[2] + "printf(\"\\n3. Print relation current history\");");
			ps.println(indent[2] + "printf(\"\\n4. Print relation previous history\");");
			ps.println(indent[2] + "//printf(\"\\n5. Check Result\");");
			ps.println(indent[2] + "printf(\"\\n0. Exit\");");
			ps.println(indent[2] + "printf(\"\\nChoice :\");");
			ps.println(indent[2] + "scanf(\"%d\", &choice);");
			ps.println();
			ps.println(indent[2] + "if (choice == 1) {");
			ps.println(indent[3] + "printf(\"\\nrel = \");");
			ps.println(indent[3] + "scanf(\"%d\", &rel);");
			ps.println();
			ps.println(indent[3] + "printf(\"varCount = \");");
			ps.println(indent[3] + "scanf(\"%d\", &varCount);");
			ps.println();
			ps.println(indent[3] + "UID = (int*) malloc (sizeof(int) * varCount);");
			ps.println(indent[3] + "printf(\"Enter UID Array:\");");
			ps.println();
			ps.println(indent[3] + "for (i = 0; i < varCount; i ++){");
			ps.println(indent[4] + "scanf(\"%d\", &UID[i]);");
			ps.println(indent[4] + "printf(\"\\nUID[%d] = %d\", i,UID[i]);");
			ps.println(indent[3] + "}");
			ps.println();
			ps.println(indent[3] + "printf(\"\\ntimestamp = \");");
			ps.println(indent[3] + "scanf(\"%ld\", &timestamp);");
			ps.println();
			ps.println(indent[3] + "printf(\"Result = %d\", LogicDroid_Module_checkEvent(rel, UID, varCount, timestamp));");
			ps.println(indent[2] + "}");
			ps.println();
			ps.println(indent[2] + "if (choice == 2) {");
			ps.println(indent[3] + "printf(\"\\nrel = \");");
			ps.println(indent[3] + "scanf(\"%d\", &rel);");
			ps.println();
			ps.println(indent[3] + "printf(\"varCount = \");");
			ps.println(indent[3] + "scanf(\"%d\", &varCount);");
			ps.println();
			ps.println(indent[3] + "UID = (int*) malloc (sizeof(int) * varCount);");
			ps.println(indent[3] + "printf(\"Enter UID Array:\");");
			ps.println(indent[3] + "for (i = 0; i < varCount; i ++){");
			ps.println(indent[4] + "scanf(\"%d\", &UID[i]);");
			ps.println(indent[4] + "printf(\"\\nUID[%d] = %d\", i,UID[i]);");
			ps.println(indent[3] + "}");
			ps.println();
			ps.println(indent[3] + "printf(\"\\nvalue = \");");
			ps.println(indent[3] + "scanf(\"%d\", &value);");
			ps.println();
			ps.println(indent[3] + "LogicDroid_Module_renewMonitorVariable(UID, varCount, value, rel);");
			ps.println(indent[2] + "}");
			ps.println();
			ps.println(indent[2] + "if (choice == 3) {");
			ps.println(indent[3] + "printf(\"\\nrel = \");");
			ps.println(indent[3] + "scanf(\"%d\", &rel);");
			ps.println();
			ps.println(indent[3] + "printf(\"varCount = \");");
			ps.println(indent[3] + "scanf(\"%d\", &varCount);");
			ps.println();
			ps.println(indent[3] + "for (i = 0; i < power(varCount); i++) {");
			ps.println(indent[4] + "if (i % app_num == 0)");
			ps.println(indent[5] + "printf(\"\\n\");");
			ps.println(indent[4] + "printf(\"%d - \",hist[currentHist]->atoms[rel][i]);");
			ps.println(indent[3] + "}");
			ps.println(indent[2] + "}");
			ps.println();
			ps.println(indent[2] + "if (choice == 4) {");
			ps.println(indent[3] + "printf(\"\\nrel = \");");
			ps.println(indent[3] + "scanf(\"%d\", &rel);");
			ps.println();
			ps.println(indent[3] + "printf(\"varCount = \");");
			ps.println(indent[3] + "scanf(\"%d\", &varCount);");
			ps.println();
			ps.println(indent[3] + "for (i = 0; i < power(varCount); i++) {");
			ps.println(indent[4] + "if (i % app_num == 0)");
			ps.println(indent[5] + "printf(\"\\n\");");
			ps.println(indent[4] + "printf(\"%d - \",hist[!currentHist]->atoms[rel][i]);");
			ps.println(indent[3] + "}");
			ps.println(indent[2] + "}");
			ps.println();
			ps.println(indent[2] + "//if (choice == 5) {");
			ps.println(indent[3] + "//printf(\"\\nResult = %d\",LogicDroid_Module_checkEvent(0, NULL, 0, 0));");
			ps.println(indent[2] + "//}");
			ps.println();
			ps.println(indent[2] + "if (choice == 0)");
			ps.println(indent[3] + "break;");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "cleanup_monitor_module();");
			ps.println(indent[1] + "return 0;");
			ps.println("}");
			ps.println();

			ps.println(indent[0] + "char History_Process(History *next, History *prev) {");
			ps.println(indent[1] + "int recordIdx;");
			ps.println(indent[1] + "for (recordIdx = 0; recordIdx < dependancyTableRecordNo; recordIdx++) { ");
			ps.println(indent[2] + "(*functionPointer[dependancyTable[recordIdx].id])(next, prev, recordIdx);");
			ps.println(indent[1] + "}");
			ps.println(indent[1] + "return next->propositions[0][0][0];");
			ps.println(indent[0] + "}");		
			ps.println();














			ps.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	private static void traverse_space(Formula policy, StringBuilder sb, StringBuilder memsetSb, String indent, int policy_number, ArrayList<Integer> atomsIdx)
	{
		if (policy.type.equals(GlobalVariable.ATOM))
		{
			int atomNumber = mapping.get(((Atom)policy).rel);
			sb.append(indent + "retVal->propositions[" + policy_number + "][" + idx + "] = retVal->atoms[" + atomNumber + "];\n");

			Print.updateResourceAllocation ("|1|" + policy_number + "|" + idx + "|" + atomNumber);

			atomsIdx.add(idx);
		}
		else if (policy_number == 0 || idx > 0) // the head of the recursive formulas will be directed towards the atomic formula
		{
			if (policy.varCount > 0)
			{
				sb.append(indent + "retVal->propositions[" + policy_number + "][" + idx + "] = (char*) malloc(sizeof(char) * " + sizeArr[policy.varCount] + ");\n");
				memsetSb.append(indent + "memset(h->propositions[" + policy_number + "][" + idx + "], 0, " + sizeArr[policy.varCount] + ");\n");

				Print.updateResourceAllocation ("|0|" + policy_number + "|" + idx + "|" + policy.varCount);

			}
			else if (policy.varCount == 0)
			{
				sb.append(indent + "retVal->propositions[" + policy_number + "][" + idx + "] = (char*) malloc(sizeof(char));\n");
				memsetSb.append(indent + "memset(h->propositions[" + policy_number + "][" + idx + "], 0, 1);\n");

				Print.updateResourceAllocation ("|0|" + policy_number + "|" + idx + "|" + policy.varCount);
			}
		}
		idx = idx + 1;
		for (int i = 0; i < policy.count; i++) {
			traverse_space(policy.sub.get(i), sb, memsetSb, indent, policy_number, atomsIdx);
		}
	}
	
	private static void traverse_timetag(Formula policy, StringBuilder sb, StringBuilder memsetSb, String indent) {
		if (policy.type == GlobalVariable.DIAMONDDOT) {
			if (((DiamondDotFormula)policy).metric >= 0) { // metric -1 means that it is original LTL formula
				if (policy.varCount > 0) {
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) malloc(sizeof(int) * " + sizeArr[policy.varCount] + ") ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int) * " + sizeArr[policy.varCount] + ");\n");
					Print.updateTimeTagSize(policy.varCount);
					idx++;
				}
				else if (policy.varCount == 0) {
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) malloc(sizeof(short)) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int));\n");
					idx++;
				}
			}
		}
		else if (policy.type == GlobalVariable.DIAMOND) {
			if (((DiamondFormula)policy).metric >= 0) { // metric -1 means that it is original LTL formula
				if (policy.varCount > 0) {
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) malloc(sizeof(int) * " + sizeArr[policy.varCount] + ") ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int) * " + sizeArr[policy.varCount] + ");\n");
					Print.updateTimeTagSize(policy.varCount);
					idx++;
				}
				else if (policy.varCount == 0) {
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) malloc(sizeof(short)) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int));\n");
					idx++;
				}
			}
		}
		else if (policy.type == GlobalVariable.SINCE) {
			if (((SinceFormula)policy).metric >= 0) { // metric -1 means that it is original LTL formula
				if (policy.varCount > 0) {
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) malloc(sizeof(int) * " + sizeArr[policy.varCount] + ") ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int) * " + sizeArr[policy.varCount] + ");\n");
					Print.updateTimeTagSize(policy.varCount);
					idx++;
				}
				else if (policy.varCount == 0) {
					sb.append(indent + "retVal->time_tag[" + idx + "] = (int*) malloc(sizeof(short)) ;\n");
					memsetSb.append(indent + "memset(h->time_tag[" + idx + "], 0, sizeof(int));\n");
					idx++;
				}
			}
		}
		for (int i = 0; i < policy.count; i++) {
			traverse_timetag(policy.sub.get(i), sb, memsetSb,indent);
		}
	}
	
	private static void traverse_vars(Formula policy, StringBuilder sb, HashSet<String> usedVars, ArrayList<String> objects)
	{
		for (int i = policy.varCount - 1; i >= 0; i--)
		{
			if (!objects.contains(policy.vars.get(i)))
			if (!usedVars.contains(policy.vars.get(i)))
			{
				usedVars.add(policy.vars.get(i));
				sb.append(" " + policy.vars.get(i) + ",");
			}
		}
		
		for (int i = 0; i < policy.count; i++)
		{
			traverse_vars(policy.sub.get(i), sb, usedVars, objects);
		}
	}

	public static void generate_source_code(Policy policy_list){
		StringBuilder indentSb = new StringBuilder();
				
		StringBuilder sizeSb = new StringBuilder("app_num");
		sizeArr[0] = "";
		for (int i = 0; i < indent.length; i++)
		{
			indent[i] = indentSb.toString();
			sizeArr[i + 1] = sizeSb.toString();
			indentSb.append("  ");
			sizeSb.append(" * app_num");
		}

		ArrayList<String> result = new ArrayList<String>();
		FormulaIndexTraversal indexTraverser = new FormulaIndexTraversal(indent, sizeArr);

		for (int i = policy_list.formulaCount - 1; i >= 0; i--) {
				indexTraverser.reset();
				indexTraverser.traverse_index(policy_list.formulas.get(i), 1, result, i);
		}

		
		System.out.println("\n");

		for (int i = 0; i < result.size(); i++) {
			System.out.println(result.get(i));
		}
	}

	public static void generate_instruction_table(Policy policy_list){
		StringBuilder indentSb = new StringBuilder();
				
		StringBuilder sizeSb = new StringBuilder("app_num");
		sizeArr[0] = "";
		for (int i = 0; i < indent.length; i++)
		{
			indent[i] = indentSb.toString();
			sizeArr[i + 1] = sizeSb.toString();
			indentSb.append("  ");
			sizeSb.append(" * app_num");
		}

		ArrayList<String> result = new ArrayList<String>();
		FormulaIndexTraversal indexTraverser = new FormulaIndexTraversal(indent, sizeArr);

		indexTraverser.setDebugMode(1);
		
		for (int i = policy_list.formulaCount - 1; i >= 0; i--) {
				indexTraverser.reset();
				indexTraverser.traverse_index(policy_list.formulas.get(i), 1, result, i);
		}	

	}

	public static void generate_dependancy_table(Policy policy_list, int debugMode){
		StringBuilder indentSb = new StringBuilder();

		StringBuilder sizeSb = new StringBuilder("app_num");
		sizeArr[0] = "";
		for (int i = 0; i < indent.length; i++)
		{
			indent[i] = indentSb.toString();
			sizeArr[i + 1] = sizeSb.toString();
			indentSb.append("  ");
			sizeSb.append(" * app_num");
		}

		ArrayList<String> result = new ArrayList<String>();
		FormulaIndexTraversal indexTraverser = new FormulaIndexTraversal(indent, sizeArr);

		indexTraverser.setDebugMode(debugMode);

		for (int i = policy_list.formulaCount - 1; i >= 0; i--) {
				indexTraverser.reset();
				indexTraverser.traverse_index(policy_list.formulas.get(i), 1, result, i);
		}	

		if (debugMode == 2) {
			int callRelationID = 0;
			System.out.print("inputString=\"" + Print.numberOfStaticAtoms + "|" + policy_list.tempCount + "|" + Print.timeTagSize + "|" + Print.atomDeclaration + "|" + Print.formulaDeclaration + "|" + Print.numberOfResourcesAllocationRecords + Print.resourceAllocation + "|00" + Print.numberOfInstructions + Print.dependancyTable + "\" module_policyID=" + policy_list.ID + " relationSize=" + policy_list.relationCount + " relations=" );
			for (int i=0; i < policy_list.relationCount; i++) {
				System.out.print("\"" + policy_list.relations.get(i) + "\",");
				if (policy_list.relations.get(i).equalsIgnoreCase("call") || policy_list.relations.get(i).equalsIgnoreCase("calls"))
					callRelationID = i;
			}
			for (int i=0; i < 9 - policy_list.relationCount; i++)
				System.out.print("\"\",");
			System.out.println("\"\" callRelationID=" + callRelationID);
		}
	}
}
