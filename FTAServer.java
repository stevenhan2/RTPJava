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
		//tcpThread.start();

		boolean terminated = false;
		while(terminated == false){
			Scanner scan = new Scanner(System.in);
			System.out.println("terminate to close, window W to set the max window-size at FTA-Server");
			String str1 = scan.nextLine();
			if(str1.equals("terminate")){
				//terminate(tcpThread);
				terminated = true;
			}else if(str1.length() > 6 && str1.substring(0, 6).equals("window")){
				System.out.println(str1.substring(6));
				int windowSize = Integer.parseInt(str1.substring(7));
				window(windowSize);
			}else{
				System.out.println("sorry, unrecognized command");
			}
			
		}	

	}

	public static void terminate(Thread thread){
		//thread.stop();
	}

	public static void window(int windowSize){
		System.out.println("window size set to " + windowSize);
		//window code
	}


	private static class MyRunnable implements Runnable {
    	public void run(){
       	System.out.println("Hello World!");
    	}
  	}


}
