/*
 * Copyright 2007 Steven Osborn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* 
 * Code modifications by Daniel Ricciotti
 * This code was built using the IconifiedText tutorial by Steven Osborn
 * http://www.anddev.org/iconified_textlist_-_the_making_of-t97.html
 * 
 * Copyright 2008 Daniel Ricciotti
 */

/* 
 * Code modifications by Moritz Wundke
 * Extending Daniel Ricciotti CheckBox list
 * http://www.anddev.org/viewtopic.php?p=20754
 * 
 * Copyright 2009 Moritz Wundke
 */
package tyt.android.bigplanettracks.tracks.listview;

import tyt.android.bigplanettracks.tracks.TrackTabViewActivity;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExtendedCheckBoxListView extends LinearLayout {

	private TextView mText;// part of components of view
	private CheckBox mCheckBox; // part of components of view
	private ExtendedCheckBox mCheckBoxText;

	// public static String mClickItemedText;

	public ExtendedCheckBoxListView(Context context, ExtendedCheckBox aCheckBoxifiedText) {
		super(context);

		// Set orientation to be horizontal
		this.setOrientation(HORIZONTAL);

		mCheckBoxText = aCheckBoxifiedText;
		mCheckBox = new CheckBox(context);
		mCheckBox.setPadding(0, 0, 20, 0);

		// Set the initial state of the checkbox.
		mCheckBox.setChecked(aCheckBoxifiedText.getChecked());

		// Set the right listener for the checkbox, used to update
		// our data holder to change it's state after a click too
		mCheckBox.setOnClickListener(new OnClickListener() {
			/**
			 * When clicked change the state of the 'mCheckBoxText' too!
			 */
			@Override
			public void onClick(View v) {
				mCheckBoxText.setChecked(getCheckBoxState());
				if (!getCheckBoxState()) {
					TrackTabViewActivity.selectAllCheckBox.setChecked(false);
				}
			}
		});

		// Add the checkbox
		addView(mCheckBox, new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		mText = new TextView(context);
		mText.setText(aCheckBoxifiedText.getText());
		
		addView(mText, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		// Remove some controls in order to prevent a strange flickering when
		// clicking on the TextView!
		mText.setClickable(false);
		mText.setFocusable(false);
		mText.setFocusableInTouchMode(false);

		setOnClickListener(new OnClickListener() {
			/**
			 * Check or unchecked the current checkbox!
			 */
			@Override
			public void onClick(View v) {
				toggleCheckBoxState();
			}
		});
	}

	public void setText(String words) {
		mText.setText(words);
	}

	public void toggleCheckBoxState() {
		setCheckBoxState(!getCheckBoxState());
	}

	public void setCheckBoxState(boolean bool) {
		mCheckBox.setChecked(bool);
		mCheckBoxText.setChecked(bool);
	}

	public boolean getCheckBoxState() {
		return mCheckBox.isChecked();
	}
}
