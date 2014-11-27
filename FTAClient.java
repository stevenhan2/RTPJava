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
			System.out.println("Options: connect-get F, connect, get F, post F, window W, disconnect");
			String str1 = scan.nextLine();
			if(str1.equals("connect-get")){
				connectGet();
			}else if(str1.equals("connect")){
				connect();
			}else if(str1.equals("get")){
				get();
			}else if(str1.equals("post")){
				post();
			}else if(str1.equals("window")){
				window();
			}else if(str1.equals("disconnect")){
				//terminate(tcpThread);
				terminated = true;
			}else{
				System.out.println("sorry, unrecognized commaned");
			}
			
		}	

  	}

  	public static void connectGet(){
  		//code for connectGet
  	}

  	public static void connect(){

  	}

  	public static void get(){

  	}

  	public static void post(){

  	}

  	public static void window(){

  	}

  	public static void disconnect(){
  		
  	}

  	public static class MyRunnable implements Runnable {
    	public void run(){
       	System.out.println("Hello World!");
    	}
  	}

}
