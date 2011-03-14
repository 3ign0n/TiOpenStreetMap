package tyt.android.bigplanettracks.maps;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tyt.android.bigplanettracks.maps.storage.BitmapCacheWrapper;
import tyt.android.bigplanettracks.maps.storage.LocalStorageWrapper;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/**
 * Предназначен для выполнения скалирования
 * 
 * @author hudvin
 * 
 */
public class TileScaler{

	private static ExecutorService mThreadPool = Executors.newFixedThreadPool(5);;

	public static void get(final RawTile tile, final Handler handler){
		mThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				Bitmap bitmap = getScaler(tile).scale();
				handler.handle(tile, bitmap, true);
			}
		});
	}

	public static Bitmap get(final RawTile tile){
		return getScaler(tile).scale();
	}
	
	private static Scaler getScaler(final RawTile tile){
		Scaler scaler = new Scaler(tile);
		return scaler;
	}
	
	private static class Scaler{
		
		private RawTile tile;
		
		public Scaler(RawTile tile){
			this.tile=tile;
		}
		
		public Bitmap scale(){
			Bitmap bitmap =  findTile(tile.x, tile.y, tile.z, tile.s);
			return bitmap;
		}
		
		/**
		 * Возвращает размеры тайла при зуммировании
		 * 
		 * @param zoom
		 * @return
		 */
		private int getTileSize(int zoom) {
			return (int) (256 / Math.pow(2, zoom));
		}

		private Bitmap findTile(int x, int y, int z, int s) {
			Bitmap bitmap = null;
			int offsetX;
			int offsetY;
			int offsetParentX;
			int offsetParentY;
			int parentTileX;
			int parentTileY;
			// получение отступа от начала координат на текущев уровне
			offsetX = x * 256; // отступ от начала координат по ox
			offsetY = y * 256; // отступ от начала координат по oy
			int tmpZ = z;
			while (bitmap == null && tmpZ <= 17) {
				tmpZ++;

				// получение отступа от начала координат на предыдущем уровне
				offsetParentX = (int) (offsetX / Math.pow(2, tmpZ - z));
				offsetParentY = (int) (offsetY / Math.pow(2, tmpZ - z));

				// получение координат тайла на предыдущем уровне
				parentTileX = offsetParentX / 256;
				parentTileY = offsetParentY / 256;

				// необходимо возвращать, во сколько раз увеличить!!!
				
				RawTile tmpTile = new RawTile(parentTileX,
						parentTileY, tmpZ, s);
				
				if(bitmap==null){
					bitmap =  BitmapCacheWrapper.getInstance().getTile(tmpTile);	
				}
				if(bitmap==null){
					bitmap = LocalStorageWrapper.get(tmpTile);
				}
				
				if (bitmap == null) {
				} else { // родительский тайл найден и загружен
					// получение отступа в родительском тайле
					offsetParentX = offsetParentX - parentTileX * 256;
					offsetParentY = offsetParentY - parentTileY * 256;

					// получение уровня скалирования
					int scale = tmpZ - z;
					// получение размера тайла в родительском тайле
					int tileSize = getTileSize(scale);

					// копирование области и скалирование
					int[] pixels = new int[tileSize * tileSize];
					if (offsetParentY >= 0 && offsetParentX >= 0 && tileSize>0) {
						bitmap.getPixels(pixels, 0, tileSize, offsetParentX,
								offsetParentY, tileSize, tileSize);
						bitmap = Bitmap.createBitmap(pixels, tileSize, tileSize,
								Config.ARGB_8888);
						pixels = null;
						return Bitmap.createScaledBitmap(bitmap, 256, 256, false);
					}
				}
			}
			return null;
		}
		
	}
	
}
