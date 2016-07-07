/*
 * A connection class of TTP
 * References:
 * Computer Networking: A top-down approach, Figure 3.20, Figure 3.21
 */

package services;

import java.io.IOException;
import java.net.SocketException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import datatypes.Datagram;
import datatypes.DatagramUtils;
import datatypes.MyProtocol;
import datatypes.TTPPacket;
import datatypes.TTPReceiver;
import datatypes.TTPSegment;
import datatypes.WindowSender;

public class TTPConnection {
	private static final int MAX_SEG_SIZE = 1024;
	private short srcPort;
	private short dstPort;
	private String srcAddr;
	private String dstAddr;
	private DatagramService ds;
	private int errno = 0;
	private int TIMEOUT;
	private AtomicBoolean isClosed;

	//receiver
	private TTPReceiver ttpReceiver;
	
	//sender
	private WindowSender ttpSender;

	private int nextSeq;
	

	public TTPConnection(short srcPort, short dstPort, String srcAddr, String dstAddr, int timeoutInterval,
			DatagramService ds, int windowSize) throws SocketException {
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.srcAddr = srcAddr;
		this.dstAddr = dstAddr;
		this.ds = ds;
		this.TIMEOUT = timeoutInterval;
		this.isClosed = new AtomicBoolean(false);
		this.ttpReceiver = new TTPReceiver(srcPort, dstPort, srcAddr, dstAddr, ds);
		this.ttpSender = new WindowSender(srcPort, dstPort, srcAddr, dstAddr, ds, windowSize);
		this.nextSeq = 0;
	}
	
	/*
	 * set error number
	 */

	public void setErrno(int errno) {
		this.errno = errno;
	}

	
	/*
	 * return true if the sender has finished all sending
	 */
	public boolean isSendFinished() {
		return this.ttpSender.isSendFinished();
	}

	/*
	 * close connection
	 */
	public void close() {
		if (!this.isClosed.get() && !this.ttpReceiver.isClosed()) {//server
			sendControlPacket(new TTPPacket(MyProtocol.FLAG_FIN), MyProtocol.FLAG_FIN_ACK);
			this.isClosed .set(true);
			this.ttpSender.close();
			this.ttpReceiver.close();
		} else if (!this.isClosed.get() && this.ttpReceiver.isClosed()){//client
			this.isClosed.set(true);
			this.ttpSender.close();
		}
		
	}

	/*
	 * for debugging, print thread status
	 */
	public void isAlive(){
		this.ttpSender.isAlive();
		
	}
	



	
	/*
	 * check if the src of the received packet is in accordance with the
	 * connection
	 */

	private boolean isCorrectSrc(String src, short port) {
		return this.dstAddr.equals(src) && this.dstPort == port;
	}
	
	/*
	 * A thread that receives ACK of a control packet. Control packet has to be
	 * sent one-by-one!
	 */
	private class ControlPacketReceiveAck implements Runnable {
		private AtomicBoolean acked;
		private int flagExpected;

		public ControlPacketReceiveAck(AtomicBoolean acked, int flagExpected) {
			this.acked = acked;
			this.flagExpected = flagExpected;
		}

		@Override
		public void run() {
			TTPPacket packet;
			while (!isClosed.get() && !acked.get()) {
				packet = null;
				try {
					packet = recvTTPPacket();
					if ((packet != null) && (packet.getFlag() == this.flagExpected) && (packet.getSeqIndex() == -1)) {
						acked.set(true);
						return;
					}
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}

		}

	}

	/*
	 * A thread that sends a control packet until acked. Control packet has to be sent
	 * one-by-one!
	 */
	private class ControlPacketSender implements Runnable {
		TTPPacket packet;
		AtomicBoolean acked;
		long beginTime;

		public ControlPacketSender(TTPPacket packet, AtomicBoolean acked) {
			this.packet = packet;
			this.acked = acked;
			this.beginTime = System.currentTimeMillis();
		}

		@Override
		public void run() {
			while (!isClosed.get() && !acked.get()) {

				// timeout
				if (System.currentTimeMillis() - beginTime > TIMEOUT) {
					System.err.println("ControlPacketSender timeout!");
					return;
				}

				try {
					sendTTPPacket(this.packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(TIMEOUT);
				} catch (InterruptedException e) {
				}
			}

		}

	}

	/* 
	 * a blocking method that sands a control packet.
	 */
	public void sendControlPacket(TTPPacket packet, int flagExpected) {
		AtomicBoolean acked = new AtomicBoolean(false);
		Thread sender = new Thread(new ControlPacketSender(packet, acked));
		Thread receiver = new Thread(new ControlPacketReceiveAck(acked, flagExpected));
		receiver.setDaemon(true);
		sender.start();
		receiver.start();
		try {
			//System.out.println("wait for control packet sender: " + MyProtocol.displayFlag(packet.getFlag()));
			sender.join();
			//receiver.join();
		} catch (InterruptedException e) {
		}
	}

	/*
	 * A method to send a single TTPPacket.
	 */
	
	public void sendTTPPacket(TTPPacket packet) throws IOException {
		byte[] data = DatagramUtils.toByteArray(packet);
		short checksum = DatagramUtils.cacChecksum(data);
		Datagram sendDatagram = new Datagram(srcAddr, dstAddr, srcPort, dstPort, (short) data.length, checksum, packet);
		ds.sendDatagram(sendDatagram);
	}

	/*
	 * The public non-blocking send(byte[]) API
	 */
	
	public void send(byte[] data) throws IOException {
		int len = data.length;
		if (len < MAX_SEG_SIZE) {
			TTPPacket[] packets = new TTPPacket[1];
			packets[0] = new TTPPacket(MyProtocol.FLAG_DAT, new TTPSegment(data, 0), this.nextSeq);
			this.nextSeq++;
			this.ttpSender.enqueuePackets(packets);
		} else {
			int totalSegmentNumber = (int) (len / MAX_SEG_SIZE);
			if (len % MAX_SEG_SIZE != 0) {
				totalSegmentNumber += 1;
			}
			TTPPacket[] packets = createTTPPacketArray(data, totalSegmentNumber);
			//System.err.println("packets[] length: " + packets.length);
			this.ttpSender.enqueuePackets(packets);
		}

	}
	
	/*
	 * A method to prepare TTPPacket array to send.
	 */

	private TTPPacket[] createTTPPacketArray(byte[] data, int totalSegmentNumber) {
		TTPPacket[] packets = new TTPPacket[totalSegmentNumber];
		byte[] buf;
		int off = 0;
		int len = data.length;
		int i;
		for (i = 0; i < totalSegmentNumber - 1; i++) {
			buf = new byte[MAX_SEG_SIZE];
			System.arraycopy(data, off, buf, 0, MAX_SEG_SIZE);
			packets[i] = new TTPPacket(MyProtocol.FLAG_DAT, new TTPSegment(buf, off), this.nextSeq);
			this.nextSeq++;
			off += MAX_SEG_SIZE;
		}
		int lastSegmentSize = len - off;
		buf = new byte[lastSegmentSize];
		System.arraycopy(data, off, buf, 0, lastSegmentSize);
		packets[i] = new TTPPacket(MyProtocol.FLAG_DAT, new TTPSegment(buf, off), this.nextSeq);
		this.nextSeq++;
		return packets;
	}

	/*
	 * The public receive() AIP
	 */
	public byte[] receive() {
		return this.ttpReceiver.receive();
	}

	/*
	 * A method to receive TTPPacket with checksum-checking and source-checking
	 */
	public TTPPacket recvTTPPacket() throws ClassNotFoundException, IOException {
		Datagram recvDatagram = ds.receiveDatagram();
		TTPPacket packet = (TTPPacket) recvDatagram.getData();
		if (recvDatagram == null || packet == null
				|| !isCorrectSrc(recvDatagram.getSrcaddr(), recvDatagram.getSrcport())
				|| !DatagramUtils.checkCheckSum(packet, recvDatagram.getChecksum())) {
			return null;
		}
		return packet;
	}

}
