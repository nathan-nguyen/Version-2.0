package ntu.fyp.sjy.policymonitoring;

public class Print {

	public static int numberOfInstructions = 0;

	public static int numberOfAtoms = 0;

	public static int numberOfStaticAtoms = 0;

	public static int numberOfDynamicAtoms = 0;

	public static String atomDeclaration = "";

	public static String formulaDeclaration = "";

	public static String resourceAllocation = "";

	public static int numberOfResourcesAllocationRecords = 0;

	public static String dependencyTable = "";

	public static int timeTagSize = 0;

	public static void updateTimeTagSize(int newTimeTagSize){
		if (newTimeTagSize > timeTagSize)
			timeTagSize = newTimeTagSize;
	}

	public static void updateResourceAllocation(String update) {
		resourceAllocation += update;
		numberOfResourcesAllocationRecords++;
	}

    	public static String formulaPrintOut = "";
    	public static String inputString = "";
    	public static int policyID = 0;
    	public static String[] relations = null;
    	public static int callRelationID = 0;

    public static void reset(){
        numberOfInstructions = 0;
        numberOfAtoms = 0;
        numberOfStaticAtoms = 0;
        numberOfDynamicAtoms = 0;
        atomDeclaration = "";
        formulaDeclaration = "";
        resourceAllocation = "";
        numberOfResourcesAllocationRecords = 0;
        dependencyTable = "";
	timeTagSize = 0;
    }

}
