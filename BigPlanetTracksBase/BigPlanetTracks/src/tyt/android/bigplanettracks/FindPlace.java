package tyt.android.bigplanettracks;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import tyt.android.bigplanettracks.maps.Place;
import tyt.android.bigplanettracks.maps.loader.BaseLoader;
import tyt.android.bigplanettracks.maps.xml.GeoLocationHandler;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RelativeLayout.LayoutParams;

public class FindPlace extends ListActivity implements Runnable {

	private String queryString = "";
	
	private boolean hasConnection;

	private Handler handler;

	private List<Place> places = new ArrayList<Place>();

	private View waitView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new SpeechListAdapter(this));

		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				if (places.size() == 0) {
					finish();
					String message = "";
					if (hasConnection) {
						message = getString(R.string.msg_no_search_results).replace("{query}", queryString);
					} else {
						message = getString(R.string.msg_no_connection);
					}
					Toast.makeText(FindPlace.this, message, Toast.LENGTH_LONG).show();
				} else if (places.size() == 1) {
					Intent i = new Intent(BigPlanet.SearchAction);
					i.putExtra("place", places.get(0));
					i.putExtra("query", queryString);
					sendBroadcast(i);
					finish();
				} else {
					setListAdapter(new SpeechListAdapter(FindPlace.this));
					getListView().setOnItemClickListener(
							new OnItemClickListener() {

								public void onItemClick(AdapterView<?> parent,
										View view, int position, long id) {
									Intent i = new Intent(BigPlanet.SearchAction);
									i.putExtra("place", places.get(position));
									i.putExtra("query", queryString);
									sendBroadcast(i);
									finish();
								}
							});

					waitView.setVisibility(View.GONE);
					setTitle(R.string.msg_search_results);
				}
			}

		};

		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			queryString = queryIntent.getStringExtra(SearchManager.QUERY);
			Thread t = new Thread(this);
			t.start();
		}

		setTitle(R.string.msg_please_wait);
		waitView = View.inflate(this, R.layout.progress_dialog, null);
		LayoutParams p = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		addContentView(waitView, p);
	}

	public void run() {
		HttpURLConnection connection = null;
		try {
			String lang = "zh-TW";
			String guery = URLEncoder.encode(queryString, "UTF-8");
			URL u = new URL("http://maps.google.com/maps/geo?nl="+lang+"&output=xml&oe=utf8&q=" + guery);
			connection = (HttpURLConnection) u.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept-Language", lang);
			connection.setReadTimeout(BaseLoader.CONNECTION_TIMEOUT);
			connection.setConnectTimeout(BaseLoader.CONNECTION_TIMEOUT);
			connection.connect();
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				hasConnection = false;
				return;
			} else {
				hasConnection = true;
			}

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			StringBuffer data = new StringBuffer();
			String line;
			while ((line = reader.readLine()) != null) {
				data.append(line);
			}
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			GeoLocationHandler handler = new GeoLocationHandler();
			xr.setContentHandler(handler);
			xr.parse(new InputSource(new StringReader(data.toString())));
			places = handler.getPlaces();
		} catch (Exception e) {

		} finally {
			connection.disconnect();
			handler.sendEmptyMessage(0);
		}

	}

	private class SpeechListAdapter extends BaseAdapter {

		public SpeechListAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return places.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			SpeechView sv;
			Place place = places.get(position);
			sv = new SpeechView(mContext, place.getAddress(), place.getName());
			return sv;
		}

		private Context mContext;

	}

	public class SpeechView extends LinearLayout {
		public SpeechView(Context context, String name, String description) {
			super(context);
			View v = View.inflate(FindPlace.this, R.layout.geobookmark, null);
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
