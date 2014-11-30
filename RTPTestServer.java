import java.net.*;
public class RTPTestServer {
    public static void main(String[] args) {
       System.out.println("Beginning RTPTestServer");
		// RTPSocket s1 = new RTPSocket();

		int bindport = Integer.parseInt(args[0]);
		InetSocketAddress bindsource = null;

		try {
			bindsource = new InetSocketAddress("localhost", bindport);
		} catch (Exception e) {
			System.out.println("Was unable to get ephemeral port and create source address");
		}

		if (bindsource != null){
			System.out.println("Creating RTPSocket to serve on " + bindport);
			RTPSocket serverRTPSocket = new RTPSocket();

			System.out.println("Trying to bind to localhost:" + bindport);
			boolean bindTry = serverRTPSocket.bind(bindsource);
			System.out.println("Bind success: " + bindTry);

			System.out.println("Listening");
			serverRTPSocket.listen();

			System.out.println("Accept loop begin");
			boolean accept = false;
			while (!accept){
				accept = serverRTPSocket.accept();
			}

			System.out.println("Accept returned true");
			// System.out.println(serverRTPSocket.toString());

			String helloWorldString = new String(serverRTPSocket.receive());
			System.out.println("helloWorldString:" + helloWorldString);

			String foobarString = new String(serverRTPSocket.receive());
			System.out.println("foobarString:" + foobarString);

			System.out.println(serverRTPSocket.toString());
		}	
    }
}