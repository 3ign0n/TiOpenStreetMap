// Created by plusminus on 20:36:01 - 26.09.2008
package org.andnav.osm.util;

import org.andnav.osm.views.util.constants.MathConstants;

/**
 * 
 * @author Nicolas Gramlich
 *
 */
public class MyMath implements MathConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================
	
	
	public static double gudermannInverse(double aLatitude){
		return Math.log(Math.tan(PI_4 + (DEG2RAD * aLatitude / 2)));
	}
	
	public static double gudermann(double y){
		return RAD2DEG * Math.atan(Math.sinh(y));
	}
	
	
	public static int mod(int number, final int modulus){
		if(number > 0)
			return number % modulus;
		
		while(number < 0)
			number += modulus;
		
		return number;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
