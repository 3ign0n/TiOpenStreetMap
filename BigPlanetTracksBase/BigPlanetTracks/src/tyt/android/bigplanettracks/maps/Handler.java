package tyt.android.bigplanettracks.maps;

import android.graphics.Bitmap;

/**
 * Обработчик сообщений от потока
 * 
 * @author hudvin
 * 
 */
public abstract class Handler {

	public void handle(Object object) {
	};

	public void handle(RawTile tile, byte[] data) {
	}

	public void handle(RawTile tile, Bitmap bmp4scale, boolean isScaled) {
	};

}
