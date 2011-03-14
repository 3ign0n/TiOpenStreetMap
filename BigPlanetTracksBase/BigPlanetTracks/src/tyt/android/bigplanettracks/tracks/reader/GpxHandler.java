package tyt.android.bigplanettracks.tracks.reader;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tyt.android.bigplanettracks.tracks.MyTimeUtils;
import tyt.android.bigplanettracks.tracks.db.TrackDBAdapter;

import android.location.Location;
import android.util.Log;

/**
 * @author TYTung, taiyuchen
 * @version 0.1
 */
public class GpxHandler extends DefaultHandler {

	// ===========================================================
	// Fields
	// ===========================================================

	private boolean in_name_tag = false;
	private boolean in_desc_tag = false;
	boolean in_trkseg_tag = false;
	boolean in_trkpt_tag = false;
	private boolean in_time_tag = false;
	private boolean in_ele_tag = false;

	private ParsedDataSet myParsedDataSet = new ParsedDataSet();
	
	private Location location = null;
	private boolean isValidTimeFormat;
	private boolean isTimestampAdded;
	private String errorTimeString = "";

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public ParsedDataSet getParsedData() {
		return myParsedDataSet;
	}

	// ===========================================================
	// Methods
	// ===========================================================
	@Override
	public void startDocument() throws SAXException {
//		Log.i("Message:", "startDocument");
	}

	@Override
	public void endDocument() throws SAXException {
//		Log.i("Message:", "endDocument");
	}

	/**
	 * Gets be called on opening tags like: <tag> Can provide attribute(s), when
	 * xml was like: <tag attribute="attributeValue">
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attributes) throws SAXException {
		// super.startElement(namespaceURI, localName, qName, attributes);
		if (localName.equals("name")) {
			in_name_tag = true;
		} else if (localName.equals("desc")) {
			in_desc_tag = true;
		} else if (localName.equals("trkseg")) {
			in_trkseg_tag = true;
		} else if (localName.equals("trkpt")) {
			in_trkpt_tag = true;
			if (location == null)
				location = new Location("");
			location.setLatitude(Double.parseDouble(attributes.getValue("lat")));
			location.setLongitude(Double.parseDouble(attributes.getValue("lon")));
		} else if (localName.equals("time")) {
			in_time_tag = true;
		} else if (localName.equals("ele")) {
			in_ele_tag = true;
		} else {
//			Log.i("Messgae", "in other tag");
		}
	}

	/**
	 * Gets be called on closing tags like: </tag>
	 */
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (localName.equals("name")) {
			in_name_tag = false;
		} else if (localName.equals("desc")) {
			in_desc_tag = false;
		} else if (localName.equals("trkseg")) {
			in_trkseg_tag = false;
		} else if (localName.equals("trkpt")) {
			in_trkpt_tag = false;
			if (isValidTimeFormat) {
				myParsedDataSet.addLocation(location);
			} else {
				Log.i("Message", "Time format checking Not pass: " + errorTimeString);
			}
			location = null;
		} else if (localName.equals("time")) {
			in_time_tag = false;
		} else if (localName.equals("ele")) {
			in_ele_tag = false;
		}
	}

	/**
	 * Gets be called on the following structure: <tag>characters</tag>
	 */
	@Override
	public void characters(char[] ch, int start, int length) {
		if (in_name_tag) {
			String name = new String(ch, start, length);
			myParsedDataSet.setTrackName(name);
		} else if (in_desc_tag) {
			String desc = new String(ch, start, length);
			myParsedDataSet.setTrackDescription(desc);
		} else if (in_time_tag) {
			String strGMTTime = new String(ch, start, length);
			isValidTimeFormat = MyTimeUtils.isGMTTimeFormat(strGMTTime);
			if (isValidTimeFormat) {
				if (location == null)
					location = new Location("");
				TrackDBAdapter.setGMTTimeString(location, strGMTTime);
				long millisecond = MyTimeUtils.getGMTTime(strGMTTime);
				location.setTime(millisecond);
//				System.out.println(millisecond);
				
				if (!isTimestampAdded) {
					myParsedDataSet.setTrackStartGMTTime(strGMTTime);
					isTimestampAdded = true;
				}
			} else {
				errorTimeString = strGMTTime;
			}
		} else if (in_ele_tag) {
			String elevation = new String(ch, start, length);
			if (location == null)
				location = new Location("");
			location.setAltitude(Double.parseDouble(elevation));
		}
	}

}
