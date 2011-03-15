// Created by plusminus on 20:36:01 - 26.09.2008
package org.andnav.osm.views.util;

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

	/**
	 * Calculates i.e. the increase of zoomlevel needed when the visible latitude needs to be bigger by <code>factor</code>.  
	 * 
	 * Assert.assertEquals(1, getNextSquareNumberAbove(1.1f));
	 * Assert.assertEquals(2, getNextSquareNumberAbove(2.1f));
	 * Assert.assertEquals(2, getNextSquareNumberAbove(3.9f));
	 * Assert.assertEquals(3, getNextSquareNumberAbove(4.1f));
	 * Assert.assertEquals(3, getNextSquareNumberAbove(7.9f));
	 * Assert.assertEquals(4, getNextSquareNumberAbove(8.1f));
	 * Assert.assertEquals(5, getNextSquareNumberAbove(16.1f));
	 * 
	 * Assert.assertEquals(-1, - getNextSquareNumberAbove(1 / 0.4f) + 1);
	 * Assert.assertEquals(-2, - getNextSquareNumberAbove(1 / 0.24f) + 1);
	 * 
	 * @param factor
	 * @return
	 */
	public static int getNextSquareNumberAbove(final float factor){
		int out = 0;
		int cur = 1;
		int i = 1;
		while(true){
			if(cur > factor)
				return out;
			
			out = i;
			cur *= 2;
			i++;
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
