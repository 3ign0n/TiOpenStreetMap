package tyt.android.bigplanettracks.tracks.writer;

import java.util.ArrayList;

import tyt.android.bigplanettracks.BigPlanet;
import tyt.android.bigplanettracks.maps.storage.SQLLocalStorage;
import tyt.android.bigplanettracks.tracks.TrackTabViewActivity;
import tyt.android.bigplanettracks.tracks.db.TrackDBAdapter;

import android.database.Cursor;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author taiyuchen, TYTung
 * @version 0.1
 */
public class GpxTrackWriter {
	private ArrayList<Location> locationList = null;
	private GpxFile gpxFile = null;
	private long trackID=0;
	private Handler trackListViewHandler=null;
	public static final int EXPORT_SUCCESS=1;
	public static final int EXPORT_FAIL=0; 
	
	class myThread extends Thread {
		
		public void run() {
			
			Message m = null;
			if (trackID > 0) {
//				Log.i("Message", "Pick NO."+trackID+" DB record to export to the GPX File");
				try{
					//Retrive the track's attributes from Sqlite
					Cursor myCursor = BigPlanet.DBAdapter.getTrack(trackID);
					String trackName = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_name));
					String trackDescription = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_description));
					myCursor.close();
					if (trackName.equals(""))
						trackName = "NewTrack";
					
					locationList = new ArrayList<Location>();
					myCursor = BigPlanet.DBAdapter.getTrackPoints(trackID);
					for (int i=0; i < myCursor.getCount(); i++) {
						double latitude = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_latitude));
						double longitude = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_longitude)); 
						String strGMTTime = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_time));
						double altitude = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_altitude)); 
						
						Location location = new Location("");
						location.setLatitude(latitude);
						location.setLongitude(longitude);
						location.setAltitude(altitude);
						TrackDBAdapter.setGMTTimeString(location, strGMTTime);
						locationList.add(location);
						myCursor.moveToNext();
					}
					myCursor.close();
					
					// Need to check the capacity of the SD card before generating the GPX file
					
					// Generate the GPX File
					System.out.println("------------");
					Log.i("Message", "Generate GPX File...");
					gpxFile = new GpxFile(trackName, trackDescription);
					for (Location location: locationList) {
						gpxFile.saveLocation(location);
					}
					gpxFile.closeFile();
					Log.i("Message", "Finish...the GPX file exists in the directory: "+SQLLocalStorage.TRACK_PATH+"export/");
					System.out.println("------------");
					
					//sent message back to TrackListViewAvtivity handler
					String obj = "Success!";
					m = trackListViewHandler.obtainMessage(EXPORT_SUCCESS, 1, 1, obj);
					
					if (trackListViewHandler != null)
						trackListViewHandler.sendMessage(m);
					else
						throw new Error("trackListViewHandler is Null");
					
				} catch(Exception e) {
					//sent message back to TrackListViewAvtivity handler
					String obj = "Fail!";
					m = trackListViewHandler.obtainMessage(EXPORT_FAIL, 0, 1, obj.toString());
					if(trackListViewHandler != null)
						trackListViewHandler.sendMessage(m);
					else
						throw new Error("trackListViewHandler is Null");
					e.printStackTrace();
					
				} finally {
					TrackTabViewActivity.myTrackExportDialog.dismiss();
				}
			} else {
				//sent message back to TrackListViewAvtivity handler
				TrackTabViewActivity.myTrackExportDialog.dismiss();
				String obj = "Fail!";
					m = trackListViewHandler.obtainMessage(EXPORT_FAIL, 0, 1, obj.toString());
					if(trackListViewHandler != null)
						trackListViewHandler.sendMessage(m);
					else
						throw new Error("trackListViewHandler is Null");
				Log.e("GpxTrackWriter", "myTrackDBAdapter == null or trackID <= 0");
			}
		}// end of run
	}
	
	public void setHandler(Handler handler) {
		this.trackListViewHandler = handler;
	}
	
	public void setTrackID(long trackID) {
		this.trackID = trackID;
	}
	
	public void saveToFile() {
//		Log.i("Message", "Start GpxFile Export Thread");
		myThread exportThread = new myThread();
		exportThread.setName("Export Thread");
		exportThread.start();
	}
	
}
