public class RTPTest {
	
	public static void test(){
		

		int srcPort = 8000;
		int destPort = 8001;
		int flags = 8;
		byte[] receiveWindow = RTPUtil.toBytes(696969);
		byte[] data = "Hello World!".getBytes();
		

		RTPDatagram datagram1 = new RTPDatagram(srcPort, destPort, flags, receiveWindow, data);
		datagram1.sequenceNumber = 500;
		datagram1.ackNumber = 23;
		datagram1.updateChecksum();




		byte[] datagram1Bytes = datagram1.getByteArray();
		long checksum1 = datagram1.checksum;

		
		System.out.println(RTPUtil.printByteArray(datagram1Bytes, 4));
		

		RTPDatagram datagram2 = new RTPDatagram(datagram1Bytes);	
		long checksum2 = datagram1.checksum;

		byte[] datagram2Bytes = datagram2.getByteArray();
		
		System.out.println(RTPUtil.printByteArray(datagram2Bytes, 4));
		
		System.out.println(datagram2.checksum);
		System.out.println(datagram1.checksum);

		System.out.println(datagram2);

		// System.out.println(datagram1);
		// System.out.println(datagram2);

		if(datagram2.checkChecksum()){
			System.out.println("checksum works");
		}
		else{
			System.out.println("checksum does not work");
		}

		
	}

}