package tyt.android.bigplanettracks.tracks.writer;

import java.io.IOException;

import tyt.android.bigplanettracks.tracks.db.TrackDBAdapter;

import android.location.Location;

public class GpxFile extends FileHandle {

	private String gpxFooter =
		"</trkseg>\n"+
		"</trk>\n"+
		"</gpx>";
	
	public GpxFile(String trackName, String trackDescription) throws IOException { 
		super(trackName);
		String gpxHeader = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<gpx\n"+
			" version=\"1.0\"\n"+
			" creator=\"Big Planet Tracks running on Android (http://android-map.blogspot.com)\"\n"+
			" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+
			" xmlns=\"http://www.topografix.com/GPX/1/0\"\n"+
			" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n"+
			"<trk>\n"+
			"<name><![CDATA["+trackName+"]]></name>\n"+
			"<desc><![CDATA["+trackDescription+"]]></desc>\n"+
			"<trkseg>\n";
		
		super.saveToFile(gpxHeader);
	}
	
	@Override
	public void closeFile() throws IOException{
		super.saveToFile(this.gpxFooter);
		super.closeFile();
	}
	
	public void saveLocation(Location loc) throws IOException {
		String strGMTTime = TrackDBAdapter.getGMTTimeString(loc);
		super.saveToFile(
				"<trkpt lat=\""+loc.getLatitude()+"\" lon=\""+loc.getLongitude()+"\">\n"+
				"<ele>"+loc.getAltitude()+"</ele>\n"+
				"<time>"+strGMTTime+"</time>\n" +
				"</trkpt>\n");
	}

}
