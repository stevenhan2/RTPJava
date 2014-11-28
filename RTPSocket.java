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

	public SetPriorityQueue<RTPDatagram> receiveDefaultRTPDatagramBuffer;
	public ArrayList<DatagramPacket> receiveSynRTPDatagramBuffer;

	public Thread listenThread;
	public Thread receiveThread;

	public RTPSocket(){
		state = CLOSED;
		RTPUtil.debug("RTPSocket()");
		stopAndWait = true;

		receiveWindow = RTPUtil.toIntBytes(50000);
		receiveWindow.put(DEFAULT_RECEIVE_WINDOW_BYTE_AMOUNT);
	}

    public RTPSocket(InetSocketAddress address){
    	RTPUtil.debug("RTPSocket(" + address + ")");
    	datagramSocket = new DatagramSocket(address);
    	datagramSocket.setTimeout(10000);
    	connectionAddress = address;
    	this();
    }

    public void connect(InetSocketAddress address){
    	RTPUtil.debug("connect(" + address + ")");
    	switch (state) {
    		case CLOSED:
				this.receiveSynRTPDatagramBuffer = new ArrayList<DatagramPacket>();
				this.receiveDefaultRTPDatagramBuffer = new SetPriorityQueue<DatagramPacket>();

    			connectionAddress = address;
	    		datagramSocket = new DatagramSocket(address);
	    		this.bind(RTPSocket.getEphemeralPort());

	    		this.receiveThread = new Thread(new ReceiveBufferLoop(this));
	    		this.receiveThread.start();

	    		// ===========================
	    		// 1) Send SYN
	    		// Set sequence number
	    		Random rand = new Random();
	    		this.sequenceNumber = rand.nextInt();

	    		// ===========================
	    		// 2) ReceiveBufferLoop will accept a SYN, the thread will look for an ACK and retransmit if can't find it

	    		Thread sendSynThread;
	    		boolean resolved = false;

	    		long firstNanoTime = System.nanoTime();

	    		while (!resolved){
	    			if (state == CLOSED){
		    			resolved = true;
		    			sendSynThread = sendSyn(this.sequenceNumber);
			    		state = SYNSENT;
			    		sendSynThread.join()
			    	} else {
			    		if (sendSynThread.ackReceived == false){
			    			state = CLOSED;
			    			return false;
			    		} else if (state == ESTABLISHED){
			    			// Means that the ReceiveBufferLoop accepted a SYN for us
			    			return true;
			    		} else {
			    			// timeout for waiting for SYN basically
			    			if (System.nanoTime() - firstNanoTime > 5000000){
			    				state = CLOSED:
			    			} else {
			    				resolved = false;
			    			}
			    		}
			    	}
	    		}

	    		break;
    	}
    }

    public void bind(InetSocketAddress bindAddress){
    	RTPUtil.debug("bind(" + bindAddress + ")");
    	this.bindAddress = bindAddress;
    	datagramSocket.bind(bindAddress);
    }

    // Needs to send and then wait for ack
    public void send(byte[] data){
    	RTPUtil.debug("send() length:" + data.length);
    	RTPDatagram rtpDatagram = new RTPDatagram(
			bindAddress.getPort(),
			connectionAddress.getPort(),
			0,
			receiveWindow,
			new byte[0]
		);

		rtpDatagram.sequenceNumber = this.sequenceNumber;

    	Thread sendThread = new Thread(new SendLoop(this, rtpDatagram, 5, 100));
    	sendThread.start();
    }

    public boolean getStopAndWait(){
    	RTPUtil.debug("getStopAndWait() returned " + stopAndWait);
    	return stopAndWait;
    }

    public boolean accept(){
    	RTPUtil.debug("accept");
    	if (state == LISTEN){
    		if (receiveSynRTPDatagramBuffer.size() > 0){
    			DatagramPacket examineUDPDatagram = receiveSynRTPDatagramBuffer.remove(0);
    			RTPDatagram examineRTPDatagram = new RTPDatagram(examineUDPDatagram.getData());
				state = SYNRCVD;
				connectionAddress = new InetSocketAddress(examineUDPDatagram.getAddress(), examineUDPDatagram.getPort());

				RTPDatagram synAckRTPDatagram = new RTPDatagram(
					bindAddress.getPort(),
					connectionAddress.getPort(),
					RTPDatagram.ACK,
					receiveWindow,
					new byte[0]
				);

				Random rand = new Random();
	    		this.sequenceNumber = rand.nextInt();
	    		synRTPDatagram.sequenceNumber = this.sequenceNumber;
	    		this.ackNumber = examineRTPDatagram.sequenceNumber + 1;

				synAckRTPDatagram.sequenceNumber = this.sequenceNumber;
				synAckRTPDatagram.ackNumber = this.ackNumber;

				Thread sendThread = new Thread(new SendLoop(this, synAckRTPDatagram, 5, 100));
		    	sendThread.start();

				// Pack and send
				sendThread.join();

				// If never received any ACK, return false
				if (stopAndWait == true){
					state = CLOSED:
					return false;
				} else {
    				state = ESTABLISHED;
    				return true;
    			}
    		}
    	}

    	return false;
    }

    public byte[] receive(){
    	RTPUtil.debug("receive()");
    	int retries = 5;
    	int waitTime = 200;
    	int count = 0;

    	RTPDatagram tmp = this.receiveDefaultRTPDatagramBuffer.peek();

    	while (count++ < retries){
	    	if (tmp.sequenceNumber == this.ackNumber){
	    		this.ackNumber += 1;
	    		sendAck(this.ackNumber);

	    		return tmp.data;
	    	}
	    }
    }

    public void sendAck(int number){
    	RTPUtil.debug("sendAck(" + number + ")");
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
		DatagramPacket ackUDPDatagram = new DatagramPacket(ackRTPDatagramArray, ackRTPDatagramArray.length, address);
		datagramSocket.send(ackUDPDatagram);
    }

	public Thread sendSyn(int number){
		RTPUtil.debug("sendSyn(" + number + ")");
    	RTPDatagram synRTPDatagram = RTPDatagram(
			bindAddress.getPort(),
			connectionAddress.getPort(),
			RTPDatagram.SYN,
			receiveWindow,
			new byte[0]
		);

		synRTPDatagram.synNumber = number;

		Thread newThread = new Thread(new SendLoop(this, synRTPDatagram, 5, 100));
		newThread.start();
		return newThread;
    }

    public void listen(){
    	RTPUtil.debug("listen()");
    	switch (state) {
    		case CLOSED:
				state = LISTEN;

				this.receiveSynRTPDatagramBuffer = new ArrayList<DatagramPacket>();
				this.receiveDefaultRTPDatagramBuffer = new SetPriorityQueue<DatagramPacket>();
				this.listenThread = new Thread(new ListenLoop(this.receiveUDPConnectionOpenBuffer, this.datagramSocket, new ArrayList<RTPSocket>()));
				this.listenThread.start();
	    		break;
    	}
    }

    public void close(){
    	RTPUtil.debug("close()");
    	switch (state) {
    		case LISTEN:	
	    		state = CLOSED;
	    		break;
    		case SYNSENT:	
	    		state = CLOSED;
	    		break;
    	}
    }

    private static class ReceiveBufferLoop implements Runnable{
    	public RTPSocket socket;
    	public boolean run;

    	public ReceiveBufferLoop(RTPSocket socket);
    		this.receiveDefaultRTPDatagramBuffer = socket.receiveDefaultRTPDatagramBuffer;
    		run = true;
    		super();
    	}

    	public void run(){
    		while (run){
	    		try {
	    			byte[] message = new byte[2048];
		    		DatagramPacket examineUDPDatagram = new DatagramPacket(message, message.length);
		    		socket.datagramSocket.receive(examineUDPDatagram);

		    		RTPDatagram examineRTPDatagram = new RTPDatagram(examineUDPDatagram.getData());

		    		if (examineRTPDatagram.flags & rtpDatagram.SYN > 0){
		    			if (socket.state == SYNSENT){
		    				if (examineUDPDatagram.getPort() == socket.connectionAddress.getPort() &&
		    					examineUDPDatagram.getAddress().equals(socket.connectionAddress.getAddress())){
		    					socket.ackNumber = examineUDPDatagram.sequenceNumber + 1;
		    					socket.sendAck(socket.ackNumber);
		    					socket.state = ESTABLISHED;
		    				}
		    			}
		    			if (!socket.receiveSynRTPDatagramBuffer.contains(examineUDPDatagram)){
		    				socket.receiveSynRTPDatagramBuffer.add(examineUDPDatagram);
		    			}
		    		}

		    		if (examineRTPDatagram.flags & rtpDatagram.ACK > 0){
		    			if (state == ESTABLISHED &&
		    				examineUDPDatagram.getPort() == socket.connectionAddress.getPort() &&
		    				examineUDPDatagram.getAddress().equals(socket.connectionAddress.getAddress()))
		    			{
			    			if (examineRTPDatagram.sequenceNumber < socket.ackNumber){
				    			socket.sendAck(examineRTPDatagram.sequenceNumber + 1);
				    		} else {
					    		socket.receiveDefaultRTPDatagramBuffer.add(new RTPDatagram(x.getData()));
				    		}
		    			} else {
	    					socket.receiveDefaultRTPDatagramBuffer.add(examineRTPDatagram);
		    			}
		    		}

		    		// We've already received this and acked it, so we'll ack it again
		    		if (examineRTPDatagram.sequenceNumber < socket.ackNumber){
		    			socket.sendAck(examineRTPDatagram.sequenceNumber + 1);
		    		} else {
			    		socket.receiveDefaultRTPDatagramBuffer.add(new RTPDatagram(x.getData()));
		    		}
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
    	public boolean ackReceived;

    	public SendLoop(RTPSocket socket, RTPDatagram rtpDatagram, int retries, int waitTime){
    		this.retries = retries;
    		this.ackReceived = false;
    		this.rtpSocket = socket;
    		this.dataToSend = dataToSend;
    		super();
    	}

    	public void run(){
    		int counter = 0;
			for (int i = 0; i < socket.receiveDefaultRTPDatagramBuffer.size(); i++){
				RTPDatagram tmp = socket.receiveDefaultRTPDatagramBuffer.get(i);
				if (tmp.flags * RTPDatagram.ACK > 0 && tmp.ackNumber = rtpSocket.sequenceNumber + 1){
					rtpSocket.sequenceNumber += 1;
					rtpSocket.stopAndWait = false;
					this.ackReceived = true;
					receiveDefaultRTPDatagramBuffer.remove(tmp);
				}
			}

    		while (counter < retries){
		    	if (!socket.stopAndWait()){
					// Send the packet
					byte[] datagramArray = datagram.getByteArray();
					DatagramPacket udpDatagram = new udpDatagram(datagramArray, datagramArray.length, address);
					datagramSocket.send(udpDatagram);

					rtpSocket.stopAndWait = true;
		    	} else {

		    		for (int i = 0; i < socket.receiveDefaultRTPDatagramBuffer.size(); i++){
		    			RTPDatagram tmp = socket.receiveDefaultRTPDatagramBuffer.get(i);
		    			if (tmp.flags * RTPDatagram.ACK > 0 && tmp.ackNumber = rtpSocket.sequenceNumber + 1){
		    				rtpSocket.sequenceNumber += 1;
		    				rtpSocket.stopAndWait = false;
		    				this.ackReceived = true;
		    				receiveDefaultRTPDatagramBuffer.remove(tmp);
		    			}
		    		}

		    		Thread.sleep(waitTime);

		    		counter++;
		    	}
			}
    	}
    }

    private static int getEphemeralPort(InetAddress address) {
    	do {
		    // NOTE: Usually this should be a field rather than a method
		    // variable so that it is not re-seeded every call.
		    Random rand = new Random();

		    // nextInt is normally exclusive of the top value,
		    // so add 1 to make it inclusive
		    int randomNum = rand.nextInt((65000 - 60000) + 1) + 60000;
		} while (RTPSocket.isPortInUse(address, randomNum));
	    
	    return randomNum;
	}

    private static boolean isPortInUse(InetAddress host, int port) {
		// Assume no connection is possible.
		boolean result = false;
		try {
			(new Socket(host, port)).close();
			result = true;
		} catch(SocketException e) {
			// Could not connect.
		}
		return result;
	}
}