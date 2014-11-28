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

	boolean stopAndWait;

	public SetPriorityQueue<RTPDatagram> receiveRTPDatagramBuffer;

	// Only for server pretty much
	public LinkedList<DatagramPacket> receiveConnectionOpenBuffer;

	public Thread listenThread;
	public Thread receiveThread;

	public RTPSocket(){
		state = CLOSED;
		stopAndWait = true;

		receiveWindow = RTPUtil.toIntBytes(50000);
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
	    		DatagramPacket synUDPDatagram = new synUDPDatagram(synRTPDatagramArray, synRTPDatagramArray.length, address);
	    		datagramSocket.send(synUDPDatagram);
	    		state = SYNSENT;

	    		// ===========================
	    		// 2) Receive a SYN ACK hopefully
	    		byte[] message = new byte[2048];
	    		DatagramPacket synAckUDPDatagram = new DatagramPacket(message, message.length);
	    		datagramSocket.receive(synAckUDPDatagram);
	    		RTPDatagram synAckRTPDatagram = new RTPDatagram(synAckUDPDatagram.getData());

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
	    		sendAck(this.ackNumber);
    			// RTPDatagram ackRTPDatagram = RTPDatagram(
	    		// 	bindAddress.getPort(),
	    		// 	address.getPort(),
	    		// 	RTPDatagram.ACK,
	    		// 	receiveWindow,
	    		// 	new byte[0]
    			// );

	    		// // Set ack number
	    		// ackRTPDatagram.ackNumber = this.ackNumber;

	    		// // Pack and send
	    		// byte[] ackRTPDatagramArray = ackRTPDatagram.getByteArray();
	    		// DatagramPacket ackUDPDatagram = new ackUDPDatagram(ackRTPDatagramArray, ackRTPDatagramArray.length, address);
	    		// datagramSocket.send(ackUDPDatagram);

	    		// ArrayList<RTPSocket> tmpPushList = new ArrayList<RTPSocket>();
	    		// tmpPushList.add(this);

	    		this.receiveConnectionOpenBuffer = new LinkedList<DatagramPacket>();
				this.receiveThread = new Thread(new ReceiveBufferLoop(this.receiveConnectionOpenBuffer, this.datagramSocket));
				this.receiveThread.start();

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

    // Needs to send and then wait for ack
    public void send(byte[] data){
    	Thread sendThread = new Thread(new SendLoop(this, data, 5, 100));
    	sendThread.start();
    }

    public boolean getStopAndWait(){
    	return stopAndWait;
    }

    public boolean accept(){
    	if (state == LISTEN){
    		while (rece)
    		for (int i = 0; i< this.receiveConnectionOpenBuffer.size(); i++){
    			DatagramPacket examineUDPDatagram = receiveConnectionOpenBuffer.get(i);
    			RTPDatagram examineRTPDatagram = new RTPDatagram(examineUDPDatagram.getData());
    			if (examineRTPDatagram.flags & RTPDatagram.SYN > 0){
    				connectionAddress = new InetSocketAddress(examineUDPDatagram.getAddress(), examineUDPDatagram.getPort());

    			} else {

    			}
    		}
    	}

    	return false;
    }

    public byte[] receive(){
    	int retries = 5;
    	int waitTime = 200;
    	int count = 0;

    	RTPDatagram tmp = this.receiveRTPDatagramBuffer.peek();

    	while (count++ < retries)
	    	if (tmp.sequenceNumber == this.ackNumber){
	    		this.ackNumber += 1;
	    		sendAck(this.ackNumber);

	    		return tmp.data;
	    	}
    }

    public void sendAck(int number){
    	RTPDatagram ackRTPDatagram = RTPDatagram(
			bindAddress.getPort(),
			connectionAddress.getPort(),
			RTPDatagram.ACK,
			receiveWindow,
			new byte[0]
		);

		ackRTPDatagram.ackNumber = number;

		// Pack and send
		byte[] ackRTPDatagramArray = ackRTPDatagram.getByteArray();
		DatagramPacket ackUDPDatagram = new ackUDPDatagram(ackRTPDatagramArray, ackRTPDatagramArray.length, address);
		datagramSocket.send(ackUDPDatagram);
    }

    public void listen(){
    	switch (state) {
    		case CLOSED:
				state = LISTEN;

				this.receiveConnectionOpenBuffer = new LinkedList<DatagramPacket>();
				this.listenThread = new Thread(new ListenLoop(this.receiveConnectionOpenBuffer, this.datagramSocket, new ArrayList<RTPSocket>()));
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

    private static class ListenLoop implements Runnable{
    	public LinkedList<DatagramPacket> listenLoopReceiveBuffer;
    	public DatagramSocket listenLoopDatagramSocket;
    	public ArrayList<RTPSocket> pushList;

    	public boolean run;

    	public ListenLoop(LinkedList<DatagramPacket> ll, DatagramSocket datagramSocket){
    		this.listenLoopReceiveBuffer = ll;
    		this.pushList = pushList;
    		run = true;
    		super();
    	}

    	public void run(){
    		while (run){
	    		try {
	    			byte[] message = new byte[2048];
		    		DatagramPacket x = new DatagramPacket(message, message.length);
		    		datagramSocket.receive(x);


		    		// this.listenLoopReceiveBuffer.add(x);
		    // 		for (int i = 0; i < this.listenLoopReceiveBuffer.size(); i++){
		    // 			for (int j = 0; j < this.pushList).size(); j++){
						// 	// A connection is identified by combination of source host address, source port, destination host address, and destination port
						// 	if (this.listenLoopReceiveBuffer.get(i).getPort() == this.pushList.get(j).bindAddress.port() &&
						// 		this.listenLoopReceiveBuffer.get(i).getPort() == this.pushList.get(j).bindAddress.port() 
						// }
		    			
		    		// }
		    	} catch (SocketTimeoutException e){
		    		continue;
		    	}

			}
    	}

    	public void stop(){
    		run = false;
    	}
    }


    private static class ReceiveBufferLoop implements Runnable{
    	public RTPSocket socket;

    	public boolean run;

    	public ReceiveBufferLoop(RTPSocket socket);
    		this.receiveRTPDatagramBuffer = socket.receiveRTPDatagramBuffer;
    		run = true;
    		super();
    	}

    	public void run(){
    		while (run){
	    		try {
	    			byte[] message = new byte[2048];
		    		DatagramPacket x = new DatagramPacket(message, message.length);
		    		socket.datagramSocket.receive(x);

		    		// We've already received this and acked it, so we'll ack it again
		    		if (x.sequenceNumber < socket.ackNumber){
		    			sendAck(x.sequenceNumber + 1);
		    		}
		    		socket.receiveRTPDatagramBuffer.add(new RTPDatagram(x.getData()));
		    	} catch (SocketTimeoutException e){
		    		continue;
		    	}

			}
    	}

    	public void stop(){
    		run = false;
    	}
    }


    private static class SendLoop implements Runnable{
    	public RTPSocket rtpSocket;
    	public byte[] dataToSend;
    	public int retries;
    	public int waitTime;

    	public SendLoop(RTPSocket socket, byte[] dataToSend, int retries, int waitTime){
    		this.retries = retries;
    		this.rtpSocket = socket;
    		this.dataToSend = dataToSend;
    		super();
    	}

    	public void run(){
    		int counter = 0;
			for (int i = 0; i < socket.receiveRTPDatagramBuffer.size(); i++){
				RTPDatagram tmp = socket.receiveRTPDatagramBuffer.get(i);
				if (tmp.flags * RTPDatagram.ACK > 0 && tmp.ackNumber = rtpSocket.sequenceNumber + 1){
					rtpSocket.sequenceNumber += 1;
					rtpSocket.stopAndWait = false;
					receiveRTPDatagramBuffer.remove(tmp);
				}
			}

    		while (counter < retries){
		    	if (!socket.stopAndWait()){
		    		RTPDatagram datagram = RTPDatagram(
						bindAddress.getPort(),
						connectionAddress.getPort(),
						0,
						receiveWindow,
						dataToSend
					);

					datagram.sequenceNumber = this.sequenceNumber;

					// Send the packet
					byte[] datagramArray = datagram.getByteArray();
					DatagramPacket udpDatagram = new udpDatagram(datagramArray, datagramArray.length, address);
					datagramSocket.send(udpDatagram);

					rtpSocket.stopAndWait = true;
		    	} else {

		    		for (int i = 0; i < socket.receiveRTPDatagramBuffer.size(); i++){
		    			RTPDatagram tmp = socket.receiveRTPDatagramBuffer.get(i);
		    			if (tmp.flags * RTPDatagram.ACK > 0 && tmp.ackNumber = rtpSocket.sequenceNumber + 1){
		    				rtpSocket.sequenceNumber += 1;
		    				rtpSocket.stopAndWait = false;
		    				receiveRTPDatagramBuffer.remove(tmp);
		    			}
		    		}

		    		Thread.sleep(waitTime);

		    		counter++;
		    	}
			}
    	}
    }



}