package tyt.android.bigplanettracks.maps.ui;

import tyt.android.bigplanettracks.R;
import tyt.android.bigplanettracks.maps.db.GeoBookmark;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Диалог для добавления/редактирования геозакладки
 * @author hudvin
 *
 */
public class AddBookmarkDialog {

	public static void show(final Context context, final GeoBookmark geoBookmark,
			final OnDialogClickListener onClickListener) {
		View v = View.inflate(context, R.layout.addgeobookmark, null);
		
		final AlertDialog dialog = new AlertDialog.Builder(context).create();
		dialog.setView(v);
		//dialog.setTitle(R.string.ADD_BOOKMARK_TITLE);
		if(geoBookmark.getId()==-1){
			dialog.setTitle(R.string.ADD_BOOKMARK_TITLE);
		} else {
			dialog.setTitle(R.string.UPDATE_BOOKMARK_TITLE);
		}
		
		final EditText nameValue = (EditText) v.findViewById(R.id.nameValue);
		final EditText descriptionValue = (EditText) v.findViewById(R.id.descriptionValue);
		
		nameValue.setText(geoBookmark.getName());
		descriptionValue.setText(geoBookmark.getDescription());
		
		final TextView validationError = (TextView) v.findViewById(R.id.validationError); 
		
		final Button cancelBtn = (Button) v.findViewById(R.id.cancelBtn);
		cancelBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				dialog.dismiss();
				onClickListener.onCancelClick();
			}
			
		});
		
		final Button addBtn = (Button) v.findViewById(R.id.addBtn);
		if(geoBookmark.getId()==-1){
			addBtn.setText(R.string.ADD_BUTTON);
		} else {
			addBtn.setText(R.string.SAVE_BUTTON);
		}
		addBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				String name = nameValue.getText().toString();
				String description = descriptionValue.getText().toString();
				if(name.trim().length() ==0){
					validationError.setText(R.string.EMPTY_NAME_ERROR);
				} else {				
					geoBookmark.setName(name);
					geoBookmark.setDescription(description);
					onClickListener.onOkClick(geoBookmark);
					dialog.dismiss();	
				}
			}
			
		});
		
		dialog.setCancelable(false);
		dialog.show();
	}
}
