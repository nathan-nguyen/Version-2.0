public class Print {

	public static int numberOfInstructions = 0;

	public static int numberOfAtoms = 0;

	public static int numberOfStaticAtoms = 0;

	public static int numberOfDynamicAtoms = 0;

	public static String atomDeclaration = "";

	public static String formulaDeclaration = "";

	public static String resourceAllocation = "";

	public static int numberOfResourcesAllocationRecords = 0;

	public static String dependancyTable = "";

	public static int timeTagSize = 0;

	public static void updateTimeTagSize(int newTimeTagSize){
		if (newTimeTagSize > timeTagSize)
			timeTagSize = newTimeTagSize;
	}

	public static void updateResourceAllocation(String update) {
		resourceAllocation += update;
		numberOfResourcesAllocationRecords++;
	}

}
