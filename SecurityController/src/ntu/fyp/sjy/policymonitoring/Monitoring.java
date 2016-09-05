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

package ntu.fyp.sjy.policymonitoring;

import ntu.fyp.sjy.policymonitoring.FormulaIndexTraversal;

import java.util.*;

public class Monitoring {
	private static String[] sizeArr = new String[11];
	private static String[] indent = new String[10];
	private static int startVirtualUID = 1100;
	private static int idx;

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
		policyObjects.add("imei");
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
			// Map objects to an application ID : virtual UID starting from 1100
		}
	}

    //NOTE: This method need to remove the redundancy code
	public static void generate_kernel_monitor(Policy policy_list, String fileName)
	{
		// Assume that we are given the root formula as the first policy in the list
		// the file name is going to be appended with .java later
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

		try
		{
			//FileOutputStream stream = new FileOutputStream(fileName + "_module.c");
			//PrintStream ps = new PrintStream(stream);

            StringBuilder unusedStringBuilder = new StringBuilder();

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

			unusedStringBuilder.append(indent[0] + "#include <stdio.h>");
			unusedStringBuilder.append(indent[0] + "#include <stdlib.h>");
			unusedStringBuilder.append(indent[0] + "#include <string.h>");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "#define MAX_APPLICATION 100000");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "typedef struct tHistory {");
			unusedStringBuilder.append(indent[1] + "char ***propositions;");
			unusedStringBuilder.append(indent[1] + "char **atoms;");
			unusedStringBuilder.append(indent[1] + "int **time_tag;");
			unusedStringBuilder.append(indent[1] + "long timestamp;");
			unusedStringBuilder.append(indent[0] + "} History;");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "#define ROOT_UID 0");
			// fixed mappings for static object from real UID to tempUID e.g. : case 1100 : return 3;
			for (int i = 0; i < policyObjectCount; i++)
			{
				C_UID temp = ObjectMapping.get(policyObjects.get(i));
				unusedStringBuilder.append(indent[0] + "#define " + policyObjects.get(i).toUpperCase() + "_UID " + temp.virtualUID + "");
			}
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "static char notInitialized = 1;");
			unusedStringBuilder.append(indent[0] + "static int mapping[MAX_APPLICATION];");

			// Handles static variables declaration
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					unusedStringBuilder.append(indent[0] + "static char *" + policy_list.relations.get(i) + ";");
				}
			}
			unusedStringBuilder.append(indent[0] + "static char** relations;");
			unusedStringBuilder.append(indent[0] + "static int relationSize = 0;");
			unusedStringBuilder.append(indent[0] + "static int app_num;");
			unusedStringBuilder.append(indent[0] + "static int module_policyID = " + policy_list.ID + ";");
			// ##########################################################
			// #                   See change log c                     #
			// ##########################################################
			//unusedStringBuilder.append(indent[0] + "static int LogicDroid_CallRelationID = -1;");
			//unusedStringBuilder.append(indent[0] + "static int LogicDroid_INTERNET_UID = -1;");
			// ##########################################################
			unusedStringBuilder.append("");
			if (num_temp > 0)
			{
				unusedStringBuilder.append(indent[0] + "static int num_temp = " + num_temp + ";");
				unusedStringBuilder.append(indent[0] + "static long metric[" + num_temp + "] = {" + metricStr.toString() + "};");
				unusedStringBuilder.append("");
			}
			unusedStringBuilder.append(indent[0] + "static int currentHist = 0;");
			unusedStringBuilder.append("");

			unusedStringBuilder.append(indent[0] + "int LogicDroid_Module_renewMonitorVariable(int* UID, int varCount, char value, int rel);");
			unusedStringBuilder.append(indent[0] + "int LogicDroid_Module_initializeMonitor(int *UID, int appCount);");
			unusedStringBuilder.append(indent[0] + "int LogicDroid_Module_checkEvent(int rel, int *UID, int varCount, long timestamp);");

			unusedStringBuilder.append("");

			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "History* History_Constructor(long timestamp);");
			unusedStringBuilder.append(indent[0] + "void History_Reset(History *h);");
			unusedStringBuilder.append(indent[0] + "void History_insertEvent(History *h, int rel, int idx);");
			unusedStringBuilder.append(indent[0] + "char History_Process(History *next, History *prev);");
			unusedStringBuilder.append(indent[0] + "void History_Dispose(History *h);");
			unusedStringBuilder.append("");

			unusedStringBuilder.append("");

			unusedStringBuilder.append(indent[0] + "History **hist = NULL;");
			unusedStringBuilder.append("");


			unusedStringBuilder.append(indent[0] + "int LogicDroid_Module_renewMonitorVariable(int *UID, int varCount, char value, int rel) {");
			unusedStringBuilder.append(indent[1] + "int varIdx = 0;");
			unusedStringBuilder.append(indent[1] + "int mul = 1;");
			unusedStringBuilder.append(indent[1] + "int i = 0;");
			unusedStringBuilder.append("");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "if (notInitialized) {");
			unusedStringBuilder.append(indent[2] + "return 0;");
			unusedStringBuilder.append(indent[1] + "}");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "for (i = varCount - 1; i >= 0; mul *= app_num, i--) {");

			unusedStringBuilder.append(indent[2] + "varIdx += mapping[UID[i]] * mul;");

			unusedStringBuilder.append(indent[1] + "}");

			unusedStringBuilder.append(indent[1] + "hist[currentHist]->atoms[rel][varIdx] = value;");
			unusedStringBuilder.append(indent[1] + "return 0;");
			unusedStringBuilder.append(indent[0] + "}");

			unusedStringBuilder.append("");

			unusedStringBuilder.append(indent[0] + "int LogicDroid_Module_initializeMonitor(int *UID, int appCount) {");
			unusedStringBuilder.append(indent[1] + "int appIdx = " + (policyObjectCount + 1) + ";");
			unusedStringBuilder.append(indent[1] + "int i;");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "printf(\"Initializing Monitor for %d applications\\n\", appCount);");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "mapping[0] = 0;");

			for (int i = 0; i < policyObjectCount; i++)
			{
				C_UID temp = ObjectMapping.get(policyObjects.get(i));
				unusedStringBuilder.append(indent[1] + "mapping[" + policyObjects.get(i).toUpperCase() + "_UID] = " + temp.localUID + ";");
			}

			unusedStringBuilder.append(indent[1] + "app_num = appCount + " + (policyObjectCount + 1) + ";");
			unusedStringBuilder.append("");
			// Free static variable
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC))
				{
					unusedStringBuilder.append(indent[1] + "free(" + policy_list.relations.get(i) + ");");
				}
			}
			unusedStringBuilder.append("");

			// Allocate space for all static variable
			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					Print.numberOfStaticAtoms ++;
					unusedStringBuilder.append(indent[1] + policy_list.relations.get(i) + " = (char*) malloc(sizeof(char) * " +
							sizeArr[policy_list.relationsCard.get(i)] + ");");
					unusedStringBuilder.append(indent[1] + "memset(" + policy_list.relations.get(i) + ", 0, sizeof(char) * app_num);");
				}
			}

			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "for (i = 0; i < appCount; i++) {");

			unusedStringBuilder.append(indent[2] + "mapping[UID[i]] = appIdx++;");

			unusedStringBuilder.append(indent[1] + "}");
			unusedStringBuilder.append("");

			{
				unusedStringBuilder.append(indent[1] + "if (hist == NULL) {");
				unusedStringBuilder.append(indent[2] + "hist = (History**) malloc(sizeof(History*) * 2);");
				unusedStringBuilder.append(indent[2] + "hist[0] = NULL;");
				unusedStringBuilder.append(indent[2] + "hist[1] = NULL;");
				unusedStringBuilder.append(indent[1] + "}");
			}
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "History_Dispose(hist[0]);");
			unusedStringBuilder.append(indent[1] + "History_Dispose(hist[1]);");
			unusedStringBuilder.append(indent[1] + "hist[0] = History_Constructor(0);");
			unusedStringBuilder.append(indent[1] + "hist[1] = History_Constructor(0);");
			unusedStringBuilder.append(indent[1] + "History_Reset(hist[0]);");
			unusedStringBuilder.append(indent[1] + "History_Reset(hist[1]);");
			unusedStringBuilder.append("");

			int callRelationID = -1;
			for (int i = 0; i < policy_list.relationCount; i++)
			{
				if (policy_list.relations.get(i).equalsIgnoreCase("call") || policy_list.relations.get(i).equalsIgnoreCase("calls"))
				{
					callRelationID = i;
					break;
				}
			}

			unusedStringBuilder.append(indent[1] + "currentHist = 0;");
			unusedStringBuilder.append(indent[1] + "notInitialized = 0;");
			unusedStringBuilder.append(indent[1] + "return module_policyID;");
			unusedStringBuilder.append(indent[0] + "}");

			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "int LogicDroid_Module_checkEvent(int rel, int *UID, int varCount, long timestamp) {");
			unusedStringBuilder.append(indent[1] + "int varIdx = 0;");
			unusedStringBuilder.append(indent[1] + "int mul = 1;");
			unusedStringBuilder.append(indent[1] + "int i = 0;");
			unusedStringBuilder.append(indent[1] + "char result;");
			unusedStringBuilder.append(indent[1] + "if (notInitialized) {");
			unusedStringBuilder.append(indent[2] + "return 0;");
			unusedStringBuilder.append(indent[1] + "}");
			unusedStringBuilder.append("");

			unusedStringBuilder.append(indent[1] + "History_Reset(hist[!currentHist]);");
			unusedStringBuilder.append(indent[1] + "currentHist = !currentHist;");
			unusedStringBuilder.append(indent[1] + "hist[currentHist]->timestamp = timestamp;");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "//if (UID != NULL) {");
			unusedStringBuilder.append(indent[2] + "for (i = varCount - 1; i >= 0; mul *= app_num, i--) {");

			unusedStringBuilder.append(indent[3] + "varIdx += mapping[UID[i]] * mul;");

			unusedStringBuilder.append(indent[2] + "}");

			unusedStringBuilder.append(indent[2] + "History_insertEvent(hist[currentHist], rel, varIdx);");
			unusedStringBuilder.append(indent[1] + "//}");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "result = History_Process(hist[currentHist], hist[!currentHist]);");

			unusedStringBuilder.append(indent[1] + "if (result) {");

			unusedStringBuilder.append(indent[2] + "currentHist = !currentHist;");
			unusedStringBuilder.append(indent[1] + "}");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "return result;");
			unusedStringBuilder.append(indent[0] + "}");
			unusedStringBuilder.append("");


			unusedStringBuilder.append(indent[0] + "History* History_Constructor(long timestamp) {");
			unusedStringBuilder.append(indent[1] + "History *retVal = (History*) malloc(sizeof(History));");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "retVal->atoms = (char**) malloc(sizeof(char*) * " + policy_list.relationCount + ");");

			Print.numberOfAtoms = policy_list.relationCount;
			Print.atomDeclaration += policy_list.relationCount;
			int atomCount = 0;

			StringBuilder memsetSb = new StringBuilder();

			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.DYNAMIC)) {
					Print.atomDeclaration += "|1|" + policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)));

					unusedStringBuilder.append(indent[1] + "retVal->atoms[" + i + "] = (char*) malloc(sizeof(char) * " +
							sizeArr[policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)))] + "); // " + policy_list.relations.get(i));
					memsetSb.append(indent[1] + "memset(h->atoms[" + i + "], 0, sizeof(char) * " +
							sizeArr[policy_list.relationsCard.get(mapping.get(policy_list.relations.get(i)))] + ");\n");
				}
				else if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					Print.atomDeclaration += "|0|" + (atomCount++) ;

					unusedStringBuilder.append(indent[1] + "retVal->atoms[" + i + "] = " + policy_list.relations.get(i) + "; // " + policy_list.relations.get(i));
				}
			}


			memsetSb.append("\n");
			unusedStringBuilder.append(indent[1] + "retVal->propositions = (char***) malloc(sizeof(char**) * " + policy_list.formulaCount + ");");

			Print.formulaDeclaration += policy_list.formulaCount;

			// Traverse all the formula for initialising the space
			StringBuilder spaceSb = new StringBuilder();
			idx = 0;
			ArrayList<ArrayList<Integer>> atomsIdx = new ArrayList<ArrayList<Integer>>(); // This is used for history disposal, as we need to know which propositions referring to the static data
			atomsIdx.add(new ArrayList<Integer>());
			traverse_space(policy_list.formulas.get(0), spaceSb, memsetSb, indent[1], 0, atomsIdx.get(0));
			// idx (a "global" variable) resulting from traverse_space will tell us how many sub formulas are there

			unusedStringBuilder.append(indent[1] + "retVal->propositions[0] = (char**) malloc(sizeof(char*) * " + idx + ");");

			Print.formulaDeclaration += "|" + idx;

			ArrayList<Integer> propositionIdx = new ArrayList<Integer>();
			propositionIdx.add(idx);

			for (int i = 1; i < policy_list.formulaCount; i++) {
				idx = 0;
				atomsIdx.add(new ArrayList<Integer>());
				traverse_space(policy_list.formulas.get(i), spaceSb, memsetSb, indent[1], i, atomsIdx.get(i));
				unusedStringBuilder.append(indent[1] + "retVal->propositions[" + i + "] = (char**) malloc(sizeof(char*) * " + idx + ");");

				Print.formulaDeclaration += "|" + idx;

				unusedStringBuilder.append(indent[1] + "retVal->propositions[" + i + "][0] = retVal->atoms[" + mapping.get(policy_list.target_recursive.get(i)) + "];");

				Print.updateResourceAllocation ("|1|" + i + "|0|" + mapping.get(policy_list.target_recursive.get(i)));
				propositionIdx.add(idx);
				atomsIdx.get(i).add(mapping.get(policy_list.target_recursive.get(i)));
			}
			unusedStringBuilder.append("");
			unusedStringBuilder.append(spaceSb.toString());
			// End Traverse

			if (num_temp > 0)
			{
				unusedStringBuilder.append(indent[1] + "retVal->time_tag = (int**) malloc(sizeof(int*) * num_temp);");
				// Traverse all the formula for temporal operator
				idx = 0;
				spaceSb = new StringBuilder();
				memsetSb.append("\n");
				for (int i = policy_list.formulaCount - 1; i >= 0; i--)
				{
					traverse_timetag(policy_list.formulas.get(i), spaceSb, memsetSb, indent[1]);
				}
				unusedStringBuilder.append(spaceSb.toString());
				// End Traverse
			}
			unusedStringBuilder.append(indent[1] + "retVal->timestamp = timestamp;");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "return retVal;");
			unusedStringBuilder.append(indent[0] + "}");

			unusedStringBuilder.append(indent[0] + "void History_Reset(History *h) {");
            unusedStringBuilder.append(memsetSb.toString());
			unusedStringBuilder.append(indent[0] + "}");

			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "void History_insertEvent(History *h, int rel, int idx) {");
			unusedStringBuilder.append(indent[1] + "h->atoms[rel][idx] = 1;");
			unusedStringBuilder.append(indent[0] + "}");

			unusedStringBuilder.append("");

			unusedStringBuilder.append(indent[0] + "// Additional function to clean up garbage");
			unusedStringBuilder.append(indent[0] + "void History_Dispose(History *h) {");
			unusedStringBuilder.append(indent[1] + "int i;");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "if (h == NULL) return;");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "// Don't remove the actual data aliased by the variables");
			for (int i = 0; i < atomsIdx.size(); i++)
			{
				ArrayList<Integer> temp = atomsIdx.get(i);
				for (int j = 0; j < temp.size(); j++)
				{
					unusedStringBuilder.append(indent[1] + "h->propositions[" + i + "][" + temp.get(j) + "] = NULL;");
				}
			}
			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					unusedStringBuilder.append(indent[1] + "h->atoms[" + i + "] = NULL;");
				}
			}
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "// clean propositions");
			for (int i = 0; i < policy_list.formulaCount; i++)
			{
				unusedStringBuilder.append(indent[1] + "for (i = 0; i < " +  propositionIdx.get(i) + "; i++) {");
				unusedStringBuilder.append(indent[2] + "free(h->propositions[" + i + "][i]);");
				unusedStringBuilder.append(indent[1] + "}");
				unusedStringBuilder.append(indent[1] + "free(h->propositions[" + i + "]);");
			}
			unusedStringBuilder.append(indent[1] + "free(h->propositions);");
			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[1] + "// Clean atoms");
			unusedStringBuilder.append(indent[1] + "for (i = 0; i < " + policy_list.relationCount + "; i++) {");
			unusedStringBuilder.append(indent[2] + "free(h->atoms[i]);");
			unusedStringBuilder.append(indent[1] + "}");
			unusedStringBuilder.append(indent[1] + "free(h->atoms);");
			unusedStringBuilder.append("");
			if (num_temp > 0)
			{
				unusedStringBuilder.append(indent[1] + "// Clean temporal metric");
				unusedStringBuilder.append(indent[1] + "for (i = 0; i < num_temp; i++) {");
				unusedStringBuilder.append(indent[2] + "free(h->time_tag[i]);");
				unusedStringBuilder.append(indent[1] + "}");
				unusedStringBuilder.append(indent[1] + "free(h->time_tag);");
				unusedStringBuilder.append("");
			}
			unusedStringBuilder.append(indent[1] + "// finally free the history reference");
			unusedStringBuilder.append(indent[1] + "free(h);");
			unusedStringBuilder.append(indent[0] + "}");

			unusedStringBuilder.append("");
			unusedStringBuilder.append(indent[0] + "void cleanup_monitor_module(void) {");
			unusedStringBuilder.append(indent[1] + "printf(\"Detaching the policy from the monitor stub in kernel\\n\");");
			unusedStringBuilder.append(indent[1] + "free(relations);");
			unusedStringBuilder.append(indent[1] + "History_Dispose(hist[0]);");
			unusedStringBuilder.append(indent[1] + "History_Dispose(hist[1]);");
			for (int i = 0; i < policy_list.relationCount; i++) {
				if (policy_list.relation_type.get(i).equals(GlobalVariable.STATIC)) {
					unusedStringBuilder.append(indent[1] + "free(" + policy_list.relations.get(i) + ");");
				}
			}
			unusedStringBuilder.append(indent[0] + "}");
			unusedStringBuilder.append("");

			//ps.close();
		}
		catch (Exception e)
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
		if (policy.type.equals(GlobalVariable.DIAMONDDOT)) {
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
		for (int i = 0; i < policy.count; i++)
		{
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
			Print.inputString = Print.numberOfStaticAtoms + "|" + policy_list.tempCount + "|" + Print.timeTagSize + "|" + Print.atomDeclaration + "|" + Print.formulaDeclaration + "|" + Print.numberOfResourcesAllocationRecords + Print.resourceAllocation + "|00" + Print.numberOfInstructions + Print.dependencyTable;
            Print.policyID = policy_list.ID;
            Print.relations = new String[policy_list.relationCount];

			for (int i=0; i < policy_list.relationCount; i++) {
				Print.relations[i] = policy_list.relations.get(i);
				if (policy_list.relations.get(i).equalsIgnoreCase("call") || policy_list.relations.get(i).equalsIgnoreCase("calls"))
                    Print.callRelationID = i;
			}

            Print.reset();
		}
	}
}
