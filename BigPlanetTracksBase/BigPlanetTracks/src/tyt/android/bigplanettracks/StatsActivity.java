/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package tyt.android.bigplanettracks;

import java.util.ArrayList;
import java.util.Random;

import tyt.android.bigplanettracks.maps.MarkerManager;
import tyt.android.bigplanettracks.maps.StatsUtilities;
import tyt.android.bigplanettracks.tracks.TrackAnalyzer;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;

/**
 * An activity that displays track statistics to the user.
 *
 * @author Sandor Dornbush, (Modified by TYTung)
 */
public class StatsActivity extends Activity implements LocationListener {

	private LocationManager locationManager;
	private UIUpdateThread thread;
	private final StatsUtilities utils;
	private long startTime;
	private static ArrayList<Location> locationListForDebug;
	private boolean isDebug = false;
	
	/**
	 * A runnable for posting to the UI thread. Will update the total time field.
	 */
	private final Runnable updateResults = new Runnable() {
		public void run() {
			if (isDebug)
				generateRandomLocation(); // for debugging and testing
			if (BigPlanet.isGPSTracking) {
				startTime = BigPlanet.recordingTime;
				updateTotalTime();
				updateAllStats();
			}
		}
	};

	/**
	 * A thread that updates the total time field every second.
	 */
	private class UIUpdateThread extends Thread {
		
		private boolean isRunning = true;

		public UIUpdateThread() {
			super("Stats.UIUpdateThread");
		}

		@Override
		public void run() {
			while (BigPlanet.isGPSTracking && isRunning) {
				long sleeptime = 1000;
				runOnUiThread(updateResults);
				try {
					Thread.sleep(sleeptime);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		
		public void finish() {
			isRunning = false;
		}
	}

	public StatsActivity() {
		utils = new StatsUtilities(this);
		if (isDebug && locationListForDebug == null)
			locationListForDebug = new ArrayList<Location>();
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stats);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		utils.updateUnits();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if (thread != null) {
			thread.finish();
		}
		unregisterLocationListener();
	}

	@Override
	protected void onResume() {
		super.onResume();
		thread = new UIUpdateThread();
		thread.start();
		registerLocationListener();
		utils.setAllToUnknown();
		updateAllStats();
	}

	private void updateTotalTime() {
		utils.setTime(R.id.total_time_register, System.currentTimeMillis() - startTime);
	}
	
	/**
	 * Updates the given location fields (latitude, longitude, altitude) and all
	 * other fields.
	 * 
	 * @param loc
	 *            may be null (will set location fields to unknown)
	 */
	private void updateLocation(Location loc) {
		if (loc == null) {
			utils.setUnknown(R.id.elevation_register);
			utils.setUnknown(R.id.latitude_register);
			utils.setUnknown(R.id.longitude_register);
			utils.setUnknown(R.id.speed_register);
		} else {
			utils.setAltitude(R.id.elevation_register, loc.getAltitude());
			utils.setLatLong(R.id.latitude_register, loc.getLatitude());
			utils.setLatLong(R.id.longitude_register, loc.getLongitude());
			utils.setSpeed(R.id.speed_register, loc.getSpeed() * 3.6);
		}
	}

	private void updateAllStats() {
		ArrayList<Location> locationList;
		if (!isDebug)
			locationList = MarkerManager.getLocationList(MarkerManager.markersG);
		else
			locationList = locationListForDebug;
		if (locationList != null && locationList.size() > 1) {
			TrackAnalyzer analyzer = new TrackAnalyzer(null, null, null, locationList, "GPS");
			analyzer.analyze(false);
			double totalDistance = analyzer.getTotalDistance();
			double averageSpeed = analyzer.getAverageSpeed();
			double maxSpeed = analyzer.getMaximumSpeed();
			if (averageSpeed > maxSpeed)
				maxSpeed = averageSpeed;
			double minElevation = analyzer.getMinAltitude();
			double maxElevation = analyzer.getMaxAltitude();
			double elevationGain = maxElevation - minElevation;
			utils.setAllStats(totalDistance, averageSpeed, maxSpeed, minElevation, maxElevation, elevationGain);
		}
	}

	/**
	 * Registers to receive location updates from the GPS location provider.
	 */
	private void registerLocationListener() {
		if (locationManager != null) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					BigPlanet.minTime, BigPlanet.minDistance, this);
		}
	}

	/**
	 * Unregisters all location listener.
	 */
	private void unregisterLocationListener() {
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		updateLocation(location);
		updateAllStats();
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		int numSatellites = extras.getInt("satellites", 0);
		provider = provider+" "+status+" "+numSatellites;
		String title = BigPlanet.getTitle(provider);
		if (BigPlanet.titleHandler != null) {
			Message m = BigPlanet.titleHandler.obtainMessage(BigPlanetTracks.SetTitle, 0, 0, title);
			BigPlanet.titleHandler.sendMessage(m);
		}
	}

	/**
	 * Simulates the recording of a random location.
	 * This is for debugging and testing only. 
	 * Useful if there is no GPS signal available.
	 */
	private void generateRandomLocation() {
		final Random random = new Random();
		Location loc = new Location("gps");
		double latitude = 25.01736 + random.nextDouble() / 10000;
		double longitude = 121.54066 + random.nextDouble() / 10000;
		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		loc.setAltitude(random.nextDouble() * 100);
		loc.setTime(System.currentTimeMillis());
		loc.setSpeed(random.nextFloat());
		onLocationChanged(loc);
		locationListForDebug.add(loc);
	}
	
}
