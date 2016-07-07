package services;
/*
 * The TTP serveice API, used to create and accept connections
 */
import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;

import datatypes.Datagram;
import datatypes.MyProtocol;
import datatypes.TTPPacket;

public class TTPService {

	public DatagramService ds;
	private int verbose;
	private final short port;

	public TTPService(short port, int verbose) throws SocketException {
		super();
		this.verbose = verbose;
		this.port = port;
		ds = new DatagramService(port, verbose);

	}

	public DatagramService getDs() {
		return ds;
	}
	

	/*
	 * Create a new connection, called by client.
	 */
	
	public TTPConnection createConnection(short srcPort, short dstPort, String srcAddr, String dstAddr, int timeout, int windowsize)
			throws SocketException, ClassNotFoundException {
		TTPConnection ttpcon = new TTPConnection(srcPort, dstPort, srcAddr, dstAddr, timeout, ds, windowsize);
		// hand-shake for connection
		TTPPacket packet = new TTPPacket(MyProtocol.FLAG_REQ);
		System.out.println("Start TTP first shake hands: " + dstAddr);
		ttpcon.sendControlPacket(packet, MyProtocol.FLAG_REQ_ACK);
		System.out.println("Start TTP second shake hands: " + dstAddr);
		packet = new TTPPacket(MyProtocol.FLAG_REQ_ACK);
		ttpcon.sendControlPacket(packet, MyProtocol.FLAG_REQ_ACK);
		
		return ttpcon;
	}

	/* 
	 * Accept one connection from this port for each call. Called by the server.
	 * Two handshakes.
	 */
	
	public TTPConnection acceptConnection(int timeoutInterval, int windowsize) throws ClassNotFoundException, IOException {
		TTPConnection ttpcon = null;
		// key: SrcAddr + SrcPort
		HashMap<String, TTPConnection> hasReceivedReq = new HashMap<String, TTPConnection>();
		while (true) {
			Datagram datagram = ds.receiveDatagram();
			TTPPacket packet = (TTPPacket) datagram.getData();
			if (packet.isReq()) {
				System.err.println("get REQ from: " + datagram.getSrcaddr() + " Port: " + datagram.getSrcport());
				ttpcon = new TTPConnection(port, datagram.getSrcport(), datagram.getDstaddr(), datagram.getSrcaddr(),
						timeoutInterval, ds, windowsize);
				ttpcon.sendTTPPacket(new TTPPacket(MyProtocol.FLAG_REQ_ACK));
				hasReceivedReq.put(datagram.getSrcaddr() + datagram.getSrcport(), ttpcon);

			} else if (packet.isReqAck()
					&& ((ttpcon = hasReceivedReq.get(datagram.getSrcaddr() + datagram.getSrcport())) != null)) {
				ttpcon.sendTTPPacket(new TTPPacket(MyProtocol.FLAG_REQ_ACK));
				System.err.println("connection accepted from: " + datagram.getSrcaddr() + " Port: " + datagram.getSrcport());
				return ttpcon;
			}

		}

		
	}
}
