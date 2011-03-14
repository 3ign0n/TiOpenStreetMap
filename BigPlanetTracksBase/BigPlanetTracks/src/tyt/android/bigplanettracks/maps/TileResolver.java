package tyt.android.bigplanettracks.maps;

import tyt.android.bigplanettracks.maps.loader.TileLoader;
import tyt.android.bigplanettracks.maps.providers.MapStrategy;
import tyt.android.bigplanettracks.maps.providers.MapStrategyFactory;
import tyt.android.bigplanettracks.maps.storage.BitmapCacheWrapper;
import tyt.android.bigplanettracks.maps.storage.LocalStorageWrapper;

import android.graphics.Bitmap;

public class TileResolver {

	private TileLoader tileLoader;

	private PhysicMap physicMap;

	private BitmapCacheWrapper cacheProvider = BitmapCacheWrapper.getInstance();

	private Handler scaledHandler;

	private Handler localLoaderHandler;

	private int strategyId = -1;

	private int loaded = 0;

	public TileResolver(final PhysicMap physicMap) {
		this.physicMap = physicMap;
		tileLoader = new TileLoader(
		// обработчик загрузки тайла с сервера
				new Handler() {
					@Override
					public void handle(RawTile tile, byte[] data) {
						LocalStorageWrapper.put(tile, data);
						Bitmap bmp = LocalStorageWrapper.get(tile);
						cacheProvider.putToCache(tile, bmp);
						updateMap(tile, bmp);
					}
				});
		new Thread(tileLoader, "TileLoader").start();

		// обработчик загрузки скалированых картинок
		this.scaledHandler = new Handler() {

			@Override
			public synchronized void handle(RawTile tile, Bitmap bitmap,
					boolean isScaled) {
				loaded++;
				if (bitmap != null) {
					if (isScaled) {
						cacheProvider.putToScaledCache(tile, bitmap);
						updateMap(tile, bitmap);
					}
				}
			}

		};
		// обработчик загрузки с дискового кеша
		this.localLoaderHandler = new Handler() {

			@Override
			public void handle(RawTile tile, Bitmap bitmap, boolean isScaled) {
				if (tile.s == -1) {
					throw new IllegalStateException();
				}
				if (bitmap != null) { // если тайл есть в файловом кеше
					loaded++;
					cacheProvider.putToCache(tile, bitmap);
					updateMap(tile, bitmap);
				} else { // если тайла нет в файловом кеше
					bitmap = cacheProvider.getScaledTile(tile);
					if (bitmap == null) {
						loaded++;
						TileScaler.get(tile, scaledHandler);
						//new Thread(new TileScaler(tile, scaledHandler)).start();
					} else { // скалированый тайл из кеша
						loaded++;
						updateMap(tile, bitmap);
					}
					load(tile);
				}
			}

		};

	}

	/**
	 * Добавляет в очередь на загрузку с сервера
	 * 
	 * @param tile
	 */
	private void load(RawTile tile) {
		if (tile.s != -1) {
			tileLoader.load(tile);
		}
	}

	private void updateMap(RawTile tile, Bitmap bitmap) {
		if (tile.s == strategyId) {
			physicMap.update(bitmap, tile);
		}
	}

	
	public Bitmap loadTile(final RawTile tile){
		return cacheProvider.getTile(tile);
	}
	
	/**
	 * Загружает заданный тайл
	 * 
	 * @param tile
	 * @return
	 */
	public void getTile(final RawTile tile) {
		if (tile.s == -1) {
//			System.out.println("6666666");
			return;
		}
		Bitmap bitmap = cacheProvider.getTile(tile);
		if (bitmap != null) {
			// возврат тайла
			loaded++;
			updateMap(tile, bitmap);
		} else {
			//bitmap = LocalStorageWrapper.get(tile);
			//if (bitmap != null) {
			//	loaded++;
				//updateMap(tile, bitmap);
			//}
			//updateMap(tile, MapControl.CELL_BACKGROUND);
			LocalStorageWrapper.get(tile, localLoaderHandler);
		}
	}

	public synchronized void setMapSource(int sourceId) {
		clearCache();
		MapStrategy mapStrategy = MapStrategyFactory.getStrategy(sourceId);
		this.strategyId = sourceId;
		tileLoader.setMapStrategy(mapStrategy);
	}

	public void clearCache() {
		cacheProvider.clear();
	}

	public void gcCache() {
		cacheProvider.gc();
	}

	public int getMapSourceId() {
		return this.strategyId;
	}

	public void setUseNet(boolean useNet) {
		tileLoader.setUseNet(useNet);
		if (useNet) {
			physicMap.reloadTiles();
		}
	}
	
	public Bitmap[][] fillMap(RawTile tile, final int size){
		Bitmap[][] cells = new Bitmap[size][size];
		for(int i=0;i<size;i++){
			for(int j=0;j<size;j++){
				int x, y;
				x = (tile.x + i);
				y = (tile.y + j);
				
				RawTile tmp = new RawTile(x,y,tile.z, tile.s);
				
				Bitmap bitmap;
				bitmap =  cacheProvider.getTile(tmp);
				if(bitmap==null){
					bitmap= LocalStorageWrapper.get(tmp);
					if(bitmap==null){
						bitmap = TileScaler.get(tmp);
						if(bitmap==null){
							// установить фон
						}
					}
				}
				cells[i][j]=bitmap;
			}
		}
		return cells;
	}

	public synchronized void incLoaded(){
		this.loaded++;
//		System.out.println("inc " + this.loaded);
	}
	
	public synchronized int getLoaded(){
//		System.out.println("getLoaded " + this.loaded);
		return this.loaded;
	}
	
	public synchronized void resetLoaded(){
//		System.out.println("inc " + this.loaded);
		this.loaded = 0;
	}
}
