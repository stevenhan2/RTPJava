import java.util.*;
import java.net.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
public class FTAClient{

// 	X: the port number at which the fta-client’s UDP socket should bind to (even number).
// Please remember that this port number should be equal to the server’s port number minus 1.
// A: the IP address of NetEmu
// P: the UDP port number of NetEmu
  	public static void main(String args[]){
  		Socket s = null; 


		// InetSocketAddress clientSocket = new InetSocketAddress(Integer.parseInt(args[0]));

		// try{
		// InetAddress ip = InetAddress.getByName(args[1]);
		// }catch (UnknownHostException e) {
	 //        e.printStackTrace();
		// }
		// InetSocketAddress UDPSocket = new InetSocketAddress(Integer.parseInt(args[2]));

		Thread tcpThread = new Thread(new MyRunnable());
   		tcpThread.run();

   		boolean terminated = false;
		while(terminated == false){
			Scanner scan = new Scanner(System.in);
			System.out.println("Options: connect, get F, post F, window W, disconnect");
			String str1 = scan.nextLine();

			// not used since we are doing bi-directional transfer
			// if(str1.equals("connect-get")){
			// 	connectGet();
			// }else 

			if(str1.equals("connect")){
				//connect(args[0], args[1]);

  				try{
		  			int serverPort = Integer.parseInt(args[0]);
		  			String ip = args[1];

		 			//  	if(args.length > 1){
		 			//  		ip = args[1];
		 			//  	}else{
					// 	ip = "localhost";
					// }
					
					s = new Socket(ip, serverPort); 
					System.out.println("connected!");
				}catch(UnknownHostException e){ 
					System.out.println("Sock:"+e.getMessage());}
				catch (IOException e){
					System.out.println("IO:"+e.getMessage());} 
				finally {
					if(s!=null) 
						try {s.close();
						} 
				catch (IOException e) {/*close failed*/}
				}		




			}else if(str1.length() > 3 && str1.substring(0,3).equals("get")){
				get(str1.substring(4));
			}else if(str1.length() > 4 && str1.substring(0,4).equals("post")){
				post(str1.substring(5));
			}else if(str1.length() > 6 && str1.substring(0, 6).equals("window")){
				int windowSize = Integer.parseInt(str1.substring(7));
				window(windowSize);
			}else if(str1.equals("disconnect")){
				//terminate(tcpThread);
				terminated = true;
			}else{
				System.out.println("sorry, unrecognized command");
			}
			
		}	

  	}

  	// public static void connectGet(){
  	// 	//code for connectGet
  	// }

  	public static void connect(String port, String ipNet){

  // 		try{
		//   	int serverPort = Integer.parseInt(port);
		//   	String ip = ipNet;

		//  	//  	if(args.length > 1){
		//  	//  		ip = args[1];
		//  	//  	}else{
		// 	// 	ip = "localhost";
		// 	// }
			  
		// 	s = new Socket(ip, serverPort); 
		// 	System.out.println("connected!");
		// }catch(UnknownHostException e){ 
		// 	System.out.println("Sock:"+e.getMessage());}
		// catch (IOException e){
		// 	System.out.println("IO:"+e.getMessage());} 
		// finally {
		// 	if(s!=null) 
		// 		try {s.close();
		// 		} 
		// catch (IOException e) {/*close failed*/}
		// }

  	}

  	public static void get(String fileName){
  		System.out.println("getting file " + fileName);
  		try{
  			System.out.println(readFile(fileName));
  		}catch (Exception E){
  			
  		}

  	}

  	public static void post(String fileName){

  		// System.out.println("posting file " + fileName);
  		// try{
  		// 	String data = readFile("example.txt");
  		// 	DataInputStream input = new DataInputStream( s.getInputStream()); 
		  // DataOutputStream output = new DataOutputStream( s.getOutputStream()); 
		  
			 //  //Step 1 send length
			 //  System.out.println("Length"+ data.length());
			 //  output.writeInt(data.length());
			 //  //Step 2 send length
			 //  System.out.println("Writing.......");
			 //  output.writeBytes(data); // UTF is a string encoding
			  
			 //  //Step 1 read length
			 //  int nb = input.readInt();
			 //  byte[] digit = new byte[nb];
			 //  //Step 2 read byte
			 //  for(int i = 0; i < nb; i++)
				// digit[i] = input.readByte();
		  
			 //  String st = new String(digit);
		  // System.out.println("Received: "+ st); 
  		// }catch (Exception E){
  			
  		// }
  	}

 	public static void window(int windowSize){
		System.out.println("window size set to " + windowSize);
		//window code
	}

  	public static void disconnect(){
  		
  	}

  	public static class MyRunnable implements Runnable {
    	public void run(){
    		System.out.println("Hello World!");

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
