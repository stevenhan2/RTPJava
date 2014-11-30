// TCPClient.java
// A client program implementing TCP socket
import java.net.*; 
import java.io.*; 
import java.util.*;


public class FTAClient { 
	public static void main (String args[]) {
		System.out.println("Beginning RTPTestClient");
		// RTPSocket s1 = new RTPSocket();

		int serverPort = Integer.parseInt(args[0]);
		int bindPort = Integer.parseInt(args[2]);

		byte[] helloWorldData = "Hello World!".getBytes();
		byte[] foobarData = "FOObar World!".getBytes();

		InetSocketAddress bindsource = null;
		InetSocketAddress destination = null; 


		// Socket s = null; 

		boolean terminated = false;
		while(!terminated){
			Scanner scan = new Scanner(System.in);
			System.out.println("Options: connect-get, disconnect");
			String str1 = scan.nextLine();
			if(str1.equals("connect-get")){

				try {
					bindPort = RTPSocket.getEphemeralPort(InetAddress.getLocalHost());
					bindsource = new InetSocketAddress("0.0.0.0" , bindPort);
					destination = new InetSocketAddress("localhost", serverPort);
				} catch (Exception e) {
					System.out.println("Was unable to get ephemeral serverPort and create source address");
				}

				if (bindsource != null){
					System.out.println("Going to try to connect to " + serverPort + " from " + bindPort);
					RTPSocket clientRTPSocket = new RTPSocket(new InetSocketAddress("localhost", serverPort));
//
					boolean bindTry = clientRTPSocket.bind(bindsource);

					System.out.println("Connecting...");
					boolean connection = clientRTPSocket.connect(destination);
					System.out.println("Connection success:" + connection);
					System.out.println(clientRTPSocket.toString());

					if (connection){
						System.out.println("Trying to send \"Hello World!\"");
						clientRTPSocket.send(helloWorldData);
						clientRTPSocket.send(foobarData);
					}
				}	
				// try{ 
		  // 			int serverPort = Integer.parseInt(args[0]);
		  // 			String ip = args[1];

				// 	String data = readFile("example.txt"); 
				// 	//"Hello, How are you?"; 
			  
		 	// 		s = new Socket(ip, serverPort); 
		  // 			DataInputStream input = new DataInputStream( s.getInputStream()); 
		  // 			DataOutputStream output = new DataOutputStream( s.getOutputStream()); 
		  
					
				// 	//Step 1 send length
			 //  		System.out.println("Length"+ data.length());
			 //  		output.writeInt(data.length());
					
				// 	//Step 2 send length
			 //  		System.out.println("Writing.......");
			 //  		output.writeBytes(data); 
				// 	// UTF is a string encoding
			  
					
				// 	//Step 1 read length
			 //  		int nb = input.readInt();
			 //  		byte[] digit = new byte[nb];
					
				// 	//Step 2 read byte
			 //  		for(int i = 0; i < nb; i++)
				// 	digit[i] = input.readByte();
		  
			 //  		String st = new String(digit);
		  // 			System.out.println("Received: "+ st); 
				// }
				// catch (UnknownHostException e){ 
				// 	System.out.println("Sock:"+e.getMessage());}
				// catch (EOFException e){
				// 	System.out.println("EOF:"+e.getMessage()); }
				// catch (IOException e){
				// 	System.out.println("IO:"+e.getMessage());} 
				// finally {
			 //  		if(s!=null) 
				//   		try {s.close();
				//   	} 
				//   	catch (IOException e) {/*close failed*/}
				// }
  			}else if(str1.equals("disconnect")){
  				terminated = true;
  			}else{
  				System.out.println("not a valid command");
  			}
  		}
  	}

  	public static String readFile(String fileName) throws IOException {
    	BufferedReader br = new BufferedReader(new FileReader(fileName));
    	try {
       		StringBuilder sb = new StringBuilder();
        	String line = br.readLine();

        	while (line != null) {
            	sb.append(line);
            	sb.append("\n");
            	line = br.readLine();
        	}
        	return sb.toString();
    	} finally {
        	br.close();
    	}
	}
}
