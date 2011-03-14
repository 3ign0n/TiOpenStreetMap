package tyt.android.bigplanettracks.maps.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tyt.android.bigplanettracks.maps.Place;

/**
 * Парсер списка найденных мест
 * @author hudvin
 */
public class GeoLocationHandler extends DefaultHandler {

	private static String CODE_TAG = "code";
	
	private static String PLACEMARK_TAG = "Placemark";
	
	private static String ADDRESS_TAG = "address";
	
	private static String COORDINATES_TAG = "coordinates";
	
	private static String LOCALITY_NAME_TAG = "LocalityName";
	
	private boolean isCodeTag = false;
	
	private boolean isAddressTag = false;
	
	private boolean isCoordinatesTag = false;
	
	private boolean isLocalityNameTag = false;
	
	private int responseCode = 0;
	
	private List<Place> places = new ArrayList<Place>();
	
	private Place currentPlace = new Place();
	
	@Override
	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		String elementName;
		if ("".equals(name)) {
			elementName = qName;
		} else {
			elementName = name;
		}
		if(elementName.equals(PLACEMARK_TAG)){
			currentPlace = new Place();
		}
		if(elementName.equals(CODE_TAG)){
			isCodeTag = true;
		}
		if(elementName.equals(ADDRESS_TAG)){
			isAddressTag = true;
		}
		if(elementName.equals(COORDINATES_TAG)){
			isCoordinatesTag = true;
		}
		if(elementName.equals(LOCALITY_NAME_TAG)){
			isLocalityNameTag = true;
		}
		
	}

	@Override
	public void endElement(String uri, String name, String qName)
			throws SAXException {
		if(name.equals(PLACEMARK_TAG)){
			places.add(currentPlace);
		}
		if(name.equals(ADDRESS_TAG)){
			isAddressTag = false;
		}
		if(name.equals(CODE_TAG)){
			isCodeTag = false;
		}
		if(name.equals(COORDINATES_TAG)){
			isCoordinatesTag = false;
		}
		if(name.equals(LOCALITY_NAME_TAG)){
			isLocalityNameTag = false;
		}
	}
	
	@Override
	public void characters(char ch[], int start, int length) {
		String chars = (new String(ch).substring(start, start + length));
		if(isCodeTag){
			responseCode = Integer.parseInt(chars);
		}
		if(isAddressTag){
			currentPlace.setAddress(chars);
		}
		if(isCoordinatesTag){
			String tokens[] =  chars.split(",");
			String lon = tokens[0];
			String lat = tokens[1];
			currentPlace.setLat(Double.parseDouble(lat));
			currentPlace.setLon(Double.parseDouble(lon));
			
		}
		if(isLocalityNameTag){
			currentPlace.setName(chars);
		}
	}

	public List<Place> getPlaces(){
		return this.places;
	}
	
	public int getResponseCode(){
		return this.responseCode;
	}
	
}
