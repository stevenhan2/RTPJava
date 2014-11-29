import java.util.*;
import java.util.concurrent.*;
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
	public CopyOnWriteArrayList<DatagramPacket> receiveSynRTPDatagramBuffer;
	public CopyOnWriteArrayList<RTPDatagram> receiveAckRTPDatagramBuffer;

	public Thread receiveThread;

	public RTPSocket(){
		this(null);
	}

    public RTPSocket(InetSocketAddress address){
    	RTPUtil.debug("RTPSocket(" + address + ")");
		try {
	    	datagramSocket = new DatagramSocket(null);
	    	datagramSocket.setSoTimeout(10000);
    	} catch (Exception e){
    		e.printStackTrace();
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
				this.receiveSynRTPDatagramBuffer = new CopyOnWriteArrayList<DatagramPacket>();
				this.receiveDefaultRTPDatagramBuffer = new SetPriorityQueue<RTPDatagram>();
				this.receiveAckRTPDatagramBuffer = new CopyOnWriteArrayList<RTPDatagram>();

    			connectionAddress = address;

    			try {
		    		this.bind(new InetSocketAddress(InetAddress.getLocalHost(),RTPSocket.getEphemeralPort(InetAddress.getLocalHost())));
		    	} catch (Exception e){
		    		e.printStackTrace();
		    	}

	    		this.receiveThread = new Thread(new ReceiveBufferLoop(this));
	    		this.receiveThread.start();

	    		// ===========================
	    		// 1) Send SYN
	    		// Set sequence number
	    		Random rand = new Random();
	    		this.sequenceNumber = (long)(rand.nextInt(500000) + 100000);

	    		// ===========================
	    		// 2) ReceiveBufferLoop will accept a SYN, the thread will look for an ACK and retransmit if can't find it

	    		SendThread sendSynThread;
	    		boolean resolved = false;

	    		long firstNanoTime = System.nanoTime();

	    		while (!resolved){
	    			RTPUtil.debug("resolving: starting");
	    			if (state == State.CLOSED){
	    				RTPUtil.debug("resolving: was closed, sending syn");
		    			resolved = true;
		    			sendSynThread = sendSyn(this.sequenceNumber);
			    		state = State.SYNSENT;
			    		try {
			    			RTPUtil.debug("resolving: joining sendSynThread");
				    		sendSynThread.join();
				    		RTPUtil.debug("resolving: was able to join sendSynThread");
				    	} catch (Exception e){
				    		e.printStackTrace();
				    	}


			    		if (sendSynThread.ackReceived == false && stopAndWait == true){
			    			RTPUtil.debug("sendSynThread.ackReceived == false && stopAndWait == true");
			    			state = State.CLOSED;
			    			return false;
			    		} else if (state == State.ESTABLISHED){
			    			// Means that the ReceiveBufferLoop accepted a SYN for us
			    			return true;
			    		} else {
			    			// timeout for waiting for SYN basically
			    			if (System.nanoTime() - firstNanoTime > 5000000){
			    				RTPUtil.debug("resolving: ran out of time");
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

    public boolean bind(InetSocketAddress bindAddress){
    	RTPUtil.debug("bind(" + bindAddress + ")");
    	this.bindAddress = bindAddress;

    	try {
			datagramSocket.bind(bindAddress);
			return true;
    	} catch (Exception e){
    		e.printStackTrace();
    		return false;
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

    	Thread sendThread = new SendThread(this, rtpDatagram, 5, 50);
    	sendThread.start();
    }

    public boolean getStopAndWait(){
    	RTPUtil.debug("getStopAndWait() returned " + stopAndWait);
    	return stopAndWait;
    }

    public boolean accept(){
    	// RTPUtil.debug("accept");
    	if (state == State.LISTEN){
    		RTPUtil.debug("Accept: and state was listen");
    		if (receiveSynRTPDatagramBuffer.size() > 0){
    			RTPUtil.debug("Something was accepted!!! Cause the Syn Buffer was longer than 0!");
    			DatagramPacket examineUDPDatagram = receiveSynRTPDatagramBuffer.remove(0);
    			RTPDatagram examineRTPDatagram = new RTPDatagram(examineUDPDatagram.getData());
				state = State.SYNRCVD;
				connectionAddress = new InetSocketAddress(examineUDPDatagram.getAddress(), examineUDPDatagram.getPort());

				RTPDatagram synAckRTPDatagram = new RTPDatagram(
					bindAddress.getPort(),
					connectionAddress.getPort(),
					RTPDatagram.ACK + RTPDatagram.SYN,
					receiveWindow,
					new byte[0]
				);

				Random rand = new Random();
	    		this.sequenceNumber = (long)(rand.nextInt(500000) + 100000);
	    		synAckRTPDatagram.sequenceNumber = this.sequenceNumber;
	    		this.ackNumber = examineRTPDatagram.sequenceNumber + 1L;

				synAckRTPDatagram.sequenceNumber = this.sequenceNumber;
				synAckRTPDatagram.ackNumber = this.ackNumber;

				Thread sendThread = new SendThread(this, synAckRTPDatagram, 5, 500);
		    	sendThread.start();

				// Pack and send

				try {
					sendThread.join();
		    	} catch (Exception e){
		    		e.printStackTrace();
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
			RTPUtil.debug("Sending ACk ----------------------\n" + ackRTPDatagram.toString());
			byte[] ackRTPDatagramArray = ackRTPDatagram.getByteArray();
			DatagramPacket ackUDPDatagram = new DatagramPacket(ackRTPDatagramArray, ackRTPDatagramArray.length, connectionAddress);
			datagramSocket.send(ackUDPDatagram);
		} catch (Exception e){
	    	e.printStackTrace();
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

		SendThread newThread = new SendThread(this, synRTPDatagram, 5, 50);
		newThread.start();
		return newThread;
    }

    public void listen(){
    	RTPUtil.debug("listen()");
    	switch (state) {
    		case CLOSED:
	    		RTPUtil.debug("State set to LISTEN");
				state = State.LISTEN;

				this.receiveSynRTPDatagramBuffer = new CopyOnWriteArrayList<DatagramPacket>();
				this.receiveAckRTPDatagramBuffer = new CopyOnWriteArrayList<RTPDatagram>();
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

		    		RTPUtil.debug("ReceiveBufferLoop: 0. received from (" + examineUDPDatagram.getAddress() + ", " + examineUDPDatagram.getPort());
		    		RTPUtil.debug("ReceiveBufferLoop: 1. Received in ReceiveBufferLoop");
		    		if ((examineRTPDatagram.flags & RTPDatagram.SYN) > 0){
		    			RTPUtil.debug("ReceiveBufferLoop: 2. It was a SYN");
		    			if (state == State.SYNSENT){
		    				if (examineUDPDatagram.getPort() == connectionAddress.getPort() &&
		    					examineUDPDatagram.getAddress().equals(connectionAddress.getAddress())){
		    					ackNumber = examineRTPDatagram.sequenceNumber + 1l;
		    					sendAck(ackNumber);
		    					state = State.ESTABLISHED;
		    					RTPUtil.debug("ReceiveBufferLoop: 3. State = Estabished");
		    				}
		    			}

		    			RTPUtil.debug("ReceiveBufferLoop: 3.33 About to see if should put anything in Syn Buffer");
		    			if (!receiveSynRTPDatagramBuffer.contains(examineUDPDatagram)){
		    				receiveSynRTPDatagramBuffer.add(examineUDPDatagram);
		    				RTPUtil.debug("ReceiveBufferLoop: 3.66 Something added in synbuffer It's size is now " + receiveSynRTPDatagramBuffer.size());
		    			}
		    		}

		    		if ((examineRTPDatagram.flags & RTPDatagram.ACK) > 0){
		    			RTPUtil.debug("ReceiveBufferLoop: 4. It was an ACK");
		    			if (examineUDPDatagram.getPort() == connectionAddress.getPort() &&
		    				examineUDPDatagram.getAddress().equals(connectionAddress.getAddress()))
		    			{
	    				// if (state == State.ESTABLISHED){
			    			if (stopAndWait && examineRTPDatagram.ackNumber == sequenceNumber + 1){
			    				// stopAndWait = false;
			    				// sequenceNumber += 1;
			    				if (state == State.SYNSENT){
			    					state = State.ESTABLISHED;
			    				}
			    			}
			    			// } else {
			    				receiveAckRTPDatagramBuffer.add(examineRTPDatagram);
			    			// }
		    			}
		    		}

		    		// We've already received this and acked it, so we'll ack it again
		    		if (state == State.ESTABLISHED && examineRTPDatagram.sequenceNumber < ackNumber){
		    			sendAck(examineRTPDatagram.sequenceNumber + 1L);
		    		} else {
			    		receiveDefaultRTPDatagramBuffer.add(examineRTPDatagram);
		    		}
		    	} catch (SocketTimeoutException e){
		    		continue;
		    	} catch (Exception e){
		    		e.printStackTrace();
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
	   		this.waitTime = waitTime;

	   		RTPUtil.debug("SendThread created for:");
	   		RTPUtil.debug(rtpDatagram.toString());
    	}

    	public void run(){
    		int counter = 0;
    		RTPUtil.debug("SendThread: running");
    		try {
				byte[] datagramArray = this.rtpDatagram.getByteArray();
				DatagramPacket udpDatagram = new DatagramPacket(datagramArray, datagramArray.length, rtpSocket.connectionAddress);
				this.rtpSocket.datagramSocket.send(udpDatagram);
				this.rtpSocket.stopAndWait = true;

	    		while (counter < retries && !ackReceived){
		    		for (RTPDatagram tmp : this.rtpSocket.receiveAckRTPDatagramBuffer){
		    			if (tmp.flags * RTPDatagram.ACK > 0 && tmp.ackNumber == rtpSocket.sequenceNumber + 1){
							this.rtpSocket.sequenceNumber += 1;
							this.rtpSocket.stopAndWait = false;
							this.ackReceived = true;
							this.rtpSocket.receiveAckRTPDatagramBuffer.remove(tmp);
						}
		    		}

		    		Thread.currentThread().sleep(waitTime);
		    		RTPUtil.debug("SendThread: sleeping");

		    		counter++;
		    	}
		    } catch (Exception e){
		    	e.printStackTrace();
		    }
    	}
    }

    public static int getEphemeralPort(InetAddress address) {
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


	 public String toString(){
    	String returnString = "====================Printing RTPSocket=======================";

    	returnString += 
    	"\nBind address:" + bindAddress +
    	"\nConnection address:" + connectionAddress +
    	"\nSequence number:" + sequenceNumber +
    	"\nAck number:" + ackNumber +
    	"\nState:" + state + 
		"\nreceiveDefaultRTPDatagramBuffer.size(): " + receiveDefaultRTPDatagramBuffer.size() +
		"\nreceiveSynRTPDatagramBuffer.size(): " + receiveSynRTPDatagramBuffer.size() +
		"\nreceiveAckRTPDatagramBuffer.size(): " + receiveAckRTPDatagramBuffer.size();


    	returnString += "\n================================================================";
    	return returnString;
    }
}