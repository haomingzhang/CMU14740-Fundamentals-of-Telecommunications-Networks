/*
 * A ftp server that uses TTPService
 */
package applications;

import java.io.IOException;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import services.TTPConnection;
import services.TTPService;

public class ServerFtp extends Thread {
	// available port array
	static List<Short> portArr = new ArrayList<Short>();
	static List<TTPService> ttpsArr = new ArrayList<TTPService>();
	// max number of port 
	static final int MAX_PORT_RANGE = 10;

	public ServerFtp(short port) {
		short beg = port;
		for (int i = 0; i < MAX_PORT_RANGE; i++) {
			portArr.add(beg);
			beg++;
		}
	}

	public void run() {

		System.out.println("Starting Server ...");

		// initialize TTPService on all available port
		for (int i = 0; i < MAX_PORT_RANGE; i++) {
			try {
				ttpsArr.add(new TTPService(portArr.get(i), 10));
			} catch (SocketException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		for (TTPService ts : ttpsArr) {
			Thread worker = new Thread(new WorkerThread(ts));
			worker.start();
		}

	}

}
/**
 * This class is used for handling TTPconnection for each client
 */

class WorkerThread implements Runnable {
	TTPService ts;

	WorkerThread(TTPService ts) {
		this.ts = ts;
	}

	@Override
	public void run() {
		TTPConnection con = null;
		try {
			// server accept client
			con = ts.acceptConnection(500, 100);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		byte[] buffer = null;
		String filename = null;
		while (true) {
			buffer = con.receive();
			filename = new String(buffer);
			if (filename != null && filename.length() > 0) {
				System.err.println(filename);
				break;
			}
		}
		System.err.println("Server is sending file back!");
		byte[] data;
		byte[] md5;
		try {
			data = Files.readAllBytes(Paths.get("serverRoot/" + filename));
			md5 = calcMD5(data);
			con.send(md5);
			while (!con.isSendFinished()) {
				System.out.println("wait for sending finished!");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			con.send(data);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		while (!con.isSendFinished()) {
			System.err.println("wait for sending finished!");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		System.err.println("closing connection!");
		con.close();
		System.err.println("connection closed.");
	}

	/**
	 * calculate md5 number
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] calcMD5(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		return messageDigest.digest(data);
	}

}
