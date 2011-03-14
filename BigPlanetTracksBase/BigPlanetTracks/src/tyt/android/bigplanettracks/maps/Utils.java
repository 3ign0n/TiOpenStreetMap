package tyt.android.bigplanettracks.maps;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class Utils {

	public static int getZoomLevel(double x) {
		if(x<1) x = 1/x;
		int counter = 1;
		while (x > 2) {
			counter++;
			x = x / 2;
		}
		if(x<1) return counter*-1;
		return counter;
	}

	public static boolean verify(String key) {
		boolean result = false;
		try {
			if (!SHA1Hash.encode(key).equalsIgnoreCase("671b82291403cf7bc530b40bb302dd08fb4a3ce0")) {
				BigPlanetApp.isDemo = true;
				result = true;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return !result;
	}
}
