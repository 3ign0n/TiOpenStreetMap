package tyt.android.bigplanettracks.maps.providers;

import java.util.HashMap;
import java.util.Map;

public class MapStrategyFactory {

	public static final int GOOGLE_VECTOR = 0;

	public static final int GOOGLE_SATELLITE = 1;

	public static final int GOOGLE_TERRAIN = 2;
	
	public static final int OPENSTREET_VECTOR = 4;

	public static Map<Integer, MapStrategy> strategies;
	static {
		strategies = new HashMap<Integer, MapStrategy>();
		strategies.put(GOOGLE_VECTOR, new GoogleVectorMapStrategy());
		strategies.put(GOOGLE_SATELLITE, new GoogleSatelliteMapStrategy());
		strategies.put(GOOGLE_TERRAIN, new GoogleTerrainMapStrategy());
		strategies.put(OPENSTREET_VECTOR, new OpenStreetMapStrategy());
	}

	private MapStrategyFactory() {
	}

	public static MapStrategy getStrategy(int sourceId) {
		return strategies.get(sourceId);
	}

}
