package tyt.android.bigplanettracks.tracks.reader;

import java.util.ArrayList;

import android.location.Location;

/**
 * @author taiyuchen, TYTung
 * @version 0.1
 */
public class ParsedDataSet {

	private String trackName;
	private String trackDescription;
	private String trackStartGMTTime;
	private ArrayList<Location> locationList = null;

	public ParsedDataSet() {
		trackName = "";
		trackDescription = "";
		trackStartGMTTime = "";
		locationList = new ArrayList<Location>();
	}

	public void setTrackName(String trackName) {
		this.trackName = trackName;
	}

	public String getTrackName() {
		return trackName;
	}

	public void setTrackDescription(String trackDescription) {
		this.trackDescription = trackDescription;
	}

	public String getTrackDescription() {
		return trackDescription;
	}

	public String getTrackStartGMTTime() {
		return trackStartGMTTime;
	}

	public void setTrackStartGMTTime(String trackStartGMTTime) {
		this.trackStartGMTTime = trackStartGMTTime;
	}

	public void addLocation(Location trackPoint) {
		locationList.add(trackPoint);
	}

	public ArrayList<Location> getLocationList() {
		return locationList;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (locationList == null | locationList.size() == 0)
			return "is null or empty";
		else {
			sb.append("Track name=" + trackName + ", description=" + trackDescription + "\n");
			for (Location loc : getLocationList()) {
				sb.append("lat=" + loc.getLatitude() + 
						", lon=" + loc.getLongitude() + 
						", ele=" + loc.getAltitude() + 
						", time=" + loc.getTime() + "\n");
			}
			return sb.toString();
		}
	}
}
