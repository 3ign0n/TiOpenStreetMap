package tyt.android.bigplanettracks.tracks;

import java.util.ArrayList;
import java.util.List;

import tyt.android.bigplanettracks.BigPlanet;
import tyt.android.bigplanettracks.R;
import tyt.android.bigplanettracks.maps.Place;
import tyt.android.bigplanettracks.tracks.db.TrackDBAdapter;
import tyt.android.bigplanettracks.tracks.listview.ExtendedCheckBox;
import tyt.android.bigplanettracks.tracks.listview.ExtendedCheckBoxListAdapter;
import tyt.android.bigplanettracks.tracks.listview.IconAdapter;
import tyt.android.bigplanettracks.tracks.reader.FileImporter;
import tyt.android.bigplanettracks.tracks.reader.MyParseThread;
import tyt.android.bigplanettracks.tracks.writer.GpxTrackWriter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TabHost.OnTabChangeListener;

/**
 * @author taiyuchen, TYTung
 * @version 0.1
 */
public class TrackTabViewActivity extends TabActivity{
	
	private TabHost mTabHost;
	private RelativeLayout myRelativeLayout;
	private ListView myListView;
	private List<String> gpxFileItems = null;
	public static CheckBox selectAllCheckBox;
//	private LayoutInflater mlin; //to get the Context's layout
	private ExtendedCheckBoxListAdapter mListAdapter;
	private IconAdapter listViewAdapter;
	private FileImporter fp;
	private Handler mainHandler;
	public static ProgressDialog myGPXParseDialog = null;
	
	private static long trackID;
	private ArrayList<Place> placeList = null;
	private ArrayList<Location> locationList = null;
	private static final int EDIT_DIALOG = 0;
	private static final int GET_INFORMATION_DIALOG = 1;
	public static ProgressDialog myTrackExportDialog;
	private Handler trackListViewHandler;
	private View editTrackDialogLayout;
	private View statisticsView;
	private String trackName;
	private String trackDescription;
	private String trackStartGMTTime;
	
	private TextView track_name_text;
	private TextView track_distance_text;
	private TextView track_time_text;
	private TextView track_speed_text;
	private TextView track_maxSpeed_text;
	private TextView track_minAltitude_text;
	private TextView track_maxAltitude_text;
	private TextView track_pointNumber_text;
	private TextView track_description_text;
	private TextView track_start_time_text;
	
	private EditText trackNameEditText;
	private EditText trackDescEditText;
	
	private final float density = BigPlanet.density;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BigPlanet.DBAdapter.open();
		setContentView(R.layout.tab_layout);
//		mlin=LayoutInflater.from(this);
//		mlin.inflate(R.layout.tab_layout,null);
		
		mainHandler = new Handler() {
			
			public void handleMessage(Message msg) {
				switch (msg.what)
				{
					case MyParseThread.ALL_Files_PARSE_SUCCESS:
						/*All parse successfully*/
						new AlertDialog.Builder(TrackTabViewActivity.this)
						.setTitle(getString(R.string.track_importing_result))
						.setMessage(getString(R.string.track_importing_successfully))
						.setPositiveButton(getString(R.string.OK),new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialoginterface, int i)
								{
									showAllImportedTracks();
								}
							}).show();
						break;
						
					case MyParseThread.Some_Files_PARSE_FAIL:
						/*PART parse fail*/
						final String fail_file_list = (String)msg.obj;
						new AlertDialog.Builder(TrackTabViewActivity.this)
						.setTitle(getString(R.string.track_importing_result))
						.setMessage(getString(R.string.some_error)+" "+(String)msg.obj)
						.setPositiveButton(getString(R.string.OK),new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialoginterface, int i)
								{
									Toast.makeText(
											TrackTabViewActivity.this,
											getString(R.string.import_fail)+":"+fail_file_list,
											Toast.LENGTH_LONG).show();
									showAllImportedTracks();
								}
							}).show();
						 break;
				}
		 }};
		mainHandler.removeMessages(0);
		
		//------------------------------------------------------------------------------------------------------------------------------
		trackListViewHandler = new Handler() {
			
			public void handleMessage(Message msg) {
				switch (msg.what)
				{
					case GpxTrackWriter.EXPORT_SUCCESS:
						/* Export GpxFile Successfully*/
						Toast.makeText(TrackTabViewActivity.this, getString(R.string.save_successfully), 
								Toast.LENGTH_SHORT).show();
						break;
						
					case GpxTrackWriter.EXPORT_FAIL:
						/*Export GpxFile Fail*/
						Toast.makeText(TrackTabViewActivity.this, getString(R.string.save_fail), 
								Toast.LENGTH_SHORT).show();
						break;
				}
		}};
		trackListViewHandler.removeMessages(0);
		
		//-----------------------------------------------------------------------------------------------------------------------
		createImportTrackListView();
		createBrosweTrackListView();
		
		mTabHost = getTabHost();
		mTabHost.setOnTabChangedListener(new OnTabChangeListener(){

		@Override
		public void onTabChanged(String tabId) {
//			Log.i("Message", "Tab's id="+tabId);
			if (tabId == "browse_tab") {
				createBrosweTrackListView();
			}
		}});
		
		mTabHost.addTab(mTabHost.newTabSpec("import_tab").setIndicator(getString(R.string.IMPORT_TRACK_MENU),getResources().getDrawable(R.drawable.track_import_icon)).setContent(R.id.myRelativeLayout));
		mTabHost.addTab(mTabHost.newTabSpec("browse_tab").setIndicator(getString(R.string.BROWSE_TRACK_MENU),getResources().getDrawable(R.drawable.track_broswe_icon)).setContent(R.id.broswe_linear_layout));	
		if(BigPlanet.DBAdapter.checkIfDBHasTracks()) {
			mTabHost.setCurrentTab(1);
		} else {
			mTabHost.setCurrentTab(0);
		}
		
		TabWidget tw = getTabWidget(); 
		for (int i=0; i<tw.getChildCount(); i++) { 
			RelativeLayout relLayout = (RelativeLayout)tw.getChildAt(i); 
			TextView tv = (TextView)relLayout.getChildAt(1);// index 0: Tab's text
			ImageView im = (ImageView)relLayout.getChildAt(0);// index 1:Tab's image
			im.setPadding(0, 0, 0, 10);
			tv.setTextSize(12.0f);
		}
	
		//View convertView=mlin.inflate(R.layout.tab_layout, null);
		//myRelativeLayout = new RelativeLayout(this);
		//myRelativeLayout=(RelativeLayout)convertView.findViewById(R.id.myRelativeLayout);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		BigPlanet.DBAdapter.close();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder;
		AlertDialog alertDialog;
		builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		
		switch(id)
		{
			case EDIT_DIALOG: //Dialog id = 0
				//get the Edit_Track_Dialog's Layout
				editTrackDialogLayout = inflater.inflate(R.layout.track_edit_layout,(ViewGroup) findViewById(R.id.layout_root));
				trackNameEditText = (EditText)editTrackDialogLayout.findViewById(R.id.name_edit_text);
				trackDescEditText = (EditText)editTrackDialogLayout.findViewById(R.id.desc_edit_text);
				builder.setView(editTrackDialogLayout);
				builder.setTitle(getString(R.string.edit_track_dialog_tile));
				builder.setInverseBackgroundForced(true);
				
				builder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Save these string values into Sqlite Database
						String changed_track_name = trackNameEditText.getText().toString().trim();
						/**********filter non-allowed characters out due to the rule of File Name*************/
						if (!changed_track_name.equals("")) {
							changed_track_name = fileterOutForFileNameRule(changed_track_name);
						}
						String changed_track_des = trackDescEditText.getText().toString();
						
						BigPlanet.DBAdapter.updateTrack(trackID, changed_track_name, changed_track_des);
						
						Toast.makeText(TrackTabViewActivity.this,getString(R.string.edit_successfully), 
								Toast.LENGTH_SHORT).show();
						createBrosweTrackListView();
					}});
				
				builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}});
				
				break;
				
			case GET_INFORMATION_DIALOG:
				statisticsView = inflater.inflate(R.layout.track_statistics_layout,(ViewGroup) findViewById(R.id.statisticsView));
				track_name_text = (TextView) statisticsView.findViewById(R.id.track_name_content_view);
				track_distance_text = (TextView) statisticsView.findViewById(R.id.track_distance_content_view);
				track_time_text = (TextView) statisticsView.findViewById(R.id.track_time_content_view);
				track_speed_text = (TextView) statisticsView.findViewById(R.id.track_speed_content_view);
				track_maxSpeed_text = (TextView) statisticsView.findViewById(R.id.track_maxSpeed_content_view);
				track_minAltitude_text = (TextView) statisticsView.findViewById(R.id.track_min_elevation_content_view);
				track_maxAltitude_text = (TextView) statisticsView.findViewById(R.id.track_max_elevation_content_view);
				track_pointNumber_text = (TextView) statisticsView.findViewById(R.id.track_waypoints_content_view);
				track_start_time_text = (TextView) statisticsView.findViewById(R.id.track_start_time_content_view);
				track_description_text = (TextView) statisticsView.findViewById(R.id.track_description_content_view);
				builder.setView(statisticsView);
				builder.setTitle(getString(R.string.whole_track_information_dialog_title));
				builder.setInverseBackgroundForced(true);
				break;
				
			default:
				alertDialog = null;
		}
		
		alertDialog = builder.create();
		
		return alertDialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id)
		{
			case EDIT_DIALOG:
				getNameDescriptionFromDB();
				((AlertDialog)dialog).setTitle(getString(R.string.edit_track_dialog_tile));
				((AlertDialog)dialog).setInverseBackgroundForced(true);
				trackNameEditText.setText(trackName);
				EditText des_text = (EditText)dialog.findViewById(R.id.desc_edit_text);
				des_text.setText(trackDescription);
				break;
				
			case GET_INFORMATION_DIALOG:
				if (!BigPlanet.DBAdapter.isMeasureUpdated(trackID)) {
					getTrackStatisticsFromDB();
				} else {
					calculateWaypointsFromDB();
				}
				((AlertDialog)dialog).setTitle(getString(R.string.whole_track_information_dialog_title));
				((AlertDialog)dialog).setInverseBackgroundForced(true);
				break;
		}
		super.onPrepareDialog(id, dialog);
	}
		
	private void getNameDescriptionFromDB() {
		Cursor myCursor = BigPlanet.DBAdapter.getTrack(trackID);
		trackName = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_name));
		trackDescription = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_description));
		trackStartGMTTime = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_startTime));
		myCursor.close();
	}
	
	// slow
	private void calculateWaypointsFromDB() {
		getNameDescriptionFromDB();
		
		ArrayList<Location> locationList = getLocationListFromDB(trackID);
		
		TrackAnalyzer analyzer = new TrackAnalyzer(trackName, trackDescription, trackStartGMTTime, locationList);
		analyzer.analyzeAndUpdate(trackID);
		long totalTime = analyzer.getTotalTime();
		float totalDistance = analyzer.getTotalDistance();
		float averageSpeed = analyzer.getAverageSpeed();
		float maximumSpeed = analyzer.getMaximumSpeed();
		double minElevation = analyzer.getMinAltitude();
		double maxElevation = analyzer.getMaxAltitude();
		long trackPoints = analyzer.getTrackPoints();
		
		showText(totalTime, totalDistance, averageSpeed, maximumSpeed, minElevation, maxElevation, trackPoints);
	}
	
	// very fast
	private void getTrackStatisticsFromDB() {
		getNameDescriptionFromDB();
		
		Cursor myCursor = BigPlanet.DBAdapter.getTrack(trackID);
		long totalTime = myCursor.getLong(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_totalTime));
		float totalDistance = myCursor.getFloat(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_totalDistance));
		float averageSpeed = myCursor.getFloat(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_averageSpeed));
		float maximumSpeed = myCursor.getFloat(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_maximumSpeed));
		double minElevation = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_minAltitude));
		double maxElevation = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_maxAltitude));
		long trackPoints = myCursor.getLong(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_trackPoints));
		myCursor.close();
				
		showText(totalTime, totalDistance, averageSpeed, maximumSpeed, minElevation, maxElevation, trackPoints);
	}
	
	private void showText(long totalTime, float totalDistance, float averageSpeed, float maximumSpeed, 
			double minElevation, double maxElevation, long trackPoints) {
		if (trackName.equals(""))
			trackName = getString(R.string.new_track);
		track_name_text.setText(trackName);
		track_distance_text.setText(IconAdapter.generateDistanceString(totalDistance, this));
		track_time_text.setText(IconAdapter.generateTimeString(totalTime, this));
		track_speed_text.setText(IconAdapter.generateSpeedString(averageSpeed, this));
		track_maxSpeed_text.setText(IconAdapter.generateSpeedString(maximumSpeed, this));
		track_minAltitude_text.setText(IconAdapter.generateAltitudeString(minElevation, this));
		track_maxAltitude_text.setText(IconAdapter.generateAltitudeString(maxElevation, this));
		track_pointNumber_text.setText(String.valueOf(trackPoints));
		track_description_text.setText(trackDescription);
		track_start_time_text.setText(MyTimeUtils.getLocalTimeString(trackStartGMTTime));
	}
	
	private void createImportTrackListView() {
		WindowManager wm = this.getWindowManager();
		Display display = wm.getDefaultDisplay();
		int height = display.getHeight();
		int width = display.getWidth();
		
		fp = new FileImporter();
		
		myRelativeLayout = (RelativeLayout) findViewById(R.id.myRelativeLayout);
		TextView myImportTextView = (TextView) findViewById(R.id.import_empty);
		/* New ListView */
		myListView = new ListView(this);
		LayoutParams param = new LayoutParams(width,height-(int)(100*density));
		/* Add ListView INTO myLinearLayout */
		myRelativeLayout.addView(myListView, 0, param);
		
		/* New SelectAllCheckBox */
		selectAllCheckBox = new CheckBox(this);
		selectAllCheckBox.setEnabled(false);
		selectAllCheckBox.setWidth((int)(115*density));
		selectAllCheckBox.setHint(getString(R.string.select_all));
		selectAllCheckBox.setHintTextColor(Color.BLACK);
		
		/*Set CheckBox False*/
		selectAllCheckBox.setChecked(false);
		selectAllCheckBox.setOnClickListener(new CheckBox.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectAllCheckBox.isChecked()) {
					for (ExtendedCheckBox extendedCheckBox :mListAdapter.getListItems()) {
						//ExtendedCheckBox extendedCheckBox = (ExtendedCheckBox)mListAdapter.getItem(i);
//						System.out.println("selected gpx File="+extendedCheckBox.getText());
						extendedCheckBox.setChecked(true); 	
					}
				} else {
					for (ExtendedCheckBox extendedCheckBox :mListAdapter.getListItems()) {
						//ExtendedCheckBox extendedCheckBox = (ExtendedCheckBox)mListAdapter.getItem(i);
//						System.out.println("Unselected gpx File="+extendedCheckBox.getText());
						extendedCheckBox.setChecked(false);
					} 
				}
				resetContentView();
			}
		});
		
		/*-----------------------------------------------------------------------------------------------------------*/
		/* add ImportTracks Button */
		Button btn = new Button(this);
		btn.setEnabled(false);
		btn.setWidth((int)(115*density));
		//bt.setHeight(80);
		btn.setText(getString(R.string.import_track));
		
		btn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean select_at_least_one_track = false;
				for (ExtendedCheckBox checkBox :mListAdapter.getListItems()) {
					if (checkBox.getChecked()) {
						select_at_least_one_track = true;
						break;
					}
				}
				
				if (select_at_least_one_track) {
					final CharSequence strDialogTitle = getString(R.string.str_dialog_title);
					final CharSequence strDialogBody = getString(R.string.str_dialog_body);
					
					// show ProgressDialog
					myGPXParseDialog = ProgressDialog.show(
							TrackTabViewActivity.this,
							strDialogTitle,
							strDialogBody, 
							true);
					fp.parseGPXFile(mListAdapter.getListItems(),getMainHandler(),"File");

				} else {
					Toast.makeText(
							TrackTabViewActivity.this,
							getString(R.string.have_yet_select_track_message),
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		/*---------------------------------------------------------------------------------------------------------------*/
		LinearLayout myLinearLayout = new LinearLayout(this);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		myLinearLayout.setHorizontalGravity(Gravity.RIGHT);
		myLinearLayout.addView(selectAllCheckBox, layoutParams);
		myLinearLayout.addView(btn, layoutParams);
		myLinearLayout.setBaselineAligned(true);
		myLinearLayout.setBackgroundColor(Color.WHITE);
		layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		myRelativeLayout.addView(myLinearLayout, layoutParams);
		// Build the list adapter
		mListAdapter = new ExtendedCheckBoxListAdapter(this);
		this.gpxFileItems = fp.findAllGPXFiles();
		for (String itemName : gpxFileItems) {
//			System.out.println("add gpx File="+itemName);
			mListAdapter.addItem(new ExtendedCheckBox(itemName,false));
		}
		// Add some items
		/* 
		for ( int i = 1; i < 20; i++ ) {
			String newItem = "Item " + i;
			mListAdapter.addItem(new ExtendedCheckBox(newItem,false));
		}
		*/
		// Bind it to the activity!
		if (this.gpxFileItems.size()!=0) {
			 /* Add LinearLayout INTO ContentView */
			myImportTextView.setVisibility(View.GONE);
			myListView.setAdapter(this.mListAdapter);
			btn.setEnabled(true);
			selectAllCheckBox.setEnabled(true);
		} else {
//			Log.i("Message", "No Content!");
			myImportTextView.setVisibility(View.VISIBLE);
		}
	}
	
	private void createBrosweTrackListView() {
//		Log.i("Message", "createBrosweTrackListView");
		ListView myListView = (ListView) findViewById(R.id.list);
		TextView myTextView = (TextView) findViewById(R.id.browse_empty);
		
		myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				trackID = id;
				alert_dialog_selection();
			}});
		
		Cursor myCursor = BigPlanet.DBAdapter.getAllTracks();
		if (myCursor.moveToFirst()) {
			myTextView.setVisibility(View.GONE);
			listViewAdapter = new IconAdapter(this, myCursor);
			myListView.setAdapter(listViewAdapter);
		} else {
			myTextView.setVisibility(View.VISIBLE);
		}
		myCursor.close();
	}
	
	private ArrayList<Location> getLocationListFromDB(long trackID) {
		ArrayList<Location> locationList = new ArrayList<Location>();
		Cursor myCursor = BigPlanet.DBAdapter.getTrackPoints(trackID);
		for (int i=0; i < myCursor.getCount(); i++) {
			double latitude = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_latitude));
			double longitude = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_longitude)); 
			double altitude = myCursor.getDouble(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_altitude)); 
			String strGMTTime = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_time));
			
			Location location = new Location("");
			location.setLatitude(latitude);
			location.setLongitude(longitude);
			location.setAltitude(altitude);
			location.setTime(MyTimeUtils.getGMTTime(strGMTTime));
			locationList.add(location);
			myCursor.moveToNext();
		}
		myCursor.close();
		return locationList;
	}

	private void resetContentView(){
//		System.out.println("ResetContentView");
		myListView.setAdapter(null);
		myListView.setAdapter(this.mListAdapter);
	}
	
	public Handler getMainHandler() {
		return this.mainHandler;
	}
	
	private void showAllImportedTracks() {
		createBrosweTrackListView();
		mTabHost.setCurrentTab(1);
	}
	
	private ArrayList<Place> convertToPlaceList(ArrayList<Location> locationList) {
		ArrayList<Place> myPlaceList = new ArrayList<Place>();
		for (int i=0; i<locationList.size(); i++) {
			Place place = new Place();
			Location location = locationList.get(i);
			place.setLocation(location);
			place.setLat(location.getLatitude());
			place.setLon(location.getLongitude());
			myPlaceList.add(place);
		}
		return myPlaceList;
	}
	
	private void alert_dialog_selection(){
		new AlertDialog.Builder(TrackTabViewActivity.this)
			.setTitle(getString(R.string.track_menu_title))
			.setItems(R.array.items_track_what_to_do_dialog,
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which)
					{	
						case 0:
							if (!BigPlanet.isGPSTracking) {
								final ProgressDialog myProgressDialog = ProgressDialog.show(TrackTabViewActivity.this,	  
										getString(R.string.drawing_track_progressdialog_title), getString(R.string.drawing_track_progressdialog_body), true);
								
								// Show this track on the map
								locationList = getLocationListFromDB(trackID);
								new Thread(){
									public void run() {
										try {
											placeList = convertToPlaceList(locationList);
											BigPlanet.addMarkersForDrawing(TrackTabViewActivity.this, placeList, 2);
											finish();
										} catch(Exception e) {
											e.printStackTrace();
										} finally {
											myProgressDialog.dismiss();
										}
									}
								}.start();
							} else {
								Toast.makeText(TrackTabViewActivity.this, R.string.go_to_track_fail, Toast.LENGTH_SHORT).show();
							}
							break;

						case 1:
							// Edit this track
							showDialog(EDIT_DIALOG);
							break;
							
						case 2:
							// Delete this track
							new AlertDialog.Builder(TrackTabViewActivity.this)
							.setMessage(getString(R.string.sure_to_delete_track))
							.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener(){
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									deleteSelectedTrackFromDB();
								}})
							.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener(){
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}}).show();
							break;
							
						case 3:
							// Get more information
							showDialog(GET_INFORMATION_DIALOG);
							break;
							
						case 4:
							// Save the track into SD card
							final CharSequence strDialogTitle = getString(R.string.str_export_dialog_title);
							final CharSequence strDialogBody = getString(R.string.str_export_dialog_body);
							// show Progress Dialog
							myTrackExportDialog = ProgressDialog.show(
									TrackTabViewActivity.this,
									strDialogTitle,
									strDialogBody, 
									true);
							GpxTrackWriter gpxTrackWriter = new GpxTrackWriter();
							gpxTrackWriter.setTrackID(trackID);
							gpxTrackWriter.setHandler(trackListViewHandler);
							gpxTrackWriter.saveToFile();
							break;
					}//end of switch
				}
			}
		).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
			{
				@Override 
				public void onClick(DialogInterface d, int which) { 
					d.dismiss(); 
				} 
			}
		).show();
	}

	private void deleteSelectedTrackFromDB() {
		if (trackID > 0 ) {	
			try{
				if (BigPlanet.DBAdapter.deleteTrack(trackID)) {
					Toast.makeText(this, getString(R.string.delete_track_successfully), 
								Toast.LENGTH_SHORT).show();
					createBrosweTrackListView();
				} else {
					Toast.makeText(this, getString(R.string.delete_track_fail), 
								Toast.LENGTH_SHORT).show();
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else
			Log.e("Error", "TrackTabViewActivity.trackID <= 0");
	}

	public static String fileterOutForFileNameRule(String trackName) {
		trackName = trackName.replaceAll("[\\/:*?\"<>|]", "");
//		Log.i("Message","the trackName after Filter Out Method="+trackName);
		return trackName;
	}

}
