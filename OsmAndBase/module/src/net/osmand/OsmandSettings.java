package net.osmand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;


import net.osmand.activities.OsmandApplication;
import net.osmand.activities.RouteProvider.RouteService;
import net.osmand.activities.search.SearchHistoryHelper;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.osm.LatLon;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

public class OsmandSettings {

	public enum ApplicationMode {
		/*
		 * DEFAULT("Default"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
		 */

		DEFAULT(R.string.app_mode_default), 
		CAR(R.string.app_mode_car), 
		BICYCLE(R.string.app_mode_bicycle), 
		PEDESTRIAN(R.string.app_mode_pedestrian);

		private final int key;
		
		ApplicationMode(int key) {
			this.key = key;
		}
		public static String toHumanString(ApplicationMode m, Context ctx){
			return ctx.getResources().getString(m.key);
		}

	}

	public enum DayNightMode {
		AUTO(R.string.daynight_mode_auto), 
		DAY(R.string.daynight_mode_day), 
		NIGHT(R.string.daynight_mode_night),
		SENSOR(R.string.daynight_mode_sensor);

		private final int key;
		
		DayNightMode(int key) {
			this.key = key;
		}
		
		public  String toHumanString(Context ctx){
			return ctx.getResources().getString(key);
		}

		public boolean isSensor() {
			return this == SENSOR;
		}

		public boolean isAuto() {
			return this == AUTO;
		}

		public boolean isDay() {
			return this == DAY;
		}

		public boolean isNight() {
			return this == NIGHT;
		}
		
		public static DayNightMode[] possibleValues(Context context) {
	         SensorManager mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);         
	         Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	         if (mLight != null) {
	        	 return DayNightMode.values();
	         } else {
	        	 return new DayNightMode[] { AUTO, DAY, NIGHT };
	         }
		}

	}

	// These settings are stored in SharedPreferences
	public static final String SHARED_PREFERENCES_NAME = "net.osmand.settings"; //$NON-NLS-1$

	public static final int CENTER_CONSTANT = 0;
	public static final int BOTTOM_CONSTANT = 1;
	
	public static final Editor getWriteableEditor(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit();
	}
	
	public static final SharedPreferences getSharedPreferences(Context ctx){
		return ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
	}
	
	public static final SharedPreferences getPrefs(Context ctx){
		return ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_INTERNET_TO_DOWNLOAD_TILES = "use_internet_to_download_tiles"; //$NON-NLS-1$
	public static final boolean USE_INTERNET_TO_DOWNLOAD_TILES_DEF = true;
	private static Boolean CACHE_USE_INTERNET_TO_DOWNLOAD_TILES = null;
	private static long lastTimeInternetConnectionChecked = 0;
	private static boolean internetConnectionAvailable = true;

	public static boolean isUsingInternetToDownloadTiles(SharedPreferences prefs) {
		if(CACHE_USE_INTERNET_TO_DOWNLOAD_TILES == null){
			CACHE_USE_INTERNET_TO_DOWNLOAD_TILES = prefs.getBoolean(USE_INTERNET_TO_DOWNLOAD_TILES, USE_INTERNET_TO_DOWNLOAD_TILES_DEF);
		}
		return CACHE_USE_INTERNET_TO_DOWNLOAD_TILES;
	}
	
	public static void setUseInternetToDownloadTiles(boolean use, Editor edit) {
		edit.putBoolean(USE_INTERNET_TO_DOWNLOAD_TILES, use);
		CACHE_USE_INTERNET_TO_DOWNLOAD_TILES = use;
	}
	
	public static boolean isInternetConnectionAvailable(Context ctx){
		long delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked;
		if(delta < 0 || delta > 15000){
			ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo active = mgr.getActiveNetworkInfo();
			if(active == null){
				internetConnectionAvailable = false;
			} else {
				NetworkInfo.State state = active.getState();
				internetConnectionAvailable = state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
			}
		}
		return internetConnectionAvailable;
	}
	

	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_TRACKBALL_FOR_MOVEMENTS = "use_trackball_for_movements"; //$NON-NLS-1$
	public static final boolean USE_TRACKBALL_FOR_MOVEMENTS_DEF = true;

	public static boolean isUsingTrackBall(SharedPreferences prefs) {
		return prefs.getBoolean(USE_TRACKBALL_FOR_MOVEMENTS, USE_TRACKBALL_FOR_MOVEMENTS_DEF);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_HIGH_RES_MAPS = "use_high_res_maps"; //$NON-NLS-1$
	public static final boolean USE_HIGH_RES_MAPS_DEF = false;

	public static boolean isUsingHighResMaps(SharedPreferences prefs) {
		return prefs.getBoolean(USE_HIGH_RES_MAPS, USE_HIGH_RES_MAPS_DEF);
	}
	

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_POI_OVER_MAP = "show_poi_over_map"; //$NON-NLS-1$
	public static final Boolean SHOW_POI_OVER_MAP_DEF = false;

	public static boolean isShowingPoiOverMap(SharedPreferences prefs) {
		return prefs.getBoolean(SHOW_POI_OVER_MAP, SHOW_POI_OVER_MAP_DEF);
	}
	
	public static boolean setShowPoiOverMap(Context ctx, boolean val) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_POI_OVER_MAP, val).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_TRANSPORT_OVER_MAP = "show_transport_over_map"; //$NON-NLS-1$
	public static final boolean SHOW_TRANSPORT_OVER_MAP_DEF = false;

	public static boolean isShowingTransportOverMap(SharedPreferences prefs) {
		return prefs.getBoolean(SHOW_TRANSPORT_OVER_MAP, SHOW_TRANSPORT_OVER_MAP_DEF);
	}
	
	public static boolean setShowTransortOverMap(Context ctx, boolean val) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_TRANSPORT_OVER_MAP, val).commit();
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String USER_NAME = "user_name"; //$NON-NLS-1$

	public static String getUserName(SharedPreferences prefs) {
		return prefs.getString(USER_NAME, "NoName"); //$NON-NLS-1$
	}

	public static boolean setUserName(Context ctx, String name) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(USER_NAME, name).commit();
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String USER_OSM_BUG_NAME = "user_osm_bug_name"; //$NON-NLS-1$

	public static String getUserNameForOsmBug(SharedPreferences prefs) {
		return prefs.getString(USER_OSM_BUG_NAME, "NoName/Osmand"); //$NON-NLS-1$
	}

	public static boolean setUserNameForOsmBug(Context ctx, String name) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(USER_OSM_BUG_NAME, name).commit();
	}
	
	public static final String USER_PASSWORD = "user_password"; //$NON-NLS-1$
	public static String getUserPassword(SharedPreferences prefs){
		return prefs.getString(USER_PASSWORD, ""); //$NON-NLS-1$
	}
	
	public static boolean setUserPassword(Context ctx, String name){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(USER_PASSWORD, name).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String APPLICATION_MODE = "application_mode"; //$NON-NLS-1$

	public static ApplicationMode getApplicationMode(SharedPreferences prefs) {
		String s = prefs.getString(APPLICATION_MODE, ApplicationMode.DEFAULT.name());
		try {
			return ApplicationMode.valueOf(s);
		} catch (IllegalArgumentException e) {
			return ApplicationMode.DEFAULT;
		}
	}

	public static boolean setApplicationMode(Context ctx, ApplicationMode p) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(APPLICATION_MODE, p.name()).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String DAYNIGHT_MODE = "daynight_mode"; //$NON-NLS-1$
	public static DayNightMode getDayNightMode(SharedPreferences prefs) {
		String s = prefs.getString(DAYNIGHT_MODE, DayNightMode.AUTO.name());
		try {
			return DayNightMode.valueOf(s);
		} catch (IllegalArgumentException e) {
			return DayNightMode.AUTO;
		}
	}

	public static boolean setDayNightMode(Context ctx, DayNightMode p) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(APPLICATION_MODE, p.name()).commit();
	}
		
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String ROUTER_SERVICE = "router_service"; //$NON-NLS-1$

	public static RouteService getRouterService(SharedPreferences prefs) {
		int ord = prefs.getInt(ROUTER_SERVICE, RouteService.OSMAND.ordinal());
		// that fix specially for 0.5.2 release
		if(ord < RouteService.values().length){
			return RouteService.values()[ord];
		} else {
			return RouteService.OSMAND;
		}
	}

	public static boolean setRouterService(Context ctx, RouteService p) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putInt(ROUTER_SERVICE, p.ordinal()).commit();
	}	

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_CURRENT_TRACK = "save_current_track"; //$NON-NLS-1$
	public static final String RELOAD_INDEXES = "reload_indexes"; //$NON-NLS-1$
	public static final String DOWNLOAD_INDEXES = "download_indexes"; //$NON-NLS-1$

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_TRACK_TO_GPX = "save_track_to_gpx"; //$NON-NLS-1$
	public static final boolean SAVE_TRACK_TO_GPX_DEF = false; 

	public static boolean isSavingTrackToGpx(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SAVE_TRACK_TO_GPX, SAVE_TRACK_TO_GPX_DEF);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String FAST_ROUTE_MODE = "fast_route_mode"; //$NON-NLS-1$
	public static final boolean FAST_ROUTE_MODE_DEF = true; 

	public static boolean isFastRouteMode(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(FAST_ROUTE_MODE, FAST_ROUTE_MODE_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_TRACK_INTERVAL = "save_track_interval"; //$NON-NLS-1$

	public static int getSavingTrackInterval(SharedPreferences prefs) {
		return prefs.getInt(SAVE_TRACK_INTERVAL, 5);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_OSM_BUGS = "show_osm_bugs"; //$NON-NLS-1$
	public static final boolean SHOW_OSM_BUGS_DEF = false;

	public static boolean isShowingOsmBugs(SharedPreferences prefs) {
		return prefs.getBoolean(SHOW_OSM_BUGS, SHOW_OSM_BUGS_DEF);
	}
	
	public static boolean setShowingOsmBugs(Context ctx, boolean val) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_OSM_BUGS, val).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String DEBUG_RENDERING_INFO = "debug_rendering"; //$NON-NLS-1$
	public static final boolean DEBUG_RENDERING_INFO_DEF = false; 

	public static boolean isDebugRendering(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(DEBUG_RENDERING_INFO, DEBUG_RENDERING_INFO_DEF);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_YANDEX_TRAFFIC = "show_yandex_traffic"; //$NON-NLS-1$
	public static final boolean SHOW_YANDEX_TRAFFIC_DEF = false;

	public static boolean isShowingYandexTraffic(SharedPreferences prefs) {
		return prefs.getBoolean(SHOW_YANDEX_TRAFFIC, SHOW_YANDEX_TRAFFIC_DEF);
	}
	
	public static boolean setShowingYandexTraffic(Context ctx, boolean val) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_YANDEX_TRAFFIC, val).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_FAVORITES = "show_favorites"; //$NON-NLS-1$
	public static final boolean SHOW_FAVORITES_DEF = false;

	public static boolean isShowingFavorites(SharedPreferences prefs) {
		return prefs.getBoolean(SHOW_FAVORITES, SHOW_FAVORITES_DEF);
	}
	
	public static boolean setShowingFavorites(Context ctx, boolean val) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_FAVORITES, val).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_SCREEN_ORIENTATION = "map_screen_orientation"; //$NON-NLS-1$
	
	public static int getMapOrientation(SharedPreferences prefs){
		return prefs.getInt(MAP_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_VIEW_ANGLE = "show_view_angle"; //$NON-NLS-1$
	public static final boolean SHOW_VIEW_ANGLE_DEF = false;

	public static boolean isShowingViewAngle(SharedPreferences prefs) {
		return prefs.getBoolean(SHOW_VIEW_ANGLE, SHOW_VIEW_ANGLE_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String AUTO_ZOOM_MAP = "auto_zoom_map"; //$NON-NLS-1$
	public static final boolean AUTO_ZOOM_MAP_DEF = false;

	public static boolean isAutoZoomEnabled(SharedPreferences prefs) {
		return prefs.getBoolean(AUTO_ZOOM_MAP, AUTO_ZOOM_MAP_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String ROTATE_MAP = "rotate_map"; //$NON-NLS-1$
	public static final int ROTATE_MAP_TO_BEARING_DEF = 0;
	public static final int ROTATE_MAP_NONE = 0;
	public static final int ROTATE_MAP_BEARING = 1;
	public static final int ROTATE_MAP_COMPASS = 2;
	
	// return 0 - no rotate, 1 - to bearing, 2 - to compass
	public static int getRotateMap(SharedPreferences prefs) {
		return prefs.getInt(ROTATE_MAP, ROTATE_MAP_TO_BEARING_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String POSITION_ON_MAP = "position_on_map"; //$NON-NLS-1$

	public static int getPositionOnMap(SharedPreferences prefs) {
		return prefs.getInt(POSITION_ON_MAP, CENTER_CONSTANT);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAX_LEVEL_TO_DOWNLOAD_TILE = "max_level_download_tile"; //$NON-NLS-1$

	public static int getMaximumLevelToDownloadTile(SharedPreferences prefs) {
		return prefs.getInt(MAX_LEVEL_TO_DOWNLOAD_TILE, 18);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_VIEW_3D = "map_view_3d"; //$NON-NLS-1$
	public static final boolean MAP_VIEW_3D_DEF = false;

	public static boolean isMapView3D(SharedPreferences prefs) {
		return prefs.getBoolean(MAP_VIEW_3D, MAP_VIEW_3D_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_ENGLISH_NAMES = "use_english_names"; //$NON-NLS-1$
	public static final boolean USE_ENGLISH_NAMES_DEF = false;

	public static boolean usingEnglishNames(SharedPreferences prefs) {
		return prefs.getBoolean(USE_ENGLISH_NAMES, USE_ENGLISH_NAMES_DEF);
	}

	public static boolean setUseEnglishNames(Context ctx, boolean useEnglishNames) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(USE_ENGLISH_NAMES, useEnglishNames).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_STEP_BY_STEP_RENDERING = "use_step_by_step_rendering"; //$NON-NLS-1$
	public static final boolean USE_STEP_BY_STEP_RENDERING_DEF = true;

	public static boolean isUsingStepByStepRendering(SharedPreferences prefs) {
		return prefs.getBoolean(USE_STEP_BY_STEP_RENDERING, USE_STEP_BY_STEP_RENDERING_DEF);
	}
	
	public static boolean setUsingStepByStepRendering(Context ctx, boolean rendering) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(USE_STEP_BY_STEP_RENDERING, rendering).commit();
	}


	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_VECTOR_DATA = "map_vector_data"; //$NON-NLS-1$
	public static final String MAP_TILE_SOURCES = "map_tile_sources"; //$NON-NLS-1$
	
	public static boolean isUsingMapVectorData(SharedPreferences prefs){
		return prefs.getBoolean(MAP_VECTOR_DATA, false);
	}

	public static ITileSource getMapTileSource(SharedPreferences prefs) {
		String tileName = prefs.getString(MAP_TILE_SOURCES, null);
		if (tileName != null) {
			
			List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
			for (TileSourceTemplate l : list) {
				if (l.getName().equals(tileName)) {
					return l;
				}
			}
			File tPath = new File(Environment.getExternalStorageDirectory(), ResourceManager.TILES_PATH);
			File dir = new File(tPath, tileName);
			if(dir.exists()){
				if(tileName.endsWith(SQLiteTileSource.EXT)){
					return new SQLiteTileSource(dir);
				} else if (dir.isDirectory()) {
					String url = null;
					File readUrl = new File(dir, "url"); //$NON-NLS-1$
					try {
						if (readUrl.exists()) {
							BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(readUrl), "UTF-8")); //$NON-NLS-1$
							url = reader.readLine();
							url = url.replaceAll(Pattern.quote("{$z}"), "{0}"); //$NON-NLS-1$ //$NON-NLS-2$
							url = url.replaceAll(Pattern.quote("{$x}"), "{1}"); //$NON-NLS-1$//$NON-NLS-2$
							url = url.replaceAll(Pattern.quote("{$y}"), "{2}"); //$NON-NLS-1$ //$NON-NLS-2$
							reader.close();
						}
					} catch (IOException e) {
						Log.d(LogUtil.TAG, "Error reading url " + dir.getName(), e); //$NON-NLS-1$
					}
					return new TileSourceManager.TileSourceTemplate(dir, dir.getName(), url);
				}
			}
				
		}
		return TileSourceManager.getMapnikSource();
	}

	public static String getMapTileSourceName(SharedPreferences prefs) {
		String tileName = prefs.getString(MAP_TILE_SOURCES, null);
		if (tileName != null) {
			return tileName;
		}
		return TileSourceManager.getMapnikSource().getName();
	}

	// This value is a key for saving last known location shown on the map
	public static final String LAST_KNOWN_MAP_LAT = "last_known_map_lat"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_LON = "last_known_map_lon"; //$NON-NLS-1$
	public static final String IS_MAP_SYNC_TO_GPS_LOCATION = "is_map_sync_to_gps_location"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_ZOOM = "last_known_map_zoom"; //$NON-NLS-1$
	
	public static final String MAP_LAT_TO_SHOW = "map_lat_to_show"; //$NON-NLS-1$
	public static final String MAP_LON_TO_SHOW = "map_lon_to_show"; //$NON-NLS-1$
	public static final String MAP_ZOOM_TO_SHOW = "map_zoom_to_show"; //$NON-NLS-1$

	public static LatLon getLastKnownMapLocation(SharedPreferences prefs) {
		float lat = prefs.getFloat(LAST_KNOWN_MAP_LAT, 0);
		float lon = prefs.getFloat(LAST_KNOWN_MAP_LON, 0);
		return new LatLon(lat, lon);
	}

	public static void setMapLocationToShow(Context ctx, double latitude, double longitude) {
		setMapLocationToShow(ctx, latitude, longitude, getLastKnownMapZoom(getSharedPreferences(ctx)), null);
	}
	
	public static void setMapLocationToShow(Context ctx, double latitude, double longitude, int zoom) {
		setMapLocationToShow(ctx, latitude, longitude, null);
	}
	
	public static LatLon getAndClearMapLocationToShow(SharedPreferences prefs){
		if(!prefs.contains(MAP_LAT_TO_SHOW)){
			return null;
		}
		float lat = prefs.getFloat(MAP_LAT_TO_SHOW, 0);
		float lon = prefs.getFloat(MAP_LON_TO_SHOW, 0);
		prefs.edit().remove(MAP_LAT_TO_SHOW).commit();
		return new LatLon(lat, lon);
	}
	
	public static int getMapZoomToShow(SharedPreferences prefs) {
		return prefs.getInt(MAP_ZOOM_TO_SHOW, 5);
	}
	
	public static void setMapLocationToShow(Context ctx, double latitude, double longitude, int zoom, String historyDescription) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putFloat(MAP_LAT_TO_SHOW, (float) latitude);
		edit.putFloat(MAP_LON_TO_SHOW, (float) longitude);
		edit.putInt(MAP_ZOOM_TO_SHOW, zoom);
		edit.putBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, false);
		edit.commit();
		if(historyDescription != null){
			SearchHistoryHelper.getInstance().addNewItemToHistory(latitude, longitude, historyDescription, ctx);
		}
	}
	
	public static void setMapLocationToShow(Context ctx, double latitude, double longitude, String historyDescription) {
		setMapLocationToShow(ctx, latitude, longitude, getLastKnownMapZoom(getSharedPreferences(ctx)), historyDescription);
	}

	// Do not use that method if you want to show point on map. Use setMapLocationToShow
	public static void setLastKnownMapLocation(Context ctx, double latitude, double longitude) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putFloat(LAST_KNOWN_MAP_LAT, (float) latitude);
		edit.putFloat(LAST_KNOWN_MAP_LON, (float) longitude);
		edit.commit();
	}

	public static boolean setSyncMapToGpsLocation(Context ctx, boolean value) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, value).commit();
	}

	public static boolean isMapSyncToGpsLocation(SharedPreferences prefs) {
		return prefs.getBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, true);
	}

	public static int getLastKnownMapZoom(SharedPreferences prefs) {
		return prefs.getInt(LAST_KNOWN_MAP_ZOOM, 5);
	}

	public static void setLastKnownMapZoom(Context ctx, int zoom) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putInt(LAST_KNOWN_MAP_ZOOM, zoom);
		edit.commit();
	}

	public final static String POINT_NAVIGATE_LAT = "point_navigate_lat"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_LON = "point_navigate_lon"; //$NON-NLS-1$

	public static LatLon getPointToNavigate(SharedPreferences prefs) {
		float lat = prefs.getFloat(POINT_NAVIGATE_LAT, 0);
		float lon = prefs.getFloat(POINT_NAVIGATE_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}

	public static boolean clearPointToNavigate(SharedPreferences prefs) {
		return prefs.edit().remove(POINT_NAVIGATE_LAT).remove(POINT_NAVIGATE_LON).commit();
	}

	public static boolean setPointToNavigate(Context ctx, double latitude, double longitude) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putFloat(POINT_NAVIGATE_LAT, (float) latitude).putFloat(POINT_NAVIGATE_LON, (float) longitude).commit();
	}

	public static final String LAST_SEARCHED_REGION = "last_searched_region"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_CITY = "last_searched_city"; //$NON-NLS-1$
	public static final String lAST_SEARCHED_POSTCODE= "last_searched_postcode"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_STREET = "last_searched_street"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_BUILDING = "last_searched_building"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_INTERSECTED_STREET = "last_searched_intersected_street"; //$NON-NLS-1$

	public static String getLastSearchedRegion(SharedPreferences prefs) {
		return prefs.getString(LAST_SEARCHED_REGION, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedRegion(Context ctx, String region) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putString(LAST_SEARCHED_REGION, region).putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET,
				"").putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}
	
	public static String getLastSearchedPostcode(SharedPreferences prefs){
		return prefs.getString(lAST_SEARCHED_POSTCODE, null);	
	}
	
	public static boolean setLastSearchedPostcode(Context ctx, String postcode){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET, "").putString( //$NON-NLS-1$
				LAST_SEARCHED_BUILDING, "").putString(lAST_SEARCHED_POSTCODE, postcode); //$NON-NLS-1$
		if(prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public static Long getLastSearchedCity(SharedPreferences prefs) {
		return prefs.getLong(LAST_SEARCHED_CITY, -1);
	}

	public static boolean setLastSearchedCity(Context ctx, Long cityId) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putLong(LAST_SEARCHED_CITY, cityId).putString(LAST_SEARCHED_STREET, "").putString( //$NON-NLS-1$
				LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		edit.remove(lAST_SEARCHED_POSTCODE);
		if(prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public static String getLastSearchedStreet(SharedPreferences prefs) {
		return prefs.getString(LAST_SEARCHED_STREET, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedStreet(Context ctx, String street) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putString(LAST_SEARCHED_STREET, street).putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		if (prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public static String getLastSearchedBuilding(SharedPreferences prefs) {
		return prefs.getString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedBuilding(Context ctx, String building) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(LAST_SEARCHED_BUILDING, building).remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}

	public static String getLastSearchedIntersectedStreet(SharedPreferences prefs) {
		if (!prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			return null;
		}
		return prefs.getString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedIntersectedStreet(Context ctx, String street) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit();
	}

	public static boolean removeLastSearchedIntersectedStreet(Context ctx) {
		SharedPreferences prefs = getPrefs(ctx);
		return prefs.edit().remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}

	public static final String SELECTED_POI_FILTER_FOR_MAP = "selected_poi_filter_for_map"; //$NON-NLS-1$

	public static boolean setPoiFilterForMap(Context ctx, String filterId) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(SELECTED_POI_FILTER_FOR_MAP, filterId).commit();
	}

	public static PoiFilter getPoiFilterForMap(Context ctx, OsmandApplication application) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String filterId = prefs.getString(SELECTED_POI_FILTER_FOR_MAP, null);
		PoiFilter filter = application.getPoiFilters().getFilterById(filterId);
		if (filter != null) {
			return filter;
		}
		return new PoiFilter(null, application);
	}
	

	// this value string is synchronized with settings_pref.xml preference name
	public static final String VOICE_PROVIDER = "voice_provider"; //$NON-NLS-1$
	
	public static String getVoiceProvider(SharedPreferences prefs){
		return prefs.getString(VOICE_PROVIDER, null);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String RENDERER = "renderer"; //$NON-NLS-1$
	
	public static String getVectorRenderer(SharedPreferences prefs){
		return prefs.getString(RENDERER, null);
	}
	
	public static final String VOICE_MUTE = "voice_mute"; //$NON-NLS-1$
	public static final boolean VOICE_MUTE_DEF = false;
	
	public static boolean isVoiceMute(SharedPreferences prefs){
		return prefs.getBoolean(VOICE_MUTE, VOICE_MUTE_DEF);
	}
	
	public static boolean setVoiceMute(Context ctx, boolean mute){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(VOICE_MUTE, mute).commit();
	}
	
	// for background service
	public static final String MAP_ACTIVITY_ENABLED = "map_activity_enabled"; //$NON-NLS-1$
	public static final boolean MAP_ACTIVITY_ENABLED_DEF = false; 
	public static boolean getMapActivityEnabled(SharedPreferences prefs) {
		return prefs.getBoolean(MAP_ACTIVITY_ENABLED, MAP_ACTIVITY_ENABLED_DEF);
	}
	
	public static boolean setMapActivityEnabled(Context ctx, boolean en) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(MAP_ACTIVITY_ENABLED, en).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_ENABLED = "service_off_enabled"; //$NON-NLS-1$
	public static final boolean SERVICE_OFF_ENABLED_DEF = false; 
	public static boolean getServiceOffEnabled(SharedPreferences prefs) {
		return prefs.getBoolean(SERVICE_OFF_ENABLED, SERVICE_OFF_ENABLED_DEF);
	}
	
	public static boolean setServiceOffEnabled(Context ctx, boolean en) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SERVICE_OFF_ENABLED, en).commit();
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_PROVIDER = "service_off_provider"; //$NON-NLS-1$
	public static String getServiceOffProvider(SharedPreferences prefs) {
		return prefs.getString(SERVICE_OFF_PROVIDER, LocationManager.GPS_PROVIDER);
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_INTERVAL = "service_off_interval"; //$NON-NLS-1$
	public static final int SERVICE_OFF_INTERVAL_DEF = 5 * 60 * 1000;
	public static int getServiceOffInterval(SharedPreferences prefs) {
		return prefs.getInt(SERVICE_OFF_INTERVAL, SERVICE_OFF_INTERVAL_DEF);
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_WAIT_INTERVAL = "service_off_wait_interval"; //$NON-NLS-1$
	public static final int SERVICE_OFF_WAIT_INTERVAL_DEF = 90 * 1000;
	public static int getServiceOffWaitInterval(SharedPreferences prefs) {
		return prefs.getInt(SERVICE_OFF_WAIT_INTERVAL, SERVICE_OFF_WAIT_INTERVAL_DEF);
	}
	
	
	public static final String FOLLOW_TO_THE_ROUTE = "follow_to_route"; //$NON-NLS-1$
	
	public static boolean isFollowingByRoute(SharedPreferences prefs){
		return prefs.getBoolean(FOLLOW_TO_THE_ROUTE, false);
	}
	
	public static boolean setFollowingByRoute(Context ctx, boolean val){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(FOLLOW_TO_THE_ROUTE, val).commit();
	}
	
	public static final String SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME = "show_arrival_time"; //$NON-NLS-1$
	
	public static boolean isShowingArrivalTime(SharedPreferences prefs){
		return prefs.getBoolean(SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME, true);
	}
	
	public static boolean setShowingArrivalTime(Context ctx, boolean val){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME, val).commit();
	}
	
	
	
}
