package datatypes;

/**
 * This class is used to buffer packets for upper level app use
 */
public class PacketBuffer {

	public static PacketBuffer createDefault() {
		return new PacketBuffer();
	}
	
	private Datagram[] packets;
	private int start = 0;
	private int end = 0;
	// number of free slot in buffer
	private int available;
	private int totalSlot;

	public PacketBuffer() {
		// buffer size
		totalSlot = 64;
		packets = new Datagram[totalSlot];
		available = totalSlot;
	}
	// add packet to buffer
	public boolean add(Datagram packet) {
		if(available > 0) {
			packets[end] = packet;
			end = (end + 1) % totalSlot;
			available -= 1;
			return true;
		}
		else {
			System.err.println(
				"Packet buffer is running out, packet loss may begin");
		}
		return false;
	}
	// pop packet from buffer
	public Datagram pop() {
		if(available == totalSlot)
			return null;
		Datagram packet = packets[start];
		start = (start+ 1) % totalSlot;
		available += 1;
		return packet;
	}
}
