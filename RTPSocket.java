import java.util.*;
import java.net.*;

public class RTPSocket {
	public enum State {
		CLOSED, LISTEN, SYNRCVD, SYNSENT, ESTABLISHED, FINWAIT1, CLOSING, FINWAIT2, TIMEWAIT, CLOSEWAIT, LASTACK
	}

	// A connection is identified by combination of source host address, source port, destination host address, and destination port
	public static final int DEFAULT_RECEIVE_WINDOW_BYTE_AMOUNT = 65535;

	public State state;
	public DatagramSocket datagramSocket;

	public InetSocketAddress bindAddress;
	public InetSocketAddress connectionAddress;

	public byte[] receiveWindow;

	public long sequenceNumber;
	public long ackNumber;

	boolean stopAndWait;

	public SetPriorityQueue<RTPDatagram> receiveDefaultRTPDatagramBuffer;
	public ArrayList<DatagramPacket> receiveSynRTPDatagramBuffer;
	public ArrayList<RTPDatagram> receiveAckRTPDatagramBuffer;

	public Thread receiveThread;

	public RTPSocket(){
		this(null);
	}

    public RTPSocket(InetSocketAddress address){
    	RTPUtil.debug("RTPSocket(" + address + ")");
    	if (address != null){
    		try {
		    	datagramSocket = new DatagramSocket(address);
		    	datagramSocket.setSoTimeout(10000);
	    	} catch (Exception e){
	    		RTPUtil.debug(e.toString());
	    	}
    	}
    	connectionAddress = address;

		state = State.CLOSED;
		stopAndWait = true;

		receiveWindow = RTPUtil.toIntBytes(50000);
    }

    public boolean connect(InetSocketAddress address){
    	RTPUtil.debug("connect(" + address + ")");
    	switch (state) {
    		case CLOSED:
				this.receiveSynRTPDatagramBuffer = new ArrayList<DatagramPacket>();
				this.receiveDefaultRTPDatagramBuffer = new SetPriorityQueue<RTPDatagram>();

    			connectionAddress = address;

    			try {
		    		datagramSocket = new DatagramSocket(address);
		    		this.bind(new InetSocketAddress(InetAddress.getLocalHost(),RTPSocket.getEphemeralPort(InetAddress.getLocalHost())));
		    	} catch (Exception e){
		    		RTPUtil.debug(e.toString());
		    	}

	    		this.receiveThread = new Thread(new ReceiveBufferLoop(this));
	    		this.receiveThread.start();

	    		// ===========================
	    		// 1) Send SYN
	    		// Set sequence number
	    		Random rand = new Random();
	    		this.sequenceNumber = rand.nextInt();

	    		// ===========================
	    		// 2) ReceiveBufferLoop will accept a SYN, the thread will look for an ACK and retransmit if can't find it

	    		SendThread sendSynThread;
	    		boolean resolved = false;

	    		long firstNanoTime = System.nanoTime();

	    		while (!resolved){
	    			if (state == State.CLOSED){
		    			resolved = true;
		    			sendSynThread = sendSyn(this.sequenceNumber);
			    		state = State.SYNSENT;
			    		try {
				    		sendSynThread.join();
				    	} catch (Exception e){
				    		RTPUtil.debug(e.toString());
				    	}
			    	// } else {
			    		if (sendSynThread.ackReceived == false && stopAndWait == true){
			    			state = State.CLOSED;
			    			return false;
			    		} else if (state == State.ESTABLISHED){
			    			// Means that the ReceiveBufferLoop accepted a SYN for us
			    			return true;
			    		} else {
			    			// timeout for waiting for SYN basically
			    			if (System.nanoTime() - firstNanoTime > 5000000){
			    				state = State.CLOSED;
			    			} else {
			    				resolved = false;
			    			}
			    		}
			    	}
	    		}
	    		break;
    	}
    	return false;
    }

    public void bind(InetSocketAddress bindAddress){
    	RTPUtil.debug("bind(" + bindAddress + ")");
    	this.bindAddress = bindAddress;

    	try {
			datagramSocket.bind(bindAddress);
    	} catch (Exception e){
    		RTPUtil.debug(e.toString());
    	}
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

    	Thread sendThread = new SendThread(this, rtpDatagram, 5, 100);
    	sendThread.start();
    }

    public boolean getStopAndWait(){
    	RTPUtil.debug("getStopAndWait() returned " + stopAndWait);
    	return stopAndWait;
    }

    public boolean accept(){
    	RTPUtil.debug("accept");
    	if (state == State.LISTEN){
    		if (receiveSynRTPDatagramBuffer.size() > 0){
    			DatagramPacket examineUDPDatagram = receiveSynRTPDatagramBuffer.remove(0);
    			RTPDatagram examineRTPDatagram = new RTPDatagram(examineUDPDatagram.getData());
				state = State.SYNRCVD;
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
	    		synAckRTPDatagram.sequenceNumber = this.sequenceNumber;
	    		this.ackNumber = examineRTPDatagram.sequenceNumber + 1;

				synAckRTPDatagram.sequenceNumber = this.sequenceNumber;
				synAckRTPDatagram.ackNumber = this.ackNumber;

				Thread sendThread = new SendThread(this, synAckRTPDatagram, 5, 100);
		    	sendThread.start();

				// Pack and send

				try {
					sendThread.join();
		    	} catch (Exception e){
		    		RTPUtil.debug(e.toString());
		    	}

				// If never received any ACK, return false
				if (stopAndWait == true){
					state = State.CLOSED;
					return false;
				} else {
    				state = State.ESTABLISHED;
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

    	RTPDatagram tmp = (RTPDatagram) this.receiveDefaultRTPDatagramBuffer.peek();

    	while (count++ < retries){
	    	if (tmp.sequenceNumber == this.ackNumber){
	    		this.ackNumber += 1;
	    		sendAck(this.ackNumber);

	    		return tmp.data;
	    	}
	    }
	    return null;
    }

    public void sendAck(long number){
    	RTPUtil.debug("sendAck(" + number + ")");
    	RTPDatagram ackRTPDatagram = new RTPDatagram(
			bindAddress.getPort(),
			connectionAddress.getPort(),
			RTPDatagram.ACK,
			receiveWindow,
			new byte[0]
		);

		ackRTPDatagram.ackNumber = number;

		try {
			// Pack and send
			byte[] ackRTPDatagramArray = ackRTPDatagram.getByteArray();
			DatagramPacket ackUDPDatagram = new DatagramPacket(ackRTPDatagramArray, ackRTPDatagramArray.length, connectionAddress);
			datagramSocket.send(ackUDPDatagram);
		} catch (Exception e){
	    	RTPUtil.debug(e.toString());
	    }
    }

	public SendThread sendSyn(long number){
		RTPUtil.debug("sendSyn(" + number + ")");
    	RTPDatagram synRTPDatagram = new RTPDatagram(
			bindAddress.getPort(),
			connectionAddress.getPort(),
			RTPDatagram.SYN,
			receiveWindow,
			new byte[0]
		);

		synRTPDatagram.sequenceNumber = number;

		SendThread newThread = new SendThread(this, synRTPDatagram, 5, 100);
		newThread.start();
		return newThread;
    }

    public void listen(){
    	RTPUtil.debug("listen()");
    	switch (state) {
    		case CLOSED:
				state = State.LISTEN;

				this.receiveSynRTPDatagramBuffer = new ArrayList<DatagramPacket>();
				this.receiveDefaultRTPDatagramBuffer = new SetPriorityQueue<RTPDatagram>();

				this.receiveThread = new Thread(new ReceiveBufferLoop(this));
				this.receiveThread.start();
	    		break;
    	}
    }

    public void close(){
    	RTPUtil.debug("close()");
    	switch (state) {
    		case LISTEN:	
	    		state = State.CLOSED;
	    		break;
    		case SYNSENT:	
	    		state = State.CLOSED;
	    		break;
    	}
    }

    private class ReceiveBufferLoop implements Runnable {
    	// public RTPSocket socket;
    	public boolean keepRunning;

    	public ReceiveBufferLoop(RTPSocket socket){
			super();
    		keepRunning = true;
    	}

    	public void run(){
    		while (keepRunning){
	    		try {
	    			byte[] message = new byte[2048];
		    		DatagramPacket examineUDPDatagram = new DatagramPacket(message, message.length);
		    		datagramSocket.receive(examineUDPDatagram);

		    		RTPDatagram examineRTPDatagram = new RTPDatagram(examineUDPDatagram.getData());

		    		if ((examineRTPDatagram.flags & RTPDatagram.SYN) > 0){
		    			if (state == State.SYNSENT){
		    				if (examineUDPDatagram.getPort() == connectionAddress.getPort() &&
		    					examineUDPDatagram.getAddress().equals(connectionAddress.getAddress())){
		    					ackNumber = examineRTPDatagram.sequenceNumber + 1l;
		    					sendAck(ackNumber);
		    					state = State.ESTABLISHED;
		    				}
		    			}
		    			if (!receiveSynRTPDatagramBuffer.contains(examineUDPDatagram)){
		    				receiveSynRTPDatagramBuffer.add(examineUDPDatagram);
		    			}
		    		}

		    		if ((examineRTPDatagram.flags & RTPDatagram.ACK) > 0){
		    			if (state == State.ESTABLISHED &&
		    				examineUDPDatagram.getPort() == connectionAddress.getPort() &&
		    				examineUDPDatagram.getAddress().equals(connectionAddress.getAddress()))
		    			{
			    			if (stopAndWait && examineRTPDatagram.ackNumber == sequenceNumber + 1){
			    				stopAndWait = false;
			    				sequenceNumber += 1;
			    			} else {
			    				receiveAckRTPDatagramBuffer.add(examineRTPDatagram);
			    			}

				    		// else {if 
					    		// receiveDefaultRTPDatagramBuffer.add(examineRTPDatagram);
					    		// receiveAckRTPDatagramBuffer.add(examineRTPDatagram);
				    		// }
		    			// } 
		    			// else {
	    					// receiveDefaultRTPDatagramBuffer.add(examineRTPDatagram);
	    					// receiveAckRTPDatagramBuffer.add(examineRTPDatagram);
		    			}
		    		}

		    		// We've already received this and acked it, so we'll ack it again
		    		if (examineRTPDatagram.sequenceNumber < ackNumber){
		    			sendAck(examineRTPDatagram.sequenceNumber + 1);
		    		} else {
			    		receiveDefaultRTPDatagramBuffer.add(examineRTPDatagram);
		    		}
		    	} catch (SocketTimeoutException e){
		    		continue;
		    	} catch (Exception e){
		    		RTPUtil.debug(e.toString());
		    	}

			}
    	}

    	public void stop(){
    		keepRunning = false;
    	}
    }


    private static class SendThread extends Thread{
    	public RTPSocket rtpSocket;
    	public byte[] dataToSend;
    	public int retries;
    	public int waitTime;
    	public boolean ackReceived;
    	public RTPDatagram rtpDatagram;

    	public SendThread(RTPSocket socket, RTPDatagram rtpDatagram, int retries, int waitTime){
    		super();
    		this.retries = retries;
    		this.ackReceived = false;
    		this.rtpSocket = socket;
    		this.dataToSend = dataToSend;
	   		this.rtpDatagram = rtpDatagram;
    	}

    	public void run(){
    		int counter = 0;
    		try {
				byte[] datagramArray = this.rtpDatagram.getByteArray();
				DatagramPacket udpDatagram = new DatagramPacket(datagramArray, datagramArray.length, rtpSocket.connectionAddress);
				this.rtpSocket.datagramSocket.send(udpDatagram);
				this.rtpSocket.stopAndWait = true;

	    		while (counter < retries){
		    		for (RTPDatagram tmp : this.rtpSocket.receiveAckRTPDatagramBuffer){
		    			if (tmp.flags * RTPDatagram.ACK > 0 && tmp.ackNumber == rtpSocket.sequenceNumber + 1){
							this.rtpSocket.sequenceNumber += 1;
							this.rtpSocket.stopAndWait = false;
							this.rtpSocket.receiveAckRTPDatagramBuffer.remove(tmp);
						}
		    		}

		    		Thread.sleep(waitTime);

		    		counter++;
		    	}
		    } catch (Exception e){
		    	RTPUtil.debug(e.toString());
		    }
    	}
    }

    private static int getEphemeralPort(InetAddress address) {
    	int randomNum;
    	Random rand = new Random();
    	do {
		    // NOTE: Usually this should be a field rather than a method
		    // variable so that it is not re-seeded every call.
		    

		    // nextInt is normally exclusive of the top value,
		    // so add 1 to make it inclusive
		    randomNum = rand.nextInt((65000 - 60000) + 1) + 60000;
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
		} catch(Exception e){

		}
		return result;
	}
}