package tyt.android.bigplanettracks.maps.storage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import tyt.android.bigplanettracks.maps.Preferences;
import tyt.android.bigplanettracks.maps.RawTile;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SQLLocalStorage implements ILocalStorage {

	private static ILocalStorage localStorage;

	private static String X_COLUMN = "x";

	private static String Y_COLUMN = "y";

	private static String Z_COLUMN = "z";

	private static String S_COLUMN = "s";

	private static String IMAGE_COLUMN = "image";

	private static String TILES_TABLE = "tiles";

	private static String TABLE_DDL = "CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s))";

	private static String INDEX_DDL = "CREATE INDEX IF NOT EXISTS IND on tiles (x,y,z,s)";

	public static String SD_PATH = "/sdcard/";
	public static String MAP_PATH = SD_PATH + "RMaps/maps/";
	public static String TRACK_PATH = SD_PATH + "BigPlanetTracks/";
	public static String TRACK_IMPORT_PATH = TRACK_PATH + "import/";
	public static String TRACK_EXPORT_PATH = TRACK_PATH + "export/";
	
	public static String SQLITEDB = "Big Planet Tracks.sqlitedb";

	private static String DELETE_SQL = "DELETE FROM tiles";

	private static String GET_SQL = "SELECT * FROM tiles WHERE x=? AND y=? AND z=? AND s=?";

	private static String COUNT_SQL = "SELECT COUNT(*) FROM tiles WHERE x=? AND y=? AND z=? AND s=?";
	
	private static SQLiteDatabase db;

	public static ILocalStorage getInstance() {
		if (localStorage == null) {
			localStorage = new SQLLocalStorage();
		}
		return localStorage;
	}
	
	public static void resetLocalStorage() {
		db.close();
		localStorage = null;
	}
	
	public static void updateSDPaths() {
		MAP_PATH = SD_PATH + "RMaps/maps/";
		TRACK_PATH = SD_PATH + "BigPlanetTracks/";
		TRACK_IMPORT_PATH = TRACK_PATH + "import/";
		TRACK_EXPORT_PATH = TRACK_PATH + "export/";
	}

	/**
	 * Конструктор Инициализация файлового кеша(если необходимо)
	 */
	private SQLLocalStorage() {
		// for dynamically loading different DBs from Preferences.getSQLitePath()
		String sqliteFilePath = MAP_PATH + Preferences.getSQLiteName();
		db = SQLiteDatabase.openDatabase(sqliteFilePath, null,
				SQLiteDatabase.CREATE_IF_NECESSARY);
		db.execSQL(SQLLocalStorage.TABLE_DDL);
		db.execSQL(SQLLocalStorage.INDEX_DDL);
	}

	public void clear() {
		db.execSQL(SQLLocalStorage.DELETE_SQL);
	}

	public BufferedInputStream get(RawTile tile) {
		String sql = SQLLocalStorage.GET_SQL;

		Cursor c = db.rawQuery(sql, 
				new String[] { 
				String.valueOf(tile.x),
				String.valueOf(tile.y), 
				String.valueOf(tile.z),
				String.valueOf(tile.s), });

		BufferedInputStream io = null;
		if (c.getCount() != 0) {
			c.moveToFirst();
			byte[] d = c.getBlob(c.getColumnIndex(SQLLocalStorage.IMAGE_COLUMN));
			io = new BufferedInputStream(new ByteArrayInputStream(d), 4096);
		}
		c.close();
		return io;
	}

	public boolean isExists(RawTile tile) {
		Cursor c = db.rawQuery(SQLLocalStorage.COUNT_SQL, 
				new String[] { 
				String.valueOf(tile.x),
				String.valueOf(tile.y), 
				String.valueOf(tile.z),
				String.valueOf(tile.s), });
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count==1;
	}

	public void put(RawTile tile, byte[] data) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(SQLLocalStorage.X_COLUMN, tile.x);
		initialValues.put(SQLLocalStorage.Y_COLUMN, tile.y);
		initialValues.put(SQLLocalStorage.Z_COLUMN, tile.z);
		initialValues.put(SQLLocalStorage.S_COLUMN, tile.s);
		initialValues.put(SQLLocalStorage.IMAGE_COLUMN, data);
		db.insert(SQLLocalStorage.TILES_TABLE, null, initialValues);
	}

}
