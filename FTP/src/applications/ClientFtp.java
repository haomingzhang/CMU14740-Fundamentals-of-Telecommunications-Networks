/*
 * A ftp client that uses TTPService
 */
package applications;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static java.nio.file.StandardOpenOption.*;
import services.TTPConnection;
import services.TTPService;

public class ClientFtp extends Thread {
	short portClient;
	short portServer;
	// request file name
	String fileName;

	public ClientFtp(short portClient, short portServer, String fileName) {
		this.portClient = portClient;
		this.portServer = portServer;
		this.fileName = fileName;
	}

	public void run() {
		System.out.println("Starting client ...");
		try {
			TTPService ts = new TTPService(portClient, 10);
			// client request connection
			TTPConnection con = ts.createConnection(portClient, portServer, "127.0.0.1", "127.0.0.1", 500, 100);
			System.out.println("Connection Established!\n");
			byte[] data = fileName.getBytes();
			// send request file name
			con.send(data);
			while (!con.isSendFinished()) {
				System.out.println("wait for sending finished!");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			System.out.println("sending file name finished!");
			// prepare to receive file
			Path p = Paths.get("clientRoot/" + fileName);
			OutputStream out = new BufferedOutputStream(Files.newOutputStream(p));
			byte[] buffer;
			byte[] md5;
			md5 = con.receive();
			while ((buffer = con.receive()) != null) {
				out.write(buffer, 0, buffer.length);
			}
			// clear buffer
			out.flush();
			System.out.println("File transmission completed!");
			// compare md5
			byte[] readData = Files.readAllBytes(p);
			if (!new String(md5).equals(calcMD5(readData))) {
				System.out.println("File not pass md5 check!");
			} else {
				System.out.println("File pass md5 check!");
			}
			con.close();
			System.out.println("Client connection closed!");
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * calculate md5 number
	 * 
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static String calcMD5(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		return new String(messageDigest.digest(data));
	}

}
