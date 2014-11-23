import java.net.*;

public static final int DEFAULT_RECEIVE_WINDOW_BYTE_AMOUNT = 65535;

public enum State {
	CLOSED, LISTEN, SYNRCVD, SYNSENT, ESTABLISHED, FINWAIT1, CLOSING, FINWAIT2, TIMEWAIT, CLOSEWAIT, LASTACK
}

public class RTPSocket {
	public State state;
	public DatagramSocket datagramSocket;

	public InetSocketAddress bindAddress;
	public ByteBuffer receiveWindow;

	public ByteBuffer sendBuffer;
	public ByteBuffer receiveBuffer;

	public RTPSocket(){
		state = CLOSED;

		receiveWindow = new ByteBuffer();
		receiveWindow.put(DEFAULT_RECEIVE_WINDOW_BYTE_AMOUNT);
	}

    public RTPSocket(InetSocketAddress address){
    	datagramSocket = new DatagramSocket(address);
    	this();
    }

    public void connect(InetSocketAddress address){
    	switch (state) {
    		case CLOSED:
	    		datagramSocket = new DatagramSocket(address);

	    		RTPDatagram synRTPDatagram = RTPDatagram(
	    			new Port(bindAddress.getPort()),
	    			new Port(address.getPort()),
	    			RTPDatagram.SYN,
	    			receiveWindow,
	    			new ByteBuffer()
    			);

	    		byte[] synRTPDatagramArray = synRTPDatagram.array();
	    		DatagramPacket synIPDatagram = new synIPDatagram(synRTPDatagramArray, synRTPDatagramArray.length, address);

	    		datagramSocket.send(synIPDatagram);

	    		DatagramPacket ackIPDatagram;

	    		datagramSocket.receive(ackIPDatagram);

    			state = SYNSENT;
	    		break;
    	}
    }

    public void bind(InetSocketAddress bindAddress){
    	this.bindAddress = bindAddress;
    	datagramSocket.bind(bindAddress);
    }

    public void send(ByteBuffer buffer){
    }

    public void listen(){
    	switch (state) {
    		case CLOSED:
				state = LISTEN;
	    		break;
    	}
    }

    public void close(){
    	switch (state) {
    		case LISTEN:	
	    		state = CLOSED;
	    		break;
    		case SYNSENT:	
	    		state = CLOSED;
	    		break;
    	}
    }





}