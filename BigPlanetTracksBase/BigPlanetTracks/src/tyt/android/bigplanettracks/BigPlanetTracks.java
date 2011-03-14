package tyt.android.bigplanettracks;

import tyt.android.bigplanettracks.maps.NavControls;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TabHost;

public class BigPlanetTracks extends TabActivity implements OnTouchListener {	

	protected static final int SetTitle = 0;
	
	private WakeLock wakeLock;
	
	/*
	 * Tabs/View navigation:
	 */
	private static final int NUM_TABS = 2;
	private int currentTab = 0;

	private NavControls navControls;

	private final int icons[] = {
			R.drawable.menu_by_map,
			R.drawable.menu_by_time };

	private final Runnable nextActivity = new Runnable() {
		public void run() {
			currentTab = (currentTab + 1) % NUM_TABS;
			navControls.setLeftIcon(icons[(currentTab + NUM_TABS - 1) % NUM_TABS]);
			navControls.setRightIcon(icons[(currentTab + NUM_TABS + 1) % NUM_TABS]);
			getTabHost().setCurrentTab(currentTab);
			navControls.show();
		}
	};

	private final Runnable prevActivity = new Runnable() {
		public void run() {
			currentTab--;
			if (currentTab < 0) {
				currentTab = NUM_TABS - 1;
			}
			navControls.setLeftIcon(icons[(currentTab + NUM_TABS - 1) % NUM_TABS]);
			navControls.setRightIcon(icons[(currentTab + NUM_TABS + 1) % NUM_TABS]);
			getTabHost().setCurrentTab(currentTab);
			navControls.show();
		}
	};

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (BigPlanet.isGPSTracking == true) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				navControls.show();
			}
		}
		return false;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		
		// R.drawable.globe won't appear but needed
		final TabHost tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec("tab1")
			.setIndicator("Map", getResources().getDrawable(R.drawable.globe))
			.setContent(new Intent(this, BigPlanet.class)));
		tabHost.addTab(tabHost.newTabSpec("tab2")
			.setIndicator("Stats", getResources().getDrawable(R.drawable.globe))
			.setContent(new Intent(this, StatsActivity.class)));
		
		// Hide the tab widget itself. We'll use overlayed prev/next buttons to
		// switch between the tabs:
		tabHost.getTabWidget().setVisibility(View.GONE);

		RelativeLayout layout = new RelativeLayout(this);
		LayoutParams params = new LayoutParams(
				LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		layout.setLayoutParams(params);
		navControls = new NavControls(this, layout, prevActivity, nextActivity);
		navControls.setLeftIcon(icons[NUM_TABS - 1]);
		navControls.setRightIcon(icons[1]);
		navControls.show();
		tabHost.addView(layout);
		layout.setOnTouchListener(this);
		
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.globe);
		
		BigPlanet.titleHandler = handler;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		acquireWakeLock();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		releaseWakeLock();
	}
	
	private void acquireWakeLock() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (wakeLock == null) {
			wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "wakeLock");
			if (!wakeLock.isHeld()) {
				wakeLock.acquire();
			}
		}
	}
	
	private void releaseWakeLock() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
			wakeLock = null;
		}
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			String message = (String) msg.obj;
			switch (msg.what) {
			case SetTitle:
				setTitle(message);
				break;
			}
		}
	};
		
}