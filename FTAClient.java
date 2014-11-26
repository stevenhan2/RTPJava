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

		Thread thread = new Thread(new MyRunnable());
   		thread.start();
  	}

  	public static class MyRunnable implements Runnable {
    	public void run(){
       	System.out.println("Hello World!");
    	}
  	}

}
