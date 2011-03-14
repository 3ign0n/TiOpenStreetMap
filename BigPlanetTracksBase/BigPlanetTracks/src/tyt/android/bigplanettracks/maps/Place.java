package tyt.android.bigplanettracks.maps;

import java.io.Serializable;

import android.location.Location;

public class Place implements Serializable{
	private static final long serialVersionUID = -6698333998587735262L;

	private String name;
	private double lat;
	private double lon;
	private Location location;
	private String address;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}
	
}
