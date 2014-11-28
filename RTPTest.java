import java.net.*;
public class RTPTest {

	public static void testRTPSocket(){
		RTPUtil.debug("Beginning RTPSocket tests");
		// RTPSocket s1 = new RTPSocket();

		RTPUtil.debug("Going to try to connect to 7080 from 8070");
		RTPSocket s2 = new RTPSocket(new InetSocketAddress("localhost", 7080));
		s2.bind(new InetSocketAddress("localhost", 8070));
		RTPUtil.debug("Listening");
		s2.listen();
	}

	public static void testRTPUtilByteOperations(){
		System.out.println("Hello World");
		int a = 255;
		int b = 2387058;
		byte[] x = RTPUtil.toBytes(a);
		byte[] y = RTPUtil.toBytes(b);
		System.out.println("x :" + RTPUtil.printByteArray(x, 4));
		System.out.println("y :" + RTPUtil.printByteArray(y, 4));

		// if(RTPUtil.compare(x, y) == 1 && (a > b) || 
		// 	RTPUtil.compare(x, y) == 0 && (a == b) || 
		// 	RTPUtil.compare(x, y) == -1 && (a < b)){

		// 	System.out.println("Compare Test: PASS");
		// }else{
		// 	System.out.println("Compare Test: FAIL");
		// }


		byte[] z = RTPUtil.add(x,y);

		//System.out.println("z :" + RTPUtil.printByteArray(z, 4));


		if(RTPUtil.compare(z, RTPUtil.toBytes(a + b)) == 0 ){
			System.out.println("Add Test: PASS");
		}else{
			System.out.println("Add Test: FAIL");
		}

		z = RTPUtil.subtract(x,y);


		//System.out.println("z :" + RTPUtil.printByteArray(z, 4));

		if(RTPUtil.compare(z, RTPUtil.toBytes(a - b)) == 0 ){
			System.out.println("Subtract Test: PASS");
		}else{
			System.out.println("Subtract Test: FAIL");
		}
	}
	
	public static void testRTPDatagram(){
		int srcPort = 8000;
		int destPort = 8001;
		int flags = 8;
		byte[] receiveWindow = RTPUtil.toBytes(696969);
		byte[] data = "Hello World!".getBytes();

		RTPDatagram datagram1 = new RTPDatagram(srcPort, destPort, flags, receiveWindow, data);
		datagram1.sequenceNumber = 500;
		datagram1.ackNumber = 23;
		datagram1.updateChecksum();

		System.out.println(datagram1.toString());

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

		if(datagram2.checkChecksum()){
			System.out.println("checksum works");
		}
		else{
			System.out.println("checksum does not work");
		}

		RTPDatagram datagram3 = new RTPDatagram(srcPort, destPort, 4, receiveWindow, data);
		RTPDatagram datagram4 = new RTPDatagram(srcPort, destPort, 2, receiveWindow, data);
		RTPDatagram datagram5 = new RTPDatagram(srcPort, destPort, 1, receiveWindow, data);
		RTPDatagram datagram6 = new RTPDatagram(srcPort, destPort, 3, receiveWindow, data);
	
	}

}