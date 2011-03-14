package tyt.android.bigplanettracks.maps.tools;

import java.util.Stack;

import tyt.android.bigplanettracks.maps.RawTile;
import tyt.android.bigplanettracks.maps.loader.BaseLoader;
import tyt.android.bigplanettracks.maps.providers.MapStrategy;
import tyt.android.bigplanettracks.maps.storage.LocalStorageWrapper;

import android.os.Handler;

public class MapSaver {

	private RawTile[] tiles;

	private MapStrategy mapStrategy;

	private Handler handler;

	private int totalKB = 0;

	private int totalSuccessful = 0;

	private int totalUnsuccessful = 0;

	private ThreadLoader threadLoader;

	public MapSaver(Stack<RawTile> tiles, MapStrategy mapStrategy,
			Handler handler) {
		this.tiles = new RawTile[tiles.size()];
		tiles.toArray(this.tiles);
		this.handler = handler;
		this.mapStrategy = mapStrategy;
	}

	public void download() {
		threadLoader = new ThreadLoader(tiles);
		threadLoader.start();
	}

	public void stopDownload() {
		threadLoader.stopLoader();
	}

	public int getTotalKB() {
		return totalKB / (1024);
	}

	public int getTotalSuccessful() {
		return totalSuccessful;
	}

	public int getTotalUnsuccessful() {
		return totalUnsuccessful;
	}

	private void handle(RawTile tile, byte[] data, int meta) {
		if (data == null && meta == 1) {
			totalSuccessful++;
		} else if (data == null) {
			totalUnsuccessful++;
		} else {
			totalSuccessful++;
			totalKB += data.length;
			LocalStorageWrapper.put(tile, data);
		}
		handler.sendEmptyMessage(0);
	}

	class ThreadLoader extends BaseLoader {

		public ThreadLoader(RawTile[] tiles) {
			super(tiles);
		}

		@Override
		protected MapStrategy getStrategy() {
			return MapSaver.this.mapStrategy;
		}

		@Override
		protected void handle(RawTile tile, byte[] data, int meta) {
			MapSaver.this.handle(tile, data, meta);
		}
		
		protected boolean checkTile(RawTile tile) {
			return !LocalStorageWrapper.isExists(tile);
		}

	}

}
