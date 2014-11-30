// TCPServer.java
// A server program implementing TCP socket
import java.net.*; 
import java.io.*;
import java.util.*;


public class FTAServer { 
  	public static void main (String args[]){ 
		try{ 
			int serverPort = Integer.parseInt(args[0]);
			ServerSocket listenSocket = new ServerSocket(serverPort); 
			boolean terminated = false;
			System.out.println("server start listening at port " + serverPort + "... ... ...");

			while(true){
				Socket clientSocket = listenSocket.accept(); 
				ConnectionThread c = new ConnectionThread(clientSocket);	
			}	
		} catch(IOException e) {
			System.out.println("Listen :"+e.getMessage());
		} 
	}

}

class ConnectionThread extends Thread { 
	DataInputStream input; 
	DataOutputStream output; 
	Socket clientSocket; 
	
	public ConnectionThread (Socket aClientSocket) { 
		try { 
					clientSocket = aClientSocket; 
					input = new DataInputStream( clientSocket.getInputStream()); 
					output =new DataOutputStream( clientSocket.getOutputStream()); 
					this.start(); 
		} 
			catch(IOException e) {
			System.out.println("ConnectionThread:"+e.getMessage());
			} 
	 	 } 

	  public void run() { 
		try { // an echo server 
		  //  String data = input.readUTF();
				
			  FileWriter out = new FileWriter("test.txt");
			  BufferedWriter bufWriter = new BufferedWriter(out);
		   
			  //Step 1 read length
			  int nb = input.readInt();
			  System.out.println("Read Length"+ nb);
			  byte[] digit = new byte[nb];
			  //Step 2 read byte
			   System.out.println("Writing.......");
			  for(int i = 0; i < nb; i++)
				digit[i] = input.readByte();
			  
			   String st = new String(digit);
			  bufWriter.append(st);
			   bufWriter.close();
				System.out.println ("receive from : " + 
				clientSocket.getInetAddress() + ":" +
				clientSocket.getPort() + " message - " + st);
			  
			  //Step 1 send length
			  output.writeInt(st.length());
			  //Step 2 send length
			  output.writeBytes(st); // UTF is a string encoding
		  //  output.writeUTF(data); 
			} 
			catch(EOFException e) {
			System.out.println("EOF:"+e.getMessage()); } 
			catch(IOException e) {
			System.out.println("IO:"+e.getMessage());}  
   
			finally { 
			  try { 
				  clientSocket.close();
			  }
			  catch (IOException e){/*close failed*/}
			}
		}
}

