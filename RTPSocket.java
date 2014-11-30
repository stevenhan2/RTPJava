import java.util.*;
import java.util.concurrent.*;
import java.net.*;

public class RTPSocket {
	public enum State {
		CLOSED, LISTEN, SYNRCVD, SYNSENT, ESTABLISHED, FINWAIT1, CLOSING, FINWAIT2, TIMEWAIT, CLOSEWAIT, LASTACK
	}

	// A connection is identified by combination of source host address, source port, destination host address, and destination port
	public static final int DEFAULT_RECEIVE_WINDOW_BYTE_AMOUNT = 65535;
	public static final int DEFAULT_WAIT_AMOUNT = 100;
	public static final int DEFAULT_RETRY_AMOUNT = 12;

	public State state;
	public DatagramSocket datagramSocket;

	public InetSocketAddress bindAddress;
	public InetSocketAddress connectionAddress;

	public byte[] receiveWindow;

	public long sequenceNumber;
	public long ackNumber;

	boolean stopAndWait;

	public CopyOnWriteArrayList<DatagramPacket> receiveSynRTPDatagramBuffer;

	public volatile LinkedList<RTPDatagram> sendBuffer;

	public RTPDatagram receiveDataDatagram;
	// public RTPDatagram expectedAckRTPDatagram;

	public ReceiveBufferThread receiveThread;
	public SendThread sendThread;

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

		state = RTPSocket.State.CLOSED;
		stopAndWait = true;

		receiveWindow = RTPUtil.toIntBytes(50000);
    }

    public boolean connect(InetSocketAddress address){
    	RTPUtil.debug("connect(" + address + ")");
    	switch (state) {
    		case CLOSED:
				this.receiveSynRTPDatagramBuffer = new CopyOnWriteArrayList<DatagramPacket>();
				this.sendBuffer = new LinkedList<RTPDatagram>();
    			connectionAddress = address;
    			this.stopAndWait = false;

    			try {
		    		this.bind(new InetSocketAddress("0.0.0.0",RTPSocket.getEphemeralPort(InetAddress.getLocalHost())));
		    	} catch (Exception e){
		    		e.printStackTrace();
		    	}

	    		this.receiveThread = new ReceiveBufferThread();
	    		this.receiveThread.start();

		    	this.sendThread = new SendThread();
		    	this.sendThread.start();

	    		// ===========================
	    		// 1) Send SYN
	    		// Set sequence number
	    		Random rand = new Random();
	    		this.sequenceNumber = (long)(rand.nextInt(500000) + 100000);

	    		// ===========================
	    		// 2) ReceiveBufferThread will accept a SYN, the thread will look for an ACK and retransmit if can't find it

	    		boolean resolved = false;
	    		long firstNanoTime = System.nanoTime();

	    		while (!resolved){
	    			RTPUtil.debug("connect(): starting");
	    			if (state == RTPSocket.State.CLOSED){
	    				RTPUtil.debug("connect(): was closed, sending syn");
		    			resolved = true;
			    		state = RTPSocket.State.SYNSENT;
		    			sendSyn(this.sequenceNumber);

			    		try {
			    			synchronized(this.sendThread){
				    			RTPUtil.debug("connect(): waiting sendSynThread");
				    			this.sendThread.wait();
					    		RTPUtil.debug("connect(): was able to wait sendSynThread");
				    		}
				    	} catch (Exception e){
				    		e.printStackTrace();
				    	}

			    		if (state == RTPSocket.State.ESTABLISHED){
			    			// Means that the ReceiveBufferThread accepted a SYN for us
			    			return true;
			    		} else {
			    			// timeout for waiting for SYN basically
			    			if (System.nanoTime() - firstNanoTime > 5000000){
			    				RTPUtil.debug("connect(): ran out of time");
			    				state = RTPSocket.State.CLOSED;
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

    	// sendBuffer.add(data);
    	RTPUtil.debug("send() length:" + data.length);
    	RTPDatagram rtpDatagram = new RTPDatagram(
			bindAddress.getPort(),
			connectionAddress.getPort(),
			0,
			receiveWindow,
			data
		);

		rtpDatagram.sequenceNumber = this.sequenceNumber;

		sendBuffer.add(rtpDatagram);
    }

    public boolean accept(){
    	if (state == RTPSocket.State.LISTEN){
    		if (receiveSynRTPDatagramBuffer.size() > 0){

    			RTPUtil.debug("Something was accepted.");
    			DatagramPacket examineUDPDatagram = receiveSynRTPDatagramBuffer.remove(0);
    			RTPDatagram examineRTPDatagram = new RTPDatagram(examineUDPDatagram.getData());
				state = RTPSocket.State.SYNRCVD;
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
				synAckRTPDatagram.ackNumber = this.ackNumber;

				RTPUtil.debug("Putting SYNACK in buffer");
				sendBuffer.add(synAckRTPDatagram);
				RTPUtil.debug("sendBUffer size:" + sendBuffer.size());

				try {
					synchronized(this.sendThread){
						while (stopAndWait == true){
							this.sendThread.wait();
						}
					}
		    	} catch (Exception e){
		    		e.printStackTrace();
		    	}

				// If never received any ACK, return false
				if (stopAndWait == true){
					state = RTPSocket.State.CLOSED;
					return false;
				} else {
    				state = RTPSocket.State.ESTABLISHED;
    				return true;
    			}
    		}
    	}

    	return false;
    }

    public byte[] receive(){
    	RTPUtil.debug("receive()");
    	int count = 0;

    	while (count++ < DEFAULT_RETRY_AMOUNT){
    		RTPDatagram tmp = this.receiveDataDatagram;
    		if (tmp != null){
		    	if (tmp.sequenceNumber == this.ackNumber){
		    		receiveDataDatagram = null;
		    		this.ackNumber = tmp.sequenceNumber + 1L;
		    		sendAck(this.ackNumber);
		    		return tmp.data;
		    	}
	    	}

	    	try {
		    	synchronized(this.receiveThread){
		    		this.receiveThread.wait(DEFAULT_WAIT_AMOUNT);
		    	}
		    } catch (Exception e){
		    	e.printStackTrace();
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
			ackRTPDatagram.updateChecksum();
			byte[] ackRTPDatagramArray = ackRTPDatagram.getByteArray();
			DatagramPacket ackUDPDatagram = new DatagramPacket(ackRTPDatagramArray, ackRTPDatagramArray.length, connectionAddress);
			datagramSocket.send(ackUDPDatagram);
		} catch (Exception e){
	    	e.printStackTrace();
	    }
    }

	public void sendSyn(long number){
		RTPUtil.debug("sendSyn(" + number + ")");
    	RTPDatagram synRTPDatagram = new RTPDatagram(
			bindAddress.getPort(),
			connectionAddress.getPort(),
			RTPDatagram.SYN,
			receiveWindow,
			new byte[0]
		);

		sendBuffer.add(synRTPDatagram);
    }

    public void listen(){
    	RTPUtil.debug("listen()");
    	switch (state) {
    		case CLOSED:
	    		RTPUtil.debug("State set to LISTEN");
				state = RTPSocket.State.LISTEN;

				this.receiveSynRTPDatagramBuffer = new CopyOnWriteArrayList<DatagramPacket>();

				this.receiveThread = new ReceiveBufferThread();
				this.receiveThread.start();

				this.sendBuffer = new LinkedList<RTPDatagram>();
		    	this.sendThread = new SendThread();
		    	this.sendThread.start();
	    		break;
    	}
    }

    public void close(){
    	RTPUtil.debug("close()");
    	switch (state) {
    		case LISTEN:	
	    		state = RTPSocket.State.CLOSED;
	    		break;
    		case SYNSENT:	
	    		state = RTPSocket.State.CLOSED;
	    		break;
    	}
    }

    private class ReceiveBufferThread extends Thread {
    	public boolean keepRunning;

    	public ReceiveBufferThread(){
			super();
    		keepRunning = true;
    	}

    	public void run(){
    		while (keepRunning){
    			RTPUtil.debug("ReceiveBufferThread: Looping...");
    			RTPUtil.debug(RTPSocket.this.toString());
	    		try {
	    			byte[] message = new byte[2048];
		    		DatagramPacket examineUDPDatagram = new DatagramPacket(message, message.length);
		    		datagramSocket.receive(examineUDPDatagram);

		    		byte[] actualLengthDataBytes = RTPUtil.cutByteArray(examineUDPDatagram.getData(), examineUDPDatagram.getLength());
		    
		    		RTPDatagram examineRTPDatagram = new RTPDatagram(actualLengthDataBytes);

		    		if (!examineRTPDatagram.checkChecksum()){
		    			RTPUtil.debug("ReceiveBufferThread: Checksum failed.");
		    			// RTPUtil.debug(examineRTPDatagram.toString());
		    			continue;
		    		} else {
		    			RTPUtil.debug("ReceiveBufferThread: Checksum was correct.");
		    		}

		    		// RTPUtil.debug("ReceiveBufferThread: 0. received from (" + examineUDPDatagram.getAddress() + ", " + examineUDPDatagram.getPort());
		    		RTPUtil.debug("ReceiveBufferThread: 1. Received in ReceiveBufferThread");
		    		RTPUtil.debug("ReceiveBufferThread: 1.5 " + examineRTPDatagram.toString());

		    		if ((examineRTPDatagram.flags & RTPDatagram.SYN) > 0){
		    			RTPUtil.debug("ReceiveBufferThread: 2. It was a SYN");
		    			if (state == RTPSocket.State.SYNSENT){
		    				if (examineUDPDatagram.getPort() == connectionAddress.getPort() &&
		    					examineUDPDatagram.getAddress().equals(connectionAddress.getAddress())){
		    					ackNumber = examineRTPDatagram.sequenceNumber + 1l;
		    					sendAck(ackNumber);
		    					state = RTPSocket.State.ESTABLISHED;
		    					RTPUtil.debug("ReceiveBufferThread: 3. State = Estabished");
		    				}
		    			} else {
			    			RTPUtil.debug("ReceiveBufferThread: 3.33 About to see if should put anything in Syn Buffer");

			    			// Needs custom detection
			    			boolean shouldAdd = true;

			    			if (connectionAddress != null && 
			    				examineUDPDatagram.getPort() == connectionAddress.getPort() &&
		    					examineUDPDatagram.getAddress().equals(connectionAddress.getAddress())){
			    				shouldAdd = false;
			    			} else {
				    			for (int i = 0; i < receiveSynRTPDatagramBuffer.size(); i++){
				    				if (new RTPDatagram(receiveSynRTPDatagramBuffer.get(i).getData()).equals(examineRTPDatagram)){
				    					RTPUtil.debug("ReceiveBufferThread: 3.50 Found same UDP packet in syn buffer");
				    					shouldAdd = false;
				    					break;
				    				}
				    			}
				    		}
			    			// if (!receiveSynRTPDatagramBuffer.contains(examineUDPDatagram)){
			    			if (shouldAdd){
			    				receiveSynRTPDatagramBuffer.add(examineUDPDatagram);
			    				RTPUtil.debug("ReceiveBufferThread: 3.66 Something added in synbuffer It's size is now " + receiveSynRTPDatagramBuffer.size());
			    			}
		    			}
		    		}

		    		if ((examineRTPDatagram.flags & RTPDatagram.ACK) > 0){
		    			RTPUtil.debug("ReceiveBufferThread: 4. It was an ACK");
		    			if (examineUDPDatagram.getPort() == connectionAddress.getPort() &&
		    				examineUDPDatagram.getAddress().equals(connectionAddress.getAddress()))
		    			{
			    			if (stopAndWait && examineRTPDatagram.ackNumber == sequenceNumber + 1){
			    				RTPUtil.debug("ACK: Received an ACK from SendThread: ack" + examineRTPDatagram.ackNumber + "\tseq:" + examineRTPDatagram.sequenceNumber);
									RTPSocket.this.sequenceNumber += 1;
									RTPSocket.this.stopAndWait = false;

							// 		RTPSocket.this.expectedAckRTPDatagram = null;
							// 		RTPUtil.debug(RTPSocket.this.toString());

			    				// expectedAckRTPDatagram = examineRTPDatagram;
			    				if (state == RTPSocket.State.SYNRCVD){
			    					state = RTPSocket.State.ESTABLISHED;
			    				}

			    				synchronized(this){
				    				notify();
				    			}
			    			}
		    			}
		    		}

		    		// We've already received this and acked it, so we'll ack it again
		    		if (state == RTPSocket.State.ESTABLISHED && examineRTPDatagram.flags == 0){
		    			// if (examineRTPDatagram.sequenceNumber < ackNumber){
		    			// 	RTPUtil.debug("this should have been acked already");
			    		// 	sendAck(examineRTPDatagram.sequenceNumber + 1L);
			    		// } else 
			    		if (examineRTPDatagram.sequenceNumber == ackNumber) {
			    			receiveDataDatagram = examineRTPDatagram;
			    			synchronized(this){
			    				notify();
			    			}
				    		// receiveDefaultRTPDatagramBuffer.add(examineRTPDatagram);
			    		}
			    	} else if (state == RTPSocket.State.SYNSENT || state == RTPSocket.State.SYNRCVD){
			    		if (examineRTPDatagram.flags == 0){
			    			receiveDataDatagram = examineRTPDatagram;
			    			synchronized(this){
			    				notify();
			    			}
			    			// receiveDefaultRTPDatagramBuffer.add(examineRTPDatagram);
			    		}
			    	}
		    	} catch (SocketTimeoutException e){
		    		RTPUtil.debug("ReceiveBufferThread: SocketTimeoutException");
		    		continue;
		    	} catch (Exception e){
		    		e.printStackTrace();
		    	}

			}
    	}

    	public void stopRunning(){
    		keepRunning = false;
    	}
    }


    private class SendThread extends Thread{
    	public boolean keepRunning;

    	public SendThread(){
    		super();
    		RTPUtil.debug("SendThread created.");
    		keepRunning = true;
    	}

    	public void stopRunning(){
    		keepRunning = false;
    	}

    	public void run(){
    		RTPUtil.debug("SendThread: started. keepRunning:" + keepRunning);
    		while (keepRunning){
	    		// RTPUtil.debug("SendThread: Sendbuffer size: " + RTPSocket.this.sendBuffer.size());
    			if (RTPSocket.this.sendBuffer.size() > 0){
    				RTPUtil.debug("SendThread: found something in RTPSocket.this.sendBuffer");
    				RTPDatagram rtpDatagram = RTPSocket.this.sendBuffer.pop();
    				rtpDatagram.sequenceNumber = RTPSocket.this.sequenceNumber;

		    		int counter = 0;
		    		try {

		    			do {
					   		rtpDatagram.updateChecksum();
					   		RTPUtil.debug("SendThread: " + rtpDatagram.toString());
							byte[] datagramArray = rtpDatagram.getByteArray();
							DatagramPacket udpDatagram = new DatagramPacket(datagramArray, datagramArray.length, RTPSocket.this.connectionAddress);
							RTPSocket.this.datagramSocket.send(udpDatagram);
							RTPSocket.this.stopAndWait = true;

				    		if (stopAndWait){
				    			synchronized(RTPSocket.this.receiveThread){
						    		RTPSocket.this.receiveThread.wait(DEFAULT_WAIT_AMOUNT);
						    	}
				    		}

				    		counter++;

			    		} while (counter < DEFAULT_RETRY_AMOUNT && stopAndWait);
				    } catch (Exception e){
				    	e.printStackTrace();
				    }

				    synchronized(this){
					    notify();
					}
				}
	    	}
    	}
    }

    public static int getEphemeralPort(InetAddress address) {
    	return 4000;
  //   	int randomNum;
  //   	Random rand = new Random();
  //   	do {
		//     // NOTE: Usually this should be a field rather than a method
		//     // variable so that it is not re-seeded every call.
		    

		//     // nextInt is normally exclusive of the top value,
		//     // so add 1 to make it inclusive
		//     randomNum = rand.nextInt((65000 - 60000) + 1) + 60000;
		// } while (RTPSocket.isPortInUse(address, randomNum));
	    
	 //    return randomNum;
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
		"\nreceiveSynRTPDatagramBuffer.size(): " + receiveSynRTPDatagramBuffer.size();

    	returnString += "\n================================================================";
    	return returnString;
    }
}