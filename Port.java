import java.nio.ByteBuffer;

public static final int NUMBER_OF_PORTS = 65535;

public class Port {
	private int portNumber;
    public Port(int portNumberParam){
    	portNumber = portNumberParam % (NUMBER_OF_PORTS + 1)
    }

    public port(byte higherByte, byte lowerByte){
        portNumber = ((((int)higherByte) << 8)  + ((int)lowerByte)) & 0x0000FFFF;
    }

    public int getInt(){
    	return portNumber;
    }

    public ByteBuffer getByteBuffer(){
    	ByteBuffer toReturn = ByteBuffer.allocate(2);
    	toReturn.put((byte) (portNumber >> 8) & 0xFF);
        toReturn.put((byte) (portNumber) & 0xFF);
    	return toReturn;
    }
}