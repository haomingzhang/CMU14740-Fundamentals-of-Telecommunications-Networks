package datatypes;
/*
 * A class proving public static methods
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class DatagramUtils {
	/*
	 * Convert an object to byte[]
	 * Reference:
	 * toByteArray are taken from: http://tinyurl.com/69h8l7x
	 */
    public static byte[] toByteArray(Object obj) throws IOException {
        byte[] ret = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            ret = baos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (baos != null) {
                baos.close();
            }
        }
        return ret;
    }
    
    /*
     * calculate checksum
     */
	public static short cacChecksum(byte[] data) {
		short ret = 0;
		Checksum checksum = new CRC32();
		checksum.update(data, 0, data.length);
		ret = (short) checksum.getValue();
		return ret;
	}
	
	/*
	 * check checksum, return true if correct
	 */
	public static boolean checkCheckSum(TTPPacket packet, short checkSum){
		try {
			return cacChecksum(toByteArray(packet)) == checkSum;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/*
	 * check if the port is legal
	 */
	public static boolean checkPort(String port) {
		try {
			int p = Integer.parseInt(port);
			if (p < 1024 || p > 65535) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	/*
	 * check if the IP address is legal
	 */
	public static boolean checkIP(String ip) {
		String[] nums = ip.split("\\.");
		if (nums.length != 4) {
			return false;
		}
		try {
			for (String num : nums) {
				if (num == null || num.length() == 0) {
					return false;
				}
				int n = Integer.parseInt(num);
				if (n < 0 || n > 255) {
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}
    
}
