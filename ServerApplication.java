package serverApplication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerApplication {

	public static void main(String[] args) {
		
		
		try{
		DatagramSocket ds = new DatagramSocket(58665);
		
		
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
		while(true){
			System.out.println("Server is waiting for a request message");
			DatagramPacket dp = new DatagramPacket(receiveData, receiveData.length);
			ds.receive(dp);
			String requestMessage = new String(dp.getData());
			
			System.out.println("RECEIVED: " + requestMessage);
            InetAddress IPAddress = dp.getAddress();
            int port = dp.getPort();
            ServerApplicationOperations serverOperation = new ServerApplicationOperations(requestMessage);
            String response = serverOperation.getResponse();
            System.out.println("Response sent to client:" +response);
            sendData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            ds.send(sendPacket);
		}
		}catch(Exception ex){
			
			System.out.println(ex.getMessage());
			
		}
	

}



	}


