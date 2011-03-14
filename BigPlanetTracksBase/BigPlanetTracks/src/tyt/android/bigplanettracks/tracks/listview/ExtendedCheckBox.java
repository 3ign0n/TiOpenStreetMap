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

public class ExtendedCheckBox implements Comparable<ExtendedCheckBox> {
	private String mText = "";
	private boolean mChecked;

	public ExtendedCheckBox(String text, boolean checked) {
		/* constructor */
		mText = text;
		mChecked = checked;
	}

	public void setChecked(boolean value) {
		this.mChecked = value;
	}

	public boolean getChecked() {
		return this.mChecked;
	}

	public String getText() {
		return mText;
	}

	public void setText(String text) {
		mText = text;
	}

	/** Make CheckBoxifiedText comparable by its name */
	// @Override
	public int compareTo(ExtendedCheckBox other) {
		if (this.mText != null)
			return this.mText.compareTo(other.getText());
		else
			throw new IllegalArgumentException();
	}
}
