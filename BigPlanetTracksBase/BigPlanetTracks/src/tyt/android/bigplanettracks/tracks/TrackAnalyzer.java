package tyt.android.bigplanettracks.tracks;

import java.util.ArrayList;
import java.util.Collections;

import tyt.android.bigplanettracks.BigPlanet;

import android.location.Location;
import android.util.Log;

/**
 * @author TYTung
 * @version 0.2
 */
public class TrackAnalyzer {
	
	public static final int measureVersion = 1;
	
	private String trackName;
	private String trackDescription;
	private String startGMTTime;
	private ArrayList<Location> locationList;
	private	String trackSource;
	
	private ArrayList<Double> altitudeList;
	private ArrayList<Float> speedList;
	private long totalTime;
	private float totalDistance;
	private float averageSpeed;
	private float maximumSpeed;
	private double minAltitude;
	private double maxAltitude;
	private int trackPoints;
	
	public TrackAnalyzer(String trackName, String trackDescription, String startGMTTime, 
			ArrayList<Location> locationList, String trackSource) {
		this.trackName = trackName;
		this.trackDescription = trackDescription;
		this.startGMTTime = startGMTTime;
		this.locationList = locationList;
		this.trackSource = trackSource;
		
		altitudeList = new ArrayList<Double>();
		speedList = new ArrayList<Float>();
//		totalTime = 0;
//		totalDistance = 0f;
//		averageSpeed = 0f;
//		maximumSpeed = 0f;
//		minAltitude = 0d;
//		maxAltitude = 0d;
		trackPoints = locationList.size();
	}
	
	public TrackAnalyzer(String trackName, String trackDescription, String startGMTTime, 
			ArrayList<Location> locationList) {
		this(trackName, trackDescription, startGMTTime, locationList, null);
	}
	
	public void analyze(boolean hasLog) {
		if (hasLog)
			Log.i("Message", "Perform TrackAnalyzer");
		if (locationList.size() > 1) {
			computeTotalTime();
			computeTotalDistance();
			computeAverageSpeed();
			computeMaximumSpeed();
			computeAltitude();
		}
		if (hasLog) {
			Log.i("Message", "totalTime="+MyTimeUtils.getTimeString(totalTime));
			Log.i("Message", "totalDistance="+totalDistance+"m");
			Log.i("Message", "averageSpeed="+averageSpeed+"km/hr");
			Log.i("Message", "maximumSpeed="+maximumSpeed+"km/hr");
			Log.i("Message", "minAltitude="+minAltitude+"m");
			Log.i("Message", "maxAltitude="+maxAltitude+"m");
			Log.i("Message", "trackPoints="+trackPoints);
		}
	}
	
	public void analyzeAndUpdate(long trackID) {
		Log.i("Message", "trackName="+trackName);
		Log.i("Message", "trackDescription="+trackDescription);
		Log.i("Message", "trackStartGMTTime="+startGMTTime);
		analyze(true);
		BigPlanet.DBAdapter.open();
		BigPlanet.DBAdapter.updateTrack(trackID, totalTime, totalDistance, 
				averageSpeed, maximumSpeed, minAltitude, maxAltitude, trackPoints, measureVersion);
		Log.i("Message", "measureVersion has been updated: "+measureVersion);
	}

	public void analyzeAndSave() {
		Log.i("Message", "trackName="+trackName);
		Log.i("Message", "trackDescription="+trackDescription);
		Log.i("Message", "trackStartGMTTime="+startGMTTime);

		BigPlanet.DBAdapter.open();
		long trackID = BigPlanet.DBAdapter.insertTrack(trackName, trackDescription, startGMTTime, locationList, trackSource);
		Log.i("Message", "insertTrack() finished");
		
		analyze(true);
		
		BigPlanet.DBAdapter.updateTrack(trackID, totalTime, totalDistance, 
				averageSpeed, maximumSpeed, minAltitude, maxAltitude, trackPoints, measureVersion);
		Log.i("Message", "Insert a new track successfully");
		Log.i("Message", "-------------------------------");
	}
	
	private void computeTotalTime() {
		Location firstLocation = locationList.get(0);
		Location lastLocation = locationList.get(locationList.size()-1);
		long firstTimePoint = firstLocation.getTime();
		long lastTimePoint = lastLocation.getTime();
		totalTime = lastTimePoint-firstTimePoint;
		if (totalTime < 0)
			totalTime = 0;
	}
	
	private void computeTotalDistance() {
		totalDistance = 0;
		float speed;
		for (int i=0; i<locationList.size(); i++) {
			Location location = locationList.get(i);
			altitudeList.add(location.getAltitude());
			if (i >= 1) {
				Location previous_location = locationList.get(i-1);
				float distance = location.distanceTo(previous_location);
//				if (distance <= 10) {
					totalDistance = totalDistance + distance;
//				} else {
//					totalDistance = totalDistance + 0;
//				}
				long time = location.getTime()-previous_location.getTime();
//				System.out.println("ms M: "+time+"\t"+distance);
				if (distance == 0 || time == 0) {
					speed = 0f;
				} else {
					speed = (distance / (time/1000)) *3600/1000; // km/hr
				}
				speedList.add(speed);
			}
		}
		// filter out the possible incorrect speed
		ArrayList<Float> candicateSpeedList = new ArrayList<Float>();
		for (int i=0; i<speedList.size(); i++) {
//			System.out.println(i+"\t"+speedList.get(i));
			if (i >= 2) {
				float nextSpeed = speedList.get(i);
				float previousSpeed = (speedList.get(i-1)+speedList.get(i-2))/2;
				if (Math.abs(nextSpeed - previousSpeed) < 1.5) { // magic number
					candicateSpeedList.add(nextSpeed);
				}
			}
		}
		if (candicateSpeedList.size() > 0) {
			speedList.clear();
			speedList.addAll(candicateSpeedList);
		}
	}
	
	private void computeAverageSpeed() {
		if (totalTime > 0)
			averageSpeed = (totalDistance / (totalTime/1000)) *3600/1000; // km/hr
		else
			averageSpeed = 0;
	}
	
	private void computeMaximumSpeed() {
		if (speedList.size() > 0) {
			Collections.sort(speedList);
			maximumSpeed = speedList.get(speedList.size()-1);
		} else {
			maximumSpeed = 0;
		}
	}
	
	private void computeAltitude() {
		if (altitudeList.size() > 0) {
			Collections.sort(altitudeList);
			minAltitude = altitudeList.get(0);
			maxAltitude = altitudeList.get(altitudeList.size()-1);
		}
	}
	
	public long getTotalTime() { 
		return totalTime;
	}
	
	public float getTotalDistance() {
		return totalDistance;
	}
	
	public float getAverageSpeed() {
		return averageSpeed;
	}
	
	public float getMaximumSpeed() {
		return maximumSpeed;
	}
	
	public double getMinAltitude() {
		return minAltitude;
	}
	
	public double getMaxAltitude() {
		return maxAltitude;
	}
	
	public int getTrackPoints() {
		return locationList.size();
	}
	
}
