import java.net.*;
public class RTPTestClient {
    public static void main(String[] args) {
		RTPUtil.debug("Beginning RTPTestClient");
		// RTPSocket s1 = new RTPSocket();

		int serverPort = Integer.parseInt(args[0]);
		int bindPort = 0;

		InetSocketAddress bindsource = null;
		InetSocketAddress destination = null;
		try {
			bindPort = RTPSocket.getEphemeralPort(InetAddress.getLocalHost());
			bindsource = new InetSocketAddress("localhost", bindPort);
			destination = new InetSocketAddress("localhost", serverPort);
		} catch (Exception e) {
			RTPUtil.debug("Was unable to get ephemeral serverPort and create source address");
		}

		if (bindsource != null){
			RTPUtil.debug("Going to try to connect to " + serverPort + " from " + bindPort);
			RTPSocket clientRTPSocket = new RTPSocket(new InetSocketAddress("localhost", serverPort));

			RTPUtil.debug("Connecting...");
			boolean connection = clientRTPSocket.connect(destination);
			RTPUtil.debug("Connection success:" + connection);
		}	
    }
}