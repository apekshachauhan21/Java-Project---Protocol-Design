package serverApplication;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.util.ArrayList;
public class ServerApplicationOperations {

	private String request; // request message
	private String response;// response message
	private int[] measurementValue;// measurement Ids present in Data.txt
	private float[] temperatureValue;// temperature values present in Data.txt 
	private int code;// response Code
	
	/*Single argument constructor for initializing the request message and generating the 
	corresponding response message*/
	ServerApplicationOperations(String request){
		this.request = request;
		fileReader();
		this.response = responseMessageGeneration(this.request);
		
	}
	
	
	
	// returns the response message
	public String getResponse( ){
		return response;
	}
	
	/* The function generates a response message based on the input request message and also by checking the checksum value 
	 *Syntax, Measurement ID and thus assigns a code to the response Message. First the function breaks the request message into <id>value</id> and <measurement>value</measurement>
	 *Then passes the entire request message into the responseCodeGeneration function and adds the code to the response message 
	 */
	public String responseMessageGeneration(String request) {
		
		int second = request.indexOf("<", request.indexOf("<") + 1);
		int third = request.indexOf("<", second + 1);
		int forth = request.indexOf("<", third + 1);
		int fifth = request.indexOf("<", forth + 1);
		int sixth = request.indexOf("<", fifth + 1);
		
		String response = ""; // local response variable
		
		String subString1 = request.substring(second, third);// <id> value </id>
		String subString2 = request.substring(forth, sixth);// <measurement>value</measurement>
		int mValueSecond = subString2.indexOf("<", request.indexOf("<") + 1);
		String mValue = subString2.substring(subString2.indexOf(">") + 1, mValueSecond);
		int measurementValueInt = Integer.parseInt(mValue);
		
		//Call to responseCodeGeneration function
		this.code = responsecodeGeneration(request);
		
		String responseSub;// local variable for storing the response message without integrity check
		if (this.code == 0) {
			responseSub = "<response>" + subString1 + "<code>" + this.code + "</code>" + subString2 + "<value>"
					+ temperatureValueCompute(measurementValueInt) + "</value>" + "</response>";
			char responseCheck = integrityCheck(responseSub);
			int reponseCheckSumValue = (int) responseCheck;
			
			response = responseSub + reponseCheckSumValue;
		} else {
			responseSub = "<response>" + subString1 + "<code>" + code + "</code>" + subString2 + "</response>";
			char responseCheck = integrityCheck(responseSub);
			int reponseCheckSumValue = (int) responseCheck;
			response = responseSub + reponseCheckSumValue;
		}
		
		return response;
		
	}
    /*
     * The function generates response code for a request message. Below are the Code Values:
     * 0 : request message no Syntax error, no check sum error, and measurement value exits in the data file
     * 1 : Integrity check for request failed
     * 2 : Syntax check failed
     * 3 : Measurement Value not found 
     * Function breaks the request into sub elements and then makes the necessary checks
     */
	public int responsecodeGeneration(String request) {
		
		int first = request.indexOf("<");
		int second = request.indexOf("<", request.indexOf("<") + 1);
		int third = request.indexOf("<", second + 1);
		int forth = request.indexOf("<", third + 1);
		int fifth = request.indexOf("<", forth + 1);
		int sixth = request.indexOf("<", fifth + 1);
		
		//<request>
		String openningRequestTag = request.substring(request.indexOf("<"), request.indexOf(">") + 1);
		
		//breaks into <id>value</id>
		String id = request.substring(second, forth);
		int iValueSecond = id.indexOf("<", id.indexOf("<") + 1);
		String iValue = id.substring(id.indexOf(">") + 1, iValueSecond);
		String idNew = id.replace(iValue, "");
		
		//<measurement>value</measurement>
		String measurement = request.substring(forth, sixth);
		int mValueSecond = measurement.indexOf("<", request.indexOf("<") + 1);
		String mValue = measurement.substring(measurement.indexOf(">") + 1, mValueSecond);
		mValue = mValue.replaceAll("\\D+","");
		int measurementValue = Integer.parseInt(mValue);
		String newMeasurement = measurement.replace(mValue, "");
		
		//</request>
		String requestclosingTag = request.substring(sixth, request.lastIndexOf(">") + 1);
		
		//CheckSum value in the request string
		String checkSumString = request.substring(request.lastIndexOf(">") + 1, request.length());
		checkSumString = checkSumString.replaceAll("\\D+", "");
		int integrityCheckNum = Integer.parseInt(checkSumString);
		
		//Check the syntax of the request message
		Boolean requestCompare = openningRequestTag.equals("<request>") && idNew.equals("<id></id>")
				&& newMeasurement.equals("<measurement></measurement>") && requestclosingTag.equals("</request>");
		Boolean order = false;
		if(request.indexOf(openningRequestTag)<request.indexOf("<id>")&&request.indexOf("<id>")<request.indexOf("</id>")&&request.indexOf("</id>")<request.indexOf("<measurement>")&&request.indexOf("<measurement>")<request.indexOf("</measurement>")&&request.indexOf("</measurement>")<request.indexOf("</request>")){
			order = true;
		}
		Boolean syntaxCheck = requestCompare&&order;
		
		//Checks for the integrity check of the request and computed integrity check
		Boolean integrityCheck = false;
		String test = request.replace((request.substring(request.lastIndexOf(">") + 1, request.length())), "");
		
		char checkSum = integrityCheck(test);
		int checkSumValue = (int) checkSum;
		
		if (checkSumValue == integrityCheckNum){
			
			integrityCheck = true;
			
		}
		
		//Checks if the measurement value exits in Data.txt file
		int measurementCheck1 = measurementValueCheck(measurementValue);
		
		boolean measurementCheck = false;
		if(measurementCheck1== measurementValue){
			measurementCheck = true;
			
		}
		if (syntaxCheck && integrityCheck && measurementCheck){
			
			this.code = 0;
		}
		else if (!syntaxCheck)
			this.code = 2;
		
		else if (!integrityCheck)
			this.code = 1;
		
		else if (!measurementCheck || measurementValue < 0 || measurementValue > 65535)
			this.code = 3;
        return this.code;
	}

    /*
     * Function checks if the measurement value exits in the data file
     */
	public int measurementValueCheck(int value) {
		
		
		int temp = 0; // returns the measurement value
		
		for (int i = 0; i < measurementValue.length; i++) {
			
			if (measurementValue[i] == value) {
				temp = measurementValue[i];
				
			} 
		}
		return temp;
	}
	
	/*
	 * Function returns the temperature value for the corresponding measurement value 
	 */
	public float temperatureValueCompute(int value) {
		

		int index = 0;
		float temperatureMeasurementValue;
		
		for (int i = 0; i < measurementValue.length; i++) {
			if (measurementValue[i] == value) {
				index = i;
			}

		}
		temperatureMeasurementValue = temperatureValue[index];

		return temperatureMeasurementValue;
	}

	/*
	 * Reads data from the file and fetches the measurement value and temperature value 
	 */
	public void fileReader(){
		String fileName = "data.txt";
		String line = null;
		ArrayList<String> lineRead = new ArrayList<String>();
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(fileName);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				lineRead.add(line);
			}
			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + fileName + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + fileName + "'");

		}
        
		int numberOfLines = lineRead.size();
		measurementValue = new int[numberOfLines];
		temperatureValue = new float[numberOfLines];
		
		for(int i =0;i<numberOfLines;i++){
			String[] stringSplit = lineRead.get(i).split("\t");
			measurementValue[i] = Integer.parseInt(stringSplit[0]);
			temperatureValue[i] = Float.parseFloat(stringSplit[1]);
			
			
		}
	}
	
    /*
     * Calculates the Check	Sum Value for the a string
     */
	public char integrityCheck(String request) {
		int length = request.length();
		char[] charArray = new char[length];
		for (int i = 0; i < length; i++) {
			charArray[i] = request.charAt(i);

		}
		int charLength = charArray.length;
		int count, loopCount = 0;
		if (charLength % 2 == 0) {
			count = charLength / 2;
		} else
			count = (charLength + 1) / 2;

		char[] wordSeqChar = new char[count];
		int[] wordSeqInt = new int[count];

		int MSB, LSB;
		for (int i = 0; i < count; i++) {
			if (i < count - 2) {
				MSB = (int) charArray[loopCount];
				LSB = (int) charArray[loopCount + 1];
				loopCount = loopCount + 2;
				wordSeqChar[i] = wordSeqCalculation(MSB, LSB);
				wordSeqInt[i] = (int) wordSeqChar[i];
			} else {
				MSB = (int) charArray[loopCount];
				LSB = 0;
				wordSeqChar[i] = (wordSeqCalculation(MSB, LSB));
				wordSeqInt[i] = (int) wordSeqChar[i];

			}
		}
		int S = 0, index, C = 7919, D = 65536;
		for (int i = 0; i < wordSeqInt.length; i++) {
			index = S ^ wordSeqInt[i];
			S = (C * index) % D;
		}
		return (char) S;
	}
	
    // Function returns a 16 bit unsigned Integer function
	public char wordSeqCalculation(int MSB, int LSB) {
		int[] msbArray_temp = new int[8];
		int[] msbArray = new int[8];
		int i = 0;
		while (MSB > 0) {
			msbArray_temp[i] = (MSB % 2);
			MSB = (MSB / 2);
			i++;
		}
		i = 8;
		int sum = 0;
		for (int j = 0; j < 8; j++) {
			msbArray[j] = msbArray_temp[msbArray_temp.length - (j + 1)];
		}
		int power = 0;
		for (int j = 0; j < 8; j++) {
			power = (int) (Math.pow(2, i));
			sum = sum + msbArray[msbArray.length - (j + 1)] * power;

			i++;
		}

		int sequence = sum + LSB;
		char sequenceWord = (char) sequence;
		return sequenceWord;

	}

	
}
