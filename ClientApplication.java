/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project640;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class ClientApplication
{
	public static String requestMessage;
	public static int measurementId;
	
	public static void main(String args[]) throws Exception 
	{
		processNewRequest();
	}
	
	private static void processNewRequest() throws Exception 
	{
		//Read Random Measurement ID
		measurementId = readRandomMeasurementIdFromSource();
		processRequest(measurementId);
	}
	
	private static void processRequest (int measurementId) throws Exception 
	{
		//Generate Random Request ID
		int requestId =  generateRandomRequestId();
				
		//Assemble the request message
		String requestMessageWithoutIntegrityCheck = assembleRequestMessage(measurementId, requestId);
                requestMessageWithoutIntegrityCheck = requestMessageWithoutIntegrityCheck.replaceAll("\\s","");
                System.out.println("Request Message with out integrity check value : " + requestMessageWithoutIntegrityCheck);
		
		//Generate Integrity Check Field Value
		int requestIntegrityCheckValue = (int)integrityCheck(requestMessageWithoutIntegrityCheck);
		//generating final request (includes actual request message and the integrity check value)
		StringBuilder finalRequestMessageBuilder = new StringBuilder(requestMessageWithoutIntegrityCheck);
		finalRequestMessageBuilder.append(requestIntegrityCheckValue);
		requestMessage = finalRequestMessageBuilder.toString();
		requestMessage = requestMessage.replaceAll("\\s", "");
		//send request to the server
                sendRequest(requestMessage);
	}
	
	private static int readRandomMeasurementIdFromSource()
	{
		BufferedReader br = null;
		List<String>measurementIds = new ArrayList<String>();
		
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader("data.txt"));

			while ((sCurrentLine = br.readLine()) != null)
			{
				String[] tokens = sCurrentLine.split("\t");
				measurementIds.add(tokens[0]);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally {
			try {
				if (br != null) { 
					br.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		Random randomizer = new Random();
		String randomMeasurementId = measurementIds.get(randomizer.nextInt(measurementIds.size()));
                randomMeasurementId = randomMeasurementId.replaceAll("\\D+","");
		return Integer.parseInt(randomMeasurementId);
	}

	private static int generateRandomRequestId()
	{
	    Random randomGenerator = new Random();	
		int randomNumber = (char) randomGenerator.nextInt(65536);	//char is unsigned
		return randomNumber;
	}

	private static String assembleRequestMessage(int measurementId, int requestId)
	{
		StringBuilder requestMessage = new StringBuilder();
		
		requestMessage.append("<request>");
		requestMessage.append("<id>");
		requestMessage.append(requestId);
		requestMessage.append("</id>");
		requestMessage.append("<measurement>");
		requestMessage.append(measurementId);
		requestMessage.append("</measurement>");
		requestMessage.append("</request>");
		
		return requestMessage.toString();
	}

	private static void sendRequest(String requestMessage) throws Exception
                
               
	{
		//Convert the request message to bytes and send it to server
		byte[] requestBytes = requestMessage.getBytes();
		InetAddress serverIPAddress = InetAddress.getByName("localhost");
		int serverPort = 58665  ;
		DatagramPacket sendPacket = new DatagramPacket(requestBytes, requestBytes.length, serverIPAddress, serverPort);
		
		DatagramSocket clientSocket = new DatagramSocket();
                System.out.println("Sending the request Message:" +requestMessage);
		System.out.println("Sending the request to the server...");
		clientSocket.send(sendPacket);
		
		byte[] responseBytes = new byte[9999];
		DatagramPacket receivePacket = new DatagramPacket(responseBytes, responseBytes.length);
		
		//set the initial timeout interval as 1 sec.
		int timeoutInterval = 1000;
		int retryCounter = 1;
		
		//Retry 3 times if the response is not received on time 
		while(retryCounter< 5) 
		{
			try {
				System.out.println("Receiving the response from the server. Timeout interval is set to :" + timeoutInterval);
				//start the timer with the pre-defined timeout interval
				clientSocket.setSoTimeout(timeoutInterval);
				clientSocket.receive(receivePacket);
				
				//process the response and print the response code.
				processResponse(responseBytes);
				break;
			}
			catch(InterruptedIOException ex)
			{
				if(retryCounter == 4)
				{
					//After the 4th timeout event,the transmitter should declare communication failure and print an error message
					System.out.println("Client socket timeout! Exception message: " + ex.getMessage());
					break;
				}

				retryCounter++;
				
				//increase timeout interval by one second every for re-try
				timeoutInterval = timeoutInterval + 1000;
			}
		}
		
		clientSocket.close();
	}

	private static void processResponse(byte[] responseBytes) throws Exception
	{
		String responseMessage = new String (responseBytes);
                System.out.println("Response Message from the server:" +responseMessage);
		String responseMessagewithoutChecksum_temp =	responseMessage.substring(responseMessage.lastIndexOf(">") + 1,
				responseMessage.length());
	    String responseMessagewithoutChecksum = responseMessage.replace(responseMessagewithoutChecksum_temp ,"") ;
		int expectedResponseIntegrityValue = (int)integrityCheck(responseMessagewithoutChecksum);
		//calculate integrity check value for the response message 
		//int expectedResponseIntegrityValue = integrityCheck(responseMessage);
			
		//compare with integrity check value supplied in the response message
		int actualResponseIntegrityValue = getResponseIntegrityValue(responseMessage);
		
		if(actualResponseIntegrityValue != expectedResponseIntegrityValue)
		{ 
			//repeating the send same request again and again as the integrity check values are not matching.
			processRequest(measurementId);
		}
		
		int responseCode = getResponseCode(responseMessage);
		//If the error was that the request message’s integrity check failed (response code 1), ask user if he/she would like to resend the request
		if(responseCode == 1) 
		{
			Scanner scanner = new Scanner(System.in);
			System.out.print("Request message’s integrity check failed with an response code 1. Would you like to resend the request? Please print Y/N");
			String userChoice = scanner.nextLine();
			if("Y".equalsIgnoreCase(userChoice))
			{
				//resend the same request
				sendRequest(requestMessage);
			}
		}
		
                else if(responseCode == 2) 
		{
			Scanner scanner = new Scanner(System.in);
			System.out.print("Error:malformed request.The syntax of the request message is not correct. Would you like to resend the request? Please print Y/N");
			String userChoice = scanner.nextLine();
			if("Y".equalsIgnoreCase(userChoice))
			{
				//resend the same request
				sendRequest(requestMessage);
			}
		}
                else if(responseCode == 3) 
		{
			Scanner scanner = new Scanner(System.in);
			System.out.print("Error:non-existent measurement.The measurement with the requested measurement ID does not exist. Would you like to resend the request? Please print Y/N");
			String userChoice = scanner.nextLine();
			if("Y".equalsIgnoreCase(userChoice))
			{
				//resend the same request
				sendRequest(requestMessage);
			}
		}
		
                else if(responseCode == 0) 
		{
			System.out.println("MeasurementValue : " + getMeasurementValue(responseMessage));

			Scanner scanner = new Scanner(System.in);
			System.out.print("Would you like to submit another request? Please print Y/N");
			String userChoice = scanner.nextLine();
			if("Y".equalsIgnoreCase(userChoice)) {
				//send a new request
				processNewRequest();
			}
		}
	}
	
	private static int getResponseCode(String responseMessage)
	{
		int beginIndex = responseMessage.indexOf(">", responseMessage.indexOf("<code")) + 1;
		int endIndex = responseMessage.indexOf("</code");
		String responseCodeString = responseMessage.substring(beginIndex, endIndex);
                responseCodeString = responseCodeString.replaceAll("\\D+","");
		int responseCode = Integer.parseInt(responseCodeString);
		System.out.println("response code : " + responseCode);
		return responseCode;
	}
	
	private static float getMeasurementValue(String responseMessage)
	{
		int beginIndex = responseMessage.indexOf(">", responseMessage.indexOf("value>")) + 1;
		int endIndex = responseMessage.indexOf("</value");
		String reponseMessageString = responseMessage.substring(beginIndex, endIndex);
                float measurementValue = Float.parseFloat(reponseMessageString);
		return measurementValue;
	}

	private static int getResponseIntegrityValue(String responseMessage) {
                String responseIntegrityCheck = responseMessage.substring(responseMessage.lastIndexOf(">") + 1,responseMessage.length());
                responseIntegrityCheck = responseIntegrityCheck.replaceAll("\\D+","");
		return Integer.parseInt(responseIntegrityCheck);
	}

	//Function to do integrity check
	public static char integrityCheck(String name)
	{
		int length = name.length();
		char[] charArray = new char[length];
		for (int i = 0; i<length; i++) 
		{
			charArray[i] = name.charAt(i);

		}
		int charLength = charArray.length;
		int count, loopCount = 0;
		if (charLength % 2 == 0) 
		{
			count = charLength / 2;
		} 
		else
			count = (charLength + 1) / 2;

		char[] wordSeqChar = new char[count];
		int[] wordSeqInt = new int[count];

		int MSB, LSB;
		for (int i = 0; i<count; i++)
		{
			if (i<count - 2) 
			{
				MSB =  (int)charArray[loopCount];
				LSB =  (int)charArray[loopCount + 1];
				loopCount = loopCount + 2;
				wordSeqChar[i] = wordSeqCalculation(MSB, LSB);
				wordSeqInt[i] = (int) wordSeqChar[i];
			} 
			else
			{
				MSB = (int)charArray[loopCount];
				LSB = 0;
				wordSeqChar[i] = (wordSeqCalculation(MSB, LSB));
				wordSeqInt[i] = (int) wordSeqChar[i];

			}
		}
	int S=0, index, C = 7919, D = 65536;
	for(int i=0;i<wordSeqInt.length;i++){
		index = S^wordSeqInt[i];
		S = (C*index)%D;
	    }
	
		return (char)S;
	}

	//Function for wordsequence calculation
	public static char wordSeqCalculation(int MSB, int LSB) {
		int[] msbArray_temp = new int[8];
		int[] msbArray = new int[8];
		int i = 0;
		while (MSB> 0) {
			msbArray_temp[i] =  (MSB % 2);
			MSB = (MSB / 2);
			i++;
		}
		i = 8;
		int sum = 0;
		for (int j = 0; j< 8; j++) {
			msbArray[j] = msbArray_temp[msbArray_temp.length - (j + 1)];
		}
		int power = 0;
		for (int j = 0; j< 8; j++) {
			power = (int) (Math.pow(2, i));
			sum = sum + msbArray[msbArray.length - (j + 1)] * power;
			
			i++;
		}

		int sequence = sum + LSB;
		char sequenceWord = (char) sequence;
		return sequenceWord;
	}
}




