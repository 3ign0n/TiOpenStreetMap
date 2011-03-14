package tyt.android.bigplanettracks.maps.db;

import java.util.ArrayList;
import java.util.List;

import tyt.android.bigplanettracks.maps.RawTile;
import tyt.android.bigplanettracks.maps.storage.SQLLocalStorage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DAO {

	private static final String DATABASE_NAME = "bookmarks.db";

	public static final String TABLE_GEOBOOKMARKS = "geobookmarks";

	private static final String COLUMN_OFFSETY = "offsety";

	private static final String COLUMN_OFFSETX = "offsetx";

	private static final String COLUMN_X = "x";

	private static final String COLUMN_Y = "y";

	private static final String COLUMN_Z = "z";

	private static final String COLUMN_S = "s";

	private static final String COLUMN_DESCRIPTION = "description";

	private static final String COLUMN_NAME = "name";

	private static final String COLUMN_ID = "id";

	public static final String TABLE_DDL = "CREATE TABLE IF NOT EXISTS " + TABLE_GEOBOOKMARKS
			+ "(" + 
			COLUMN_ID + " integer primary key autoincrement," + 
			COLUMN_NAME + " text," + 
			COLUMN_DESCRIPTION + " text," + 
			COLUMN_OFFSETX + " integer," + 
			COLUMN_OFFSETY + " integer," + 
			COLUMN_S + " integer," + 
			COLUMN_X + " integer," + 
			COLUMN_Y + " integer," + 
			COLUMN_Z + " integer" + ");";

	private static SQLiteDatabase db;

	public DAO(Context context) {
		if (db == null) {
			String sqliteFilePath = SQLLocalStorage.TRACK_PATH + DATABASE_NAME;
			db = SQLiteDatabase.openDatabase(sqliteFilePath, null,
					SQLiteDatabase.CREATE_IF_NECESSARY);
			db.execSQL(TABLE_DDL);
		}
	}

	public void saveGeoBookmark(GeoBookmark bookmark) {
		if (bookmark.getId() != -1) {
			updateGeoBookmark(bookmark);
		} else {
			ContentValues initialValues = new ContentValues();
			// initialValues.put(DAO.COLUMN_ID, bookmark.getId());
			initialValues.put(DAO.COLUMN_NAME, bookmark.getName());
			initialValues.put(DAO.COLUMN_DESCRIPTION, bookmark.getDescription());
			initialValues.put(DAO.COLUMN_S, bookmark.getTile().s);
			initialValues.put(DAO.COLUMN_Z, bookmark.getTile().z);
			initialValues.put(DAO.COLUMN_X, bookmark.getTile().x);
			initialValues.put(DAO.COLUMN_Y, bookmark.getTile().y);

			initialValues.put(DAO.COLUMN_OFFSETX, bookmark.getOffsetX());
			initialValues.put(DAO.COLUMN_OFFSETY, bookmark.getOffsetY());
			// save to database
			db.insert(DAO.TABLE_GEOBOOKMARKS, null, initialValues);
		}
	}

	private void updateGeoBookmark(GeoBookmark bookmark) {
		ContentValues args = new ContentValues();
		args.put(COLUMN_NAME, bookmark.getName());
		args.put(COLUMN_DESCRIPTION, bookmark.getDescription());
		db.update(TABLE_GEOBOOKMARKS, args, COLUMN_ID + "=" + bookmark.getId(), null);
	}

	public void removeGeoBookmark(int geoBookmarkId) {
		db.delete(TABLE_GEOBOOKMARKS, COLUMN_ID + "=" + geoBookmarkId, null);
	}

	public List<GeoBookmark> getBookmarks() {
		List<GeoBookmark> bookmarks = new ArrayList<GeoBookmark>();
		try {
			Cursor c = db.query(true, DAO.TABLE_GEOBOOKMARKS, null, null, null,
					null, null, null, null);

			int numRows = c.getCount();
			c.moveToFirst();
			for (int i = 0; i < numRows; ++i) {
				GeoBookmark bookmark = new GeoBookmark();
				bookmark.setId(c.getInt(c.getColumnIndex(COLUMN_ID)));
				bookmark.setName(c.getString(c.getColumnIndex(COLUMN_NAME)));
				bookmark.setDescription(c.getString(c.getColumnIndex(COLUMN_DESCRIPTION)));
				bookmark.setOffsetX(c.getInt(c.getColumnIndex(COLUMN_OFFSETX)));
				bookmark.setOffsetY(c.getInt(c.getColumnIndex(COLUMN_OFFSETY)));

				RawTile tile = new RawTile(
						c.getInt(c.getColumnIndex(COLUMN_X)), 
						c.getInt(c.getColumnIndex(COLUMN_Y)), 
						c.getInt(c.getColumnIndex(COLUMN_Z)), 
						c.getInt(c.getColumnIndex(COLUMN_S)));

				bookmark.setTile(tile);
				bookmarks.add(bookmark);
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}
		return bookmarks;
	}

	@Override
	public void finalize() {
		db.acquireReference();
	}

}
