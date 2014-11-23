public class RTPTest {
	
	public static void test(){
		byte b = (byte) -4;
		System.out.println(((int)b) & 0xFF);
		
		int x = 0x0000000000000000000000000000001;
		System.out.println(x);
	}

}