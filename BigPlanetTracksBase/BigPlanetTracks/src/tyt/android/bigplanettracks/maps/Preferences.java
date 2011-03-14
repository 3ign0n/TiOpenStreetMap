package tyt.android.bigplanettracks.maps;

import tyt.android.bigplanettracks.maps.storage.SQLLocalStorage;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;

/**
 * Предназначен для хранения настроек
 * 
 * @author hudvin
 * 
 */
public class Preferences {

	private static final String OFFSET_Y = "offsetY";

	private static final String OFFSET_X = "offsetX";

	private static final String TILEZ = "tilez";

	private static final String TILEY = "tiley";

	private static final String TILEX = "tilex";

	private static final String USE_NET = "useNet";

	private static final String SOURCE_ID = "sourceId";
	
	private static final String SQLite_Name = "SQLite";

	private static SharedPreferences prefs;

	public static final String MAP_SOURCE = "MAP_SOURCE";

	public static final String NETWORK_MODE = "NETWORK_MODE";

	public static final String GPS_OFFSET_X = "GPS_OffsetX";
	public static final String GPS_OFFSET_Y = "GPS_OffsetY";

	public static void init(Application app) {
		prefs = app.getSharedPreferences("global", Context.MODE_PRIVATE);
	}

	/**
	 * Сохраняет угловой тайл
	 * 
	 * @param tile
	 */
	public static void putTile(RawTile tile) {
		put(Preferences.TILEX, tile.x);
		put(Preferences.TILEY, tile.y);
		put(Preferences.TILEZ, tile.z);
	}

	/**
	 * Возвращает угловой тайл
	 * 
	 * @return
	 */
	public static RawTile getTile() {
		int x, y, z; // Taiwan = (106, 54, 13)
		x = prefs.getInt(Preferences.TILEX, 106); //0
		y = prefs.getInt(Preferences.TILEY, 54);  //0
		z = prefs.getInt(Preferences.TILEZ, 13);  //16
		return new RawTile(x, y, z, -1);
	}

	/**
	 * Сохраняет отступ
	 * 
	 * @param offset
	 */
	public static void putOffset(Point offset) {
		put(Preferences.OFFSET_X, offset.x);
		put(Preferences.OFFSET_Y, offset.y);
	}

	/**
	 * Возвращает отступ
	 * 
	 * @return
	 */
	public static Point getOffset() {
		// Taiwan = (-85, -156)
		int x = prefs.getInt(Preferences.OFFSET_X, -85);  //0
		int y = prefs.getInt(Preferences.OFFSET_Y, -156); //0
		return new Point(x, y);
	}

	public static void putGPSOffset(Point offset) {
		put(Preferences.GPS_OFFSET_X, offset.x);
		put(Preferences.GPS_OFFSET_Y, offset.y);
	}

	public static Point getGPSOffset() {
		// Taiwan = (0, 0), only China need to set these values
		int x = prefs.getInt(Preferences.GPS_OFFSET_X, 0);
		int y = prefs.getInt(Preferences.GPS_OFFSET_Y, 0);
		return new Point(x, y);
	}

	public static void putSourceId(int sourceId) {
		if(sourceId==-1){
			throw new IllegalArgumentException();
		}
		put(SOURCE_ID, sourceId);
	}

	public static int getSourceId() {
		int sourceId = prefs.getInt(Preferences.SOURCE_ID, 0);
		return sourceId;
	}

	public static void putSQLiteName(String strSQLiteName) {
		put(Preferences.SQLite_Name, strSQLiteName);
	}

	public static String getSQLiteName() {
		String strSQLiteName = prefs.getString(Preferences.SQLite_Name, SQLLocalStorage.SQLITEDB);
		return strSQLiteName;
	}

	public static void putUseNet(boolean useNet) {
		put(Preferences.USE_NET, useNet);
	}

	public static boolean getUseNet() {
		boolean useNet = prefs.getBoolean(Preferences.USE_NET, true);
		return useNet;
	}

	private static void put(String name, Object value){
		SharedPreferences.Editor editor = prefs.edit();
		if(value.getClass() == Boolean.class){
			editor.putBoolean(name, (Boolean)value);
		}
		if(value.getClass() == String.class){
			editor.putString(name, (String)value);
		}
		if(value.getClass() == Integer.class){
			editor.putInt(name, ((Integer)value).intValue());
		}
		
		
		editor.commit();
		
	}
	
	
}
