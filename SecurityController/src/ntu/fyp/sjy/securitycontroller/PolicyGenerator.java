package ntu.fyp.sjy.securitycontroller;

import java.util.ArrayList;

public class PolicyGenerator {
	static String basicControl = "";
	
	final static String BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<policy>" +
			"<policyid>1</policyid>" +
			"<formulas>" +
			"<formula>" +
			"<type>main</type>" +
			"<exist>" +
			"<var>x</var>" +
			"<or>";
	
	final static String END = 
			"</or>"+
			"</exist>" +
			"</formula>";
			
	
	
	final static String END2 = "</formulas>" +
			"</policy>";
	
	public static String policyGenerator(ArrayList<String> basicList, ArrayList<String> advancedList, ArrayList<String> advancedSinkList, Boolean transCheck, int timeInterval){
		if (basicList.size() == 0 && advancedList.size() == 0)
			return "";
		
		final String TRANS = "<formula>"+
				"<type>rec:trans</type>"+
				"<or>"+
				"<atom>"+
				"<atom_type>dynamic</atom_type>"+
				"<rel> call </rel>"+
				"<var> x </var>" +
				"<var> y </var>" +
				"</atom>" +
				"<exist>" +
				"<var> z </var>" +
				"<and>" +
				"<diamonddot><metric> "+timeInterval+" </metric>" +
				"<atom><atom_type>dynamic</atom_type>" +
				"<rel> trans </rel>" +
				"<var> x </var>" +
				"<var> z </var>" +
				"</atom>" +
				"</diamonddot><atom><atom_type>dynamic</atom_type>" +
				"<rel> call </rel>" +
				"<var> z </var>" +
				"<var> y </var>" +
				"</atom>" +
				"</and>" +
				"</exist>" +
				"</or>" +
				"</formula>";
		
		String policy = BEGIN;
		for(int i=0; i<basicList.size(); i++){
			basicControl = basicList.get(i);
			String temp = "call";
			if(transCheck)
				temp = "trans";
			
			String basicAtom = 
					"<and>" +
					"<atom>" +
					"<atom_type>dynamic</atom_type>" +
					"<rel>"+temp+"</rel>" +
					"<var>x</var>" +
					"<var>object:" + basicControl +
					"</var>" +
					"</atom>" +
					"<not>" +
					"<atom>" +
					"<atom_type>static</atom_type>" +
					"<rel>trusted"+ basicControl.replaceFirst(basicControl.substring(0, 1), basicControl.substring(0, 1).toUpperCase()) +
					"</rel><var>x</var>" +
					"</atom>" +
					"</not>" +
					"<not>" +
					"<atom>" +
					"<atom_type>static</atom_type>" +
					"<rel>system</rel>" +
					"<var>x</var>" +
					"</atom>" +
					"</not>" +
					"</and>" ;
			policy = policy.concat(basicAtom);
		}
		for(int i=0; i<advancedList.size(); i++){
			for(int j=0; j<advancedSinkList.size(); j++){
				String advancedAtom = 
						"<and>" +
						"<atom>" +
						"<atom_type>dynamic</atom_type>" +
						"<rel>call</rel>" +
						"<var>x</var>" +
						"<var>object:"+ advancedSinkList.get(j) +
						"</var>" +
						"</atom>" +
						"<diamonddot><metric> "+ timeInterval + " </metric>"+
						"<atom>" +
						"<atom_type>dynamic</atom_type>" +
						"<rel>call</rel>"+
						"<var>x</var>" +
						"<var>object:"+ advancedList.get(i).split("_")[0] +
						"</var>"+
						"</atom>" +
						"</diamonddot>"+ 
						"<not>" +
						"<atom>" +
						"<atom_type>static</atom_type>" +
						"<rel>trusted_"+ advancedList.get(i).split("_")[0]+"_"+advancedSinkList.get(j) +
						"</rel>" +
						"<var>x</var>" +
						"</atom>" +
						"</not>" +
						"<not" +
						">" +
						"<atom>" +
						"<atom_type>static</atom_type>" +
						"<rel>system</rel>" +
						"<var>x</var>" +
						"</atom>" +
						"</not>" +
						"</and>";
				policy = policy.concat(advancedAtom);
			}
		}
		policy = policy.concat(END);
		if(transCheck){
			policy = policy.concat(TRANS);
		}
		policy = policy.concat(END2);
		System.out.println(policy);
		return policy;
	}
}
