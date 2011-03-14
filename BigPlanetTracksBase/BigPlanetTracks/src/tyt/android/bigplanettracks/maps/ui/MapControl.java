package tyt.android.bigplanettracks.maps.ui;

import java.util.ArrayList;
import java.util.List;

import tyt.android.bigplanettracks.BigPlanet;
import tyt.android.bigplanettracks.R;
import tyt.android.bigplanettracks.maps.AbstractCommand;
import tyt.android.bigplanettracks.maps.MarkerManager;
import tyt.android.bigplanettracks.maps.MarkerManager.Marker;
import tyt.android.bigplanettracks.maps.PhysicMap;
import tyt.android.bigplanettracks.maps.RawTile;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Виджет, реализующий карту
 * 
 * @author hudvin
 * 
 */
public class MapControl extends RelativeLayout {

	private static final int TILE_SIZE = 256;

	public static final int ZOOM_MODE = 0;

	public static final int SELECT_MODE = 1;

	private int mapMode = ZOOM_MODE;

	private Panel main;

	private Canvas cs;

	private DoubleClickDetector dcDetector = new DoubleClickDetector();

	private PhysicMap pmap;

	/*
	 * Toolbar with zoom cont
	 */
	private ZoomPanel zoomPanel;

	private boolean isNew = true;

	private Bitmap cb = null;

	private final static int BCG_CELL_SIZE = 16;

	private OnMapLongClickListener onMapLongClickListener;

	private MarkerManager markerManager;

	public static Bitmap CELL_BACKGROUND = BitmapUtils.drawBackground(BCG_CELL_SIZE, TILE_SIZE, TILE_SIZE);

	public static Bitmap EMPTY_BACKGROUND = BitmapUtils.drawEmptyBackground(TILE_SIZE);

	private Point scalePoint = new Point();

	private SmoothZoomEngine szEngine;

	public Handler h;
	
	private Context context;
	
	private final float density = BigPlanet.density;
	private int bestZoomFromDB = -2;
	private int initialZoomFromDB = -2;
	private int minZoomFromDB = 10; // (tile.Zoom = 17-10 = 7)
	
	private List<Marker> markersTemp = new ArrayList<Marker>();

	// MyTracks
	private final Drawable arrow[] = new Drawable[18];
	private final int arrowWidth, arrowHeight;
	
	/**
	 * Constructor
	 * 
	 * @param context
	 * @param width
	 * @param height
	 * @param startTile
	 */
	public MapControl(Context context, int width, int height,
			RawTile startTile, MarkerManager markerManager) {
		super(context);
		this.context = context;
		scalePoint.set(width / 2, height / 2);
		this.markerManager = markerManager;
		buildView(width, height, startTile);
		
		arrow[0] = context.getResources().getDrawable(R.drawable.arrow_0);
		arrow[1] = context.getResources().getDrawable(R.drawable.arrow_20);
		arrow[2] = context.getResources().getDrawable(R.drawable.arrow_40);
		arrow[3] = context.getResources().getDrawable(R.drawable.arrow_60);
		arrow[4] = context.getResources().getDrawable(R.drawable.arrow_80);
		arrow[5] = context.getResources().getDrawable(R.drawable.arrow_100);
		arrow[6] = context.getResources().getDrawable(R.drawable.arrow_120);
		arrow[7] = context.getResources().getDrawable(R.drawable.arrow_140);
		arrow[8] = context.getResources().getDrawable(R.drawable.arrow_160);
		arrow[9] = context.getResources().getDrawable(R.drawable.arrow_180);
		arrow[10] = context.getResources().getDrawable(R.drawable.arrow_200);
		arrow[11] = context.getResources().getDrawable(R.drawable.arrow_220);
		arrow[12] = context.getResources().getDrawable(R.drawable.arrow_240);
		arrow[13] = context.getResources().getDrawable(R.drawable.arrow_260);
		arrow[14] = context.getResources().getDrawable(R.drawable.arrow_280);
		arrow[15] = context.getResources().getDrawable(R.drawable.arrow_300);
		arrow[16] = context.getResources().getDrawable(R.drawable.arrow_320);
		arrow[17] = context.getResources().getDrawable(R.drawable.arrow_340);
		arrowWidth = arrow[BigPlanet.lastHeading].getIntrinsicWidth();
		arrowHeight = arrow[BigPlanet.lastHeading].getIntrinsicHeight();
		for (int i = 0; i <= 17; i++) {
			arrow[i].setBounds(0, 0, arrowWidth, arrowHeight);
		}
		
		final Handler updateControlsHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case 0:
					updateZoomControls();
					break;
				}
				super.handleMessage(msg);
			}
		};
		
		szEngine = SmoothZoomEngine.getInstance();
		szEngine.setReloadMapCommand(new AbstractCommand() {

			public void execute(Object object) {
				double sf = (Float) object;
				pmap.zoomS(sf);
				updateControlsHandler.sendEmptyMessage(0);
			}

		});
		szEngine.setUpdateScreenCommand(new AbstractCommand() {
			
			public void execute(Object object) {
				pmap.scaleFactor = (Float) object;
				postInvalidate();
			}

		});
	}

	public int getMapMode() {
		return mapMode;
	}

	/**
	 * Устанавливает режим карты и состояние зум-контролов(выбор объекта для
	 * добавления в закладки либо навигация)
	 * 
	 * @param mapMode
	 */
	public void setMapMode(int mapMode) {
		this.mapMode = mapMode;
		updateZoomControls();
	}

	public void setOnMapLongClickListener(
			OnMapLongClickListener onMapLongClickListener) {
		this.onMapLongClickListener = onMapLongClickListener;
	}

	/**
	 * Устанавливает размеры карты и дочерних контролов
	 * 
	 * @param width
	 * @param height
	 */
	public void setSize(int width, int height) {
		if (main != null) {
			removeView(main);
		}
		pmap.resetCell(width, height);
		buildView(width, height, pmap.getDefaultTile());
	}

	/**
	 * Возвращает движок карты
	 * 
	 * @return
	 */
	public PhysicMap getPhysicalMap() {
		return pmap;
	}

	public void goTo(int x, int y, int z, int offsetX, int offsetY) {
		getPhysicalMap().goTo(x, y, z, offsetX, offsetY);
		updateZoomControls();
		updateScreen();
	}

	/**
	 * Строит виджет, устанавливает обработчики, размеры и др.
	 * 
	 * @param width
	 * @param height
	 * @param startTile
	 */
	private void buildView(int width, int height, RawTile startTile) {
		h = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				updateZoomControls();
			}
		};
		
		// создание панели с картой
		main = new Panel(this.getContext());
		addView(main, 0, new ViewGroup.LayoutParams(width, height));
		// создание зум-панели
		if (zoomPanel == null) { // если не создана раньше
			zoomPanel = new ZoomPanel(this.getContext());
			// обработчик уменьшения
			zoomPanel.setOnZoomOutClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.i("MapControl", "OnZoomOutClick");
					BigPlanet.isMapInCenter = false;
					int zoomLevel = PhysicMap.getZoomLevel();
					if (zoomLevel >= 17) {
						return;
					}
					//zoomPanel.setIsZoomOutEnabled(false); // avoid double click to cause grey screen
					scalePoint.set(pmap.getWidth() / 2, pmap.getHeight() / 2);
					smoothZoom(-1);
				}
			});

			// обработчик увеличения
			zoomPanel.setOnZoomInClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.i("MapControl", "OnZoomInClick");
					BigPlanet.isMapInCenter = false;
					int zoomLevel = PhysicMap.getZoomLevel();
					if (zoomLevel <= -2) {
						return;
					}
					//zoomPanel.setIsZoomInEnabled(false); // avoid double click to cause grey screen
					scalePoint.set(pmap.getWidth() / 2, pmap.getHeight() / 2);
					smoothZoom(1);
				}
			});

			addView(zoomPanel, new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));

		}
		// Convert the dips to pixels 
		zoomPanel.setPadding((width - (int)(160*(density))) / 2, height - (int)(50*(density)), 0, 0);

		if (pmap == null) { // если не был создан раньше
			pmap = new PhysicMap(width, height, startTile, new AbstractCommand() {

				/**
				 * Callback, выполняющий перерисовку карты по запросу
				 */
				@Override
				public void execute() {
					updateScreen();
				}

			});
		}
		pmap.quickHack();
	}

	private void smoothZoom(int direction) {
//		System.out.println(getPhysicalMap().getTileResolver().getLoaded());
		szEngine.addToScaleQ(direction);
	}

	public synchronized void updateScreen() {
		if (main != null) {
			main.postInvalidate();
			Intent i = new Intent(BigPlanet.UpdateScreenAction);
			context.sendBroadcast(i);
		}
	}
	
	private void invokeGoToMyLocation(double lat, double lon, int zoom) {
		Intent i = new Intent(BigPlanet.UpdateScreenAction);
		i.putExtra("type", 1);
		i.putExtra("lat", lat);
		i.putExtra("lon", lon);
		i.putExtra("zoom", zoom);
		context.sendBroadcast(i);
	}

	/**
	 * Устанавливает состояние zoomIn/zoomOut контролов в зависимости от уровня
	 * зума
	 */
	public void updateZoomControls() {
		pmap.getTileResolver().clearCache();
		int zoomLevel = PhysicMap.getZoomLevel();
		markerManager.updateAll(zoomLevel);
		if (getMapMode() == MapControl.SELECT_MODE) {
			zoomPanel.setVisibility(View.INVISIBLE);
		} else {
			zoomPanel.setVisibility(View.VISIBLE);
			if (zoomLevel >= 17) {
				zoomPanel.setIsZoomOutEnabled(false);
				zoomPanel.setIsZoomInEnabled(true);
			} else if (zoomLevel <= -2) {
				zoomPanel.setIsZoomOutEnabled(true);
				zoomPanel.setIsZoomInEnabled(false);
			} else {
				zoomPanel.setIsZoomOutEnabled(true);
				zoomPanel.setIsZoomInEnabled(true);
			}
		}
	}

	/**
	 * Перерисовывает карту
	 * 
	 * @param canvas
	 * @param paint
	 */
	private synchronized void doDraw(Canvas c, Paint paint) {
		if (cb == null || cb.getHeight() != pmap.getHeight()) {
			cs = new Canvas();
			cb = Bitmap.createBitmap(pmap.getWidth(), pmap.getHeight(), Bitmap.Config.RGB_565);
			cs.setBitmap(cb);
		}
//		System.out.println("doDraw scaleFactor " + pmap.scaleFactor);
		Bitmap tmpBitmap;
		for (int i = 2; i < pmap.cells.length+2; i++) {
			for (int j = 2; j < pmap.cells[0].length+2; j++) {
				if ((i > 1 && i < pmap.cells.length+2) && ((j > 1 && j < pmap.cells[0].length+2))) {
					tmpBitmap = pmap.getCell(i - 2, j - 2);
					if (tmpBitmap != null) {
						isNew = false;
						cs.drawBitmap(tmpBitmap, 
								(i - 2) * TILE_SIZE + pmap.getGlobalOffset().x, 
								(j - 2) * TILE_SIZE + pmap.getGlobalOffset().y, paint);
					}
				} else {
					if (pmap.scaleFactor == 1) {
						cs.drawBitmap(CELL_BACKGROUND, 
								(i - 2) * TILE_SIZE + pmap.getGlobalOffset().x, 
								(j - 2) * TILE_SIZE + pmap.getGlobalOffset().y, paint);
					} else {
						cs.drawBitmap(EMPTY_BACKGROUND, 
								(i - 2) * TILE_SIZE + pmap.getGlobalOffset().x, 
								(j - 2) * TILE_SIZE + pmap.getGlobalOffset().y, paint);
					}
				}
			}
		}

		if (pmap.scaleFactor == 1) {
			// отрисовка маркеров
			if(!BigPlanet.isGPSTracking){
				for (int i = 2; i < pmap.cells.length+2; i++) {
					for (int j = 2; j < pmap.cells[0].length+2; j++) {
						if ((i > 1 && i < pmap.cells.length+2) && ((j > 1 && j < pmap.cells[0].length+2))) {
							RawTile tile = pmap.getDefaultTile();
							int z = PhysicMap.getZoomLevel();
							int tileX = tile.x + (i - 2);
							int tileY = tile.y + (j - 2);
							List<Marker> markers = markerManager.getMarkers(tileX, tileY, z);
							
							for (Marker marker : markers) {
								if (BigPlanet.currentLocation != null && BigPlanet.currentLocation.getSpeed()>0) {
									Drawable drawable = arrow[BigPlanet.lastHeading];
									cs.drawBitmap(((BitmapDrawable)drawable).getBitmap(),
											(i - 2) * TILE_SIZE	+ pmap.getGlobalOffset().x
											+ (int) marker.getOffset().x
											- 15*density, 
											(j - 2) * TILE_SIZE + pmap.getGlobalOffset().y
											+ (int) marker.getOffset().y
											- 26*density, paint);
								} else {
									cs.drawBitmap(marker.getMarkerImage().getImage(),
											(i - 2) * TILE_SIZE	+ pmap.getGlobalOffset().x
											+ (int) marker.getOffset().x
											- marker.getMarkerImage().getOffsetX()*density, 
											(j - 2) * TILE_SIZE + pmap.getGlobalOffset().y
											+ (int) marker.getOffset().y
											- marker.getMarkerImage().getOffsetY()*density, paint);
								}
							}
						}
					}
				}
			}
		}

		int paintColor[] = new int[] {
				Color.RED,
				Color.BLUE,
				Color.CYAN,
			};
		
		int length = paintColor.length;
		
		boolean isEnabled[] = new boolean[] {
				true,
				BigPlanet.isGPSTracking,
				BigPlanet.isGPSTrackSaved,
			};
			
		int markerType[] = new int[] {
				MarkerManager.START_GREEN_MARKER,
				MarkerManager.START_BLUE_MARKER,
				MarkerManager.START_BLUE_MARKER,
		};
		
		List<List<Marker>> allMarkerList = new ArrayList<List<Marker>>();
		allMarkerList.add(MarkerManager.markersDB);
		allMarkerList.add(MarkerManager.markersG);
		allMarkerList.add(MarkerManager.savedTrackG);
		
		boolean isDrawingMarkerG[] = new boolean[length];
		boolean isDrawing = false;
		for (int m = 0; m < allMarkerList.size(); m++) {
			if (isEnabled[m] && allMarkerList.get(m).size() != 0) {
				isDrawingMarkerG[m] = true;
				isDrawing = true;
			}
		}
		
		if (isDrawing) {
			for (int m = 0; m < allMarkerList.size(); m++) {
				if (isDrawingMarkerG[m]) {
					float x1 = 0, x2 = 0, y1 = 0, y2 = 0, startPointX = 0, startPointY = 0;
					boolean isDrawingStartMarker = false;
					boolean isSetStartPoint = false;
					paint.setColor(paintColor[m]);
					paint.setStrokeWidth(3);
					List<Marker> markerList = allMarkerList.get(m);
					for (int k = 0; k < markerList.size(); k++) {
						Marker marker = markerList.get(k);
						for (int i = 2; i < pmap.cells.length+2; i++) {
							for (int j = 2; j < pmap.cells[0].length+2; j++) {
								if ((i > 1 && i < pmap.cells.length+2) && ((j > 1 && j < pmap.cells[0].length+2))) {
									RawTile tile = pmap.getDefaultTile();
									int z = PhysicMap.getZoomLevel();
									int tileX = tile.x + (i - 2);
									int tileY = tile.y + (j - 2);
									
									boolean result = markerManager.isDrawingMarkerG(tileX, tileY, z, marker);
									if (result) {
										if (isSetStartPoint) {
											x2 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x + (int) marker.getOffset().x;
											y2 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y + (int) marker.getOffset().y+3;
											if (x2 != 0 && y2 != 0) {
												cs.drawLine(x1, y1, x2, y2, paint);
												x1 = x2;
												y1 = y2;
											}
										} else {
											x1 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x + (int) marker.getOffset().x;
											y1 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y + (int) marker.getOffset().y+3;
											if (x1 != 0 && y1 != 0) {
												startPointX = x1;
												startPointY = y1;
												isSetStartPoint = true;
											}	
										}
										
										if (k == 0) {
											isDrawingStartMarker = true;
										} else if (k == markerList.size()-1) {
											markerList.get(k).setMarkerImage(MarkerManager.images.get(markerType[m]+1)); // END_MY_TRACK_MARKER
											cs.drawBitmap(markerList.get(k).getMarkerImage().getImage(),
													x2 - markerList.get(k).getMarkerImage().getOffsetX()*density,
													y2 - markerList.get(k).getMarkerImage().getOffsetY()*density,paint);
										}
									}
								}
							}
						}
					} // end for (int k = 0; k < markerGList.size(); k++)
					if (isDrawingStartMarker) {
						isDrawingStartMarker = false;
						markerList.get(0).setMarkerImage(MarkerManager.images.get(markerType[m])); // START_MY_TRACK_MARKER
						cs.drawBitmap(markerList.get(0).getMarkerImage().getImage(),
								startPointX - markerList.get(0).getMarkerImage().getOffsetX()*density,
								startPointY - markerList.get(0).getMarkerImage().getOffsetY()*density,paint);
					}
				}
			}
		}
		Matrix matr = new Matrix();
		matr.postScale((float) pmap.scaleFactor, (float) pmap.scaleFactor, scalePoint.x, scalePoint.y);
		c.drawColor(BitmapUtils.BACKGROUND_COLOR);
		c.drawBitmap(cb, matr, paint);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		new Thread() {
			@Override
			public void run() {
				while (isNew) {
					try {
						Thread.sleep(100);
						postInvalidate();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}.start();
	}

	/**
	 * Панель, на которую выводится карта
	 * 
	 * @author hudvin
	 * 
	 */
	class Panel extends View {
		Paint paint;

		public Panel(Context context) {
			super(context);
			paint = new Paint();

		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			
			if (BigPlanet.autoDisplayDB && MarkerManager.markersDB.size()!=0 && MarkerManager.markers.size()!=0) {
				if(BigPlanet.autoDisplayDBforMarker){
					markersTemp.add(MarkerManager.markers.get(0));
				}
				BigPlanet.autoDisplayDBforMarker = false;
				
				boolean check = checkMarkersDBforDisplay();
				if (bestZoomFromDB<minZoomFromDB && !check) {
					bestZoomFromDB = bestZoomFromDB+1;
					invokeGoToMyLocation(BigPlanet.autoDisplayDB_Lat, BigPlanet.autoDisplayDB_Lon, bestZoomFromDB);
					// It shows the progress of zooming out and can be disabled to speedup.
					doDraw(canvas, paint);
				} else {
					BigPlanet.autoDisplayDB = false;
					BigPlanet.autoDisplayDBforMarker = true;
					invokeGoToMyLocation(BigPlanet.autoDisplayDB_Lat, BigPlanet.autoDisplayDB_Lon, bestZoomFromDB+1);
					doDraw(canvas, paint);
					bestZoomFromDB = initialZoomFromDB; // initial value
				}
				
			} else {
				if (BigPlanet.autoDisplayDBforMarker && MarkerManager.markersDB.size()!=0) {
					MarkerManager.markers.clear();
					if (!BigPlanet.clearYellowPersonMarker) {
						MarkerManager.markers.add(markersTemp.get(0));
					} else {
						// clear the yellow person marker after loading the track from DB
						// don't execute markers.add()
						BigPlanet.clearYellowPersonMarker = false;
					}
					BigPlanet.autoDisplayDBforMarker = false;
				}
				doDraw(canvas, paint);
			}
		}
		
		public boolean checkMarkersDBforDisplay() {
			// check all points
			int factor = 1;
			int num = MarkerManager.markersDB.size();
			// quick check
			if (num > 40) {
				factor = 10;
				num = Math.round(num/factor);
			}
			int countDB = 0;
			for (int k=0; k < num; k++) {
				int index = k*factor;
				for (int i = 2; i < pmap.cells.length+2; i++) {
					for (int j = 2; j < pmap.cells[0].length+2; j++) {
						if ((i > 1 && i < pmap.cells.length+2) && ((j > 1 && j < pmap.cells[0].length+2))) {
							RawTile tile = pmap.getDefaultTile();
							int tileX = tile.x + (i - 2);
							int tileY = tile.y + (j - 2);

							try {
								if (MarkerManager.markersDB.get(index).tile.x == tileX && 
										MarkerManager.markersDB.get(index).tile.y == tileY && 
										MarkerManager.markersDB.get(index).tile.z == bestZoomFromDB) {
									countDB = countDB + 1;
								}	
							} catch (IndexOutOfBoundsException e) {
								if (MarkerManager.markersDB.get(index-1).tile.x == tileX && 
										MarkerManager.markersDB.get(index-1).tile.y == tileY && 
										MarkerManager.markersDB.get(index-1).tile.z == bestZoomFromDB) {
									countDB = countDB + 1;
								}
							}
						}
					}
				}
			}
//			Log.i("MapControl", "auto map zooming out: z="+(17-bestZoomFromDB)+", "+ countDB +"="+num);
			if (countDB == num) {
				return true;
			} else {
				return false;
			}
		}
		
		long touchTime = 0;

		/**
		 * Обработка касаний
		 */
		@Override
		public boolean onTouchEvent(final MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				pmap.inMove = false;
				touchTime = System.currentTimeMillis();
//				System.out.println("touchTime " + touchTime);
				pmap.getNextMovePoint().set((int) event.getX(), (int) event.getY());
				break;
			case MotionEvent.ACTION_MOVE:
				long now = System.currentTimeMillis();
				long diff = now - touchTime;
				if (pmap.scaleFactor == 1){
//					System.out.println("inmove " + pmap.inMove);
					pmap.inMove = true;
					pmap.moveCoordinates(event.getX(), event.getY());
				}
				// for Auto-Follow
				BigPlanet.disabledAutoFollow(MapControl.this.context);
				if (pmap.inMove && diff>50) {
					touchTime = now;
//					System.out.println("diff " + diff);
					pmap.inMove = false;
					pmap.moveCoordinates(event.getX(), event.getY());
					pmap.quickHack();
					pmap.loadFromCache();
					updateScreen();
				}
				break;
			case MotionEvent.ACTION_UP:
				if (dcDetector.process(event)) { // double-tap
					if (pmap.scaleFactor == 1) { // zoom has stopped
						if (mapMode == MapControl.ZOOM_MODE) {
							
						} else {
							if (onMapLongClickListener != null) {
								onMapLongClickListener.onMapLongClick(0, 0);
							}
						}
					}
				} else { // not double-tap
					if (pmap.inMove) {
						pmap.inMove = false;
						pmap.moveCoordinates(event.getX(), event.getY());
						pmap.quickHack();
						pmap.loadFromCache();
						updateScreen();
						// pmap.reloadTiles();
					}
				}
				break;
			}

			return true;
		}
	}

	public void setMapSource(int sourceId) {
		getPhysicalMap().getTileResolver().setMapSource(sourceId);
		getPhysicalMap().reloadTiles();
		updateScreen();
	}

}
