package edu.ncsu.csc.drwaclient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import android.util.Log;

public class AutomatedTestThread implements Runnable {
	private final int BULK_PORT = 8001;
//	private final int WEB_PORT = 8002;
//	private final int STREAMING_PORT = 8003;
	private final int INBUF_SIZE = 4096;

	private String server_ip;
	private String chosen_scenario;

	public AutomatedTestThread(String... params) {
		server_ip = params[0];
		chosen_scenario = params[1];
	}

	public void run() {
    	if (chosen_scenario.matches("Bulk download")) {
    		while (true) {
	        	Long throughput = (long) 0;
    			Socket sock;
    			InputStream in;
    			OutputStream out;
	        	long start_time = 0;
	        	PrintWriter outWriter = null;
	        	
	    		try
	    		{
	    			sock = new Socket(server_ip, BULK_PORT);
	    			in = sock.getInputStream();
	    			out = sock.getOutputStream();
	    			BufferedReader inReader = new BufferedReader(new InputStreamReader(in));
	    			outWriter = new PrintWriter(out, true);
	    			char[] inbuf = new char[INBUF_SIZE];
	        		int bytes_read = 0;
	        		start_time = System.currentTimeMillis();
	    			while ((bytes_read = inReader.read(inbuf, 0, INBUF_SIZE)) > 0) {
	    				throughput += bytes_read;
	    				if (Thread.currentThread().isInterrupted())
	    					break;
	    			}
	        		long stop_time = System.currentTimeMillis();
	    			throughput = throughput * 8000 / (stop_time - start_time);
	    			outWriter.println(throughput);
	    			in.close();
	    			out.close();
	    			sock.close();
	    			if (Thread.currentThread().isInterrupted()) {
	    			    throw new InterruptedException();
	    			}
	                
	    			// Wait for 30 seconds between two tests
	    			synchronized(this){
	                    wait(30000);
	                }
	    		} catch (InterruptedException ex) {
	    			// Interrupted, stop the test
	    			break;
	    		} catch (Exception ex) {
	    			// Ignore temporary problems, continue testing
	    			Log.e("DRWAClient", "Exception in AutomatedTestThread::run()", ex);
	    		}
    		}
    	} else if (chosen_scenario.matches("Web surfing")) {
    		// TBD
    	} else if (chosen_scenario.matches("Video streaming")) {
    		// TBD
    	}
	}
}
