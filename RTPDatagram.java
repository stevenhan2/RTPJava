import java.io.*;
import java.util.zip.CRC32;
import java.lang.*;

public class RTPDatagram implements Comparable<RTPDatagram>{
	// In practice, the PSH, URG, and the urgent data pointer are not used.

	// REMOVED FROM TCP
	// public static final int URG = 0b100000;

	public static final int ACK = 8;

	// REMOVED FROM TCP
	// public static final int PSH = 0b001000;
	public static final int RST = 4;
	public static final int SYN = 2;
	public static final int FIN = 1;

	public int sourcePort;
	public int destPort;

	public long sequenceNumber;
	public long ackNumber;

	// Reserved 4 bits for future purposes (possible flags)
	public int reserved;
	public int flags;

	public int receiveWindowSize;
	public byte[] receiveWindow;

	// CRC32
	public long checksum;

	// REMOVED FROM TCP
	// public int urgentDataPointer;


	public byte[] data;

	public RTPDatagram(int src, int dest, int flags, byte[] receiveWindow, byte[] data){
		this.sourcePort = src;
		this.destPort = dest;
		this.flags = flags;
		this.reserved = 0;

		this.receiveWindow = receiveWindow;
		receiveWindowSize = receiveWindow.length;
		this.data = data;
	}

	public RTPDatagram(byte[] rawData){
		RTPUtil.debug("Creating RTPDatagram from raw bytes");
		RTPUtil.debug("-----------------------------------------------------------------------------");
		int rawDataPointer = 0;

		this.sourcePort = ((RTPUtil.toInt(rawData[rawDataPointer]) << 8) + (RTPUtil.toInt(rawData[rawDataPointer + 1])))  & 0x0000FFFF;


			// ((((int)rawData[rawDataPointer]) << 8)  + ((int)rawData[rawDataPointer + 1])) & 0x0000FFFF;
		rawDataPointer += 2;
		RTPUtil.debug("sourcePort:" + this.sourcePort);

		this.destPort = ((((int)rawData[rawDataPointer]) << 8)  + ((int)rawData[rawDataPointer + 1])) & 0x0000FFFF;
		rawDataPointer += 2;
		RTPUtil.debug("destPort:" + this.destPort);

		this.sequenceNumber = ((RTPUtil.toLong(rawData[rawDataPointer]) << 24) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 1]) << 16) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 2]) << 8) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 3])));
		rawDataPointer += 4;
		RTPUtil.debug("sequenceNumber:" + this.sequenceNumber);

		this.ackNumber = ((RTPUtil.toLong(rawData[rawDataPointer]) << 24) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 1]) << 16) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 2]) << 8) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 3])));
		rawDataPointer += 4;
		RTPUtil.debug("ackNumber:" + this.ackNumber);

		this.reserved = rawData[rawDataPointer] & 0xF;
		this.flags = (rawData[rawDataPointer] >> 4) & 0xF;
		rawDataPointer += 1;
		RTPUtil.debug("reserved:" + this.reserved);
		RTPUtil.debug("flags:" + this.flags);

		this.receiveWindowSize = ((int)rawData[rawDataPointer]) & 0x000000FF;
		rawDataPointer += 1;
		RTPUtil.debug("receiveWindowSize:" + this.receiveWindowSize);

		this.receiveWindow = new byte[this.receiveWindowSize];
		for (int i = 0; i < this.receiveWindowSize; i++){
			this.receiveWindow[i] = rawData[rawDataPointer + i];
		}
		rawDataPointer += this.receiveWindowSize;

		this.checksum = ((RTPUtil.toLong(rawData[rawDataPointer]) << 24) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 1]) << 16) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 2]) << 8) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 3]))) & 0x00000000FFFFFFFFL;
		rawDataPointer += 4;
		RTPUtil.debug("checksum:" + this.checksum);

		data = new byte[rawData.length - rawDataPointer];

		for (int i = 0; i < data.length; i++){
			data[i] = rawData[rawDataPointer + i];
		}
		RTPUtil.debug("-----------------------------------------------------------------------------");
	}

	public ByteArrayOutputStream getHeaderByteArrayNoChecksum(){
		ByteArrayOutputStream bb = new ByteArrayOutputStream();

		RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toShortBytes(sourcePort), bb);
		RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toShortBytes(destPort), bb);
    	
    	RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toIntBytes(sequenceNumber), bb);
    	RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toIntBytes(ackNumber), bb);

    	int reservedAndFlagsByte = (reserved & 0xF) | ((flags & 0xF) << 4);
    	RTPUtil.debug(Integer.toBinaryString(reservedAndFlagsByte));

    	bb.write((reservedAndFlagsByte & 0xFF));
    	bb.write((receiveWindowSize & 0xFF));

    	RTPUtil.writeByteArrayToByteArrayOutputStream(receiveWindow, bb);

    	return bb;
	}

	public boolean checkChecksum(){
		ByteArrayOutputStream bb = getHeaderByteArrayNoChecksum();

    	CRC32 crc = new CRC32();
    	crc.update(bb.toByteArray());
    	crc.update(data);

    	RTPUtil.debug("checking " + this.checksum + " against " + crc.getValue());
    	return new Long(this.checksum).equals(new Long(crc.getValue()));
	}

	public void updateChecksum(){
		ByteArrayOutputStream bb = getHeaderByteArrayNoChecksum();

    	CRC32 crc = new CRC32();
    	crc.update(bb.toByteArray());
    	crc.update(data);

    	this.checksum = crc.getValue();
	}

    public byte[] getByteArray(){
    	ByteArrayOutputStream bb = getHeaderByteArrayNoChecksum();

    	RTPUtil.writeByteArrayToByteArrayOutputStream(RTPUtil.toIntBytes(this.checksum), bb);
    	RTPUtil.writeByteArrayToByteArrayOutputStream(data, bb);

    	return bb.toByteArray();
    }

    public String toString(){
    	String returnString = "--------------------Printing RTPDatagram-----------------------";

    	returnString += 
    	"\nSource port:" + sourcePort +
    	"\nDestination port:" + destPort +
    	"\nSequence number:" + sequenceNumber +
    	"\nAck number:" + ackNumber +
    	"\nflags:" + flags +
    	"\nchecksum:" + checksum + 
    	"\nData:" + new String(data);


    	returnString += "\n----------------------------------------------------------------";
    	return returnString;
    }

	@Override public int compareTo(RTPDatagram datagram) {
		if (this.sourcePort != datagram.sourcePort){
			return new Integer(this.sourcePort).compareTo(new Integer(datagram.sourcePort));
		} else if (this.destPort != datagram.destPort) {
			return new Integer(this.destPort).compareTo(new Integer(datagram.destPort));
		} else if (this.sequenceNumber != datagram.sequenceNumber){
			return new Long(this.sequenceNumber).compareTo(new Long(datagram.sequenceNumber));
		} else {
			return new Long(this.ackNumber).compareTo(new Long(datagram.ackNumber));
		}
	}

	@Override public boolean equals(Object obj) {
		if (!(obj instanceof RTPDatagram))
			return false;
		if (obj == this)
			return true;

		RTPDatagram datagram = (RTPDatagram) obj;

		if (this.sourcePort != datagram.sourcePort){
			return false;
		} else if (this.destPort != datagram.destPort) {
			return false;
		} else if (this.sequenceNumber != datagram.sequenceNumber){
			return false;
		} else if (this.ackNumber != datagram.ackNumber) {
			return false;
		} else {
			return true;
		}
	}
}