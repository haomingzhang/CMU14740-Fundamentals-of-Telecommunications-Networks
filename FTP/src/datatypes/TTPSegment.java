package datatypes;

import java.io.Serializable;
/**
 *This class is used for handling segmentation
 */
public class TTPSegment implements Serializable{

	private static final long serialVersionUID = 1L;
	

	// sequence number
	private int index;
	
	private byte[] data = new byte[0];

	
	public TTPSegment(byte[] d, int index){
		this.setData(d);
		this.setIndex(index);
	}
	
	public void setData(byte[] d) {
		if(d == null) {
			data = new byte[0];
			return;
		}
		data = d;
		index = -1;
	}
	public byte[] getData() {
		return data;
	}
	

	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public int getSegmentSize() {
		return data.length;
	}

}
