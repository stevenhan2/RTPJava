// TCPClient.java
// A client program implementing TCP socket
import java.net.*; 
import java.io.*; 
import java.util.*;


public class FTAClient { 
	public static void main (String args[]) {// arguments supply message and hostname of destination  
		Socket s = null; 

		boolean terminated = false;
		while(terminated == false){
			Scanner scan = new Scanner(System.in);
			System.out.println("Options: connect-get, disconnect");
			String str1 = scan.nextLine();
			if(str1.equals("connect-get")){
				try{ 
		  			int serverPort = Integer.parseInt(args[0]);
		  			String ip = args[1];

		 //  	if(args.length > 1){
		 //  		ip = args[1];
		 //  	}else{
			// 	ip = "localhost";
			// }
					String data = readFile("example.txt"); //"Hello, How are you?"; 
			  
		 			s = new Socket(ip, serverPort); 
		  			DataInputStream input = new DataInputStream( s.getInputStream()); 
		  			DataOutputStream output = new DataOutputStream( s.getOutputStream()); 
		  
			  //Step 1 send length
			  		System.out.println("Length"+ data.length());
			  		output.writeInt(data.length());
			  //Step 2 send length
			  		System.out.println("Writing.......");
			  		output.writeBytes(data); // UTF is a string encoding
			  
			  //Step 1 read length
			  		int nb = input.readInt();
			  		byte[] digit = new byte[nb];
			  //Step 2 read byte
			  		for(int i = 0; i < nb; i++)
					digit[i] = input.readByte();
		  
			  		String st = new String(digit);
		  			System.out.println("Received: "+ st); 
				}
				catch (UnknownHostException e){ 
					System.out.println("Sock:"+e.getMessage());}
				catch (EOFException e){
					System.out.println("EOF:"+e.getMessage()); }
				catch (IOException e){
					System.out.println("IO:"+e.getMessage());} 
				finally {
			  		if(s!=null) 
				  		try {s.close();
				  	} 
				  	catch (IOException e) {/*close failed*/}
				}
  			}else if(str1.equals("disconnect")){
  				terminated = true;
  			}
  		}
  	}

  	public static String readFile(String fileName) throws IOException {
    	BufferedReader br = new BufferedReader(new FileReader(fileName));
    	try {
       		StringBuilder sb = new StringBuilder();
        	String line = br.readLine();

        	while (line != null) {
            	sb.append(line);
            	sb.append("\n");
            	line = br.readLine();
        	}
        	return sb.toString();
    	} finally {
        	br.close();
    	}
	}
}
