public class RTPTest {
	
	public static void test(){
		byte b = (byte) -4;
		System.out.println(((int)b) & 0xFF);
		
		int x = 0x0000000000000000000000000000001;
		System.out.println(x);

		int srcPort = 8000;
		int destPort = 8001;
		int flags = 8;
		byte[] receiveWindow = (10000000);
		byte[] data = "Hello World!".getBytes();
		RTPDatagram datagram = new RTPDatagram(srcPort, destPort, flags, receiveWindow, data);

		if(datagram.checkChecksum()){
			System.out.println("checksum works");
		}
		else{
			System.out.println("checksum does not work");
		}

		
	}

}