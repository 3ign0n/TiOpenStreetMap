package tyt.android.bigplanettracks.maps.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import tyt.android.bigplanettracks.maps.RawTile;


import android.graphics.Bitmap;
import android.util.Log;

/**
 * Предоставляет возможность хранения заданного количества Bitmap Автоматически
 * выполняет удаление самых старых элементов
 * 
 * @author hudvin
 * 
 */
public class ExpiredHashMap {

	private int maxSize;

	private HashMap<ExpRawTile, Bitmap> expCacheMap = new HashMap<ExpRawTile, Bitmap>();

	public ExpiredHashMap(int maxSize) {
		this.maxSize = maxSize;
	}

	public void clear() {
		this.expCacheMap.clear();
	}

	public synchronized void put(RawTile tile, Bitmap bitmap) {
		expCacheMap.put(new ExpRawTile(tile, System.currentTimeMillis()),
				bitmap);
	}

	public synchronized Bitmap get(RawTile tile) {
		Bitmap bmp = expCacheMap.get(tile);
		if (bmp != null) {
			expCacheMap.put(new ExpRawTile(tile, System.currentTimeMillis()),
					bmp);
		}
		return bmp;
	}

	/**
	 * Удаляет определенную часть самых старых элементов в кеше
	 */
	public void gc() {
		if (expCacheMap.size() >= maxSize) {
			Iterator<ExpRawTile> it = expCacheMap.keySet().iterator();
			List<ExpRawTile> listToSort = new ArrayList<ExpRawTile>();
			while (it.hasNext()) {
				listToSort.add(it.next());
			}
			Collections.sort(listToSort);
			for (int i = 0; i < expCacheMap.size() / 2; i++) {
				expCacheMap.remove(listToSort.get(i));
			}
			Log.i("CACHE", "clean");
		}
	}

	private class ExpRawTile extends RawTile implements Comparable<ExpRawTile> {

		private static final long serialVersionUID = -7039594975313513993L;
		private long addedOn = -1;

		public ExpRawTile(RawTile tile, long addedOn) {
			super(tile.x, tile.y, tile.z, tile.s);
			this.addedOn = addedOn;
		}

		public int compareTo(ExpRawTile another) {
			return (int) (addedOn - another.addedOn);
		}

	}

}
