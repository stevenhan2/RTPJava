import java.util.*;
import java.net.*;

public class FTAServer extends Thread{
	   private volatile boolean terminated = false;

	public void run(){
		System.out.println("Hello World");
	}

	/**
	*fta-server X A P
	*X: the port number at which the fta-server’s UDP socket should bind to (odd number)
	*A: the IP address of NetEmu
	*P: the UDP port number of NetEmu
	*/

	public static void main(String args[]){
		boolean terminated = false;
		InetSocketAddress serverSocket = new InetSocketAddress(Integer.parseInt(args[0]));
		try{
		InetAddress ip = InetAddress.getByName(args[1]);

		}catch (UnknownHostException e) {
            e.printStackTrace();
			}
		InetSocketAddress UDPSocket = new InetSocketAddress(Integer.parseInt(args[2]));

		while(!terminated){
			Scanner scan = new Scanner(System.in);
			System.out.println("terminate to close, connect to connect");
			String str1 = scan.nextLine();


			(new FTAServer()).start();

			if(str1.equals("terminate")){
				terminated = true;
			}
		}
	}




}
