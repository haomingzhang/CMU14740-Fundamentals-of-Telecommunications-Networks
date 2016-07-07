package datatypes;

/**
 * This class defined the flags used in TTP packet
 */
public class MyProtocol {
	// data packet
	public static final int FLAG_DAT = 1;
	// request packet to initialize connection
	public static final int FLAG_REQ = 2;
	// acknowledge packet
	public static final int FLAG_ACK = 4;
	// server response packet in three-way handshake
	public static final int FLAG_REQ_ACK = 6;
	// packet to disconnect
	public static final int FLAG_FIN = 8;
	// server response packet for disconnection
	public static final int FLAG_FIN_ACK = 12;
	// force to disconnect
	public static final int FLAG_RST = 16;

	public static String displayFlag(int flag) {
		switch (flag) {
		case FLAG_DAT:
			return "DATA";
		case FLAG_REQ:
			return "REQ";
		case FLAG_ACK:
			return "ACK";
		case FLAG_REQ_ACK:
			return "REQ_ACK";
		case FLAG_FIN:
			return "FIN";
		case FLAG_FIN_ACK:
			return "FIN_ACK";
		case FLAG_RST:
			return "RST";
		default:
			return "unknown flag";

		}

	}
}
