package tyt.android.bigplanettracks.maps.geoutils;

import tyt.android.bigplanettracks.maps.RawTile;

public class GeoUtils {
	static int TILE_SIZE = 256;

	/**
	 * returns a Rectangle2D with x = lon, y = lat, width=lonSpan,
	 * height=latSpan for an x,y,zoom as used by google.
	 */
	public static Point getLatLong(int x, int y, int zoom) {
		double lon = -180; // x
		double lonWidth = 360; // width 360

		// double lat = -90; // y
		// double latHeight = 180; // height 180
		double lat = -1;
		double latHeight = 2;

		int tilesAtThisZoom = 1 << (17 - zoom);
		lonWidth = 360.0 / tilesAtThisZoom;
		lon = -180 + (x * lonWidth);
		latHeight = -2.0 / tilesAtThisZoom;
		lat = 1 + (y * latHeight);

		// convert lat and latHeight to degrees in a transverse mercator projection
		// note that in fact the coordinates go from about -85 to +85 not -90 to 90!
		latHeight += lat;
		latHeight = (2 * Math.atan(Math.exp(Math.PI * latHeight))) - (Math.PI / 2);
		latHeight *= (180 / Math.PI);

		lat = (2 * Math.atan(Math.exp(Math.PI * lat))) - (Math.PI / 2);
		lat *= (180 / Math.PI);

		latHeight -= lat;

		if (lonWidth < 0) {
			lon = lon + lonWidth;
			lonWidth = -lonWidth;
		}

		if (latHeight < 0) {
			lat = lat + latHeight;
			latHeight = -latHeight;
		}

		Point point = new Point();
		point.x = (int) lat;
		point.y = (int) lon;

		return point;
	}
	
	public static boolean isValid(RawTile tile){
		int tileCount = (int) Math.pow(2, 17 - tile.z);
		return (tile.x<tileCount && tile.y<tileCount);
	}
	
	/**
	 * Returns the pixel offset of a latitude and longitude within a single typical google tile.
	 * @param lat
	 * @param lng
	 * @param zoom
	 * @return
	 */
	public static Point getPixelOffsetInTile(double lat, double lng, int zoom) {
		Point pixelCoords = toZoomedPixelCoords(lat, lng, zoom);

		return new Point(pixelCoords.x % TILE_SIZE, pixelCoords.y % TILE_SIZE);
	}

	/**
	 * returns the lat/lng as an "Offset Normalized Mercator" pixel coordinate,
	 * this is a coordinate that runs from 0..1 in latitude and longitude with 0,0 being
	 * top left. Normalizing means that this routine can be used at any zoom level and
	 * then multiplied by a power of two to get actual pixel coordinates.
	 * @param lat in degrees
	 * @param lng in degrees
	 * @return
	 */
	public static Point toNormalisedPixelCoords(double lat, double lng) {
		// first convert to Mercator projection
		// first convert the lat lon to mercator coordintes.
		if (lng > 180) {
			lng -= 360;
		}

		lng /= 360;
		lng += 0.5;

		lat = 0.5 - ((Math.log(Math.tan((Math.PI / 4)
				+ ((0.5 * Math.PI * lat) / 180))) / Math.PI) / 2.0);

		return new Point(lng, lat);
	}

	/**
	 * returns a point that is a google tile reference for the tile containing
	 * the lat/lng and at the zoom level.
	 * 
	 * @param lat
	 * @param lng
	 * @param zoom
	 * @return
	 */
	public static Point toTileXY(double lat, double lng, int zoom) {
		Point normalised = toNormalisedPixelCoords(lat, lng);
		int scale = 1 << (17 - zoom);

		// can just truncate to integer, this looses the fractional
		// "pixel offset"
		return new Point((int) (normalised.x * scale), (int) (normalised.y * scale));
	}

	/**
	 * returns a point that is a google pixel reference for the particular
	 * lat/lng and zoom assumes tiles are 256x256.
	 * 
	 * @param lat
	 * @param lng
	 * @param zoom
	 * @return
	 */
	public static Point toZoomedPixelCoords(double lat, double lng, int zoom) {
		Point normalised = toNormalisedPixelCoords(lat, lng);
		double scale = (1 << (17 - zoom)) * TILE_SIZE;

		return new Point((int) (normalised.x * scale), (int) (normalised.y * scale));
	}

	public static String TileXYToQuadKey(int tileX, int tileY, int levelOfDetail) {
		StringBuilder quadKey = new StringBuilder();
		for (int i = levelOfDetail; i > 0; i--) {
			char digit = '0';
			int mask = 1 << (i - 1);
			if ((tileX & mask) != 0) {
				digit++;
			}
			if ((tileY & mask) != 0) {
				digit++;
				digit++;
			}
			quadKey.append(digit);
		}
		return quadKey.toString();
	}

}
