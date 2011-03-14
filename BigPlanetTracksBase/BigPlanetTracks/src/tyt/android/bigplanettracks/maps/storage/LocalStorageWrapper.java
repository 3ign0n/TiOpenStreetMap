package tyt.android.bigplanettracks.maps.storage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tyt.android.bigplanettracks.maps.Handler;
import tyt.android.bigplanettracks.maps.RawTile;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Обертка над LocalStorage Производит декодирование тайла в синхронном и в
 * асинхронном режиме
 * 
 * @author hudvin
 * 
 */
public class LocalStorageWrapper {

	private static ILocalStorage localStorage = SQLLocalStorage.getInstance();

	private static ExecutorService mThreadPool = Executors.newFixedThreadPool(5);
	
	public static void switchLocalStorage() {
		SQLLocalStorage.resetLocalStorage();
		localStorage = SQLLocalStorage.getInstance();
	}

	/**
	 * Декодирует тайл
	 * 
	 * @param tile
	 * @return
	 */
	public static Bitmap get(final RawTile tile) {
		BufferedInputStream outStream = localStorage.get(tile);
		Bitmap bmp = null;
		if (outStream != null) {
			bmp = BitmapFactory.decodeStream(outStream);
			try {
				outStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return bmp;
	}

	public static boolean isExists(RawTile tile) {
		return localStorage.isExists(tile);
	}

	public static void put(RawTile tile, byte[] data) {
		localStorage.put(tile, data);
	}

	public static void get(final RawTile tile, final Handler handler) {
		mThreadPool.execute(new Runnable() {
			public void run() {
				handler.handle(tile, get(tile), false);
			}
		});
	}

}
