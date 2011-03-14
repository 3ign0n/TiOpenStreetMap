package tyt.android.bigplanettracks.tracks.db;

import java.util.ArrayList;

import tyt.android.bigplanettracks.maps.storage.SQLLocalStorage;
import tyt.android.bigplanettracks.tracks.TrackAnalyzer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.os.Bundle;

/**
 * @author TYTung, taiyuchen
 * @version 0.1
 */
public class TrackDBAdapter {
	
	/**
	 * TRACKS_TABLE
	 */
	protected final static String TRACKS_TABLE = "tracks";
	public final static String FIELD_trackid = "trackid"; // column 0 PK
	public final static String FIELD_name = "name"; // column 1
	public final static String FIELD_description = "description";// column 2
	public final static String FIELD_startTime = "start_time"; // column 3
	public final static String FIELD_totalTime = "total_time"; // column 4
	public final static String FIELD_totalDistance = "total_distance"; // column 5
	public final static String FIELD_averageSpeed = "average_speed"; // column 6
	public final static String FIELD_maximumSpeed = "maximum_speed"; // column 7
	public final static String FIELD_minAltitude = "min_altitude"; // column 8
	public final static String FIELD_maxAltitude = "max_altitude"; // column 9
	public final static String FIELD_trackPoints = "points"; // column 10
	public final static String FIELD_trackSource = "source"; // column 11
	public final static String FIELD_measureVersion = "measure_version"; // column 12
	
	/**
	 * WAYPOINTS_TABLE
	 */
	private final static String WAYPOINTS_TABLE = "waypoints";
	public final static String FIELD_pointid = "pointid"; // column 0 PK
//	public final static String FIELD_trackid = "trackid"; // column 1 FK
	public final static String FIELD_time = "time";// column 2
	public final static String FIELD_latitude = "latitude";// column 3
	public final static String FIELD_longitude = "longitude";// column 4
	public final static String FIELD_altitude = "altitude";// column 5
	public final static String FIELD_speed = "speed";// column 6
	public final static String FIELD_bearing = "bearing";// column 7
	public final static String FIELD_accuracy = "accuracy";// column 8
	
	private final static String GMTTime = "GMTTime";
	
	private final static String DATABASE_NAME = "tracks.db";
	
	protected static final String TRACKS_TABLE_DDL ="CREATE TABLE IF NOT EXISTS " + TRACKS_TABLE + " (" 
	+ FIELD_trackid + " INTEGER primary key autoincrement, " 
	+ " "+ FIELD_name + " VARCHAR,"
	+ " "+ FIELD_description + " VARCHAR, "
	+ " "+ FIELD_startTime + " CHAR(20), "
	+ " "+ FIELD_totalTime + " LONG, "
	+ " "+ FIELD_totalDistance + " FLOAT, "
	+ " "+ FIELD_averageSpeed + " FLOAT, "
	+ " "+ FIELD_maximumSpeed + " FLOAT, "
	+ " "+ FIELD_minAltitude + " DOUBLE, "
	+ " "+ FIELD_maxAltitude + " DOUBLE, "
	+ " "+ FIELD_trackPoints + " INTEGER, "
	+ " "+ FIELD_trackSource + " CHAR(4), "
	+ " "+ FIELD_measureVersion + " INTEGER);";

	private static final String TRACK_POINTS_TABLE_DDL ="CREATE TABLE IF NOT EXISTS " + WAYPOINTS_TABLE + " (" 
	+ FIELD_pointid + " INTEGER primary key autoincrement, " 
	+ " "+ FIELD_trackid + " INTEGER NOT NULL,"
	+ " "+ FIELD_time + " CHAR(20), "
	+ " "+ FIELD_latitude + " DOUBLE DEFAULT '0', "
	+ " "+ FIELD_longitude + " DOUBLE DEFAULT '0', "
	+ " "+ FIELD_altitude + " DOUBLE DEFAULT '0', "
	+ " "+ FIELD_speed + " FLOAT DEFAULT '0', "
	+ " "+ FIELD_bearing + " FLOAT DEFAULT '0', "
	+ " "+ FIELD_accuracy + " FLOAT DEFAULT '0');";

	private SQLiteDatabase db;
	
	//---opens the database---
	public TrackDBAdapter open() throws SQLException {
		if (db == null) {
			String sqliteFilePath = SQLLocalStorage.TRACK_PATH + DATABASE_NAME;
			db = SQLiteDatabase.openDatabase(sqliteFilePath, null,
					SQLiteDatabase.CREATE_IF_NECESSARY);
			
//			System.out.println("db.getVersion() = "+db.getVersion());
			if (db.getVersion() < 1) {
				db.beginTransaction();
				try {
					// has database of BPT 2.0 and update to BPT 2.1
					db.execSQL(SQLConstants.SQL_UPDATE_1_1);
					db.execSQL(SQLConstants.SQL_UPDATE_1_2);
					db.execSQL(SQLConstants.SQL_UPDATE_1_3);
					db.execSQL(TRACKS_TABLE_DDL);
					db.execSQL(SQLConstants.SQL_UPDATE_1_5);
					db.execSQL(SQLConstants.SQL_UPDATE_1_6);
					db.execSQL(SQLConstants.SQL_UPDATE_1_7);
					db.setTransactionSuccessful();
				} catch (SQLiteException e) {
					// no database of BPT 2.0 (i.e. new installation)
					db.execSQL(TRACKS_TABLE_DDL);
					db.execSQL(TRACK_POINTS_TABLE_DDL);
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
					db.setVersion(1);
					System.out.println("Database has upgraded to version "+db.getVersion());
				}
			} else {
				db.execSQL(TRACKS_TABLE_DDL);
				db.execSQL(TRACK_POINTS_TABLE_DDL);
			}
		}
		return this;
	}
	
	//---closes the database---	
	public void close() {
		if (db != null) {
			db.close();
			db = null;
		}
	}
	
	public long insertTrack(String trackName, String trackDescription, String startGMTTime, 
			ArrayList<Location> locationList, String trackSource) {
		long trackID = 0;
		db.beginTransaction();
		try {
			ContentValues cv = new ContentValues();
			cv.put(FIELD_name, trackName);
			cv.put(FIELD_description, trackDescription);
			cv.put(FIELD_startTime, startGMTTime);
			cv.put(FIELD_trackSource, trackSource);
			trackID = db.insert(TRACKS_TABLE, null, cv);
			
			for (Location loc: locationList) {
				String strGMTTime = "";
				if (loc.getExtras() != null)
					strGMTTime = loc.getExtras().getString(GMTTime);
				cv = new ContentValues();
				cv.put(FIELD_trackid, trackID);
				cv.put(FIELD_time, strGMTTime);
				cv.put(FIELD_latitude, loc.getLatitude());
				cv.put(FIELD_longitude, loc.getLongitude());
				cv.put(FIELD_altitude, loc.getAltitude());
				cv.put(FIELD_speed, loc.getSpeed());
				cv.put(FIELD_bearing, loc.getBearing());
				cv.put(FIELD_accuracy, loc.getAccuracy());
				db.insert(WAYPOINTS_TABLE, null, cv);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return trackID;
	}
	
	//---deletes a particular track---
	public boolean deleteTrack(long rowId) {
		int rowsAffected = 0;
		db.beginTransaction();
		try {
			rowsAffected = db.delete(WAYPOINTS_TABLE, FIELD_trackid + "=" + rowId, null);
			rowsAffected = db.delete(TRACKS_TABLE, FIELD_trackid + "=" + rowId, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return rowsAffected > 0;
	}
	
	//---retrieves all the tracks---
	public Cursor getAllTracks() {
		return db.query(TRACKS_TABLE, new String[] {
				FIELD_trackid, 
				FIELD_name,
				FIELD_description,
				FIELD_startTime,
				FIELD_totalTime,
				FIELD_totalDistance,
				FIELD_averageSpeed,
				FIELD_maximumSpeed,
				FIELD_minAltitude,
				FIELD_maxAltitude,
				FIELD_trackPoints,
				FIELD_trackSource,
				FIELD_measureVersion }, 
				null, 
				null, 
				null, 
				null, 
				FIELD_trackid + " DESC");
	}
	
	public boolean checkIfDBHasTracks() {
		boolean result = false;
		Cursor mCursor = db.rawQuery("select count("+FIELD_trackid+")"+" from "+TRACKS_TABLE, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
			result = mCursor.getFloat(0) > 0;
			mCursor.close();
		}
		return result;
	}
	
	//---retrieves a particular track---
	public Cursor getTrack(long rowId) throws SQLException {
		Cursor mCursor = db.query(true, TRACKS_TABLE, new String[] {
						FIELD_trackid, 
						FIELD_name,
						FIELD_description,
						FIELD_startTime,
						FIELD_totalTime,
						FIELD_totalDistance,
						FIELD_averageSpeed,
						FIELD_maximumSpeed,
						FIELD_minAltitude,
						FIELD_maxAltitude,
						FIELD_trackPoints,
						FIELD_trackSource,
						FIELD_measureVersion
						}, 
						FIELD_trackid + "=" + rowId, 
						null,
						null, 
						null, 
						null, 
						null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	//---updates a track---
	public boolean updateTrack(long trackID, String trackName, String trackDescription) {
		ContentValues cv = new ContentValues();
		cv.put(FIELD_name, trackName);
		cv.put(FIELD_description, trackDescription);
		return db.update(TRACKS_TABLE, cv, FIELD_trackid + "=" + trackID, null) > 0;
	}
	
	public boolean updateTrack(long trackID, long totalTime, float totalDistance, 
			float averageSpeed, float maximumSpeed, double minAltitude, double maxAltitude, 
			long trackPoints, int measureVersion) {
		ContentValues cv = new ContentValues();
		cv.put(FIELD_totalTime, totalTime);
		cv.put(FIELD_totalDistance, totalDistance);
		cv.put(FIELD_averageSpeed, averageSpeed);
		cv.put(FIELD_maximumSpeed, maximumSpeed);
		cv.put(FIELD_minAltitude, minAltitude);
		cv.put(FIELD_maxAltitude, maxAltitude);
		cv.put(FIELD_trackPoints, trackPoints);
		cv.put(FIELD_measureVersion, measureVersion);
		return db.update(TRACKS_TABLE, cv, FIELD_trackid + "=" + trackID, null) > 0;
	}
	
	//---retrieves all trackPoints---
	public Cursor getTrackPoints(long rowId) throws SQLException {
		Cursor mCursor = db.query(false, WAYPOINTS_TABLE, new String[] {
						FIELD_trackid, 
						FIELD_time,
						FIELD_latitude,
						FIELD_longitude,
						FIELD_altitude,
						FIELD_speed,
						FIELD_bearing,
						FIELD_accuracy,
						}, 
						FIELD_trackid + "=" + rowId, 
						null,
						null, 
						null, 
						null, 
						null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public static void setGMTTimeString(Location location, String strGMTTime) {
		Bundle bundle = new Bundle();
		bundle.putString(GMTTime, strGMTTime);
		location.setExtras(bundle);
	}
	
	public static String getGMTTimeString(Location location) {
		Bundle bundle = location.getExtras();
		String strGMTTime = bundle.getString(GMTTime);
		return strGMTTime;
	}
	
	public boolean isMeasureUpdated(long trackID) {
		boolean result = false;
		Cursor mCursor = db.rawQuery("select "+FIELD_measureVersion+" from "+TRACKS_TABLE+" where "+FIELD_trackid+"="+trackID, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		int measureVersion = mCursor.getInt(0);
		mCursor.close();
		if (TrackAnalyzer.measureVersion > measureVersion)
			result = true;
		return result;
	}
	
}
