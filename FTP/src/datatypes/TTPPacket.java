package datatypes;
/*
 * The Packet class of TTP
 */
import java.io.Serializable;

public class TTPPacket implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private int seqIndex;
	private byte flag;
	public TTPSegment seg;
	
	
	/*
	 * control packet
	 */
	public TTPPacket(int flag) {
		this.setSeqIndex(-1);
		this.flag = (byte) flag;
		seg = null;
	}
	
	/*
	 * ACK packet
	 */
	public TTPPacket(int flag, int seqIndex) {
		this.setSeqIndex(seqIndex);
		this.flag = (byte) flag;
		seg = null;
	}
	
	/*
	 * DATA packet
	 */
	public TTPPacket(int flag, TTPSegment seg, int seqIndex) {
		this.setSeqIndex(seqIndex);
		this.flag = (byte) flag;
		this.seg = seg;
	}
	
	public TTPSegment getSegment() {
		return seg;
	}
	
	public void setSegment(TTPSegment seg) {
		this.seg = seg;
	}
	
	public byte getFlag() {
		return flag;
	}
	
	public boolean isAck(){
		return this.flag == MyProtocol.FLAG_ACK;
	}
	
	public boolean isReq(){
		return this.flag == MyProtocol.FLAG_REQ;
	}
	
	public boolean isReqAck(){
		return this.flag == MyProtocol.FLAG_REQ_ACK;
	}
	
	public boolean isData(){
		return this.flag == MyProtocol.FLAG_DAT;
	}
	
	public boolean isFin(){
		return this.flag == MyProtocol.FLAG_FIN;
	}
	
	public int getSeqIndex() {
		return seqIndex;
	}
	public void setSeqIndex(int seqIndex) {
		this.seqIndex = seqIndex;
	}
	
	
}
