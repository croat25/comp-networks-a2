/** ------------------------------------------------------------------------
 *  CP372 Networks
 *  Shawn Cramp - 1110007290 - cram7290@mylaurier.ca
 *  Bruno Salapic - 100574460 - sala4460@mylaurier.ca
 *  Assignment Two - Go Back N Reciever
 *
 *  GBN_Receiver.java
 --------------------------------------------------------------------------- */

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class GBNReceiver {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 5) {
			System.exit(0);
		}

		// Datagram Declarations
		DatagramSocket socket = null;
		DatagramPacket sendPacket = null;
		DatagramPacket recievePacket = null;
		
		// Reliability Number and Count
		int reliability, count = 1;
		
		// Sequence Array Declarations
		int seqNum = 0;
		String datagram;
		int sizeLeft;
		byte[] data = new byte[124];
		byte[] fdata = new byte[115];
		byte[] ack = new byte[6];
		byte[] lNum = new byte[3];
		byte[] las = new byte[3];
		
		try {
			
			// Declare LocalIP, Recieving Port, Sending Port, File and Reliability Number
			InetAddress address = InetAddress.getByName(args[0]);
			socket = new DatagramSocket(Integer.parseInt(args[2]));
			RandomAccessFile f = new RandomAccessFile(args[3], "rw");
			reliability = Integer.parseInt(args[4]);
			System.out.println("Waiting...\n");
			
			while (true) {

				// Create New Recieving Packet
				recievePacket = new DatagramPacket(data, data.length);
				
				// Recieve Packet from Socket
				socket.receive(recievePacket);
				
				// Assign Packet data to dat
				datagram = new String(recievePacket.getData(), 0, recievePacket.getLength());
				System.arraycopy(data, 115, las, 0, las.length);
				
				// If Data Ends with End of File
				if (datagram.endsWith("EOF") && Integer.parseInt(new String(las, "UTF-8")) == seqNum) {
					
					// Sanity Check, End of File
					System.out.println("End of Transmission");
					
					// Copy data into lNum
					System.arraycopy(data, 118, lNum, 0, lNum.length);
					sizeLeft = Integer.parseInt(new String(lNum, "UTF-8"));
					System.out.println(Integer.toString(sizeLeft));
					
					// Copy copy data into file data
					fdata = new byte[sizeLeft];
					System.arraycopy(data, 0, fdata, 0, sizeLeft);
					
					// Write to File
					f.write(fdata);
					
					//
					ack = ("ACK" + Integer.toString(seqNum)).getBytes();
					sendPacket = new DatagramPacket(ack, ack.length, address, Integer.parseInt(args[1]));
					socket.send(sendPacket);
					break;
					
				// If Data Ends with a Sequence Number
				} else if (datagram.endsWith(Integer.toString(seqNum))) {
					ack = ("ACK" + Integer.toString(seqNum)).getBytes();
					datagram = datagram.substring(0, datagram.length() - Integer.toString(seqNum).length());
					fdata = new byte[115];
					System.arraycopy(data, 0, fdata, 0, fdata.length);
					
					seqNum++;
					seqNum = seqNum % 128;
					
					if (count < reliability || reliability == 0) {
						System.out.println("-----------");
						System.out.print("Sending Datagram: \n");
						System.out.println(datagram);
						System.out.println("-----------");
						
						f.write(fdata);
						sendPacket = new DatagramPacket(ack, ack.length, address, Integer.parseInt(args[1]));
						socket.send(sendPacket);
						count++;
						
					} else {
						System.out.println("\n\n--- Dropping Datagram ---\n");
						count = 1;
					}
					
				} else {
					ack = ("ACK" + Integer.toString(seqNum - 1)).getBytes();
					
					sendPacket = new DatagramPacket(ack, ack.length, address, Integer.parseInt(args[1]));
					socket.send(sendPacket);
				}

			}
			f.close();
			System.out.println("Recieved All Data");

		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				socket.close();

			} catch (Exception e) {
			}
		}
	}
}