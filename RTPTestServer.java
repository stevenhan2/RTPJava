import java.net.*;
public class RTPTestServer {
    public static void main(String[] args) {
       RTPUtil.debug("Beginning RTPTestServer");
		// RTPSocket s1 = new RTPSocket();

		int bindport = Integer.parseInt(args[0]);
		InetSocketAddress bindsource = null;

		try {
			bindsource = new InetSocketAddress("localhost", bindport);
		} catch (Exception e) {
			RTPUtil.debug("Was unable to get ephemeral port and create source address");
		}

		if (bindsource != null){
			RTPUtil.debug("Creating RTPSocket to serve on " + bindport);
			RTPSocket serverRTPSocket = new RTPSocket();

			RTPUtil.debug("Trying to bind to localhost:" + bindport);
			boolean bindTry = serverRTPSocket.bind(bindsource);
			RTPUtil.debug("Bind success: " + bindTry);

			RTPUtil.debug("Listening");
			serverRTPSocket.listen();

			RTPUtil.debug("Accept loop begin");
			boolean accept = false;
			while (!accept){
				accept = serverRTPSocket.accept();
			}

			RTPUtil.debug("Accept returned true");
			RTPUtil.debug(serverRTPSocket.toString());
		}	
    }
}