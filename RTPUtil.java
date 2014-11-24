import java.io.*;
public class RTPUtil {
	public static final boolean DEBUG = false;
	
	// Byte utilities
	// --------------------------------------------------------

	public static int toInt(byte b){
		return ((int)b) & 0xFF;
	}

	public static long toLong(byte b){
		return ((int)b) & 0xFFL;	
	}

	// Don't think this is needed
	// public static byte[] toBytes(int i)
	// {
	// 	byte[] result = new byte[4];

	// 	result[0] = (byte) (i >> 24);
	// 	result[1] = (byte) (i >> 16);
	// 	result[2] = (byte) (i >> 8);
	// 	result[3] = (byte) (i /*>> 0*/);

	// 	return result;
	// }

	public static byte[] toShortBytes(int i)
	{
		byte[] result = new byte[2];

		result[0] = (byte) (i >> 8);
		result[1] = (byte) (i /*>> 0*/);

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

	public static int compare(byte[] x, byte []){
		// TIM DO THIS
		// Watch out for "signed" vs "unsigned"

		// Return 0 if they're the same
		// Return 1 if x is bigger
		// Return -1 if y is bigger

		// x and y not necessarily the same length
		// Take into account things like x = 00000000 00000001, y = 000000010
		return 0;
	}

	public static byte[] add(byte[] x, byte[] y){
		// TIM DO THIS
		// Watch out for "signed" vs "unsigned"
		// x and y should both be unsigned


		// return sum of x and y in new byte array
		return null;
	}

	public static byte[] subtract(byte[] x, byte[] y){
		// TIM DO THIS
		// Same as previous
		// x and y should both be unsigned

		return null;
	}


	// Printing utilities
	// --------------------------------------------------------

	public static String printByte(byte b1){
		return String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
	}
	
	public static String printByteArray(byte[] b, int tab){
		String returnString = "";

		for (int i = 0; i < b.length; i++){
			if (i % tab == 0 && i != 0){
				returnString += "\n";
			}
			returnString += printByte(b[i]) + " ";

		}
		return returnString;
	}

	public static void debug(String s){
		if (DEBUG){
			System.out.println(s);
		}
	}

	// ByteArrayOutputStream utilities
	// --------------------------------------------------------

	public static void writeByteArrayToByteArrayOutputStream(byte[] bytes, ByteArrayOutputStream bb){
		for (int i = 0; i < bytes.length; i++){
			bb.write(bytes[i]);
		}
	}

}