import java.nio.ByteBuffer;
import java.util.zip.CRC32;

// In practice, the PSH, URG, and the urgent data pointer are not used.

// REMOVED FROM TCP
// public static final int URG = 0b100000;

public static final int ACK = 0b1000;

// REMOVED FROM TCP
// public static final int PSH = 0b001000;
public static final int RST = 0b0100;
public static final int SYN = 0b0010;
public static final int FIN = 0b0001;

public class RTPDatagram {
	public Port sourcePort;
	public Port destPort;

	public long sequenceNumber;
	public long ackNumber;

	public int headerLength;
	public int flags

	public int receiveWindowSize;
	public ByteBuffer receiveWindow;

	// CRC32
	public long checksum;

	// REMOVED FROM TCP
	// public int urgentDataPointer;


	ByteBuffer data;

	public RTPDatagram(Port src, Port dest, int flags, ByteBuffer receiveWindow, ByteBuffer data){
		this.sourcePort = src;
		this.destPort = dest;
		this.flags = flags;

		this.receiveWindow = receiveWindow;
		receiveWindowSize = (int) (Math.log((double) receiveWindow.array().length())/Math.log(2.0) + 1);
	}

	public RTPDatagram(int src, int dest, int flags, ByteBuffer receiveWindow, ByteBuffer data){
		this.sourcePort = new Port(src);
		this.destPort = new Port(dest);
		this.flags = flags;

		this.receiveWindow = receiveWindow;
		receiveWindowSize = (int) (Math.log((double) receiveWindow.array().length())/Math.log(2.0) + 1);
	}

	public RTPDatagram(byte[] rawData){
		int rawDataPointer = 0;

		this.sourcePort = new Port(rawData[rawDataPointer], rawData[rawDataPointer + 1]);
		rawDataPointer += 2;

		this.destPort = new Port(rawData[rawDataPointer], rawData[rawDataPointer + 1]);
		rawDataPointer += 2;

		this.sequenceNumber = (((long)rawData[rawDataPointer]) << 24) + 
		(((long)rawData[rawDataPointer + 1]) << 16) + 
		(((long)rawData[rawDataPointer + 2]) << 8) + 
		(((long)rawData[rawDataPointer + 3]))) & 0x00000000FFFFFFFFL;
		rawDataPointer += 4;

		this.ackNumber = (((long)rawData[rawDataPointer]) << 24) + 
		(((long)rawData[rawDataPointer + 1]) << 16) + 
		(((long)rawData[rawDataPointer + 2]) << 8) + 
		(((long)rawData[rawDataPointer + 3]))) & 0x00000000FFFFFFFFL;
		rawDataPointer += 4;

		this.headerLength = rawData[rawDataPointer] & 0xF;
		this.flags = (rawData[rawDataPointer] >> 4) & 0xF;
		rawDataPointer += 1;

		this.receiveWindowSize = (rawData[rawDataPointer] << 8) + ((int)rawData[rawDataPointer + 1]) & 0x0000FFFF;

		byte[] receiveWindowByteArray = new byte[this.receiveWindowSize];
		for (int i = 0; i < this.receiveWindowSize; i++){
			receiveWindowByteArray[i] = rawData[rawDataPointer + i];
		}
		rawDataPointer += this.receiveWindowSize;
		receiveWindow = new ByteBuffer(receiveWindowByteArray);

		this.checksum = (((long)rawData[rawDataPointer]) << 24) + 
		(((long)rawData[rawDataPointer + 1]) << 16) + 
		(((long)rawData[rawDataPointer + 2]) << 8) + 
		(((long)rawData[rawDataPointer + 3]))) & 0x00000000FFFFFFFFL;
	}

	public ByteBuffer getHeaderByteBufferNoChecksum(){
		ByteBuffer bb = new ByteBuffer();
    	bb.put(sourcePort.getByteBuffer());
    	bb.put(destPort.getByteBuffer());

    	bb.putInt((int) sequenceNumber);
    	bb.putInt((int) ackNumber);

    	int headerLengthAndFlagsByte = (headerLength & 0xF) | ((flags & 0xF) << 4)
    	RTPUtil.debug(Integer.toBinaryString(headerLengthAndFlagsByte));

    	bb.putByte((byte) (headerLengthAndFlagsByte && 0xFF));

    	bb.putByte((byte) (receiveWindowSize && 0xFF));

    	bb.put(receiveWindow);
    	return bb;
	}

	public boolean checkChecksum(){
		ByteBuffer bb = getHeaderByteBufferNoChecksum();

    	CRC32 crc = new CRC32();
    	crc.update(bb.array());
    	crc.update(data.array());
    	return (this.checksum == crc.getValue());
	}

    public ByteBuffer getByteBuffer(){
    	ByteBuffer bb = getHeaderByteBufferNoChecksum();

    	CRC32 crc = new CRC32();
    	crc.update(bb.array());
    	crc.update(data.array());
    	this.checksum = crc.getValue();

    	bb.putInt((int)(this.checksum & 0x00000000FFFFFFFFL));
    	bb.put(data);
    	return bb;
    }
}