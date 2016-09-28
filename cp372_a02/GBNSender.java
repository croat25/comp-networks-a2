/** ------------------------------------------------------------------------
 *  CP372 Networks
 *  Shawn Cramp - 1110007290 - cram7290@mylaurier.ca
 *  Bruno Salapic - 100574460 - sala4460@mylaurier.ca
 *  Assignment Two - Go Back N Sender
 *
 *  GBN_Sender.java
 --------------------------------------------------------------------------- */
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class GBNSender extends Thread {

	public static void main(String[] args) {

		// If 5 Command Line args not included, quit
		if (args.length != 5) {
			System.out.println("Invalid Argument Length, program will now quit.");
			System.exit(0);
		}
		
		// Check Window Size Validity
		if (Integer.parseInt(args[4]) > 128 && Integer.parseInt(args[4]) < 0) {
			System.out.println("Window size must be between 0 and 128");
			System.exit(0);
		}
		
		// Create new Go Back N Sender with arguments
		new GBNSender(args[0], args[1], args[2], args[3], args[4]).start();
	}

	// Define Datagram
	private DatagramSocket socket = null;
	private DatagramPacket sendPacket = null;
	private DatagramPacket recievePacket = null;

	// File Handlers
	private RandomAccessFile fileHandle;
	private byte[] file;
	
	// Local IP
	private InetAddress address;
	
	// Destination Port
	private int destport;
	
	// Window Size
	private int window;
	
	// Send Number
	private int sn = 0;
	
	// Bytes for Constructing Packet
	private byte[] seqNum = new byte[3];
	private byte[] temp = new byte[115];
	private byte[] buf;
	private byte[] ack = new byte[6];
	private byte[] lNum = new byte[3];
	private byte[] endofFile = new byte[3];
	
	// Acknowledgement
	private String ACK;
	private long transmittionTime = 0;
	private boolean sendNext = true, continueSending = true;
	private long start = System.currentTimeMillis();
	private int c = 0, a = 1, next = window, b = 0;
	
	// Last Send Number
	private int l, lastSent = 200;
	private int currentLength = 0;

	boolean eof = false;
	int left;

	// Sender Object
	GBNSender(String ad, String destPort, String sendPort, String filename, String n) {
		try {
			
			// Window Size
			this.window = Integer.parseInt(n);
			
			// File Name
			this.fileHandle = new RandomAccessFile(filename, "r");
			
			// Local IP
			this.address = InetAddress.getByName(ad);
			
			// Sending Port
			this.socket = new DatagramSocket(Integer.parseInt(sendPort));
			
			// Recieving Port
			this.destport = Integer.parseInt(destPort);
			
			this.file = new byte[(int) fileHandle.length()];
			this.l = (int) fileHandle.length();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@Override
	public void run() {
		try {

			// Read File and Close
			fileHandle.readFully(file);
			fileHandle.close();
			
			// Set Send Next to True
			sendNext = true;
			while (continueSending) {

				while (c < next && !eof && sendNext) {
					sendDatagram();

				}
				sendNext = false;
				getPackets();

			}

			// When All Data Sent
			System.out.println("All data sent.");
			transmittionTime = System.currentTimeMillis() - start;
			System.out.println("Total Transmission Time:" + transmittionTime);
			
		} catch (Exception e) {
			System.out.println(e);
			
		} finally {
			try {
				socket.close();

			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	// Read Returning Packet
	private void getPackets() throws Exception {
		socket.setSoTimeout(500);
		
		// While Loop for Window Size
		int count = 0;
		while (count < window && continueSending) {
			
			// Create ACK Packet
			recievePacket = new DatagramPacket(ack, ack.length);

			try {

				// Get ACK Packet from Socket
				socket.receive(recievePacket);
				
				// Set Recieved Packet to ACK
				ACK = new String(recievePacket.getData(), 0, recievePacket.getLength());
				System.out.println(ACK);
				
				// If ACK is Last ACK, stop sending
				if (ACK.endsWith(Integer.toString(lastSent))) {
					System.out.println("Continue Sending Set to False");
					continueSending = false;
				}
					
				// If not End of Transmission, send datagram
				if (!eof) {
					sendDatagram();
				}
					
				// Set Timeout
				socket.setSoTimeout(500);

			} catch (SocketTimeoutException e) {
				System.out.println("Read and Parse Exception");
				
				sendNext = true;
				c = (128 * b) + sn;
				next = c + window;
				currentLength = c * 115;
				break;
			}

			count++;
		}
	}

	// Create and Send Datagram
	private void sendDatagram() throws Exception {

		temp = new byte[115];
		seqNum = new byte[3];
		
		// Create Sequence Number
		int conver = c % 128;
		
		// Construct Sequence Number with Correct Sigdigs
		if (conver < 10) {
			seqNum = ("00" + Integer.toString(conver)).getBytes();
		} else if (conver < 100 && conver > 9) {
			seqNum = ("0" + Integer.toString(conver)).getBytes();
		} else {
			seqNum = Integer.toString(conver).getBytes();
		}
		
		// Remaining Length of File
		left = l - currentLength;

		// If Remaining length of file < 115
		if (left < 115) {
			
			// Set Last Sequence Number to c % 128
			lastSent = conver;
			endofFile = "EOF".getBytes();
			
			// Construct Last Number with Correct Sigdigs
			if (left < 10) {
				lNum = ("00" + Integer.toString(left)).getBytes();
			} else if (left < 100 && left > 9) {
				lNum = ("0" + Integer.toString(left)).getBytes();
			} else {
				lNum = Integer.toString(left).getBytes();
			}
			
			// Copy Array
			System.arraycopy(file, 115 * c, temp, 0, left);
			buf = new byte[temp.length + seqNum.length + lNum.length + endofFile.length];
			System.arraycopy(temp, 0, buf, 0, temp.length);
			System.arraycopy(seqNum, 0, buf, temp.length, seqNum.length);
			System.arraycopy(lNum, 0, buf, temp.length + seqNum.length, lNum.length);
			System.arraycopy(endofFile, 0, buf, temp.length + seqNum.length + lNum.length, endofFile.length);
			eof = true;
			
		} else {
			System.arraycopy(file, 115 * c, temp, 0, temp.length);
			buf = new byte[temp.length + seqNum.length];
			System.arraycopy(temp, 0, buf, 0, temp.length);
			System.arraycopy(seqNum, 0, buf, temp.length, seqNum.length);
		}

		// Send Constructed Packet
		sendPacket = new DatagramPacket(buf, buf.length, address, destport);
		socket.send(sendPacket);

		// Increment count and increase current length of sent file parts by 115
		c++;
		currentLength += 115;
	}

}
