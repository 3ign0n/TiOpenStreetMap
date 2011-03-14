package tyt.android.bigplanettracks.maps.loader;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tyt.android.bigplanettracks.maps.Handler;
import tyt.android.bigplanettracks.maps.RawTile;
import tyt.android.bigplanettracks.maps.providers.MapStrategy;

/**
 * Загрузчик тайлов с сервера
 * 
 * @author hudvin
 * 
 */
public class TileLoader implements Runnable {

	private static final int MAX_THREADS = 3;

	private MapStrategy mapStrategy;

	private Handler handler;

	private int counter = 0;
	
	public static boolean stop = false;
	
	private boolean useNet = true;

	private Stack<RawTile> loadQueue = new Stack<RawTile>();

	private static ExecutorService mThreadPool = Executors.newFixedThreadPool(MAX_THREADS);
	
	/**
	 * Конструктор
	 * 
	 * @param handler
	 *            обработчик результата загрузки
	 */
	public TileLoader(Handler handler) {
		this.handler = handler;
		TileLoader.stop = false;
	}

	public void setMapStrategy(MapStrategy mapStrategy) {
		this.mapStrategy = mapStrategy;
	}

	public synchronized void setUseNet(boolean useNet) {
		this.useNet = useNet;
	}

	/**
	 * Добавляет в очередь на загрузку
	 * 
	 * @param tile
	 */
	public synchronized void load(RawTile tile) {
		addToQueue(tile);
	}

	public synchronized void addToQueue(RawTile tile) {
		if (useNet) {
			loadQueue.push(tile);
		}
	}

	public synchronized RawTile getFromQueue() {
		return loadQueue.pop();
	}

	public synchronized void tileLoaded(RawTile tile, byte[] data) {
		if (data != null) {
			handler.handle(tile, data);
		}
		counter--;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(100);
				if (mapStrategy!=null && useNet && counter < MAX_THREADS && loadQueue.size() > 0) {
					RawTile rt = getFromQueue();
//					Log.i("LOADER", "Tile " + rt + " start loading");
					if (null != rt) {
						counter++;
						mThreadPool.execute(new ThreadLoader(rt));
					}
				}
				if (stop) { // stop this thread after exiting the application
					break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class ThreadLoader extends BaseLoader {

		public ThreadLoader(RawTile tile) {
			super(tile);
		}

		@Override
		protected MapStrategy getStrategy() {
			return TileLoader.this.mapStrategy;
		}

		@Override
		protected void handle(RawTile tile, byte[] data, int meta) {
//			System.out.println("handle " + tile);
			TileLoader.this.tileLoaded(tile, data);
		}

	}

}
