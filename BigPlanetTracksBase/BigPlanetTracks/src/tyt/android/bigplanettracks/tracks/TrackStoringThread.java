package tyt.android.bigplanettracks.tracks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;

import tyt.android.bigplanettracks.maps.storage.SQLLocalStorage;
import tyt.android.bigplanettracks.tracks.db.TrackDBAdapter;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author taiyuchen, TYTung
 * @version 0.1
 */
public class TrackStoringThread extends Thread {
	private Handler mainThreadHandler = null;
	private ArrayList<Location> locationList = null;
	public static final int SUCCESS = 1;
	public static final int FAIL = 0;
	public static final int DialogDismiss = -1;
	
	@Override
	public void run() {
		Message m = null;
		try {
			if (locationList != null) {
				if (locationList.size()>1) { 
					for (int i=0; i<locationList.size(); i++) {
						Location loc = locationList.get(i);
						if (loc==null || loc.getLatitude()==0 || loc.getLongitude()==0) {
							Log.i("TrackStoringThread", "Location==null || Latitude==0 || Longitude()==0");
							locationList.remove(i);
						} else {
							String strGMTTime = MyTimeUtils.getGMTTimeString(loc.getTime());
//							System.out.println(strGMTTime);
							TrackDBAdapter.setGMTTimeString(loc, strGMTTime);
						}
					}
					
					String trackName = "";
					String trackDescription = "";
					String trackStartGMTTime = MyTimeUtils.getGMTTimeString(locationList.get(0).getTime());
		
					TrackAnalyzer analyzer = new TrackAnalyzer(trackName, trackDescription, trackStartGMTTime, locationList, "GPS");
					analyzer.analyzeAndSave();
					
					String obj = "Success!";
					m = mainThreadHandler.obtainMessage(SUCCESS, 1, 1, obj);
					mainThreadHandler.sendMessage(m);
					
				} else { // locationList.size() <= 1
					String obj = "LocationList.size() <= 1";
					m = mainThreadHandler.obtainMessage(FAIL, 0, 1, obj);
					mainThreadHandler.sendMessage(m);
				}
			
			} else { // locationList == null
				String obj = "LocationList is Null";
				m = mainThreadHandler.obtainMessage(FAIL, 0, 1, obj);
				mainThreadHandler.sendMessage(m);
			}
			
		} catch (Exception e) {
			String obj = "Some exceptions occur";
			Log.i("TrackStoringThread", "Save the error message into the file("+SQLLocalStorage.TRACK_PATH+"error_log/error.txt)");
			try {
				File error_file_path = new File(SQLLocalStorage.TRACK_PATH+"error_log/");
				if (!error_file_path.exists())
					error_file_path.mkdirs();
				File error_file = new File(SQLLocalStorage.TRACK_PATH+"error_log/error.txt");
				
				PrintWriter pw = new PrintWriter(new FileWriter(error_file));
				e.printStackTrace(pw);
				pw.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			e.printStackTrace();
			
			m = mainThreadHandler.obtainMessage(FAIL, 0, 1, obj);
			mainThreadHandler.sendMessage(m);
			
		} finally {
			m = mainThreadHandler.obtainMessage(DialogDismiss, 0, 0, null);
			mainThreadHandler.sendMessage(m);
		}
	}
	
	public void setLocationList(ArrayList<Location> locationList) {
		this.locationList = locationList;
	}
	
	public void setMainHandler(Handler handler) {
		mainThreadHandler = handler;
	}

}
