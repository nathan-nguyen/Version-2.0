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

import java.util.*;

public class FormulaIndexTraversal {
	private int idx;
	private int timetag_idx;
	private String indent[];
	private String sizeArr[];
	
		
	private int debugMode;
	private String secondIndexToString;
	

	public FormulaIndexTraversal(String indent[], String sizeArr[]) {
		this.indent = indent;
		this.sizeArr = sizeArr;
		idx = 0;
		timetag_idx = 0;

		
		debugMode = 0;
		
	}
	
	void reset() {
		idx = 0; 
		timetag_idx = 0;
	}
	
	private String determine_var(Formula sub, int index) {
		String var = sub.vars.get(index);
		// If the sub formula is an atom, there's a possibility of being an explicit object
		if (sub.type.equals(GlobalVariable.ATOM))
		{
			String var_type = ((Atom)sub).var_type.get(index);
			if (var_type.equals(GlobalVariable.OBJECT))
			{
				//var = Integer.toString(ObjectMapping.get(var).localUID); // Get the app ID from the mapping (integer)

				// Add object:localUID
				try {
					var = "" + Integer.parseInt(var);
				}
				catch (NumberFormatException e) {
					var = Integer.toString(Monitoring.getCUID_localUID(var)); // Get the app ID from the mapping (integer)
				}
			}
			// If it's a free variable then it's OK
		}
		// Else just return variable name as it is
		return var;
	}
	
	private static class FirstIndexingReturn
	{
		public int curr_indent;
		public int curr_idx;
		public String firstIdx;
		
		
		public HashMap<String, Integer> varIdx = new HashMap<String, Integer>();
		public String firstIndexToString;
		

		
		public FirstIndexingReturn(int curr_indent, int curr_idx, String firstIdx, HashMap<String,Integer> varIdx, String firstIndexToString)
		
		{
			this.curr_idx = curr_idx;
			this.curr_indent = curr_indent;
			this.firstIdx = firstIdx;

			
			this.varIdx = varIdx;
			this.firstIndexToString = firstIndexToString;
			
		}
	}
	
	private FirstIndexingReturn FirstIndexing(StringBuilder formulaStr, Formula policy, int indent_index)
	{		
		
		HashMap<String, Integer> varIdx = new HashMap<String, Integer>();
		String firstIndexToString = "";		
		
		formulaStr.append(indent[indent_index] + "// [" + idx + "] : "+ policy.toString() + "\n");

		
		int curr_indent = indent_index;
		for (int i = 0; i < policy.varCount; i++)
		{
			String var = policy.vars.get(i);
			formulaStr.append(indent[curr_indent] + "for (" + var + " = 0; " + var + " < app_num; " + var + "++) {\n");
			
			varIdx.put(var, i);
			
			curr_indent++;
		}
		StringBuilder firstIndex = new StringBuilder();
		if (policy.varCount > 0) {			
			if (policy.varCount == 1) {
				firstIndex.append(policy.vars.get(0));
				
			}
			else {
				firstIndex.append(policy.vars.get(0) + " * " + sizeArr[policy.varCount - 1]);				
			}
			
			String var = policy.vars.get(0);
			firstIndexToString += policy.varCount + "|";
			try {
				int intVar = Integer.parseInt(var);
				firstIndexToString += "0|";
				firstIndexToString += intVar + "|";	
			} catch (NumberFormatException e) {			
				firstIndexToString += "1|";
				firstIndexToString += varIdx.get(var) + "|";
			}
			
						
		}
		int len = policy.varCount;
		for (int j = 1; j < len; j ++) {
			if (j == len - 1) {
				firstIndex.append(" + " + policy.vars.get(j));
			}
			else {
				firstIndex.append(" + " + policy.vars.get(j) + " * " + sizeArr[len - j - 1]);
			}
			
			String var = policy.vars.get(j);
			try {
				int intVar = Integer.parseInt(var);
				firstIndexToString += "0|";
				firstIndexToString += intVar + "|";	
			} catch (NumberFormatException e) {			
				firstIndexToString += "1|";
				firstIndexToString += varIdx.get(var) + "|";
			}
			
						
		}
		if (len == 0) {			
			firstIndex.append("0");
			
			firstIndexToString += "1|0|0|";	
					
		}
		idx = idx + 1;
		
		return new FirstIndexingReturn(curr_indent, idx - 1, firstIndex.toString(), varIdx, firstIndexToString);
		
	}
	
	private void FirstIndexingClose(int curr_indent, StringBuilder formulaStr, int policyVarCount) {
		curr_indent--;
		formulaStr.append("\n");
		for (int i = 0; i < policyVarCount; i++)
		{
			formulaStr.append(indent[curr_indent] + "}\n");
			curr_indent--;
		}
	}
	
	
	private String getSecondIndex(Formula sub, HashMap<String, Integer> varIdx) {
	
		int len = sub.varCount;
		StringBuilder secondIndex = new StringBuilder();

		// Fix missing index bug
		if (len == 0) {
			String var2;
			try {
				var2 = determine_var(sub, 0);
			} catch (IndexOutOfBoundsException e) {
				var2 = "0";
			}
			secondIndexToString = "1|0|" + var2 + "|";
			secondIndex.append(var2);
			return secondIndex.toString();
		}
	
		secondIndexToString = len + "|";

		for (int j = 0; j < len; j++) {				
			String var = determine_var(sub, j);	
					
			try {
				int intVar = Integer.parseInt(var);
				secondIndexToString += "0|";
				secondIndexToString += intVar + "|";	
			} catch (NumberFormatException e) {			
				secondIndexToString += "1|";
				secondIndexToString += varIdx.get(var) + "|";
			}
			if (j == 0) {
				if (len > 1) {
					secondIndex.append(var + " * " + sizeArr[len - 1]);
				}
				else {
					secondIndex.append(var);
				}
			}
			else {
				if (j == len - 1) {
					secondIndex.append(" + " + var);
				}
				else {
					secondIndex.append(" + " + var + " * " + sizeArr[len - j - 1]);
				}
			}
		}
		return secondIndex.toString();
	}
	
	private void traverse_DiamondDot(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr) {
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for diamond dot formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);	
		
		String tmpIdx = getSecondIndex(policy.sub.get(0), ret.varIdx);		
		
		if (((DiamondDotFormula)policy).metric >= 0) {
			if (debugMode < 2)
				System.out.println("\nDIAMONDDOT-METRIC|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "5|" + policy_number + "|" + curr_idx + "|" + subIndex + "|" + timetag_idx + "|" + ((DiamondDotFormula)policy).metric);
			if (debugMode == 2)
				Print.dependancyTable += "|05|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "5|" + policy_number + "|" + curr_idx + "|" + subIndex + "|" + timetag_idx + "|" + ((DiamondDotFormula)policy).metric;

			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = 0;\n");

			formulaStr.append(indent[curr_indent] + "next->time_tag[" + timetag_idx + "][" + firstIndex + "] = 0;\n");
			formulaStr.append(indent[curr_indent] + "delta = (next->timestamp - prev->timestamp);\n");
			formulaStr.append(indent[curr_indent] + "if (delta <= metric[" + timetag_idx + "])\n");
			formulaStr.append(indent[curr_indent] + "{\n");
			formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
			formulaStr.append(indent[curr_indent + 1] + "next->time_tag[" + timetag_idx + "][" + firstIndex + "] = (int) delta;\n");
			formulaStr.append(indent[curr_indent + 1] + "if (!next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] && prev->time_tag[" + timetag_idx + "][" + firstIndex + "] + delta <= metric[" + timetag_idx + "])\n");
			formulaStr.append(indent[curr_indent + 1] + "{\n");
			formulaStr.append(indent[curr_indent + 2] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex +"];\n");
			formulaStr.append(indent[curr_indent + 2] + "if (next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "]) next->time_tag[" + timetag_idx + "][" + firstIndex.toString() + "] += prev->time_tag[" + timetag_idx + "][" + firstIndex + "];\n");
			formulaStr.append(indent[curr_indent + 1] + "}\n");
			formulaStr.append(indent[curr_indent] + "}\n");			

			timetag_idx++;
		}
		else {// just original LTL formula
			if (debugMode < 2)
				System.out.print("\nDIAMONDDOT|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex);
			if (debugMode == 2)
				Print.dependancyTable += "|06|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex;

			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "] || prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "];\n");
		}
	}
	
	private void traverse_Diamond(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for diamond formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		
		String tmpIdx = getSecondIndex(policy.sub.get(0), ret.varIdx);
		
		if (((DiamondFormula)policy).metric >= 0) {

			if (debugMode < 2)
				System.out.println("\nDIAMOND-METRIC|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "5|" + policy_number + "|" + curr_idx + "|" + subIndex + "|" + timetag_idx + "|" + ((DiamondFormula)policy).metric);
			if (debugMode == 2)
				Print.dependancyTable += "|07|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "5|" + policy_number + "|" + curr_idx + "|" + subIndex + "|" + timetag_idx + "|" + ((DiamondFormula)policy).metric;

			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
			formulaStr.append(indent[curr_indent] + "next->time_tag[" + timetag_idx + "][" + firstIndex + "] = 0;\n");
			formulaStr.append(indent[curr_indent] + "delta = (next->timestamp - prev->timestamp);\n");
			formulaStr.append(indent[curr_indent] + "if (!next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] && prev->time_tag[" + 
								timetag_idx + "][" + firstIndex + "] + delta <= metric[" + timetag_idx + "])\n");
			formulaStr.append(indent[curr_indent] + "{\n");
			formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex +"];\n");
			formulaStr.append(indent[curr_indent + 1] + "next->time_tag[" + timetag_idx + "][" + firstIndex + 
								"] = delta + prev->time_tag[" + timetag_idx + "][" + firstIndex + "];\n");
			formulaStr.append(indent[curr_indent] + "}\n");
			timetag_idx++;
		}
		else { // just original LTL formula
			if (debugMode < 2)	
				System.out.println("\nDIAMOND|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex);
			if (debugMode == 2)
				Print.dependancyTable += "|08|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex;

			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "] || " +
					"prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "];\n");
		}
	}
	
	private void traverse_Since(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr)
	{
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only two sub formulas for since formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		
		String tmpIdx = getSecondIndex(policy.sub.get(0), ret.varIdx);
		
		String firstMessage = "";	
		String secondMessage;	
		boolean isMetric = ((SinceFormula)policy).metric >= 0;
		if (isMetric) {
			if (debugMode < 2)
				firstMessage = "\nSINCE-METRIC|" + ret.varIdx.size() + "|3|" + ret.firstIndexToString + secondIndexToString;
			if (debugMode == 2)
				firstMessage = "|09|" + ret.varIdx.size() + "|3|" + ret.firstIndexToString + secondIndexToString;
			secondMessage = "6|" + policy_number + "|" + curr_idx + "|" + subIndex + "|" + timetag_idx + "|" + ((SinceFormula)policy).metric;

			formulaStr.append(indent[curr_indent] + "next->time_tag[" + timetag_idx + "][" + firstIndex.toString() + "] = 0;\n");
			formulaStr.append(indent[curr_indent] + "delta = (next->timestamp - prev->timestamp);\n");
			formulaStr.append(indent[curr_indent] + "if (delta <= metric[" + timetag_idx + "]) {\n");
			formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + "] = (next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx +"] || ");
		}
		else {
			if (debugMode < 2)
				firstMessage = "\nSINCE|" + ret.varIdx.size() + "|3|" + ret.firstIndexToString + secondIndexToString;
			if (debugMode == 2)
				firstMessage = "|10|" + ret.varIdx.size() + "|3|" + ret.firstIndexToString + secondIndexToString;
			secondMessage = "4|" + policy_number + "|" + curr_idx + "|" + subIndex;

			formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + "] = (next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "] && ");
		}
		
		subIndex = idx;
		traverse_index(policy.sub.get(1), indent_index, result, policy_number);
		
		tmpIdx = getSecondIndex(policy.sub.get(1), ret.varIdx);
		firstMessage += secondIndexToString;
		secondMessage += "|" + subIndex;
		
		if (isMetric) {			
			formulaStr.append("next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + 
					"]) && (next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx +"] || " + 
					"prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + "]);\n");
			formulaStr.append(indent[curr_indent + 1] + "if (!next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "]) next->time_tag[" + timetag_idx + "][" + firstIndex.toString() + "] = delta + prev->time_tag[" + timetag_idx + "][" + firstIndex.toString() + "];\n");
			formulaStr.append(indent[curr_indent] + "}\n");
			formulaStr.append(indent[curr_indent] + "else {\n");
			formulaStr.append(indent[curr_indent + 1] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() + 
					"] = next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
			formulaStr.append(indent[curr_indent] + "}\n");
			timetag_idx++;
		}
		else { // just original LTL formula			
			formulaStr.append("prev->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex.toString() +  "]) || " +
					"next->propositions[" + policy_number + "][" + subIndex + "][" + tmpIdx + "];\n");
		}
		if (debugMode < 2)
			System.out.println(firstMessage + secondMessage);
		if (debugMode == 2)
			Print.dependancyTable += firstMessage + secondMessage;
	}
	
	private void traverse_Exist(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr) {		
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for Exists formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		
		int add_idx = curr_indent;
		ArrayList<String> varIndex = ((ExistFormula)policy).varIndex;
		int varIndexCount = ((ExistFormula)policy).varIndexCount;
		for (int j = 0; j < varIndexCount; j++)
		{
			String var = varIndex.get(j);
			formulaStr.append(indent[add_idx] + "for (" + var + " = 0; " + var + " < app_num && !next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "]; " + var + "++) {\n");
			
			ret.varIdx.put(var, ret.varIdx.size());
			
			add_idx++;			
		}		
		
		String secondIndex = getSecondIndex(policy.sub.get(0), ret.varIdx);

		if (debugMode < 2)
			System.out.println("\nEXIST|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex);		
		if (debugMode == 2)
			Print.dependancyTable += "|00|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex;	
		
		formulaStr.append(indent[add_idx] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] || next->propositions[" + 
				policy_number + "][" + subIndex + "][" + secondIndex + "];\n");

		add_idx--;				
		for (int j = 0; j < varIndexCount; j++)
		{
			formulaStr.append(indent[add_idx] + "}\n");
			add_idx--;
		}
	}
	
	private void traverse_Forall(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr) {
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for Forall formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);
		
		int add_idx = curr_indent;
		ArrayList<String> varIndex = ((UniversalFormula)policy).varIndex;
		int varIndexCount = ((UniversalFormula)policy).varIndexCount;
		for (int j = 0; j < varIndexCount; j++) {
			String var = varIndex.get(j);

			formulaStr.append(indent[add_idx] + "for (" + var + " = 0; " + var + " < app_num && next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "]; " + var + "++) {\n");
			ret.varIdx.put(var, ret.varIdx.size());
			add_idx++;
		}

		String secondIndex = getSecondIndex(policy.sub.get(0), ret.varIdx);

		if (debugMode < 2)
			System.out.print("\nFORALL|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex + "\n");			
		if (debugMode == 2)
			Print.dependancyTable += "|04|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex + "\n";

		formulaStr.append(indent[add_idx] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] && next->propositions[" + 
				policy_number + "][" + subIndex + "][" + secondIndex + "];\n");

		add_idx--;
		for (int j = 0; j < varIndexCount; j++)
		{
			formulaStr.append(indent[add_idx] + "}\n");
			add_idx--;
		}
	}
	
	private void traverse_Not(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr) {
		
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		// Assume well formed, there is only one sub formula for Not formula
		int subIndex = idx;
		traverse_index(policy.sub.get(0), indent_index, result, policy_number);					
						
		String secondIndex = getSecondIndex(policy.sub.get(0), ret.varIdx);
		if (debugMode < 2)
			System.out.println("\nNOT|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex);
		if (debugMode == 2)
			Print.dependancyTable += "|03|" + ret.varIdx.size() + "|2|" + ret.firstIndexToString + secondIndexToString + "3|" + policy_number + "|" + curr_idx + "|" + subIndex;
		formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = !next->propositions[" + policy_number + "][" + subIndex + "][" + secondIndex + "];");		
						
	}
	
	private void traverse_AndOr(Formula policy, int indent_index, ArrayList<String> result, int policy_number, 
			FirstIndexingReturn ret, StringBuilder formulaStr, String operator) {		
		int curr_indent = ret.curr_indent;
		int curr_idx = ret.curr_idx;
		String firstIndex = ret.firstIdx;
		
		
		String message = "";
		if (debugMode < 2)
			if (operator == "&&")
				message = "\nAND|" + ret.varIdx.size() + "|" + (policy.count + 1) + "|" + ret.firstIndexToString;
			else
				message = "\nOR|" + ret.varIdx.size() + "|" + (policy.count + 1) + "|" + ret.firstIndexToString;
		if (debugMode == 2)
			if (operator == "&&")
				message = "|01|" + ret.varIdx.size() + "|" + (policy.count + 1) + "|" + ret.firstIndexToString;
			else
				message = "|02|" + ret.varIdx.size() + "|" + (policy.count + 1) + "|" + ret.firstIndexToString;
		String secondMessage = (policy.count + 2) + "|" + policy_number + "|" + curr_idx;
		for (int i = 0; i < policy.count; i++) {
			int subIndex = idx;
			traverse_index(policy.sub.get(i), indent_index, result, policy_number);
			
			String secondIndex = getSecondIndex(policy.sub.get(i), ret.varIdx);	
			message += secondIndexToString;
			secondMessage += ("|" + subIndex);
			if (i == 0) {
				formulaStr.append(indent[curr_indent] + "next->propositions[" + policy_number + "][" + curr_idx + "][" + firstIndex + "] = next->propositions[" + policy_number + "][" + subIndex + "][" + secondIndex + "]");
							
			}
			else {
				formulaStr.append(" " + operator + " next->propositions[" + policy_number + "][" + subIndex + "][" + secondIndex + "]"); 
			}
			if (i == (policy.count - 1))	formulaStr.append(";");			
		}
		if (debugMode < 2)
			System.out.println(message + secondMessage);				
		if (debugMode == 2)
			Print.dependancyTable += message + secondMessage;
		
	}
	
	public void traverse_index(Formula policy, int indent_index, ArrayList<String> result, int policy_number)
	{
		if (policy.type.equals(GlobalVariable.ATOM)) {idx = idx + 1; return;}
		
		Print.numberOfInstructions ++;

		StringBuilder formulaStr = new StringBuilder();
		FirstIndexingReturn ret = FirstIndexing(formulaStr, policy, indent_index);
		int curr_indent = ret.curr_indent;
		
		if (policy.type.equals(GlobalVariable.DIAMONDDOT)) traverse_DiamondDot(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.DIAMOND)) traverse_Diamond(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.SINCE)) traverse_Since(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.EXIST)) traverse_Exist(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.FORALL)) traverse_Forall(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.NOT)) traverse_Not(policy, indent_index, result, policy_number, ret, formulaStr);
		else if (policy.type.equals(GlobalVariable.AND)) traverse_AndOr(policy, indent_index, result, policy_number, ret, formulaStr, "&&");
		else if (policy.type.equals(GlobalVariable.OR)) traverse_AndOr(policy, indent_index, result, policy_number, ret, formulaStr, "||");
		
		FirstIndexingClose(curr_indent, formulaStr, policy.varCount);
		
		result.add(formulaStr.toString());
	}

	
	public void setDebugMode(int debugMode){
		this.debugMode = debugMode;
	}
	
}
