package datatypes;

/*
 * A class that sends DATA using go-back-N.
 * Reference:
 * Computer Networking - A Top-down Approach, Figure 3.20 and 3.21
 */
import java.io.IOException;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import services.DatagramService;

public class WindowSender {
	public static final int MAX_SEG_SIZE = 1024;
	public short srcPort;
	public short dstPort;
	public String srcAddr;
	public String dstAddr;
	public DatagramService ds;
	public int timeoutInterval;
	public int index;
	public Timer[] timer;
	public int errno = 0;
	public final int TIMEOUT = 100;
	public final int SEND_SLEEP_INTERVAL = 1;
	public final int CONTROLLER_SLEEP_INTERVAL = 10;
	public final int N;
	private AtomicInteger base;
	private AtomicInteger nextSeqNum;
	private ConcurrentLinkedQueue<TTPPacket[]> packetQueue;
	private Thread senderThread;
	private Thread ackReceiverThread;
	private Thread controller;
	private AtomicInteger seqExpected;
	private AtomicInteger arrayEnd;
	private AtomicBoolean closed;

	/*
	 * A method for debugging. Print information about thread status.
	 */
	public void isAlive() {
		System.err.println("senderThread: " + senderThread.isAlive() + " ackReceiverThread: "
				+ ackReceiverThread.isAlive() + " controller: " + controller.isAlive());

	}

	/*
	 * return true if sending has finished.
	 */
	public boolean isSendFinished() {
		return ((senderThread == null && ackReceiverThread == null)
				|| (!senderThread.isAlive() && !ackReceiverThread.isAlive())) && packetQueue.isEmpty();
	}

	/*
	 * Close the WindowSender.
	 */
	public void close() {
		this.closed.set(true);
		if (timer[0] != null) {
			timer[0].cancel();
			timer[0].purge();
			timer[0] = null;
		}
	}

	/*
	 * add packets to the sending queue.
	 */
	public boolean enqueuePackets(TTPPacket[] packetArray) {
		this.packetQueue.add(packetArray);
		return true;
	}

	public WindowSender(short srcPort, short dstPort, String srcAddr, String dstAddr, DatagramService ds,
			int windowSize) throws SocketException {
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.srcAddr = srcAddr;
		this.dstAddr = dstAddr;
		this.ds = ds;
		this.N = windowSize;
		this.timer = new Timer[1];
		this.timer[0] = new Timer(true);
		this.packetQueue = new ConcurrentLinkedQueue<TTPPacket[]>();
		this.base = new AtomicInteger(0);
		this.nextSeqNum = new AtomicInteger(0);
		this.senderThread = null;
		this.ackReceiverThread = null;
		this.seqExpected = new AtomicInteger(0);
		this.arrayEnd = new AtomicInteger(0);
		this.closed = new AtomicBoolean(false);
		this.controller = new Thread(new Controller());
		this.controller.start();
	}

	/*
	 * A controller that creates sender and ackReceiver
	 */
	private class Controller implements Runnable {

		@Override
		public void run() {

			while (!closed.get()) {
				if (((senderThread == null && ackReceiverThread == null)
						|| (!senderThread.isAlive() && !ackReceiverThread.isAlive())) && !packetQueue.isEmpty()) {
					// The previous array in the queue has been sent
					TTPPacket[] packets = packetQueue.poll();

					arrayEnd.set(seqExpected.get() + packets.length - 1);

					if (packets.length == 1) {// don't use window
						AtomicBoolean acked = new AtomicBoolean(false);
						senderThread = new Thread(new SinglePacketSender(packets[0], acked));
						ackReceiverThread = new Thread(new SingleReceiveAck(acked));
						senderThread.start();
						ackReceiverThread.start();

					} else {// use window
						base.set(0);
						nextSeqNum.set(0);
						senderThread = new Thread(new SendWindow(packets));
						ackReceiverThread = new Thread(new ReceiveAcks(packets));
						synchronized (timer) {
							timer[0].cancel();
							timer[0].purge();
							timer[0] = null;
							timer[0] = new Timer(true);
						}
						senderThread.start();
						ackReceiverThread.start();

					}
					// wait for the send ending
					try {
						senderThread.join();
						ackReceiverThread.join();
					} catch (InterruptedException e) {
						//System.err.println(senderThread.isAlive() + " " + ackReceiverThread.isAlive());
					}
				}

				try {
					Thread.sleep(CONTROLLER_SLEEP_INTERVAL);
				} catch (InterruptedException e) {
				}
			}
			return;
		}

	}

	/*
	 * window-sender thread
	 */
	private class SendWindow implements Runnable {
		public TTPPacket[] sendPackets;

		public SendWindow(TTPPacket[] sendPackets) {
			this.sendPackets = sendPackets;
		}

		@Override
		public void run() {
			while (!closed.get()) {
				for (; nextSeqNum.get() < base.get() + N; nextSeqNum.incrementAndGet()) {
					if (nextSeqNum.get() == sendPackets.length) {// array ends
						return;
					}
					try {
						sendTTPPacket(sendPackets[nextSeqNum.get()]);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (base.get() == nextSeqNum.get()) {
						// timeout thread
						synchronized (timer) {
							timer[0].cancel();
							timer[0].purge();

							timer[0] = null;
							timer[0] = new Timer(true);
							timer[0].schedule(new ResendWindow(sendPackets), TIMEOUT, TIMEOUT);
						}
					}

				}
				try {
					Thread.sleep(SEND_SLEEP_INTERVAL);
				} catch (InterruptedException e) {
				}
			}
			return;
		}
	}
	/*
	 * check if the src of the received packet is in accordance with the
	 * connection
	 */

	private boolean isCorrectSrc(String src, short port) {
		return this.dstAddr.equals(src) && this.dstPort == port;
	}

	/*
	 * An ackReceiver for window-sender
	 */
	private class ReceiveAcks implements Runnable {
		public TTPPacket[] sendPackets;

		public ReceiveAcks(TTPPacket[] sendPackets) {
			this.sendPackets = sendPackets;
		}

		@Override
		public void run() {
			while (!closed.get()) {
				TTPPacket packet = null;
				Datagram recvDatagram = null;
				try {
					recvDatagram = ds.receiveDatagram();
					packet = (TTPPacket) recvDatagram.getData();

					if (recvDatagram == null || packet == null
							|| !isCorrectSrc(recvDatagram.getSrcaddr(), recvDatagram.getSrcport())
							|| !DatagramUtils.checkCheckSum(packet, recvDatagram.getChecksum())) {
						continue;
					}

					if (packet != null && !packet.isAck()) {
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

					if (packet != null && packet.isAck()) {
						int seqIndex = packet.getSeqIndex();

						// System.err.println("Ack seqIndex: " + seqIndex + "
						// seqExpected: " + seqExpected.get());

						if (seqIndex == arrayEnd.get()) {// finish sending the
															// array
							// System.err.println("seqIndex: " + seqIndex + "
							// arrayEnd: " + arrayEnd.get());
							// end the send and reset
							//System.out.println("Transmission of a send() call is ending.");
							synchronized (timer) {
								timer[0].cancel();
								timer[0].purge();
							}
							seqExpected.set(seqIndex + 1);
							return;

						}

						if (seqIndex >= seqExpected.get()) {// ignore smaller
															// ACKs
							synchronized (timer) {
								timer[0].cancel();
								timer[0].purge();

								base.addAndGet(seqIndex - seqExpected.get() + 1);
								if (base.get() < nextSeqNum.get()) {// restart
																	// timer
									timer[0] = new Timer(true);
									timer[0].schedule(new ResendWindow(this.sendPackets), TIMEOUT, TIMEOUT);
								}
							}
							seqExpected.set(seqIndex + 1);

						}

					}
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * A thread to receive ACK while sending a single packet without using window.
	 */
	private class SingleReceiveAck implements Runnable {
		private AtomicBoolean acked;

		public SingleReceiveAck(AtomicBoolean acked) {
			this.acked = acked;
		}

		@Override
		public void run() {
			TTPPacket packet;
			Datagram recvDatagram;
			while (!closed.get() && !acked.get()) {
				recvDatagram = null;
				packet = null;
				try {
					recvDatagram = ds.receiveDatagram();
					packet = (TTPPacket) recvDatagram.getData();

					// System.err.println("received packet: " +
					// MyProtocol.displayFlag(packet.getFlag()) + " seq: " +
					// packet.getSeqIndex());

					if (recvDatagram == null || packet == null
							|| !isCorrectSrc(recvDatagram.getSrcaddr(), recvDatagram.getSrcport())
							|| !DatagramUtils.checkCheckSum(packet, recvDatagram.getChecksum())) {
						continue;
					}

					if (packet != null && !packet.isAck()) {
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

					if (packet != null && packet.isAck() && (packet.getSeqIndex() == seqExpected.get())) {
						seqExpected.incrementAndGet();
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
	 * A thread to send a single packet without using window.
	 */
	
	private class SinglePacketSender implements Runnable {
		TTPPacket packet;
		AtomicBoolean acked;

		public SinglePacketSender(TTPPacket packet, AtomicBoolean acked) {
			this.packet = packet;
			this.acked = acked;
		}

		@Override
		public void run() {
			while (!closed.get() && !acked.get()) {
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
			return;

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
	 * A TimerTask to resend the while window when there is a timeout.
	 */
	class ResendWindow extends TimerTask {

		public TTPPacket[] sendPackets;

		public ResendWindow(TTPPacket[] sendPackets) {
			this.sendPackets = sendPackets;
		}

		@Override
		public void run() {
			// start timer
			for (int i = base.get(); i < nextSeqNum.get(); i++) {
				// System.err.println("base: " + base.get() + " i: " + i);
				try {
					sendTTPPacket(sendPackets[i]);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}



}