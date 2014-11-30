// TCPClient.java
// A client program implementing TCP socket
import java.net.*; 
import java.io.*; 
import java.util.*;


public class FTAClient { 
	public static void main (String args[]) {
		// RTPSocket s1 = new RTPSocket();

		int serverPort = Integer.parseInt(args[2]);
		int bindPort = Integer.parseInt(args[0]);

		InetSocketAddress bindsource = null;
		InetSocketAddress destination = null; 

		try {
			bindsource = new InetSocketAddress("0.0.0.0" , bindPort);
			destination = new InetSocketAddress("localhost", serverPort);
		} catch (Exception e) {
			System.out.println("Was unable to create source address");
		}


		// Socket s = null; 

		boolean terminated = false;
		while(!terminated){
			Scanner scan = new Scanner(System.in);
			System.out.println("Options: connect-get, disconnect");
			String inputString = scan.nextLine();
			String[] parts = inputString.split(" ");
			String operation = parts[0];



			if(operation.equalsIgnoreCase("connect-get")){
				if (bindsource != null){
					System.out.println("Going to try to connect to " + serverPort + " from " + bindPort);
					RTPSocket clientRTPSocket = new RTPSocket(new InetSocketAddress("localhost", serverPort));
					boolean bindTry = clientRTPSocket.bind(bindsource);

					System.out.println("Connecting...");
					boolean connection = clientRTPSocket.connect(destination);
					System.out.println("Connection success:" + connection);

					System.out.println(clientRTPSocket.toString());

					if (connection){
						clientRTPSocket.send(inputString.getBytes());
						byte[] file = clientRTPSocket.receive();

						String[] fileParts = (parts[1]).split("/");
						String filename = fileParts[fileParts.length - 1];
						FTAServer.writeBytes(filename, file);
					}
				}	

  			} else if (operation.equalsIgnoreCase("post")){
  				if (bindsource != null){
					System.out.println("Going to try to connect to " + serverPort + " from " + bindPort);
					RTPSocket clientRTPSocket = new RTPSocket(new InetSocketAddress("localhost", serverPort));
					boolean bindTry = clientRTPSocket.bind(bindsource);

					System.out.println("Connecting...");
					boolean connection = clientRTPSocket.connect(destination);
					System.out.println("Connection success:" + connection);

					System.out.println(clientRTPSocket.toString());

					if (connection){
						clientRTPSocket.send(inputString.getBytes());
						byte[] file = clientRTPSocket.receive();

						clientRTPSocket.send(FTAServer.readBytes(parts[1]));
					}
				}	
  			} else if (operation.equals("disconnect")){
  				terminated = true;
  			} else {
  				System.out.println("not a valid command");
  			}
  		}
  	}

  	
}
