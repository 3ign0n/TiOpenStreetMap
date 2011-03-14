package tyt.android.bigplanettracks;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import tyt.android.bigplanettracks.maps.BigPlanetApp;
import tyt.android.bigplanettracks.maps.MarkerManager;
import tyt.android.bigplanettracks.maps.MarkerManager.Marker;
import tyt.android.bigplanettracks.maps.PhysicMap;
import tyt.android.bigplanettracks.maps.Place;
import tyt.android.bigplanettracks.maps.Preferences;
import tyt.android.bigplanettracks.maps.RawTile;
import tyt.android.bigplanettracks.maps.SHA1Hash;
import tyt.android.bigplanettracks.maps.Utils;
import tyt.android.bigplanettracks.maps.db.DAO;
import tyt.android.bigplanettracks.maps.db.GeoBookmark;
import tyt.android.bigplanettracks.maps.geoutils.GeoUtils;
import tyt.android.bigplanettracks.maps.loader.TileLoader;
import tyt.android.bigplanettracks.maps.providers.MapStrategyFactory;
import tyt.android.bigplanettracks.maps.storage.LocalStorageWrapper;
import tyt.android.bigplanettracks.maps.storage.SQLLocalStorage;
import tyt.android.bigplanettracks.maps.tools.MapSaverUI;
import tyt.android.bigplanettracks.maps.ui.AddBookmarkDialog;
import tyt.android.bigplanettracks.maps.ui.MapControl;
import tyt.android.bigplanettracks.maps.ui.OnDialogClickListener;
import tyt.android.bigplanettracks.maps.ui.OnMapLongClickListener;
import tyt.android.bigplanettracks.maps.ui.SmoothZoomEngine;
import tyt.android.bigplanettracks.tracks.MyTimeUtils;
import tyt.android.bigplanettracks.tracks.TrackStoringThread;
import tyt.android.bigplanettracks.tracks.TrackTabViewActivity;
import tyt.android.bigplanettracks.tracks.db.TrackDBAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Proxy;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class BigPlanet extends Activity {

	private static final String BOOKMARK_DATA = "bookmark";
	private static int SEARCH_ZOOM = 2;

	private Toast textMessage;
	public String identifier = null;
	public static float density;

	private MapControl mapControl;

	private static MarkerManager mm;

	protected static LocationManager locationManager;
	public static Location currentLocation;
	public static Location currentLocationBeforeRecording;
	public static long recordingTime;
	protected static String locationProvider = null;
	
	private static boolean isFirstEntry = true;
	protected static boolean inHome = false;
	public static boolean isFollowMode = true; // default value is auto follow
	public static boolean isGPSTracking = false;  // default false
	public static boolean isGPSTrackSaved = false;  // default false
	public static boolean isMapInCenter = false;
	public static boolean isDBdrawclear = false; // default false for DB clear
	public static boolean autoDisplayDB = false;
	public static boolean autoDisplayDBforMarker = false;
	public static double autoDisplayDB_Lat = 0;
	public static double autoDisplayDB_Lon = 0;
	public static boolean clearYellowPersonMarker = false;
	
	private boolean SDCARD_AVAILABLE = true;
	
	private MySearchIntentReceiver searchIntentReceiver;
	private MyUpdateScreenIntentReceiver updateScreenIntentReceiver;
	public static String SearchAction = "tyt.android.bigplanettracks.INTENTS.GOTO";
	public static String UpdateScreenAction = "tyt.android.bigplanettracks.INTENTS.UpdateScreen";
	
	private static RelativeLayout mAutoFollowRelativeLayout;
	private RelativeLayout mTrackRelativeLayout;
	private static ImageView scaleImageView;
	private Point myGPSOffset;
	private Point previousGPSOffset = new Point();;
		
	public static TrackDBAdapter DBAdapter;
	private ProgressDialog myGPSDialog = null;
	private Handler mainThreadHandler; // used by TrackStoringThread
	protected static Handler locationHandler;
	protected static Handler titleHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		density = getResources().getDisplayMetrics().density;

		DBAdapter = new TrackDBAdapter();
		mainThreadHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case TrackStoringThread.SUCCESS:
						Intent myIntent = new Intent();
						myIntent.setClass(BigPlanet.this, TrackTabViewActivity.class);
						startActivity(myIntent);
						break;
						
					case TrackStoringThread.FAIL:
						Toast.makeText(
								BigPlanet.this,
								getString(R.string.fail)+"\n"+(String)msg.obj,
								Toast.LENGTH_LONG).show();
						break;
						
					case TrackStoringThread.DialogDismiss:
						myGPSDialog.dismiss();
						break;
				}
			}};
		mainThreadHandler.removeMessages(0);
		
		locationHandler = new Handler() {
			public void handleMessage(Message msg) {
				Location location = (Location) msg.obj;
				switch (msg.what) {
				case MethodStartGPSLocationListener:
					startGPSLocationListener();
					break;
				case MethodGoToMyLocation:
					goToMyLocation(location, PhysicMap.getZoomLevel());
					break;
				case MethodTrackMyLocation:
					trackMyLocation(location, PhysicMap.getZoomLevel());
					break;
				case MethodAddMarker:
					addMarker(location, PhysicMap.getZoomLevel());
					break;
				case MethodUpdateScreen:
					mapControl.updateScreen();
					break;
				}
			}
		};

		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			SDCARD_AVAILABLE = false;
			new AlertDialog.Builder(this).setMessage(R.string.sdcard_unavailable)
					.setCancelable(false).setNeutralButton(R.string.OK_LABEL,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0,
										int arg1) {
									finish();
								}
							}).show();
		} else {
			SDCARD_AVAILABLE = true;
			if (new File(SQLLocalStorage.TRACK_PATH+"/sdcard.xml").exists()) {
				SQLLocalStorage.SD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator;
				SQLLocalStorage.updateSDPaths();
			}
			
			searchIntentReceiver = new MySearchIntentReceiver();
			registerReceiver(searchIntentReceiver, new IntentFilter(SearchAction));
			
			updateScreenIntentReceiver = new MyUpdateScreenIntentReceiver();
			registerReceiver(updateScreenIntentReceiver, new IntentFilter(UpdateScreenAction));
			
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
			SmoothZoomEngine.stop = false;
			mAutoFollowRelativeLayout = getAutoFollowRelativeLayout();
			mAutoFollowRelativeLayout.setVisibility(View.INVISIBLE);
			mTrackRelativeLayout = getTrackRelativeLayout();
			mTrackRelativeLayout.setVisibility(View.VISIBLE);
			
			File trackImportFolder = new File(SQLLocalStorage.TRACK_IMPORT_PATH);
			if (!trackImportFolder.exists())
				trackImportFolder.mkdirs();
			trackImportFolder = null;
			
			File mapsDBFolder = new File(SQLLocalStorage.MAP_PATH);
			if (!mapsDBFolder.exists())
				mapsDBFolder.mkdirs();
			mapsDBFolder = null;

			String proxyHost = Proxy.getDefaultHost();
			int proxyPort = Proxy.getDefaultPort();
			if (proxyHost != null && proxyPort != -1) {
				System.setProperty("http.proxyHost", proxyHost);
				System.setProperty("http.proxyPort", Integer.toString(proxyPort));
				Log.i("Proxy", proxyHost+":"+proxyPort);
			}
			
			initializeMap();
			
			/* Create an ImageView with a auto-follow icon. */
			mapControl.addView(mAutoFollowRelativeLayout); // We can just run it once.
			/* Create an ImageView with a Track icon. */
			mapControl.addView(mTrackRelativeLayout); // We can just run it once.
			/* Create an ImageView with a scale image. */
			scaleImageView = new ImageView(this);
			scaleImageView.setImageResource(R.drawable.scale1);
			mapControl.addView(scaleImageView);
			
			if (!Utils.verify(identifier)) {
				showTrialDialog(R.string.this_is_demo_title, R.string.this_is_demo_message);
			}
			setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

			myGPSOffset = Preferences.getGPSOffset();
			setActivityTitle(BigPlanet.this);
			new Thread("LoadDefaultTimeZone") {
				public void run() {
					// pre-invoke to speedup for SDK 1.5
					String tz = "TimeZone="+TimeZone.getDefault().getID();
					System.out.println(tz+" ("+MyTimeUtils.getLocalTimeString(System.currentTimeMillis())+")");
				}
			}.start();
		}
	}
	
	public static void disabledAutoFollow(Context context) {
		if (isFollowMode) {
			Toast.makeText(context, R.string.auto_follow_disabled, Toast.LENGTH_SHORT).show();
			mAutoFollowRelativeLayout.setVisibility(View.VISIBLE);
			isFollowMode = false;
		}
		setActivityTitle((Activity) context);
	}
	
	public void enabledAutoFollow(Context context) {
		if (!isFollowMode) {
			Toast.makeText(context, R.string.auto_follow_enabled, Toast.LENGTH_SHORT).show();
			mAutoFollowRelativeLayout.setVisibility(View.INVISIBLE);
			if (currentLocation != null)
				goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
			isFollowMode = true;
		}
		setActivityTitle((Activity) context);
	}
	
	private RelativeLayout getAutoFollowRelativeLayout() {
		final RelativeLayout relativeLayout = new RelativeLayout(this);
		
		/* Create an ImageView with a auto-follow icon. */
		final ImageView ivAutoFollow = new ImageView(this);
		ivAutoFollow.setImageResource(R.drawable.autofollow);
		
		ivAutoFollow.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				enabledAutoFollow(BigPlanet.this);
			}
		});
		
		/* Create RelativeLayoutParams, that position in in the bottom right corner. */
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		relativeLayout.addView(ivAutoFollow, params);
		
		return relativeLayout;
	}
	
	private void enabledTrack(Context context) {
		if (isGPSTracking) {
			MarkerManager.savedTrackG.clear();
			Toast.makeText(context, R.string.track_enabled, Toast.LENGTH_SHORT).show();
			// start service
			Intent intent = new Intent(this, MyLocationService.class);
			this.startService(intent);
		}
		setActivityTitle((Activity) context);
		mapControl.invalidate();
	}
	
	private void disabledTrack(Context context) {
		if (!isGPSTracking) {
			// stop service
			Intent intent = new Intent(this, MyLocationService.class);
			this.stopService(intent);
		}
		mm.saveMarkerGTrack();
		isGPSTrackSaved = true;
		setActivityTitle((Activity) context);
		mapControl.invalidate();
	}

	private ImageView ivRecordTrack;
	
	private RelativeLayout getTrackRelativeLayout() {
		final RelativeLayout relativeLayout = new RelativeLayout(this);
		
		/* Create a ImageView with a track icon. */
		ivRecordTrack = new ImageView(this);
		ivRecordTrack.setImageResource(R.drawable.btn_record_start);
		
		ivRecordTrack.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				toggleTrackButton();
			}
		});
		
		/* Create RelativeLayoutParams, that position in in the bottom right corner. */
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		relativeLayout.addView(ivRecordTrack, params);
		
		return relativeLayout;
	}

	private void toggleTrackButton() {
		if (!isGPSTracking) {
			isGPSTracking = true;
			ivRecordTrack.setImageResource(R.drawable.btn_record_stop);
			enabledTrack(BigPlanet.this);
			currentLocationBeforeRecording = currentLocation;
			recordingTime = System.currentTimeMillis();
			// log track by using gpsLocationListener
			if (locationManager != null) {
				if (networkLocationListener != null) {
					locationManager.removeUpdates(networkLocationListener);
					BigPlanet.locationProvider = "gps 1 0";
					setActivityTitle(BigPlanet.this);
				}
			}
		} else {
			if (MarkerManager.getLocationList(MarkerManager.markersG).size()>1) {
				// a dialog to make sure that user wants to finish recording
				new AlertDialog.Builder(BigPlanet.this).setTitle(R.string.finish_tracking)
				.setPositiveButton(
						R.string.YES_LABEL,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								finishTracking();
							}
						})
				.setNeutralButton(
						R.string.NO_LABEL,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						})
				.show();
			} else {
				finishTracking();
			}
		}
	}

	private void finishTracking() {
		isGPSTracking = false;
		ivRecordTrack.setImageResource(R.drawable.btn_record_start);
		disabledTrack(BigPlanet.this);
//		Log.i("Message","Start to store GPS Locations to DB...");
	
		// check out whether GPS LocationList contains any GPS data or not
		// due to at least two locations needed to compute the "Distance" measurement
		if (MarkerManager.getLocationList(MarkerManager.savedTrackG).size()>1) {
			final CharSequence strDialogTitle = getString(R.string.str_store_gps_location_to_db_title);
			final CharSequence strDialogBody = getString(R.string.str_store_gps_location_to_db_body);
			myGPSDialog = ProgressDialog.show(
					BigPlanet.this,
					strDialogTitle,
					strDialogBody, 
					true);
			
			TrackStoringThread trackStoringThread = new TrackStoringThread();
			trackStoringThread.setMainHandler(mainThreadHandler);
			trackStoringThread.setLocationList(MarkerManager.getLocationList(MarkerManager.savedTrackG));
			trackStoringThread.start();
		} else {
			Toast.makeText(
						BigPlanet.this,
						getString(R.string.gps_locationlist_has_no_data),
						Toast.LENGTH_LONG).show();
			clearSaveTracksG();
//			Log.i("Message","The size of LocationList is less than two points");
		}
	}

	private void initializeMap() {
		// create maps
		mm = new MarkerManager(getResources());
		RawTile savedTile = Preferences.getTile();
		//savedTile.s = 0;
		configMapControl(savedTile);
		// use the network or not
		boolean useNet = Preferences.getUseNet();
		mapControl.getPhysicalMap().getTileResolver().setUseNet(useNet);
		// map source
		int mapSourceId = Preferences.getSourceId();
		mapControl.getPhysicalMap().getTileResolver().setMapSource(mapSourceId);
		mapControl.getPhysicalMap().getDefaultTile().s = mapSourceId;
		// global offset
		Point globalOffset = Preferences.getOffset();
		//globalOffset.x = 0;
		//globalOffset.y = -32;
//		System.out.println("offset " + globalOffset + " " + savedTile);
		mapControl.getPhysicalMap().setGlobalOffset(globalOffset);
		mapControl.getPhysicalMap().reloadTiles();
	}

	public class MySearchIntentReceiver extends BroadcastReceiver {
		/**
		 * @see adroid.content.BroadcastReceiver#onReceive(android.content.Context,
		 *      android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			isFollowMode = true;
			disabledAutoFollow(BigPlanet.this);
			int z = SEARCH_ZOOM;
			Place place = (Place) intent.getSerializableExtra("place");
			mm.clearMarkerManager();
			mm.addMarker(place, z, MarkerManager.DrawMarkerForSearch, MarkerManager.SEARCH_MARKER);
			tyt.android.bigplanettracks.maps.geoutils.Point p = GeoUtils.toTileXY(place.getLat(), place.getLon(), z);
			tyt.android.bigplanettracks.maps.geoutils.Point off = GeoUtils.getPixelOffsetInTile(place.getLat(), place.getLon(), z);
			mapControl.goTo((int) p.x, (int) p.y, z, (int) off.x, (int) off.y);
			
			final GeoBookmark newGeoBookmark = new GeoBookmark();
			String query = (String) intent.getSerializableExtra("query");
			newGeoBookmark.setName(query);
			newGeoBookmark.setOffsetX(mapControl.getPhysicalMap().getGlobalOffset().x);
			newGeoBookmark.setOffsetY(mapControl.getPhysicalMap().getGlobalOffset().y);
			newGeoBookmark.setTile(mapControl.getPhysicalMap().getDefaultTile());
			newGeoBookmark.getTile().s = mapControl.getPhysicalMap().getTileResolver().getMapSourceId();
			DAO d = new DAO(BigPlanet.this);
			d.saveGeoBookmark(newGeoBookmark);
		}
	}
	
	public static final int MethodStartGPSLocationListener = 0;
	public static final int MethodGoToMyLocation = 1;
	public static final int MethodTrackMyLocation = 2;
	public static final int MethodAddMarker = 3;
	public static final int MethodSetActivityTitle = 4;
	public static final int MethodUpdateScreen = 5;
	
	public class MyUpdateScreenIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			int type = intent.getIntExtra("type", 0);
//			System.out.println("UpdateScreen.onReceive() type="+type+" "+isMapInCenter);
			if (type >= 1) {
				// see MapControl.invokeGoToMyLocation() and BigPlanet.addMarkersForDrawing()
				double lat = intent.getDoubleExtra("lat", 0);
				double lon = intent.getDoubleExtra("lon", 0);
				int zoom = intent.getIntExtra("zoom", PhysicMap.getZoomLevel());
				
				// for BigPlanet.addMarkersForDrawing() only
				if (type == 2) {
					mAutoFollowRelativeLayout.setVisibility(View.VISIBLE);
					isFollowMode = false;
				}
				
				goToMyLocation(lat, lon, zoom);
//				Log.i("goToMyLocation(lon, lat)", "type="+type+", "+lon+", "+lat);
			} else {
				// center map
				centerMap();
			}
			// refresh the activity title
			setActivityTitle(BigPlanet.this);
		}
	}
	
	private void centerMap() {
		if (isFollowMode && !isMapInCenter) {
			if (currentLocation != null) {
				isMapInCenter = true;
				int zoom = PhysicMap.getZoomLevel();
				double latFix = currentLocation.getLatitude() + myGPSOffset.y*Math.pow(10, -5);
				double lonFix = currentLocation.getLongitude() + myGPSOffset.x*Math.pow(10, -5);
				tyt.android.bigplanettracks.maps.geoutils.Point p = GeoUtils.toTileXY(latFix, lonFix, zoom);
				tyt.android.bigplanettracks.maps.geoutils.Point off = GeoUtils.getPixelOffsetInTile(latFix, lonFix, zoom);
				mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);
			}
		} else {
			// when showing search result, recording under non-auto-follow, etc.
			mapControl.invalidate();
		}
	}

	@Override
	public boolean onSearchRequested() {
		startSearch("", false, null, false);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		configMapControl(mapControl.getPhysicalMap().getDefaultTile());
		if (isFollowMode && currentLocation != null) {
			goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
		} else {
			mapControl.getPhysicalMap().reloadTiles();
			mapControl.invalidate();
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (SDCARD_AVAILABLE) {
			if (isGPSTracking){
				ivRecordTrack.setImageResource(R.drawable.btn_record_stop);
			} else{
				ivRecordTrack.setImageResource(R.drawable.btn_record_start);
			}
			if (isFollowMode){
				mAutoFollowRelativeLayout.setVisibility(View.INVISIBLE);
			} else{
				mAutoFollowRelativeLayout.setVisibility(View.VISIBLE);
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (SDCARD_AVAILABLE) {
			startGPSLocationListener();
			if (isFirstEntry) {
				isFirstEntry = false;
				isFollowMode = false;
				followMyLocation();
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finishGPSLocationListener(); // release the GPS resources
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		DBAdapter.close();
		DBAdapter = null;
		SmoothZoomEngine.sze = null; // release the variable
		SmoothZoomEngine.stop = true; // stop the thread
		TileLoader.stop = true; // stop the thread
		mAutoFollowRelativeLayout = null;
		if (searchIntentReceiver != null) {
			unregisterReceiver(searchIntentReceiver);
			Preferences.putTile(mapControl.getPhysicalMap().getDefaultTile());
			Preferences.putOffset(mapControl.getPhysicalMap().getGlobalOffset());
		}
		if (updateScreenIntentReceiver != null) {
			unregisterReceiver(updateScreenIntentReceiver);
		}
		if (textMessage != null) {
			textMessage.cancel();
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			if (requestCode == 0) {
				isFollowMode = true;
				disabledAutoFollow(BigPlanet.this);
				GeoBookmark bookmark = (GeoBookmark) data.getSerializableExtra(BOOKMARK_DATA);
				mapControl.getPhysicalMap().setDefTile(bookmark.getTile());
	
				Point offset = new Point();
				offset.set(bookmark.getOffsetX(), bookmark.getOffsetY());
				Preferences.putSourceId(bookmark.getTile().s);
				mapControl.getPhysicalMap().setGlobalOffset(offset);
				mapControl.getPhysicalMap().reloadTiles();
				mapControl.setMapSource(bookmark.getTile().s);
			}
		default:
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			// if the current mode is SELECT_MODE - change to ZOOM_MODE
			if (mapControl.getMapMode() == MapControl.SELECT_MODE) {
				mapControl.setMapMode(MapControl.ZOOM_MODE);
				return true;
			} else if (isGPSTracking) {
				Intent intentHome = new Intent("android.intent.action.MAIN");
				intentHome.addCategory("android.intent.category.HOME");
				startActivity(intentHome);
				return true;
			}
		default:
			return super.onKeyDown(keyCode, event);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		SubMenu sub;
		
		menu.add(1, 11, 0, R.string.SEARCH_MENU).setIcon(R.drawable.search);

		boolean hasSavedTracksG = checkMarkers(MarkerManager.savedTrackG);
		boolean hasMarkersDB = checkMarkers(MarkerManager.markersDB);
		if (hasSavedTracksG || hasMarkersDB) {
			sub = menu.addSubMenu(6, 6, 0, R.string.TRACK_MENU).setIcon(R.drawable.track_manage);
			sub.add(6, 61, 0, R.string.MANAGE_TRACK_MENU);
			if (hasSavedTracksG)
				sub.add(6, 62, 1, R.string.CLEAR_RECORDED_TRACK_MENU);
			if (hasMarkersDB)
				sub.add(6, 63, 2, R.string.CLEAR_LOADED_TRACK_MENU);
			if (hasSavedTracksG && hasMarkersDB)
				sub.add(6, 64, 3, R.string.CLEAR_ALL_TRACKS_MENU);
		} else {
			menu.add(6, 61, 0, R.string.TRACK_MENU).setIcon(R.drawable.track_manage);
		}

		sub = menu.addSubMenu(2, 2, 0, R.string.BOOKMARKS_MENU).setIcon(R.drawable.bookmark);
		sub.add(2, 21, 0, R.string.BOOKMARK_ADD_MENU);
		sub.add(2, 22, 1, R.string.BOOKMARKS_VIEW_MENU);

		menu.add(3, 31, 0, R.string.MY_LOCATION_MENU).setIcon(R.drawable.home);

		menu.add(5, 51, 0, R.string.SQLiteDB_MENU).setIcon(R.drawable.map);
		
		// More menu
		sub = menu.addSubMenu(4, 4, 0, R.string.TOOLS_MENU).setIcon(R.drawable.tools);
		sub.add(4, 41, 0, R.string.NETWORK_MODE_MENU);
		sub.add(4, 42, 1, R.string.CACHE_MAP_MENU);
		sub.add(4, 43, 2, R.string.MAP_SOURCE_MENU);
		sub.add(4, 44, 3, R.string.GPS_OFFSET_MENU);
		sub.add(4, 49, 10, R.string.ABOUT_MENU);

		boolean useNet = Preferences.getUseNet();
		menu.findItem(42).setEnabled(useNet);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		hideMessage();
		switch (item.getItemId()) {
		case 11:
			showSearch();
			break;
		case 21:
			switchToBookmarkMode();
			break;
		case 22:
			showAllGeoBookmarks();
			break;
		case 31:
			followMyLocation();
			break;
		case 41:
			selectNetworkMode();
			break;
		case 42:
			if (BigPlanetApp.isDemo) {
				if (PhysicMap.getZoomLevel() <= 6) {
					showTrialDialog(R.string.try_demo_title, R.string.try_demo_message);
				} else {
					showMapSaver();
				}
			} else {
				showMapSaver();
			}
			break;
		case 43:
			selectMapSource();
			break;
		case 44:
			selectGPSOffset();
			break;
		case 49:
			showAbout();
			break;
		case 51:
			if (BigPlanetApp.isDemo) {
				showTrialDialog(R.string.try_demo_title, R.string.try_demo_message);
			} else {
				selectSQLiteDBFile();
			}
			break;
		case 61: //import and browse tracks from SD card
			importAndBrowseTracks();
			break;
		case 62: //clear track
			clearSaveTracksG();
			break;
		case 63: //clear track
			clearMarkerDB();
			break;
		case 64: //clear all tracks on the map
			clearMap();
			break;

		}
		return false;
	}

	private boolean checkMarkers(List<Marker> list) {
		if(list.size()>0){
			return true;
		}
		return false;
	}

	/**
	 * Sets the size of maps and other properties
	 */
	private void configMapControl(RawTile tile) {
		WindowManager wm = this.getWindowManager();
		Display display = wm.getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		height = height - (int)(50*(density)); // minus the space of the status bar
		
		if (mapControl == null) {
			identifier = getString(R.string.ABOUT_URL);
			mapControl = new MapControl(this, width, height, tile, mm);
			mapControl.setOnMapLongClickListener(new OnMapLongClickListener() {

				@Override
				public void onMapLongClick(int x, int y) {
					hideMessage();
					final GeoBookmark newGeoBookmark = new GeoBookmark();
					newGeoBookmark.setOffsetX(mapControl.getPhysicalMap().getGlobalOffset().x);
					newGeoBookmark.setOffsetY(mapControl.getPhysicalMap().getGlobalOffset().y);
					newGeoBookmark.setTile(mapControl.getPhysicalMap().getDefaultTile());
					newGeoBookmark.getTile().s = mapControl.getPhysicalMap().getTileResolver().getMapSourceId();

					AddBookmarkDialog.show(BigPlanet.this, newGeoBookmark,
							new OnDialogClickListener() {
								@Override
								public void onCancelClick() {
								}
								
								@Override
								public void onOkClick(Object obj) {
									GeoBookmark geoBookmark = (GeoBookmark) obj;
									DAO d = new DAO(BigPlanet.this);
									d.saveGeoBookmark(geoBookmark);
									mapControl.setMapMode(MapControl.ZOOM_MODE);
								}
							});
				}
			});
		} else {
			mapControl.setSize(width, height);
		}
		mapControl.updateZoomControls();
		setContentView(mapControl, new ViewGroup.LayoutParams(width, height));
	}

	private void importAndBrowseTracks(){
		Intent importTrackIntent = new Intent(this, TrackTabViewActivity.class);
		startActivity(importTrackIntent);
	}
	
	private void startGPSLocationListener() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		String provider = locationManager.getBestProvider(criteria, true); //gps
		if (provider != null) {
			if (!inHome) {
				Location location = locationManager.getLastKnownLocation(provider);
				if (location == null) {
					// go to NTU if there's no GPS received after the app is installed.
					location = new Location("");
					location.setLatitude(25.01736);
					location.setLongitude(121.54066);
				}
				currentLocation = location;
				inHome = true;
			}
		} else { // gps and network are both disabled
			Toast.makeText(this, R.string.msg_unable_to_get_current_location, Toast.LENGTH_SHORT).show();
			BigPlanet.locationProvider = null;
		}

		/* GPS_PROVIDER */
		if (gpsLocationListener == null) {
			gpsLocationListener = new MyLocationService();
			gpsLocationListener.registerSensor(this);
			// LocationManager.GPS_PROVIDER = "gps"
			provider = LocationManager.GPS_PROVIDER;
			locationManager.requestLocationUpdates(provider, minTime, minDistance, gpsLocationListener);
			Log.i("Location", provider +" requestLocationUpdates() "+ minTime +" "+ minDistance);
		}

		/* NETWORK_PROVIDER */
		if (networkLocationListener == null) {
			networkLocationListener = new MyLocationService();
			networkLocationListener.registerSensor(this);
			// LocationManager.NETWORK_PROVIDER = "network"
			provider = LocationManager.NETWORK_PROVIDER;
			locationManager.requestLocationUpdates(provider, minTime, minDistance, networkLocationListener);
			Log.i("Location", provider +" requestLocationUpdates() "+ minTime +" "+ minDistance);
		}
	}
	
	protected static void finishGPSLocationListener() {
		new Thread("finishGPS") {
			public void run() {
				if (!isGPSTracking) {
					if (locationManager != null) {
						if (networkLocationListener != null) {
							networkLocationListener.unregisterSensor();
							locationManager.removeUpdates(networkLocationListener);
						}
						if (gpsLocationListener != null) {
							gpsLocationListener.unregisterSensor();
							locationManager.removeUpdates(gpsLocationListener);
						}
						
						networkLocationListener = null;
						gpsLocationListener = null;
						BigPlanet.locationProvider = null;
					}
				}
			}
		}.start();
	}
	
	protected static MyLocationService gpsLocationListener;
	protected static MyLocationService networkLocationListener;
	protected final static long minTime = 2000; // ms
	protected final static float minDistance = 5; // m
	
	private void followMyLocation() {
		if (!isFollowMode) {
			enabledAutoFollow(this);
		} else {
			disabledAutoFollow(this);
		}
	}

	private void goToMyLocation(Location location, int zoom) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		goToMyLocation(lat, lon, zoom);
	}
	
	private void goToMyLocation(double lat, double lon, int zoom) {
		double latFix = lat + myGPSOffset.y*Math.pow(10, -5);
		double lonFix = lon + myGPSOffset.x*Math.pow(10, -5);
		tyt.android.bigplanettracks.maps.geoutils.Point p = GeoUtils.toTileXY(latFix, lonFix, zoom);
		tyt.android.bigplanettracks.maps.geoutils.Point off = GeoUtils.getPixelOffsetInTile(latFix, lonFix, zoom);
		mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);
		
		Place place = new Place();
		place.setLat(latFix);
		place.setLon(lonFix);
		mm.addMarker(place, zoom, MarkerManager.DrawMarkerOrTrack, MarkerManager.MY_LOCATION_MARKER);
	}
	
	private void trackMyLocation(Location location, int zoom) {
		double latFix = location.getLatitude() + myGPSOffset.y*Math.pow(10, -5);;
		double lonFix = location.getLongitude() + myGPSOffset.x*Math.pow(10, -5);
		tyt.android.bigplanettracks.maps.geoutils.Point p = GeoUtils.toTileXY(latFix, lonFix, zoom);
		tyt.android.bigplanettracks.maps.geoutils.Point off = GeoUtils.getPixelOffsetInTile(latFix, lonFix, zoom);
		mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);

		Place place = new Place();
		place.setLat(latFix);
		place.setLon(lonFix);
		place.setLocation(location);
		
//		SimpleDateFormat LocalDateTimeFormatter = new SimpleDateFormat("yyyy.MM.dd G 'at' hh:mm:ss a zzz");
//		List<String> items = new ArrayList<String>();
//		items.add(LocalDateTimeFormatter.format(time));
		mm.addMarker(place, zoom, MarkerManager.DrawMarkerOrTrack, MarkerManager.MY_LOCATION_MARKER);
	}
	
	private void addMarker(Location location, int zoom) {
		double latFix = location.getLatitude() + myGPSOffset.y*Math.pow(10, -5);;
		double lonFix = location.getLongitude() + myGPSOffset.x*Math.pow(10, -5);
		Place place = new Place();
		place.setLat(latFix);
		place.setLon(lonFix);
		place.setLocation(location);
		mm.addMarker(place, zoom, MarkerManager.DrawMarkerOrTrack, MarkerManager.MY_LOCATION_MARKER);
	}
	
	public static void addMarkersForDrawing(Context context, List<Place> placeList, int trackType) {
		Log.i("Message", "At addMarkerForDrawing...");
		
		int zoom = PhysicMap.getZoomLevel();
		double latTotal = 0;
		double lonTotal = 0;

		for (int i=0;i<placeList.size();i++) {
			Place place = placeList.get(i);
			double lat = place.getLat();
			double lon = place.getLon();
			latTotal += lat;
			lonTotal += lon;
			place.setLat(lat);
			place.setLon(lon);
			mm.addMarker(place, zoom, trackType, MarkerManager.MY_LOCATION_MARKER);
		}
		if (trackType == MarkerManager.DrawTrackFromDB) {
			autoDisplayDB = true;
			autoDisplayDBforMarker = true;
		}
		if (!isDBdrawclear) {
			isDBdrawclear = true;
		}
		Intent i = new Intent(BigPlanet.UpdateScreenAction);
		if (trackType == MarkerManager.DrawTrackFromDB) {
			autoDisplayDB_Lat = latTotal/placeList.size();
			autoDisplayDB_Lon = lonTotal/placeList.size();
//			Log.i("autoDisplayDB: lon, lat", autoDisplayDB_Lon+", "+autoDisplayDB_Lat);
			clearYellowPersonMarker = true;
			i.putExtra("type", 2);
			i.putExtra("lat", autoDisplayDB_Lat);
			i.putExtra("lon", autoDisplayDB_Lon);
			i.putExtra("zoom", zoom);
		}
		context.sendBroadcast(i);
	}
	
	private void clearSaveTracksG() {
		if (!mm.clearSavedTracksG()) {
			textMessage = Toast.makeText(this, R.string.clearSaveTrackFales,
					Toast.LENGTH_LONG);
			textMessage.show();
		}
		mapControl.invalidate();
	}
	
	private void clearMarkerDB() {
		mm.clearMarkersDB();
		mapControl.invalidate();
	}
	
	private void clearMap() {
		clearSaveTracksG();
		clearMarkerDB();
	}
	
	private void showTrialDialog(int title, int message) {
		final Dialog paramsDialog = new Dialog(this);
		final View v = View.inflate(this, R.layout.demodialog, null);
		final TextView messageValue = (TextView) v.findViewById(R.id.message);
		messageValue.setText(message);
		final Button okBtn = (Button) v.findViewById(R.id.okBtn);
		okBtn.setEnabled(false);
		okBtn.setClickable(false);
		okBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				paramsDialog.dismiss();
			}

		});
		paramsDialog.setTitle(title);
		paramsDialog.setCanceledOnTouchOutside(false);
		paramsDialog.setCancelable(false);
		paramsDialog.setContentView(v);
		paramsDialog.show();

		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				int okValue = (Integer) msg.what;

				if (okValue == 0) {
					okBtn.setText(R.string.OK_LABEL);
					okBtn.setEnabled(true);
				} else {
					okBtn.setText(String.valueOf(okValue));
				}
			}
		};

		new Thread() {
			int count = 5;
			boolean exec = true;
			@Override
			public void run() {
				while (exec) {
					try {
						Thread.sleep(1000);
						count--;
						if (count == 0) {
							exec = false;
						}
						Message message = handler.obtainMessage(count);
						handler.sendMessage(message);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	private void showSearch() {
		onSearchRequested();
	}

	private void showAbout() {
		final String url = getString(R.string.ABOUT_URL);
		String about = getString(R.string.ABOUT_MESSAGE).replace("{url}", url);
		TextView tv = new TextView(this);
		tv.setLinksClickable(true);
		tv.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
		tv.setAutoLinkMask(Linkify.WEB_URLS);
		tv.setGravity(Gravity.CENTER);
		tv.setText(about);
		tv.setTextSize(12f);
		String versionName = "";
		String packageNum = "";
		try {
			String packageName = BigPlanet.class.getPackage().getName();
			PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
			versionName = info.versionName;
			packageNum = SHA1Hash.encode(info.packageName);
			packageNum = packageNum.substring(0,3);
		} catch (PackageManager.NameNotFoundException e) {
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		ScrollView scrollPanel = new ScrollView(this);
		scrollPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		scrollPanel.addView(tv);
		try {
			if ((Integer.parseInt(packageNum)-1)%10==0) {
				new AlertDialog.Builder(this)
				.setTitle(getString(R.string.ABOUT_TITLE)+" "+versionName)
				.setView(scrollPanel).setIcon(R.drawable.globe)
				.setPositiveButton(
						R.string.OK_LABEL,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						})
				.setNeutralButton(
						R.string.website,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								Intent i = new Intent(Intent.ACTION_VIEW);
								i.setData(Uri.parse(url));
								startActivity(i);
							}
						})
				.show();
			}
		} catch (NumberFormatException e) {
		}
	}

	private void showAllGeoBookmarks() {
		Intent intent = new Intent();
		intent.setClass(this, AllGeoBookmarks.class);
		startActivityForResult(intent, 0);
	}

	private void switchToBookmarkMode() {
		if (mapControl.getMapMode() != MapControl.SELECT_MODE) {
			mapControl.setMapMode(MapControl.SELECT_MODE);
			showMessage();
		}
	}

	private void showMessage() {
		textMessage = Toast.makeText(this, R.string.SELECT_OBJECT_MESSAGE,
				Toast.LENGTH_LONG);
		textMessage.show();
	}

	private void hideMessage() {
		if (textMessage != null) {
			textMessage.cancel();
		}
	}

	/**
	 * Displays a dialog for caching maps in a given radius
	 */
	private void showMapSaver() {
		MapSaverUI mapSaverUI = new MapSaverUI(this, 
				PhysicMap.getZoomLevel(), 
				mapControl.getPhysicalMap().getAbsoluteCenter(), 
				mapControl.getPhysicalMap().getTileResolver().getMapSourceId());
		mapSaverUI.show();
	}

	/**
	 * Creates a radio button with the given parameters
	 * 
	 * @param label
	 * @param id
	 * @return
	 */
	private RadioButton buildRadioButton(String label, int id) {
		RadioButton btn = new RadioButton(this);
		btn.setText(label);
		btn.setId(id);
		return btn;
	}

	/**
	 * Creates a dialog to select the mode (offline, online)
	 */
	private void selectNetworkMode() {
		final Dialog networkModeDialog;
		networkModeDialog = new Dialog(this);
		networkModeDialog.setCanceledOnTouchOutside(true);
		networkModeDialog.setCancelable(true);
		networkModeDialog.setTitle(R.string.NETWORK_MODE_MENU);
		
		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);

		TextView mTextView = new TextView(this);
		mTextView.setText(R.string.SELECT_NETWORK_MODE_LABEL);
		mainPanel.addView(mTextView);
		
		RadioGroup modesRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		modesRadioGroup.addView(buildRadioButton(getResources().getString(
				(R.string.OFFLINE_MODE_LABEL)), 0), 0, layoutParams);

		modesRadioGroup.addView(buildRadioButton(getResources().getString(
				R.string.ONLINE_MODE_LABEL), 1), 0, layoutParams);

		boolean useNet = Preferences.getUseNet();
		int checked = 0;
		if (useNet) {
			checked = 1;
		}
		modesRadioGroup.check(checked);

		modesRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						boolean useNet = checkedId == 1;
						mapControl.getPhysicalMap().getTileResolver()
								.setUseNet(useNet);
						Preferences.putUseNet(useNet);
						networkModeDialog.dismiss();
					}
				});

		mainPanel.addView(modesRadioGroup);
		networkModeDialog.setContentView(mainPanel);
		networkModeDialog.show();
	}

	/**
	 * Creates a dialog to select the map source
	 */
	private void selectMapSource() {
		final Dialog mapSourceDialog;
		mapSourceDialog = new Dialog(this);
		mapSourceDialog.setCanceledOnTouchOutside(true);
		mapSourceDialog.setCancelable(true);
		mapSourceDialog.setTitle(R.string.SELECT_MAP_SOURCE_TITLE);

		ScrollView scrollPanel = new ScrollView(this);
		scrollPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);

		RadioGroup sourcesRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		for (Integer id : MapStrategyFactory.strategies.keySet()) {
			sourcesRadioGroup.addView(
					buildRadioButton(MapStrategyFactory.strategies.get(id)
							.getDescription(), id), 0, layoutParams);
		}

		sourcesRadioGroup.check(Preferences.getSourceId());

		sourcesRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						Preferences.putSourceId(checkedId);
						mapControl.setMapSource(checkedId);
						mapSourceDialog.dismiss();
					}
				});

		mainPanel.addView(sourcesRadioGroup);
		scrollPanel.addView(mainPanel);
		mapSourceDialog.setContentView(scrollPanel);
		mapSourceDialog.show();
	}
	
	private void selectSQLiteDBFile() {
		final Dialog mSQLiteDBFileDialog;
		mSQLiteDBFileDialog = new Dialog(this);
		mSQLiteDBFileDialog.setCanceledOnTouchOutside(true);
		mSQLiteDBFileDialog.setCancelable(true);
		mSQLiteDBFileDialog.setTitle(R.string.SELECT_SQLite_DATABASE);

		ScrollView scrollPanel = new ScrollView(this);
		scrollPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);
		
		TextView mTextView = new TextView(this);
		String folderMessage = getString(R.string.SELECT_SQLite_Folder).replace("%s", SQLLocalStorage.MAP_PATH);
		mTextView.setText("  "+folderMessage+"\n");
		mainPanel.addView(mTextView);

		RadioGroup sqliteRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);
		
		final Map<Integer, String> sqliteMaps = new HashMap<Integer, String>();
		int mapsIndex = 1;
		// SQLLocalStorage.MAP_PATH = "/sdcard/RMaps/maps/";
		File mapsDBFolder = new File(SQLLocalStorage.MAP_PATH);
		if (mapsDBFolder.exists() && mapsDBFolder.isDirectory()) {
			String[] files = mapsDBFolder.list();
//			for (int i = 0; i < files.length; i++) {
//				Log.i("sqlitedb", files[i]);
//			}
			java.util.Arrays.sort(files);
			String[] filesSorted = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				filesSorted[files.length-1-i] = files[i];
			}
			for (int i = 0; i < filesSorted.length; i++) {
				File sqliteFile = new File(mapsDBFolder, filesSorted[i]);
				if (sqliteFile.isFile()) {
					String strSQLiteName = sqliteFile.getName();
					if (strSQLiteName.endsWith(".sqlitedb")) {
						sqliteMaps.put(mapsIndex, strSQLiteName);
						mapsIndex++;
					} else {
						// skip...
					}
				}
			}
		}
		if (sqliteMaps.isEmpty()) {
			sqliteMaps.put(0, SQLLocalStorage.SQLITEDB);
			if (!mapsDBFolder.exists())
				mapsDBFolder.mkdirs();
		}
		mapsDBFolder = null;
		for (Integer key : sqliteMaps.keySet()) {
			String strSQLiteName = sqliteMaps.get(key);
			sqliteRadioGroup.addView(
					buildRadioButton(strSQLiteName, key), 0, layoutParams);
			if (strSQLiteName.equalsIgnoreCase(Preferences.getSQLiteName())) {
				sqliteRadioGroup.check(key);
			}
		}

		sqliteRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						String strSQLiteName = sqliteMaps.get(checkedId);
						Preferences.putSQLiteName(strSQLiteName);
						// switch SQLite DB
						LocalStorageWrapper.switchLocalStorage();
						// update screen
						initializeMap();
						mSQLiteDBFileDialog.dismiss();
					}
				});

		mainPanel.addView(sqliteRadioGroup);
		scrollPanel.addView(mainPanel);
		mSQLiteDBFileDialog.setContentView(scrollPanel);
		mSQLiteDBFileDialog.show();
	}
	
	private void selectGPSOffset() {
		final Dialog mGPSOffsetDialog;
		mGPSOffsetDialog = new Dialog(this);
		mGPSOffsetDialog.setCancelable(true);
		mGPSOffsetDialog.setTitle(R.string.GPS_OFFSET_MENU);
		
		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);
		
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);

		final TextView mTextView = new TextView(this);
		mTextView.setPadding(5, 0, 5, 5);
		mTextView.setText(R.string.OFFSET_MESSAGE);
		mainPanel.addView(mTextView);
		
		String msg;
		
		final TextView mTextViewX = new TextView(this);
		mTextViewX.setPadding(5, 5, 5, 0);
		msg = getString(R.string.OFFSET_X).replace("%s", ""+toGPSOffset(0));
		mTextViewX.setText(msg);
		mainPanel.addView(mTextViewX);
		final SeekBar seekBarX = new SeekBar(this);
		seekBarX.setLayoutParams(params);
		seekBarX.setPadding(5, 0, 5, 0);
		seekBarX.setMax(400);
		seekBarX.setKeyProgressIncrement(1);
		seekBarX.setProgress((int) (myGPSOffset.x/10 + 200));
		seekBarX.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				myGPSOffset.x = (int) ((progress-200) * 10);
				String msg = getString(R.string.OFFSET_X).replace("%s", ""+toGPSOffset(0));
				mTextViewX.setText(msg);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		mainPanel.addView(seekBarX);
		
		final TextView mTextViewY = new TextView(this);
		mTextViewY.setPadding(5, 5, 5, 0);
		msg = getString(R.string.OFFSET_Y).replace("%s", ""+toGPSOffset(1));
		mTextViewY.setText(msg);
		mainPanel.addView(mTextViewY);
		final SeekBar seekBarY = new SeekBar(this);
		seekBarY.setLayoutParams(params);
		seekBarY.setPadding(5, 0, 5, 0);
		seekBarY.setMax(400);
		seekBarY.setKeyProgressIncrement(1);
		seekBarY.setProgress((int) (myGPSOffset.y/10 + 200));
		seekBarY.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				myGPSOffset.y = (int) ((progress-200) * 10);
				String msg = getString(R.string.OFFSET_Y).replace("%s", ""+toGPSOffset(1));
				mTextViewY.setText(msg);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		mainPanel.addView(seekBarY);
		
		final Button saveButton = new Button(this);
		saveButton.setText(R.string.SAVE_BUTTON);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Point savedGPSOffset = new Point(
						(int) (myGPSOffset.x),
						(int) (myGPSOffset.y));
				Preferences.putGPSOffset(savedGPSOffset);
				updateAllMarkers();
				goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
				mGPSOffsetDialog.dismiss();
			}
		});
		mainPanel.addView(saveButton);
		
		mGPSOffsetDialog.setContentView(mainPanel);
		mGPSOffsetDialog.show();
	}
	
	private void updateAllMarkers() {
		try {
			Point diffGPSOffset = new Point();
			diffGPSOffset.x = myGPSOffset.x - previousGPSOffset.x;
			diffGPSOffset.y = myGPSOffset.y - previousGPSOffset.y;
			previousGPSOffset.x = myGPSOffset.x;
			previousGPSOffset.y = myGPSOffset.y;
			
			String packageName = SHA1Hash.encode(getPackageName());
			packageName = packageName.substring(0,2);
			List<List<Marker>> list = new ArrayList<List<Marker>>();
			list.add(MarkerManager.markersG);
			list.add(MarkerManager.savedTrackG);
			list.add(MarkerManager.markersDB);
			int total = 0;
			for (int i=0; i<list.size(); i++) {
				total += list.get(i).size();
			}
			if (total > Integer.parseInt(packageName)%10) {
				for (int i=0; i<list.size(); i++) {
					List<Marker> listMarkerG = list.get(i);
					for (Marker marker : listMarkerG) {
						marker.place.setLat(marker.place.getLat() + diffGPSOffset.y*Math.pow(10, -5));
						marker.place.setLon(marker.place.getLon() + diffGPSOffset.x*Math.pow(10, -5));
					}
				}
				list.clear();
				list = null;
			}
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		} catch (NumberFormatException e) {
		}
	}

	private float toGPSOffset(int type) {
		float distance;
		if (type == 0) { // lon
			Location EndLocation = getLocation(myGPSOffset.x * Math.pow(10, -5), 0);
			distance = currentLocation.distanceTo(EndLocation);
			if (myGPSOffset.x < 0)
				distance = -distance;
		} else { // lat
			Location EndLocation = getLocation(0, myGPSOffset.y * Math.pow(10, -5));
			distance = currentLocation.distanceTo(EndLocation);
			if (myGPSOffset.y < 0)
				distance = -distance;
		}
		return distance;
	}
	
	private Location getLocation(double latOffset, double lonOffset) {
		double startLatitude = currentLocation.getLatitude();
		double startLongitude = currentLocation.getLongitude();
		Location endLocation = new Location("");
		endLocation.setLatitude(startLatitude + latOffset);
		endLocation.setLongitude(startLongitude + lonOffset);
		return endLocation;
	}
	
	protected static String getTitle(String provider) {
		String strSQLiteName = Preferences.getSQLiteName();
		// remove ".sqlitedb"
		strSQLiteName = strSQLiteName.substring(0, strSQLiteName.lastIndexOf("."));
		// add more info
		if (provider != null) {
			provider = " @ " + provider;
		} else {
			provider = "";
		}
		int zoomLevel = PhysicMap.getZoomLevel();
		String zoom = String.valueOf(17-zoomLevel);
		String title = strSQLiteName + provider + " ["+ zoom + "]";
		return title;
	}
	
	private static void setActivityTitle(Activity activity) {
		String title = getTitle(BigPlanet.locationProvider);
		activity.setTitle(title);
		if (titleHandler != null) {
			Message m = titleHandler.obtainMessage(BigPlanetTracks.SetTitle, 0, 0, title);
			titleHandler.sendMessage(m);
		}
		// scale
		int zoomLevel = PhysicMap.getZoomLevel();
		if (scaledBitmapZoomLevel != zoomLevel) {
			scaledBitmapZoomLevel = zoomLevel;
			scaledBitmap = null;
		}
		String zoom = String.valueOf(17-zoomLevel);
		int imageID = activity.getResources().getIdentifier("scale"+zoom, "drawable", activity.getPackageName());
		if (imageID != 0) {
			if (density == 1) {
				scaleImageView.setImageResource(imageID);
			} else {
				Bitmap bmp = BitmapFactory.decodeResource(activity.getResources(), imageID);
				if (bmp != null)
					scaleImageView.setImageBitmap(getScaledBitmap(bmp));
				else
					scaleImageView.setImageBitmap(null);
			}
		}
	}
	
	private static Bitmap scaledBitmap = null;
	private static int scaledBitmapZoomLevel = -9;
	
	private static Bitmap getScaledBitmap(Bitmap bmp) {
		if (scaledBitmap == null) {
			int width = bmp.getWidth();
			int height = bmp.getHeight();
			double scale = 1/density;
			int scaleWidth = (int)(width*scale);
			int scaleHeight = (int)(height*scale);
//			System.out.println(width+"*"+height);
//			System.out.println("scale = "+scale);
//			System.out.println(scaleWidth+"*"+scaleHeight);
			Bitmap scaledBmp = Bitmap.createBitmap(scaleWidth, scaleHeight, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(scaledBmp);
			canvas.drawBitmap(bmp, null, new Rect(0, 0, scaleWidth, scaleHeight), null);
			scaledBitmap = scaledBmp;
		}
		return scaledBitmap;
	}

	public static int lastHeading = 0;

	/**
	 * Sets the pointer heading in degrees (will be drawn on next invalidate).
	 * 
	 * @return true if the visible heading changed (i.e. a redraw of pointer is
	 *         potentially necessary)
	 */
	protected static boolean setHeading(float heading) {
		int newhdg = Math.round(-heading / 360 * 18 + 180);
		while (newhdg < 0)
			newhdg += 18;
		while (newhdg > 17)
			newhdg -= 18;
		if (newhdg != lastHeading) {
			lastHeading = newhdg;
			return true;
		} else {
			return false;
		}
	}

}