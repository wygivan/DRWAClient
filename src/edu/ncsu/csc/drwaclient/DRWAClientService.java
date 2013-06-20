package edu.ncsu.csc.drwaclient;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DRWAClientService extends Service {
	public static final String SERVER_IP = "SERVER_IP";
	public static final String UPDOWN = "UPDOWN";
	private final static int myID = 1234;
	private boolean isRunning = false;
	private Thread testThread;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String server_ip = intent.getStringExtra(SERVER_IP);
		String updown = intent.getStringExtra(UPDOWN);
		
		if (!isRunning) {
			testThread = new Thread(new AutomatedTestThread(server_ip, updown));
			testThread.start();
			
			isRunning = true;

		    Intent i = new Intent(this, MainActivity.class);
		    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		    PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

		    Notification note = new Notification(R.drawable.ic_launcher, "DRWA automated test is running", System.currentTimeMillis());
		    note.setLatestEventInfo(this, "DRWAClient", "DRWA automated test is running", pi);
		    note.flags|=Notification.FLAG_NO_CLEAR;
		    startForeground(myID, note);
		}
	    
		return(START_NOT_STICKY);
	}
	
	@Override
	public void onDestroy() {
		try {
			testThread.interrupt();
			testThread.join();
		} catch (Exception ex) {
			Log.e("DRWAClient", "Exception in DRWAClientService::onDestroy()", ex);
		}
		
		isRunning = false;
		stopForeground(true);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return(null);
	}
}