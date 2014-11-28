import java.util.*;
import java.net.*;
public class FTAClient{

// 	X: the port number at which the fta-client’s UDP socket should bind to (even number).
// Please remember that this port number should be equal to the server’s port number minus 1.
// A: the IP address of NetEmu
// P: the UDP port number of NetEmu
  	public static void main(String args[]){

		InetSocketAddress clientSocket = new InetSocketAddress(Integer.parseInt(args[0]));

		try{
		InetAddress ip = InetAddress.getByName(args[1]);
		}catch (UnknownHostException e) {
	        e.printStackTrace();
		}
		InetSocketAddress UDPSocket = new InetSocketAddress(Integer.parseInt(args[2]));

		Thread tcpThread = new Thread(new MyRunnable());
   		tcpThread.start();

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
				connect();
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

  	public static void connect(){

  	}

  	public static void get(String fileName){
  		System.out.println("getting file " + fileName);

  	}

  	public static void post(String fileName){
  		System.out.println("posting file " + fileName);
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

}
