package tyt.android.bigplanettracks;

import java.util.List;

import tyt.android.bigplanettracks.maps.db.DAO;
import tyt.android.bigplanettracks.maps.db.GeoBookmark;
import tyt.android.bigplanettracks.maps.ui.AddBookmarkDialog;
import tyt.android.bigplanettracks.maps.ui.OnDialogClickListener;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;

public class AllGeoBookmarks extends ListActivity {

	private static final String BOOKMARK_DATA = "bookmark";
	private List<GeoBookmark> geoBookmarks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setData();
	}

	private void setData() {
		DAO dao = new DAO(this);
		geoBookmarks = dao.getBookmarks();
		setListAdapter(new GeoBookmarkListAdapter(this));
		getListView().setOnItemLongClickListener(new OnItemLongClickListener(){

			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();
				intent.putExtra(BOOKMARK_DATA, geoBookmarks.get(position));
				setResult(RESULT_OK,intent);
				finish();
				return false;
			}
			
		});
	}

	/**
	 * Создает элементы меню
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, R.string.EDIT_MENU).setIcon(R.drawable.edit);
		menu.add(0, 1, 0, R.string.DELETE_MENU).setIcon(R.drawable.delete);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean isSelected = this.getSelectedItemId() >= 0;
		menu.findItem(0).setEnabled(isSelected);
		menu.findItem(1).setEnabled(isSelected);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int bookmarkId = (int) this.getSelectedItemId();

		switch (item.getItemId()) {
		case 0: // edit

			AddBookmarkDialog.show(AllGeoBookmarks.this, 
					geoBookmarks.get(bookmarkId), new OnDialogClickListener() {

				@Override
				public void onCancelClick() {

				}

				@Override
				public void onOkClick(Object obj) {
					GeoBookmark geoBookmark = (GeoBookmark) obj;
					DAO d = new DAO(AllGeoBookmarks.this);
					d.saveGeoBookmark(geoBookmark);
					setData();
				}
			});

			break;

		case 1: // delete
			new AlertDialog.Builder(this).setTitle(R.string.REMOVE_BOOKMARK_TITLE)
					.setMessage(R.string.REMOVE_BOOKMARK_MESSAGE)
					.setPositiveButton(R.string.YES_LABEL,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									DAO dao = new DAO(AllGeoBookmarks.this);
									dao.removeGeoBookmark(geoBookmarks.get(bookmarkId).getId());
									setData();
								}
							}).setNegativeButton(R.string.NO_LABEL, null).show();
			break;
		default:
			break;
		}

		return true;
	}

	private class GeoBookmarkListAdapter extends BaseAdapter {

		public GeoBookmarkListAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return geoBookmarks.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			GeoBookmarkView sv;
			GeoBookmark bookmark = geoBookmarks.get(position);
			if (convertView == null) {
				sv = new GeoBookmarkView(mContext, bookmark.getName(), bookmark.getDescription());
			} else {
				sv = (GeoBookmarkView) convertView;
				sv.setName(bookmark.getName());
				sv.setDescription(bookmark.getDescription());
				sv.id = bookmark.getId();
			}
			return sv;
		}

		private Context mContext;

	}

	public class GeoBookmarkView extends LinearLayout {
		public GeoBookmarkView(Context context, String name, String description) {
			super(context);
			View v = View.inflate(AllGeoBookmarks.this, R.layout.geobookmark, null);
			nameLabel = (TextView) v.findViewById(android.R.id.text1);
			nameLabel.setText(name);

			descriptionLabel = (TextView) v.findViewById(android.R.id.text2);
			descriptionLabel.setText(description);
			addView(v);
		}

		public void setName(String name) {
			descriptionLabel.setText(name);
		}

		public void setDescription(String description) {
			descriptionLabel.setText(description);
		}

		protected long id;

		private TextView nameLabel;
		private TextView descriptionLabel;
	}

}
