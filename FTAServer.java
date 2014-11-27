import java.util.*;
import java.net.*;

public class FTAServer{

	/**
	*fta-server X A P
	*X: the port number at which the fta-server’s UDP socket should bind to (odd number)
	*A: the IP address of NetEmu
	*P: the UDP port number of NetEmu
	*/

	public static void main(String args[]){
		InetSocketAddress serverSocket = new InetSocketAddress(Integer.parseInt(args[0]));
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
			System.out.println("terminate to close, connect to connect");
			String str1 = scan.nextLine();
			if(str1.equals("terminate")){
				//terminate(tcpThread);
				terminated = true;
			}else if(str1.equals("connect")){
				connect();
			}else{
				System.out.println("sorry, unrecognized commaned");
			}
			
		}	

	}

	public static void terminate(Thread thread){
		//thread.stop();
	}

	public static void connect(){

	}


	private static class MyRunnable implements Runnable {
    	public void run(){
       	System.out.println("Hello World!");
    	}
  	}


}
