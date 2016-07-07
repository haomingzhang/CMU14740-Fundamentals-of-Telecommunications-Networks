import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpProcessorBuilder;

/*
 * This test class will test our simple server in several cases
 * including get request, head request, post request. It will also
 * involve test cases when the file does not exist, request a directory,
 * wrong version, not implemented method.
 */
public class TestSuite {
	
	CloseableHttpClient hc;
	String url;
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		int port = 8080;
		/* Parse parameter and do args checking */
		if (args.length < 1) {
			System.err.println("Usage: java TestSuite <server_port>");
			System.exit(1);
		}

		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Usage: java TestSuite <server_port>");
			System.exit(1);
		}

		if (port > 65535 || port < 1024) {
			System.err.println("Port number must be in between 1024 and 65535");
			System.exit(1);
		}

		TestSuite s = new TestSuite();
		s.hc = HttpClients.custom().setHttpProcessor(HttpProcessorBuilder.create().build()).build();
		s.url = "http://localhost:" + port + "/";
		
		// http get test
		String url = "index.html";
		System.out.println("get : " + (s.url + url));
		s.testGet(url);
		
		// http head test
		System.out.println("head : " + (s.url + url));
		s.testHead(url);
		
		// request directory
		url = "/";
		System.out.println("get : " + (s.url + url));
		s.testGet(url);
		
		// file not exist
		url = "notExist";
		System.out.println("get : " + (s.url + url));
		s.testGet(url);
		
		// http post test
		url = "index.html";
		System.out.println("post : " + (s.url + url));
		s.testPost(url);	
		
		//test version
		System.out.println("http version 1.1");
		s.testVersion(url);
		
	}

	/**
	 * test Get method
	 * @param url 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public void testGet(String url) throws ClientProtocolException, IOException {
		HttpGet request = new HttpGet(this.url + url);
		request.addHeader("Host", "www.example.com");
		request.addHeader("Connection", "close");
		request.setProtocolVersion(HttpVersion.HTTP_1_0);
		HttpResponse response = hc.execute(request);
		System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
		getBody(response);
	}
	
	/**
	 * test head method
	 * @param url
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void testHead(String url) throws ClientProtocolException, IOException {
		HttpHead request = new HttpHead(this.url + url);
		request.addHeader("Host", "www.example.com");
		request.addHeader("Connection", "close");
		request.setProtocolVersion(HttpVersion.HTTP_1_0);
		HttpResponse response = hc.execute(request);
		System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
	}
	
	/**
	 * test post method, server should return 501.
	 * @param url
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void testPost(String url) throws ClientProtocolException, IOException {
		HttpPost post = new HttpPost(this.url + url);
		post.setProtocolVersion(HttpVersion.HTTP_1_0);
		HttpResponse response = hc.execute(post);
		System.out.println("Response Code : "
				+ response.getStatusLine().getStatusCode());
		getBody(response);
	}
		
		
	public void getBody(HttpResponse response) throws IOException {
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		System.out.println("Resposne Body:");
		System.out.println(result.toString());
	}
	
	/**
	 * test version, server should return 505.
	 * @param url
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void testVersion(String url) throws ClientProtocolException, IOException {
		HttpGet request = new HttpGet(this.url + url);
		request.addHeader("Host", "www.example.com");
		request.addHeader("Connection", "close");
		HttpResponse response = hc.execute(request);
		System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
		getBody(response);
	}

}
