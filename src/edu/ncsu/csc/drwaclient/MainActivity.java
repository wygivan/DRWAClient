package edu.ncsu.csc.drwaclient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final int BULK_PORT = 8001;
//	private final int WEB_PORT = 8002;
//	private final int STREAMING_PORT = 8003;
	private final int INBUF_SIZE = 4096;
	
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
    private static final Pattern IP_ADDRESS =
            Pattern.compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            				+ "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            				+ "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
        					+ "|[1-9][0-9]|[0-9]))"); 
    
    private boolean check_input() {
    	boolean retval = true;
    	EditText server_ip = (EditText) findViewById(R.id.server_ip);

 		if(!IP_ADDRESS.matcher(server_ip.getText()).matches()) {
			// Invalid IP input
 			retval = false;
			AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
			alt_bld.setTitle("Warning");
			alt_bld.setMessage("Invalid IP address!");
			alt_bld.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // Do nothing after user clicked OK button
		           }
		       });
			AlertDialog alert = alt_bld.create();
			alert.show();
 		}
 		
 		return retval;
    }
    
    private class do_onetime_test extends AsyncTask<String, Void, Long> {
        protected Long doInBackground(String... params) {
        	Long throughput = (long) 0;
        	RadioButton bulk = (RadioButton) findViewById(R.id.bulk);
        	RadioButton web = (RadioButton) findViewById(R.id.web);
        	RadioButton streaming = (RadioButton) findViewById(R.id.streaming);
        	
        	if (bulk.isChecked()) {
        		try
	    		{
	    			Socket sock = new Socket(params[0], BULK_PORT);
	    			InputStream in = sock.getInputStream();
	    			OutputStream out = sock.getOutputStream();
	    			BufferedReader inReader = new BufferedReader(new InputStreamReader(in));
	    			PrintWriter outWriter = new PrintWriter(out, true);
	    			char[] inbuf = new char[INBUF_SIZE];
	        		int bytes_read = 0;
	        		long start_time = System.currentTimeMillis();
	    			while ((bytes_read = inReader.read(inbuf, 0, INBUF_SIZE)) > 0) {
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
	    			Log.e("DRWAClient", "Exception in do_onetime_test::doInBackground()", ex);
	    		}
        	} else if (web.isChecked()) {
        		// TBD
        	} else if (streaming.isChecked()) {
        		// TBD
        	}
        	
    		return throughput;
    	}
        
        protected void onPreExecute() {
        	super.onPreExecute();
      	     
        	// Disable user input
        	EditText server_ip = (EditText) findViewById(R.id.server_ip);
        	RadioGroup scenario = (RadioGroup) findViewById(R.id.scenario);
        	Button startstop = (Button) findViewById(R.id.automated);
        	Button onetime = (Button) findViewById(R.id.onetime);
        	server_ip.setEnabled(false);
 			for(int i = 0; i < scenario.getChildCount(); i++) {
 				((RadioButton) scenario.getChildAt(i)).setEnabled(false);
 	        }
  			onetime.setEnabled(false);
  			startstop.setEnabled(false);
        	
  			// Display progress bar
        	ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
        	progress.setVisibility(View.VISIBLE);
        }

        protected void onCancelled(Long throughput) {
        	// Hide progress bar
        	ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
       	 	progress.setVisibility(View.INVISIBLE);
       	 	
       	 	// Re-enable user input
       		EditText server_ip = (EditText) findViewById(R.id.server_ip);
       		RadioGroup scenario = (RadioGroup) findViewById(R.id.scenario);
       		Button startstop = (Button) findViewById(R.id.automated);
       		Button onetime = (Button) findViewById(R.id.onetime);
       	 	server_ip.setEnabled(true);
			for(int i = 0; i < scenario.getChildCount(); i++) {
				((RadioButton) scenario.getChildAt(i)).setEnabled(true);
	        }
 			onetime.setEnabled(true);
 			startstop.setEnabled(true);
        }
        
        protected void onPostExecute(Long throughput) {
        	super.onPostExecute(throughput);
        	
        	// Hide progress bar
        	ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
       	 	progress.setVisibility(View.INVISIBLE);
       	 	
       	 	// Re-enable user input
       		EditText server_ip = (EditText) findViewById(R.id.server_ip);
       		RadioGroup scenario = (RadioGroup) findViewById(R.id.scenario);
       		Button startstop = (Button) findViewById(R.id.automated);
       		Button onetime = (Button) findViewById(R.id.onetime);
       	 	server_ip.setEnabled(true);
			for(int i = 0; i < scenario.getChildCount(); i++) {
				((RadioButton) scenario.getChildAt(i)).setEnabled(true);
	        }
 			onetime.setEnabled(true);
 			startstop.setEnabled(true);
 			
 			// Show throughput
 			Toast toast = Toast.makeText(getApplicationContext(), print_througput(throughput), Toast.LENGTH_LONG);
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
    	if (!check_input())
			return;
	    
		// Run one-time test
    	EditText server_ip = (EditText) findViewById(R.id.server_ip);
    	onetime_test_task = new do_onetime_test();
    	onetime_test_task.execute(server_ip.getText().toString());
    }
    
    // Called when the user clicks the "Run/Stop automated test" button
    public void automated_test(View v) {
    	EditText server_ip = (EditText) findViewById(R.id.server_ip);
    	RadioGroup scenario = (RadioGroup) findViewById(R.id.scenario);
    	RadioButton chosen_scenario = (RadioButton) findViewById(scenario.getCheckedRadioButtonId());
    	Button startstop = (Button) findViewById(R.id.automated);
    	Button onetime = (Button) findViewById(R.id.onetime);

		if (startstop.getText().equals(getResources().getString(R.string.start_automated))) {
	    	if (!check_input())
				return;
	    	
	    	// Disable user input
			server_ip.setEnabled(false);
			for(int i = 0; i < scenario.getChildCount(); i++) {
				((RadioButton) scenario.getChildAt(i)).setEnabled(false);
	        }
			onetime.setEnabled(false);
			startstop.setText(getResources().getString(R.string.stop_automated));

			// Start automated test
			Intent intent = new Intent(this, DRWAClientService.class);
			intent.putExtra(DRWAClientService.SERVER_IP, server_ip.getText().toString());
			intent.putExtra(DRWAClientService.CHOSEN_SCENARIO, chosen_scenario.getText().toString());
			startService(intent);
	   	} else {
	    	// Stop automated test
			stopService(new Intent(this, DRWAClientService.class));
	
			// Re-enable user input
			server_ip.setEnabled(true);
			for(int i = 0; i < scenario.getChildCount(); i++) {
				((RadioButton) scenario.getChildAt(i)).setEnabled(true);
	        }
			onetime.setEnabled(true);
			startstop.setText(getResources().getString(R.string.start_automated));
	   	}
	}
}
