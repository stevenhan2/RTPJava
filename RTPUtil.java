import java.io.*;
public class RTPUtil {
	public static final boolean DEBUG = true;
	
	public static void debug(String s){
		if (DEBUG){
			System.out.println(s);
		}
	}
	
	public static int toInt(byte b){
		return ((int)b) & 0xFF;
	}

	public static byte[] toBytes(int i)
	{
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i /*>> 0*/);

		return result;
	}

	public static byte[] toIntBytes(long i)
	{
		byte[] result = new byte[4];

		result[0] = (byte) ((i >> 24) & 0x00000000FFFFFFFFL);
		result[1] = (byte) ((i >> 16) & 0x00000000FFFFFFFFL);
		result[2] = (byte) ((i >> 8) & 0x00000000FFFFFFFFL);
		result[3] = (byte) ((i /*>> 0*/) & 0x00000000FFFFFFFFL);

		return result;
	}


	public static void writeByteArrayToByteArrayOutputStream(byte[] bytes, ByteArrayOutputStream bb){
		for (int i = 0; i < bytes.length; i++){
			bb.write(bytes[i]);
		}
	}

}