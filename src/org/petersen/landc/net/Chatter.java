package org.petersen.landc.net;

import java.awt.Color;
import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.channels.IllegalBlockingModeException;

import javax.swing.JOptionPane;

import org.petersen.landc.PacketType;
import org.petersen.landc.exceptions.TrafficException;
import org.petersen.landc.ui.ChatWindow;

/**
 * @author Claus Petersen
 * 
 * Class to handle the multicast traffic for the chat
 */
public final class Chatter extends Thread {

	// the length we send our packets in
	private final short MAX_PACKET_LENGTH = 64;

	// actual space for known data in packets
	private final short DATA_LENGTH = 59;

	// the socket for data to all chats running
	private MulticastSocket multiSocket;

	// this is for reference to our controls
	private ChatWindow window;

	// the multicast address to use
	private InetAddress multiAddress;

	// the port to use
	private short thePort;

	/**
	 * the main constructor thats throws possible exceptions of the methods and
	 * constructor in the java.net
	 * 
	 * @param window
	 * @param address
	 * @param port
	 * 
	 */
	public Chatter(ChatWindow window) throws TrafficException {

		this.window = window;
		multiAddress = null;
		thePort = 0;

	}

	public void setAddress(InetAddress address) throws TrafficException {

		multiAddress = address;

		try {
			// initialize the sockets with necessary
			multiSocket = new MulticastSocket(thePort);
			multiSocket.setSoTimeout(4000);
			multiSocket.setBroadcast(true);
			multiSocket.joinGroup(multiAddress);
		} catch (SocketException e) {
			throw new TrafficException("Socket config error: " + e.getLocalizedMessage());
		} catch (IOException e) {
			throw new TrafficException("Failed to assign internally: "
					+ e.getMessage());
		}
	}

	public void setPort(short port) {

		thePort = port;
	}

	// for starting the chatter
	public void enable() {
		if (!isAlive())
			start();
	}

	// this is thread specific and is called when the thread is started
	public void run() {

		PacketType type;
		AtomicReference<String> text = new AtomicReference<String>(null);

		try {

			sendPacket(PacketType.ARE_YOU_THERE, null);

			while (!isInterrupted()) {
				
				// blocking call
				type = receivePacket(text);

				switch (type) {
				case ARE_YOU_THERE: {
					sendPacket(PacketType.HERES_MY_NAME, window.getName());
				}
					break;
				case HERES_MY_NAME: {
					window.addName(text);
				}
					break;
				case LEAVE_CHAT: {
					window.removeName(text.get());
				}
					break;
				case HERES_SOME_TEXT: {
					
					/**
					 * FUTURE: Possible bug of messages not received in sequential order,
					 * (the UDP protocol does not guarantee arrival)
					 * this will theoretically happen in higher network traffic
					 * Calls for a message assembler
					 * (maybe a thread pool to handle the incoming messages)
					 * that can sort.
					 */
					
					StringTokenizer tokenizer = new StringTokenizer(text.get(),
							"|");

					window.addText(tokenizer.nextToken(),
							tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()),
							Integer.parseInt(tokenizer.nextToken()), new Color(
									Integer.parseInt(tokenizer.nextToken())));
				}
					break;
				case REQUEST_NAME: {
					if (window.isNameTaken(text)) {
						sendPacket(PacketType.NAME_TAKEN, null);
					} else {
					} // just continue
				}
					break;
				default:
					break;
				}
			}
		} catch (TrafficException e) {

			if (e.getSystemMessage()) {
				JOptionPane.showMessageDialog(window, e.getLocalizedMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				window.dispose();
			} else {
				JOptionPane.showMessageDialog(window, e.getLocalizedMessage(),
						"Warning", JOptionPane.WARNING_MESSAGE);
			}
		} finally {
			
			// close the multicast socket
			if (multiSocket != null && multiSocket.isClosed()) {

				multiSocket.close();
			}
		}
	}

	public String getAddress() {
		return multiAddress.getHostAddress();
	}

	public int getPort() {
		return thePort;
	}

	// the method we use to send UDP packets to other clients
	public void sendPacket(PacketType type, String data)
			throws TrafficException {

		/**
		 ******************************** 
		 * Packet Structure (with data) *
		 ******************************** 
		 * 
		 * 		byte 		byte 	   byte		 byte		bytes	  byte (value -1 and -2, true and false)
		 * ---------------------------------------------------------------------
		 * | Packet Type | Packet ID | Index | Data Length | Data | Last Index |
		 * ---------------------------------------------------------------------
		 * 
		 ******************** 
		 * Packet Structure *
		 ******************** 
		 * byte (without data)
		 * ---------------
		 * | Packet Type |
		 * ---------------
		 * 
		 ******************************** 
		 * Chat Text Packet Data Field *
		 ******************************** 
		 * 
		 * Data is separated by the pipe character
		 * 
		 * 	  String   String Integer  Integer   Integer (RGB value)
		 * ----------------------------------------------
		 * | Nickname | Text | Font | Font Size | Color |
		 * ----------------------------------------------
		 * 
		 */

		// our user diagram packet class
		DatagramPacket packet = null;

		// for random packet chain numbers
		Random rnd = new Random(new Random().nextInt(1000));

		// a packet id for the packet chain
		byte packetID = (byte) rnd.nextInt(Byte.MAX_VALUE);

		// how many packets we split the data in
		double packets;

		// the divided data
		byte[][] bytesToSend = null;

		// the packet type in bytes
		byte packetByte = type.ID();

		// last packet data length
		int lastPacketLength = 0;

		// the data in bytes
		byte[] dataBytes = null;

		// is there actually any data
		if (data != null) {

			dataBytes = data.getBytes();
			packets = Math.ceil((double) dataBytes.length / DATA_LENGTH);

			if (packets <= 1) {
				lastPacketLength = dataBytes.length;
				bytesToSend = new byte[1][];
				packets = 1;
			} else {
				bytesToSend = new byte[(int) packets][];
				lastPacketLength = dataBytes.length % DATA_LENGTH;
			}
		} else {

			packets = 1;
			bytesToSend = new byte[1][];
		}

		// just counter variables
		short i;
		int l, dataLength = 0;

		// TRICKY: this is where the data is split into the packet structure
		for (i = 0; i < bytesToSend.length; i++) {

			bytesToSend[i] = new byte[MAX_PACKET_LENGTH];

			bytesToSend[i][0] = packetByte; // type

			if (data != null) {

				bytesToSend[i][1] = packetID; // packet id

				bytesToSend[i][2] = (byte) i; // index

				if (i == packets - 1) {
					bytesToSend[i][3] = (byte) lastPacketLength; // data length
				} else {
					bytesToSend[i][3] = (byte) DATA_LENGTH; // data length
				}

				for (l = 4; l < MAX_PACKET_LENGTH - 1; l++) { // data

					if (dataLength >= dataBytes.length) {
						break;
					} else {
					} // just continue

					bytesToSend[i][l] = dataBytes[dataLength];
					dataLength++;
				}

				if (i == packets - 1) { // last index
					bytesToSend[i][l] = -1;
				} else {
					bytesToSend[i][MAX_PACKET_LENGTH - 1] = -2;
				}
			} else
				; // just continue
		}

		for (byte[] ba : bytesToSend) {

			// construct a packet
			packet = new DatagramPacket(ba, 0, ba.length, multiAddress, thePort);

			// send data here and handle exceptions
			try {
				multiSocket.send(packet);
			} catch (SecurityException e) {
				throw new TrafficException("Security: " + e.getLocalizedMessage(), true);
			} catch (PortUnreachableException e) {
				throw new TrafficException("Port Unreachable: "
						+ e.getLocalizedMessage(), true);
			} catch (IllegalBlockingModeException e) {
				throw new TrafficException("Illegal Blocking: "
						+ e.getLocalizedMessage(), true);
			} catch (IOException e) {
				throw new TrafficException(e.getLocalizedMessage(), true);
			}
		}
	}

	// the method we use to receive UDP packets from other clients
	public PacketType receivePacket(AtomicReference<String> data)
			throws TrafficException {

		PacketType type;
		byte[] recvBytes = new byte[MAX_PACKET_LENGTH];
		DatagramPacket packet = new DatagramPacket(recvBytes, recvBytes.length);

		// string should be nullified (we receive)
		if (data.get() != null) {
			data.set(null);
		} else; // just continue

		// receive data here and handle exceptions
		try {
			multiSocket.receive(packet);
		} catch (SocketTimeoutException e) {
			return PacketType.NONE;
		} catch (SecurityException e) {
			throw new TrafficException("Security: " + e.getLocalizedMessage(), true);
		} catch (PortUnreachableException e) {
			throw new TrafficException("Port Unreachable: " + e.getLocalizedMessage(),
					true);
		} catch (IllegalBlockingModeException e) {
			throw new TrafficException("Illegal Blocking: " + e.getLocalizedMessage(),
					true);
		} catch (IOException e) {
			throw new TrafficException(e.getLocalizedMessage(), true);
		}

		type = PacketType.getType(recvBytes[0]);

		if (type != PacketType.ARE_YOU_THERE && type != PacketType.NAME_TAKEN
				&& type != PacketType.NONE) {

			String packetData = new String();
			byte packetID = recvBytes[1];
			short index = recvBytes[2];

			packetData = new String(recvBytes, 4, recvBytes[3]);
			index++;

			while (recvBytes[recvBytes[3] + 4] == -2) {

				// receive data here and handle exceptions
				try {
					multiSocket.receive(packet);
				} catch (SocketTimeoutException e) {
					throw new TrafficException("Timeout: " + e.getLocalizedMessage(),
							true);
				} catch (SecurityException e) {
					throw new TrafficException("Security: " + e.getLocalizedMessage(),
							false);
				} catch (PortUnreachableException e) {
					throw new TrafficException("Port Unreachable: "
							+ e.getLocalizedMessage(), false);
				} catch (IllegalBlockingModeException e) {
					throw new TrafficException("Illegal Blocking: "
							+ e.getLocalizedMessage(), false);
				} catch (IOException e) {
					throw new TrafficException(e.getLocalizedMessage(), false);
				}

				// equals packetID and next index
				if (Byte.compare(recvBytes[1], packetID) == 0
						&& recvBytes[2] == index) {
					packetData += new String(recvBytes, 4, recvBytes[3]);
					index++;
				} else
					; // just continue
			}

			data.set(packetData);
		} else
			; // just continue

		return type;
	}
}
