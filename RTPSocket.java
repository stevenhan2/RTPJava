import java.util.*;
import java.net.*;

public static final int DEFAULT_RECEIVE_WINDOW_BYTE_AMOUNT = 65535;

public enum State {
	CLOSED, LISTEN, SYNRCVD, SYNSENT, ESTABLISHED, FINWAIT1, CLOSING, FINWAIT2, TIMEWAIT, CLOSEWAIT, LASTACK
}

public class RTPSocket {
	// A connection is identified by combination of source host address, source port, destination host address, and destination port

	public State state;
	public DatagramSocket datagramSocket;

	public InetSocketAddress bindAddress;
	public InetSocketAddress connectionAddress;

	public byte[] receiveWindow;

	public int sequenceNumber;
	public int ackNumber;

	public SetPriorityQueue<RTPDatagram> receiveRTPDatagramBuffer;

	// Only for server pretty much
	public LinkedList<DatagramPacket> receiveBuffer;

	public Thread listenThread;

	public RTPSocket(){
		state = CLOSED;

		receiveWindow = new byte[];
		receiveWindow.put(DEFAULT_RECEIVE_WINDOW_BYTE_AMOUNT);
	}

    public RTPSocket(InetSocketAddress address){
    	datagramSocket = new DatagramSocket(address);
    	datagramSocket.setTimeout(10000);
    	connectionAddress = address;
    	this();
    }

    public void connect(InetSocketAddress address){
    	switch (state) {
    		case CLOSED:
	    		datagramSocket = new DatagramSocket(address);

	    		// ===========================
	    		// 1) Send SYN
	    		RTPDatagram synRTPDatagram = RTPDatagram(
	    			bindAddress.getPort(),
	    			address.getPort(),
	    			RTPDatagram.SYN,
	    			receiveWindow,
	    			new byte[0]
    			);

	    		// Set sequence number
	    		Random rand = new Random();
	    		this.sequenceNumber = rand.nextInt();
	    		synRTPDatagram.sequenceNumber = this.sequenceNumber;

	    		// Send the packet
	    		byte[] synRTPDatagramArray = synRTPDatagram.getByteArray();
	    		DatagramPacket synIPDatagram = new synIPDatagram(synRTPDatagramArray, synRTPDatagramArray.length, address);
	    		datagramSocket.send(synIPDatagram);
	    		state = SYNSENT;

	    		// ===========================
	    		// 2) Receive a SYN ACK hopefully
	    		byte[] message = new byte[2048];
	    		DatagramPacket synAckIPDatagram = new DatagramPacket(message, message.length);
	    		datagramSocket.receive(synAckIPDatagram);
	    		RTPDatagram synAckRTPDatagram = new RTPDatagram(synAckIPDatagram.getData());

	    		// If it's a SYN ACK and the ack number is the next sequence number
	    		if (synAckRTPDatagram.ackNumber = this.sequenceNumber + 1
	    			&& synAckRTPDatagram.flags & RTPDatagram.SYN > 0
	    			&& synAckRTPDatagram.flags & RTPDatagram.ACK > 0){
	    			// It has a SYN
	    			this.ackNumber = synACKRTPDatagram.sequenceNumber + 1;
	    			this.sequenceNumber ++;
	    			this.receiveWindow = synAckRTPDatagram.receiveWindow;
	    		}

	    		// ===========================
	    		// 3) Send an ACK
    			RTPDatagram ackRTPDatagram = RTPDatagram(
	    			bindAddress.getPort(),
	    			address.getPort(),
	    			RTPDatagram.ACK,
	    			receiveWindow,
	    			new byte[0]
    			);

	    		// Set sequence number and ack number
	    		ackRTPDatagram.sequenceNumber = this.sequenceNumber;
	    		ackRTPDatagram.ackNumber = this.ackNumber;

	    		// Pack and send
	    		byte[] ackRTPDatagramArray = ackRTPDatagram.getByteArray();
	    		DatagramPacket ackIPDatagram = new ackIPDatagram(ackRTPDatagramArray, ackRTPDatagramArray.length, address);
	    		datagramSocket.send(ackIPDatagram);

	    		// Things for state
    			state = ESTABLISHED;
    			connectionAddress = address;

	    		break;
    	}
    }

    public void bind(InetSocketAddress bindAddress){
    	this.bindAddress = bindAddress;
    	datagramSocket.bind(bindAddress);
    }

    public void send(byte[] data){
    }

    public void listen(){
    	switch (state) {
    		case CLOSED:
				state = LISTEN;

				this.receiveBuffer = new LinkedList<DatagramPacket>();
				this.listenThread = new Thread(new ListenLoop(this.receiveBuffer, this.datagramSocket));
				this.listenThread.start();
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

    public void addRTPDatagramToBuffer(RTPDatagram d){
    	if (this.receiveRTPDatagramBuffer == null){
    		this.receiveRTPDatagramBuffer = new LinkedList<RTPDatagram>();
    	}
    	this.receiveRTPDatagramBuffer.add(d);
    }

    public RTPSocket fork(){
    	return null;
    }

    private static class ListenLoop implements Runnable{
    	public LinkedList<DatagramPacket> listenLoopReceiveBuffer;
    	public DatagramSocket listenLoopDatagramSocket;
    	public boolean run;

    	public Runnable(LinkedList<DatagramPacket> ll, DatagramSocket datagramSocket){
    		this.listenLoopReceiveBuffer = ll;
    		run = true;
    		super();
    	}

    	public void run(){
    		while (run){
	    		try {
	    			byte[] message = new byte[2048];
		    		DatagramPacket x = new DatagramPacket(message, message.length);
		    		datagramSocket.receive(x);

		    		listenLoopReceiveBuffer.add(x);
		    	} catch (SocketTimeoutException e){
		    		continue;
		    	}

			}
    	}

    	public void stop(){
    		run = false;
    	}
    }


}