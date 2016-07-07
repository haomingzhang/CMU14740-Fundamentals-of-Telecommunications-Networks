
/**
 * This class is used to build response message for server
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Response {
	private int statusCode;
	private long contentLength;
	// MIME type
	private String contentType;
	private String statusMsg;

	public Response(int statusCode, long contentLength, String contentType, String statusMsg) {
		this.statusCode = statusCode;
		this.contentLength = contentLength;
		this.contentType = contentType;
		this.statusMsg = statusMsg;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getMsg() {
		return statusMsg;
	}

	public void setMsg(String statusMsg) {
		this.statusMsg = statusMsg;
	}

	/**
	 * return time of the response generated
	 * 
	 * @return formated time string
	 */
	public String getCurrentTime() {
		Date d = new Date();
		SimpleDateFormat dft = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz");
		dft.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dft.format(d);
	}

	/**
	 * used to build formated response string
	 * 
	 * @param content
	 *            actual file content
	 * @return formated response string
	 */
	public String buildResponse(String content) {
		StringBuilder ret = new StringBuilder();
		ret.append("HTTP/1.0 " + statusCode + " " + statusMsg + "\r\n");
		ret.append("Server: Simple/1.0\r\n");
		// date
		ret.append("Date: " + getCurrentTime() + "\r\n");
		ret.append("Content-Type: " + contentType + "\r\n");
		if (statusCode == 200) {
			ret.append("Content-Length: " + contentLength + "\r\n");
		}
		ret.append("Connection: close\r\n");
		ret.append("\r\n");
		if (content != null) {
			ret.append(content);
		}
		// build body
		return ret.toString();
	}
	
}
