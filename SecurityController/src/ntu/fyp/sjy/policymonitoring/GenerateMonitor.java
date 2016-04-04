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

package ntu.fyp.sjy.policymonitoring;

import android.content.Context;

public class GenerateMonitor {

	public static void generateMonitor(String source, int debugMode, Context context) {
		Policy fList = FormulaParser.parse_formula(source, context);

		Monitoring.set_virtual_UID(fList);

        Monitoring.generate_kernel_monitor(fList, "Monitor");

		switch (debugMode){
			case 1: printMainFormula(fList);							
				Monitoring.generate_instruction_table(fList);
				break;
			case 2:
                printMainFormula(fList);
                Monitoring.generate_dependancy_table(fList,debugMode);
				break;
			default:printMainFormula(fList);
				System.out.println("\n");
				Monitoring.generate_source_code(fList);
		}
	}

	private static void printMainFormula(Policy fList){
		for (int i = 0; i < fList.formulaCount; i++) {
			if (i == 0)
				Print.formulaPrintOut = "\nMain Formula : " + fList.formulas.get(i);
			else
				Print.formulaPrintOut += "\n - " + fList.target_recursive.get(i) + " := " + fList.formulas.get(i);
		}
	}

}
