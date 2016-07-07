package datatypes;

/*
 * A class to receive data packets.
 */
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import services.DatagramService;

public class TTPReceiver {
	private short srcPort;
	private short dstPort;
	private String srcAddr;
	private String dstAddr;
	private DatagramService ds;
	private AtomicInteger seqExpected;
	byte[] buffer;
	private AtomicBoolean closed;

	public TTPReceiver(short srcPort, short dstPort, String srcAddr, String dstAddr, DatagramService ds)
			throws SocketException {
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.srcAddr = srcAddr;
		this.dstAddr = dstAddr;
		this.ds = ds;
		this.seqExpected = new AtomicInteger(0);
		this.closed = new AtomicBoolean(false);
	}

	/*
	 * return true if this is closed.
	 */
	public boolean isClosed() {
		return this.closed.get();
	}

	/*
	 * close this class
	 */
	public void close() {
		this.closed.set(true);
	}

	/*
	 * A method to receive TTPPacket
	 */
	public TTPPacket recvTTPPacket() throws ClassNotFoundException, IOException {
		Datagram recvDatagram = ds.receiveDatagram();
		TTPPacket packet = (TTPPacket) recvDatagram.getData();
		return packet;
	}
	
	/*
	 * A method to send ACK with specified seq number
	 */
	private void sendAck(int seqIndex) throws IOException {
		TTPPacket ack = new TTPPacket(MyProtocol.FLAG_ACK, seqIndex);
		byte[] data = DatagramUtils.toByteArray(ack);
		short checksum = DatagramUtils.cacChecksum(data);
		/*
		if (seqIndex % 1000 == 0) {
			System.out.println("sending packet: " + MyProtocol.displayFlag(ack.getFlag()) + " seq: " + ack.getSeqIndex());
		}
		*/
		Datagram sendDatagram = new Datagram(srcAddr, dstAddr, srcPort, dstPort, (short) data.length, checksum, ack);
		ds.sendDatagram(sendDatagram);
	}

	/*
	 * check if the src of the received packet is in accordance with the
	 * connection
	 */

	private boolean isCorrectSrc(String src, short port) {
		return this.dstAddr.equals(src) && this.dstPort == port;
	}

	
	/*
	 * The blocking receive method to receive packets.
	 */
	public byte[] receive() {
		Datagram recvDatagram = null;
		TTPPacket packet = null;
		byte[] data = null;

		while ((!this.closed.get())) {
			try {
				recvDatagram = ds.receiveDatagram();
				packet = (TTPPacket) recvDatagram.getData();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				return null;
			}

			//System.out.println("In TTPReceiver.receive() : flag=" + MyProtocol.displayFlag(packet.getFlag()) + " seq = "
			//		+ packet.getSeqIndex() + " seqExpected=" + seqExpected);

			if (recvDatagram == null || packet == null
					|| !isCorrectSrc(recvDatagram.getSrcaddr(), recvDatagram.getSrcport())
					|| !DatagramUtils.checkCheckSum(packet, recvDatagram.getChecksum())) {
				continue;
			}

			if (packet.isFin()) {
				//System.out.println("FIN received!");
				TTPPacket ack = new TTPPacket(MyProtocol.FLAG_FIN_ACK, -1);
				byte[] retData = null;
				try {
					retData = DatagramUtils.toByteArray(ack);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				short checksum = DatagramUtils.cacChecksum(retData);
				Datagram sendDatagram = new Datagram(srcAddr, dstAddr, srcPort, dstPort, (short) retData.length,
						checksum, ack);
				try {
					ds.sendDatagram(sendDatagram);
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.close();
				return null;
			}

			if (!packet.isData()) {

				// forward to localhost
				try {
					recvDatagram.setDstaddr("127.0.0.1");
					ds.sendDatagram(recvDatagram);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
				}
				continue;
			}

			if (packet.getSeqIndex() == this.seqExpected.get()) {
				// System.out.println("In TTPReceiver.receive() : flag=" +
				// MyProtocol.displayFlag(packet.getFlag())
				// + " seqExpected=" + seqExpected);
				data = packet.getSegment().getData();
				try {
					this.sendAck(packet.getSeqIndex());
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.seqExpected.incrementAndGet();
				return data;
			} else {
				try {
					this.sendAck(this.seqExpected.get() - 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}
}