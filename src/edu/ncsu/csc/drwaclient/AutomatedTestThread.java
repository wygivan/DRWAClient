package edu.ncsu.csc.drwaclient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import android.util.Log;

public class AutomatedTestThread implements Runnable {
	private final int DOWNLINK_PORT = 8001;
	private final int UPLINK_PORT = 8002;
	private final int DURATION = 60;
	private final int BUF_SIZE = 4096;

	private final String server_ip;
	private final String updown;

	public AutomatedTestThread(String... params) {
		server_ip = params[0];
		updown = params[1];
	}

	@Override
	public void run() {
		if (updown.matches("Downlink")) {
			while (true) {
				Long throughput = (long) 0;
				Socket sock;
				InputStream in;
				OutputStream out;
				long start_time = 0;
				PrintWriter outWriter = null;

				try {
					sock = new Socket(server_ip, DOWNLINK_PORT);
					in = sock.getInputStream();
					out = sock.getOutputStream();
					BufferedReader inReader = new BufferedReader(
							new InputStreamReader(in));
					outWriter = new PrintWriter(out, true);
					char[] inbuf = new char[BUF_SIZE];
					int bytes_read = 0;
					start_time = System.currentTimeMillis();
					while ((bytes_read = inReader.read(inbuf, 0, BUF_SIZE)) > 0) {
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
					synchronized (this) {
						wait(30000);
					}
				} catch (InterruptedException ex) {
					// Interrupted, stop the test
					break;
				} catch (Exception ex) {
					// Ignore temporary problems, continue testing
					Log.e("DRWAClient",
							"Exception in AutomatedTestThread::run()", ex);
				}
			}
		} else if (updown.matches("Uplink")) {
			while (true) {
				Socket sock;
				InputStream in;
				OutputStream out;
				long start_time = 0;
				PrintWriter outWriter = null;

				try {
					sock = new Socket(server_ip, UPLINK_PORT);
					in = sock.getInputStream();
					out = sock.getOutputStream();
					BufferedReader inReader = new BufferedReader(
							new InputStreamReader(in));
					outWriter = new PrintWriter(out, true);
					char[] buf = new char[BUF_SIZE];
					start_time = System.currentTimeMillis();
					while (System.currentTimeMillis() - start_time < DURATION * 1000) {
						outWriter.print(buf);
						if (Thread.currentThread().isInterrupted())
							break;
					}
					sock.shutdownOutput();
					inReader.readLine();
					in.close();
					out.close();
					sock.close();
					if (Thread.currentThread().isInterrupted()) {
						throw new InterruptedException();
					}

					// Wait for 30 seconds between two tests
					synchronized (this) {
						wait(30000);
					}
				} catch (InterruptedException ex) {
					// Interrupted, stop the test
					break;
				} catch (Exception ex) {
					// Ignore temporary problems, continue testing
					Log.e("DRWAClient",
							"Exception in AutomatedTestThread::run()", ex);
				}
			}
		}
	}
}
