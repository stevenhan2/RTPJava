import java.net.*;
public class RTPTestClient {
    public static void main(String[] args) {
		System.out.println("Beginning RTPTestClient");
		// RTPSocket s1 = new RTPSocket();

		int serverPort = Integer.parseInt(args[0]);
		int bindPort = 0;

		byte[] helloWorldData = "Hello World!".getBytes();
		byte[] foobarData = "FOObar World!".getBytes();

		InetSocketAddress bindsource = null;
		InetSocketAddress destination = null;
		try {
			bindPort = RTPSocket.getEphemeralPort(InetAddress.getLocalHost());
			bindsource = new InetSocketAddress("0.0.0.0" , bindPort);
			destination = new InetSocketAddress("localhost", serverPort);
		} catch (Exception e) {
			System.out.println("Was unable to get ephemeral port and create source address");
		}

		if (bindsource != null){
			System.out.println("Going to try to connect to " + serverPort + " from " + bindPort);
			RTPSocket clientRTPSocket = new RTPSocket(new InetSocketAddress("localhost", serverPort));

			System.out.println("Connecting...");
			boolean connection = clientRTPSocket.connect(destination);
			System.out.println("Connection success:" + connection);
			System.out.println(clientRTPSocket.toString());

			if (connection){
				System.out.println("Trying to send \"Hello World!\"");
				clientRTPSocket.send(helloWorldData);
				System.out.println("Trying to send \"FOObar World!\"");
				clientRTPSocket.send(foobarData);

				String toasty = new String(clientRTPSocket.receive());
				System.out.println("toasty:" + toasty);

			}
		}	
    }
}