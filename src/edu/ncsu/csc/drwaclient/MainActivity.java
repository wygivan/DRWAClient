package edu.ncsu.csc.drwaclient;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final int DOWNLINK_PORT = 8001;
	private final int UPLINK_PORT = 8002;
	private final int DURATION = 60;
	private final int BUF_SIZE = 4096;

	private do_onetime_test onetime_test_task = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // Always call the superclass
		setContentView(R.layout.activity_main); // Set the user interface layout
	}

	@Override
	public void onStop() {
		super.onStop(); // Always call the superclass

		if (onetime_test_task != null)
			onetime_test_task.cancel(true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy(); // Always call the superclass

		stopService(new Intent(this, DRWAClientService.class));
	}

	// Regex to check the validity of the input IP address
	private static final Pattern IP_ADDRESS = Pattern
			.compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
					+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
					+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
					+ "|[1-9][0-9]|[0-9]))");

	private boolean check_input_set_param() {
		EditText server_ip = (EditText) findViewById(R.id.server_ip);
		EditText lambda10 = (EditText) findViewById(R.id.lambda10);
		EditText tcp_rmem_max = (EditText) findViewById(R.id.tcp_rmem_max);
		CheckBox drwa = (CheckBox) findViewById(R.id.drwa);

		if (!IP_ADDRESS.matcher(server_ip.getText()).matches()) {
			AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
			alt_bld.setTitle("Warning");
			alt_bld.setMessage("Invalid IP address!");
			alt_bld.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							// Do nothing after user clicked OK button
						}
					});
			AlertDialog alert = alt_bld.create();
			alert.show();

			return false;
		}

		try {
			int lambda10_value = Integer
					.parseInt(lambda10.getText().toString());
			int tcp_rmem_max_value = Integer.parseInt(tcp_rmem_max.getText()
					.toString());

			Process pr = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(pr.getOutputStream());

			// set lambda
			os.writeBytes("echo " + lambda10_value
					+ " > /proc/sys/net/ipv4/tcp_drwa_lambda10\n");

			// set tcp_rmem_max
			os.writeBytes("echo " + tcp_rmem_max_value
					+ " > /sys/kernel/ipv4/tcp_rmem_max\n");

			// turn on/off DRWA
			if (drwa.isChecked()) {
				os.writeBytes("echo 1 > /proc/sys/net/ipv4/tcp_drwa\n");
			} else {
				os.writeBytes("echo 0 > /proc/sys/net/ipv4/tcp_drwa\n");
			}
		} catch (NumberFormatException e) {
			AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
			alt_bld.setTitle("Warning");
			alt_bld.setMessage("Invalid lambda/tcp_rmem_max value!");
			alt_bld.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							// Do nothing after user clicked OK button
						}
					});
			AlertDialog alert = alt_bld.create();
			alert.show();

			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	private void enable_user_input(boolean onoff) {
		EditText server_ip = (EditText) findViewById(R.id.server_ip);
		EditText lambda10 = (EditText) findViewById(R.id.lambda10);
		CheckBox drwa = (CheckBox) findViewById(R.id.drwa);
		EditText tcp_rmem_max = (EditText) findViewById(R.id.tcp_rmem_max);
		RadioGroup updown = (RadioGroup) findViewById(R.id.updown);
		Button onetime = (Button) findViewById(R.id.onetime);

		server_ip.setEnabled(onoff);
		lambda10.setEnabled(onoff);
		drwa.setEnabled(onoff);
		tcp_rmem_max.setEnabled(onoff);
		for (int i = 0; i < updown.getChildCount(); i++) {
			((RadioButton) updown.getChildAt(i)).setEnabled(onoff);
		}
		onetime.setEnabled(onoff);
	}

	private class do_onetime_test extends AsyncTask<String, Void, Long> {
		@Override
		protected Long doInBackground(String... params) {
			Long throughput = (long) 0;
			RadioButton downlink = (RadioButton) findViewById(R.id.downlink);
			RadioButton uplink = (RadioButton) findViewById(R.id.uplink);

			if (downlink.isChecked()) {
				try {
					Socket sock = new Socket(params[0], DOWNLINK_PORT);
					InputStream in = sock.getInputStream();
					OutputStream out = sock.getOutputStream();
					BufferedReader inReader = new BufferedReader(
							new InputStreamReader(in));
					PrintWriter outWriter = new PrintWriter(out, true);
					char[] inbuf = new char[BUF_SIZE];
					int bytes_read = 0;
					long start_time = System.currentTimeMillis();
					while ((bytes_read = inReader.read(inbuf, 0, BUF_SIZE)) > 0) {
						throughput += bytes_read;
						if (isCancelled())
							break;
					}
					long stop_time = System.currentTimeMillis();
					throughput = throughput * 8000 / (stop_time - start_time);
					outWriter.println(throughput);
					in.close();
					out.close();
					sock.close();
				} catch (Exception ex) {
					Log.e("DRWAClient",
							"Exception in do_onetime_test::doInBackground()",
							ex);
				}
			} else if (uplink.isChecked()) {
				try {
					Socket sock = new Socket(params[0], UPLINK_PORT);
					InputStream in = sock.getInputStream();
					OutputStream out = sock.getOutputStream();
					BufferedReader inReader = new BufferedReader(
							new InputStreamReader(in));
					PrintWriter outWriter = new PrintWriter(out, true);
					char[] buf = new char[BUF_SIZE];
					long start_time = System.currentTimeMillis();
					while (System.currentTimeMillis() - start_time < DURATION * 1000) {
						outWriter.print(buf);
						if (isCancelled())
							break;
					}
					sock.shutdownOutput();
					throughput = Long.valueOf(inReader.readLine());
					in.close();
					out.close();
					sock.close();
				} catch (Exception ex) {
					Log.e("DRWAClient",
							"Exception in do_onetime_test::doInBackground()",
							ex);
				}
			}

			return throughput;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// Disable user input
			enable_user_input(false);
			Button startstop = (Button) findViewById(R.id.automated);
			startstop.setEnabled(false);

			// Display progress bar
			ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
			progress.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onCancelled(Long throughput) {
			// Hide progress bar
			ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
			progress.setVisibility(View.INVISIBLE);

			// Re-enable user input
			enable_user_input(true);
			Button startstop = (Button) findViewById(R.id.automated);
			startstop.setEnabled(true);
		}

		@Override
		protected void onPostExecute(Long throughput) {
			super.onPostExecute(throughput);

			// Hide progress bar
			ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
			progress.setVisibility(View.INVISIBLE);

			// Re-enable user input
			enable_user_input(true);
			Button startstop = (Button) findViewById(R.id.automated);
			startstop.setEnabled(true);

			// Show throughput
			Toast toast = Toast.makeText(getApplicationContext(),
					print_througput(throughput), Toast.LENGTH_LONG);
			toast.show();
		}

		private String print_througput(Long throughput) {
			String retval;

			if (throughput < 1000) {
				retval = throughput.toString() + "bps";
			} else if (throughput < 1000000) {
				retval = String.valueOf(throughput / 1000) + "Kbps";
			} else if (throughput < 1000000000) {
				retval = String.valueOf(throughput / 1000000) + "Mbps";
			} else {
				retval = String.valueOf(throughput / 1000000000) + "Gbps";
			}

			return retval;
		}
	}

	// Called when the user clicks the "Run one-time test" button
	public void onetime_test(View v) {
		if (!check_input_set_param())
			return;

		// Run one-time test
		EditText server_ip = (EditText) findViewById(R.id.server_ip);
		onetime_test_task = new do_onetime_test();
		onetime_test_task.execute(server_ip.getText().toString());
	}

	// Called when the user clicks the "Run/Stop automated test" button
	public void automated_test(View v) {
		EditText server_ip = (EditText) findViewById(R.id.server_ip);
		RadioGroup updown = (RadioGroup) findViewById(R.id.updown);
		RadioButton chosen = (RadioButton) findViewById(updown
				.getCheckedRadioButtonId());
		Button startstop = (Button) findViewById(R.id.automated);

		if (startstop.getText().equals(
				getResources().getString(R.string.start_automated))) {
			if (!check_input_set_param())
				return;

			// Disable user input
			enable_user_input(false);
			startstop
					.setText(getResources().getString(R.string.stop_automated));

			// Start automated test
			Intent intent = new Intent(this, DRWAClientService.class);
			intent.putExtra(DRWAClientService.SERVER_IP, server_ip.getText()
					.toString());
			intent.putExtra(DRWAClientService.UPDOWN, chosen.getText()
					.toString());
			startService(intent);
		} else {
			// Stop automated test
			stopService(new Intent(this, DRWAClientService.class));

			// Re-enable user input
			enable_user_input(true);
			startstop.setText(getResources()
					.getString(R.string.start_automated));
		}
	}
}
