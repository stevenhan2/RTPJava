import java.io.*;
import java.util.zip.CRC32;


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


	byte[] data;

	public RTPDatagram(int src, int dest, int flags, byte[] receiveWindow, byte[] data){
		this.sourcePort = src;
		this.destPort = dest;
		this.flags = flags;
		this.reserved = 0;

		this.receiveWindow = receiveWindow;
		receiveWindowSize = receiveWindow.length;
		RTPUtil.debug("receiveWindowSize:" + receiveWindowSize);
		this.data = data;
	}

	public RTPDatagram(byte[] rawData){
		RTPUtil.debug("Creating RTPDatagram from raw bytes");
		int rawDataPointer = 0;

		this.sourcePort = ((((int)rawData[rawDataPointer]) << 8)  + ((int)rawData[rawDataPointer + 1])) & 0x0000FFFF;
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

		this.checksum = (RTPUtil.toLong(rawData[rawDataPointer]) << 24) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 1]) << 16) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 2]) << 8) + 
		(RTPUtil.toLong(rawData[rawDataPointer + 3])) & 0x00000000FFFFFFFFL;
		rawDataPointer += 4;

		data = new byte[rawData.length - rawDataPointer];

		for (int i = 0; i < data.length; i++){
			data[i] = rawData[rawDataPointer + i];
		}
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
    	return (this.checksum == crc.getValue());
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
    	String returnString = "";

    	returnString += 

    	"Source port:" + sourcePort +
    	"\nDestination port:" + destPort +
    	"\nSequence number:" + sequenceNumber +
    	"\nack number:" + ackNumber +
    	"\nData:" + new String(data);


    	return returnString;
    }

	@Override public int compareTo(RTPDatagram datagram) {
		return ("" + this.sourcePort + this.destPort + this.sequenceNumber).compareTo("" + datagram.sourcePort + datagram.destPort + datagram.sequenceNumber);
	}
}