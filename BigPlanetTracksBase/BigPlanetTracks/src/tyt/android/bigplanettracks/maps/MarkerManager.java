package tyt.android.bigplanettracks.maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import tyt.android.bigplanettracks.BigPlanet;
import tyt.android.bigplanettracks.R;
import tyt.android.bigplanettracks.maps.geoutils.GeoUtils;
import tyt.android.bigplanettracks.maps.geoutils.Point;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

public class MarkerManager {

	public static final int MY_LOCATION_MARKER = 0;
	public static final int BOOKMARK_MARKER = 1;
	public static final int SEARCH_MARKER = 2;
	public static final int START_GREEN_MARKER = 3;
	public static final int END_GREEN_MARKER = 4;
	public static final int START_BLUE_MARKER = 5;
	public static final int END_BLUE_MARKER = 6;
	
	public static int DrawMarkerForSearch = 0;
	public static int DrawMarkerOrTrack = 1;
	public static int DrawTrackFromDB = 2;
	
	//private HashMap<Integer,MarkerImage> images = new HashMap<Integer,MarkerImage>();
	public static HashMap<Integer,MarkerImage> images = new HashMap<Integer,MarkerImage>();
		
	// private List<Marker> markers = new ArrayList<Marker>();
	public static List<Marker> markers = new ArrayList<Marker>();
	public static List<Marker> markersG = new ArrayList<Marker>();
	public static List<Marker> savedTrackG = new ArrayList<Marker>();
	public static List<Marker> markersDB = new ArrayList<Marker>();
	
	private Resources resources;
	
	public MarkerManager(Resources resources){	
		this.resources = resources;
		images.put(MY_LOCATION_MARKER, new MarkerImage(decodeBitmap(R.drawable.person),24,39));
		images.put(BOOKMARK_MARKER, new MarkerImage(decodeBitmap(R.drawable.bookmark_marker),12,32));
		images.put(SEARCH_MARKER, new MarkerImage(decodeBitmap(R.drawable.location_marker),12,32));	
		images.put(START_BLUE_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_blue_startpoint),17,36));	
		images.put(END_BLUE_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_blue_endpoint),17,36));
		images.put(START_GREEN_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_green_startpoint),17,36));	
		images.put(END_GREEN_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_green_endpoint),17,36));
	}
	
	public void clearMarkerManager() {
		markers.clear();
	}
	
	// вызывается при зуммировании, пересчет отступа и координат тайла всех маркеров
	public void updateCoordinates(int z){
		List<List<Marker>> allMarkerList = new ArrayList<List<Marker>>();
		allMarkerList.add(markers);
		allMarkerList.add(markersDB);
		allMarkerList.add(markersG);
		allMarkerList.add(savedTrackG);
		for (int m = 0; m < allMarkerList.size(); m++) {
			for (Marker marker : allMarkerList.get(m)){
				Point tileXY = GeoUtils.toTileXY(marker.place.getLat(), marker.place.getLon(), z);
				Point offsetXY = GeoUtils.getPixelOffsetInTile(marker.place.getLat(), marker.place.getLon(), z);
				marker.offset = offsetXY;
				marker.tile.x = (int) tileXY.x;
				marker.tile.y = (int) tileXY.y;
				marker.tile.z = z;
			}
		}
	}
	
	public void addMarker(Place place, int zoom, int trackType, int imageType){
		/* trackType:
		 * 0 -> DrawMarkerForSearch, 
		 * 1 -> DrawMarkerOrTrack, 
		 * 2 -> DrawTrackFromDB
		 */
		boolean isGPS;
		if (trackType == DrawMarkerForSearch) {
			isGPS = false;
		} else {
			isGPS = true;
		}
		
		if(trackType == DrawMarkerForSearch){
			Marker marker = new Marker(place, images.get(imageType), isGPS);
			updateParams(marker, zoom);
			markers.add(marker);
		}
		else if(trackType == DrawMarkerOrTrack){
			Marker marker = new Marker(place, images.get(imageType), isGPS);
			updateParams(marker, zoom);
			if(BigPlanet.isGPSTracking){
				if (markersG.size()>0){
					double lat = markersG.get(markersG.size()-1).place.getLat();
					double lon = markersG.get(markersG.size()-1).place.getLon();
					if ((marker.place.getLat() != lat) || (marker.place.getLon() != lon)){
						markersG.add(marker);
					}
				}else{
					// filter out currentLocationBeforeRecording
					Location location = BigPlanet.currentLocationBeforeRecording;
					if (location != null) {
						double lat = location.getLatitude();
						double lon = location.getLongitude();
						if ((marker.place.getLat() != lat) || (marker.place.getLon() != lon)) {
							markersG.add(marker);
							BigPlanet.currentLocationBeforeRecording = null;
						}
					}
				}
			}
			
			Iterator<Marker> it = markers.iterator();
			while(it.hasNext()){
				Marker m = it.next();
				if(m.isGPS){
					it.remove();
				}
			}
			//if(!BigPlanet.isGPSTracking){
				if(markers.size()>0){
					double lat = markers.get(0).place.getLat();
					double lon = markers.get(0).place.getLon();
					if ((marker.place.getLat() != lat) || (marker.place.getLon() != lon)){
						markers.add(marker);
					}	
				}else{
					markers.add(marker);
				}
			//}
		}
		else if(trackType == DrawTrackFromDB){
			Marker marker_DB = new Marker(place, images.get(imageType), isGPS);
			updateParams(marker_DB, zoom);
			// clear lastTime DB
			if(BigPlanet.isDBdrawclear){
				markersDB.clear();
				BigPlanet.isDBdrawclear = false;
			}
			markersDB.add(marker_DB);
		}
	}
	
	public void updateParams(Marker marker, int zoom){
		Point tileXY = GeoUtils.toTileXY(marker.place.getLat(), marker.place.getLon(), zoom);
		RawTile mTile = new RawTile((int)tileXY.x, (int)tileXY.y, zoom, -1);
		marker.tile = mTile;
		Point offset = GeoUtils.getPixelOffsetInTile(marker.place.getLat(), marker.place.getLon(), zoom);
		marker.offset = offset;
	}
	
	public void updateAll(int zoom){
		for(Marker marker : markers){
			updateParams(marker, zoom);
		}
		for(Marker marker : markersG){
			updateParams(marker, zoom);
		}
		for(Marker saveTrack_G : savedTrackG){
			updateParams(saveTrack_G, zoom);
		}
		for(Marker marker_DB : markersDB){
			updateParams(marker_DB, zoom);
		}
	}
	
	public List<Marker> getMarkers(int x, int y, int z){
		List<Marker> result = new ArrayList<Marker>();
		for(Marker marker:markers){
			if(marker.tile.x == x && marker.tile.y == y && marker.tile.z == z){
				result.add(marker);
			}
		}
		return result;
	}
	
	public boolean isDrawingMarkerG(int x, int y, int z, Marker marker) {
		boolean result = false;
		if (marker.tile.x == x && marker.tile.y == y && marker.tile.z == z) {
			result = true;
		}
		return result;
	}
	
	public void saveMarkerGTrack() {
		savedTrackG.clear();
		for (int i=0; i<markersG.size(); i++) {
			savedTrackG.add(markersG.remove(i));
		}
		markersG.clear();
	}
	
	public boolean clearSavedTracksG(){
		if(!BigPlanet.isGPSTracking){
			savedTrackG.clear();
			BigPlanet.isGPSTrackSaved = false;
			return true;
		}else{
			return false;
		}
	}
	
	public void clearMarkersDB(){
		markersDB.clear();
	}
	
	public static ArrayList<Location> getLocationList(List<Marker> markerList){
		ArrayList<Location> list = new ArrayList<Location>();
		for (int i=0; i<markerList.size(); i++) {
			Location location = markerList.get(i).place.getLocation();
			if (location != null)
				list.add(location);
		}
		return list;
	}
	
	private Bitmap decodeBitmap(int resourceId){
		return BitmapFactory.decodeResource(resources, resourceId);
	}
	
	public static class MarkerImage{
		
		private Bitmap image;
		private int offsetX;
		private int offsetY;
		
		public MarkerImage(Bitmap image, int offsetX, int offsetY){
			this.image = image;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
		
		public Bitmap getImage(){
			return this.image;
		}
		
		public int getOffsetX(){
			return this.offsetX;
		}
		
		public int getOffsetY(){
			return this.offsetY;
		}
	}
	
	public class Marker {
		
		public Place place;
		public RawTile tile;
		public Point offset;
		private boolean isGPS;
		private MarkerImage markerImage;
		
		public Marker(Place place, MarkerImage markerImage, boolean isGPS){
			this.place = place;	
			this.isGPS = isGPS;
			this.markerImage = markerImage;
		}
		
		public Point getOffset(){
			return this.offset;
		}
		
		public MarkerImage getMarkerImage(){
			return this.markerImage;
		}
		
		public void setMarkerImage(MarkerImage markerImage){
			this.markerImage = markerImage;
		}
	}
	
}
