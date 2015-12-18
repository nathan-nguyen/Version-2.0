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

import java.util.HashMap;

//import java.util.*;

public class GenerateMonitor {

	/**
	 * @param args
	 */
	private static void generateMonitor(String source, String target, int debugMode) {
		//ArrayList<Formula> fList = FormulaParser.parse_formula("formula.xml");
		//System.out.println(fList.get(0));
		Policy fList = FormulaParser.parse_formula(source);

		//String sf = "((call(x, y)) | (exist_z((F_10(trans(x, z))) & (call(z, y)))))";
		//System.out.println(FormulaParser.parse_string(sf));
				
		//FormulaParser.formula_to_xml(fList, "testPrint.xml");
		
		Monitoring.set_virtual_UID(fList);		
		
		Monitoring.generate_kernel_monitor(fList, target);
		//Monitoring.generate_framework_monitor(fList, target);

		switch (debugMode){
			case 1: printMainFormula(fList);							
				Monitoring.generate_instruction_table(fList);
				break;
			case 2: printMainFormula(fList);
				Monitoring.generate_dependancy_table(fList,debugMode);
				break;
			default:printCopyRight();
				printMainFormula(fList);			
				System.out.println("\n");
				Monitoring.generate_source_code(fList);
		}
	}

	private static void printMainFormula(Policy fList){
		for (int i = 0; i < fList.formulaCount; i++) {
			if (i == 0)
				System.out.println("\nMain Formula : " + fList.formulas.get(i));
			else
				System.out.println(" - " + fList.target_recursive.get(i) + " := " + fList.formulas.get(i));
		}
		System.out.println("\n");
	}
	
	private static void printCopyRight(){
		System.out.println("\n\n########################################################################");
		System.out.println("#        PolicyMonitoring  Copyright (C) 2012-2013  Hendra Gunadi      #");
		System.out.println("#             This program comes with ABSOLUTELY NO WARRANTY;          #");
    		System.out.println("#     This is free software, and you are welcome to redistribute it    #");
     		System.out.println("# under certain conditions; See COPYING and COPYING.LESSER for details #");
     		System.out.println("########################################################################");
     		System.out.println();
	}

	public static void main(String[] args) {

		if (args.length == 1)
		{
			String source = args[0];
			String target = "Monitor";
						
			generateMonitor(source, target, 0);			
		}
		else if (args.length > 1){
			String source = args[0];
			String target = "Monitor";
			
			generateMonitor(source, target, Integer.parseInt(args[1]));
		}
		else
		{
			System.out.println("Input xml file required");
		}
	}

}
