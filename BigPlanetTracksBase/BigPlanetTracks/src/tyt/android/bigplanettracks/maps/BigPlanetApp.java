package tyt.android.bigplanettracks.maps;

import android.app.Application;

public class BigPlanetApp extends Application {

	public static boolean isDemo = false;
	
	public BigPlanetApp() {
		super();
	}

	@Override
	public void onCreate() {
		Preferences.init(this);
	}

}
