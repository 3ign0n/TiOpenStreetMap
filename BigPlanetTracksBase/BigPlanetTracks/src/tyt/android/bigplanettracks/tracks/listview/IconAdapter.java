package tyt.android.bigplanettracks.tracks.listview;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import tyt.android.bigplanettracks.R;
import tyt.android.bigplanettracks.tracks.MyTimeUtils;
import tyt.android.bigplanettracks.tracks.db.TrackDBAdapter;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author taiyuchen, TYTung
 * @version 0.1
 */
public class IconAdapter extends BaseAdapter {
	
	private Context _ctx;
	private ArrayList<Integer> trackIDList;
	private ArrayList<String> trackNameList;
	private ArrayList<String> trackDescriptionList;
	private ArrayList<String> trackStartTimeList;
	private ArrayList<Long> trackTotalTimeList;
	private ArrayList<Float> trackDistanceList;
	private ArrayList<Float> trackAverageSpeedList;
	private ArrayList<Float> trackMaximumSpeedList;
	private ArrayList<Integer> trackPointsList;
	private ArrayList<String> trackSourceList;
	
	private LayoutInflater mlin; //to get the Context's layout
	private Bitmap trackIcon;
	public String trackSource;
	
	static class Holder{
		TextView name;
		TextView measure;
		TextView desc;
		TextView source;
		ImageView icon;
	}
	
	public IconAdapter(Context ctx, Cursor myCursor) {
		_ctx = ctx;
		mlin = LayoutInflater.from(ctx);
		trackIDList = new ArrayList<Integer>();
		trackNameList = new ArrayList<String>();
		trackDescriptionList = new ArrayList<String>();
		trackStartTimeList = new ArrayList<String>();
		trackTotalTimeList = new ArrayList<Long>();
		trackDistanceList = new ArrayList<Float>();
		trackAverageSpeedList = new ArrayList<Float>();
		trackMaximumSpeedList = new ArrayList<Float>();
		trackPointsList = new ArrayList<Integer>();
		trackSourceList = new ArrayList<String>();
		
		for (int i=0; i<myCursor.getCount(); i++)
		{
			trackIDList.add(myCursor.getInt(0));
			trackNameList.add(myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_name)));
			trackDescriptionList.add(myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_description)));
			String strGMTTime = myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_startTime));
			trackStartTimeList.add(MyTimeUtils.getLocalTimeString(strGMTTime));
			//-----------Track Measurement------------------------------------------------------
			trackTotalTimeList.add(myCursor.getLong(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_totalTime)));
			trackDistanceList.add(myCursor.getFloat(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_totalDistance)));
			trackAverageSpeedList.add(myCursor.getFloat(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_averageSpeed)));
			trackMaximumSpeedList.add(myCursor.getFloat(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_maximumSpeed)));
			trackPointsList.add(myCursor.getInt(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_trackPoints)));
			//---------------------------------------------------------------------------------
			trackSourceList.add(myCursor.getString(myCursor.getColumnIndexOrThrow(TrackDBAdapter.FIELD_trackSource)));
			myCursor.moveToNext();
		}
		myCursor.close();
	}

	@Override
	public int getCount() {
		return trackNameList.size();
	}

	@Override
	public Object getItem(int position) {
		return getView(position, null, null);
	}

	@Override
	public long getItemId(int position) {
		return trackIDList.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder = null;
		if (convertView == null) {
			convertView = mlin.inflate(R.layout.track_icon_list, null);
			holder = new Holder();
			holder.name = (TextView)convertView.findViewById(R.id.track_name);
			holder.measure = (TextView)convertView.findViewById(R.id.track_meaure);
			holder.desc = (TextView)convertView.findViewById(R.id.desc_text);
			holder.icon = (ImageView)convertView.findViewById(R.id.track_icon);
			holder.source = (TextView)convertView.findViewById(R.id.track_source);
			convertView.setTag(holder);
		} else {
			holder=(Holder)convertView.getTag();
		}

		holder.name.setText((String)trackNameList.get(position));
		
		long time = (long)trackTotalTimeList.get(position);
		String track_consumedTime = generateTimeString(time, this._ctx);
		float distance = trackDistanceList.get(position);
		String track_distance = generateDistanceString(distance, this._ctx);
		long trackPoints = trackPointsList.get(position);
		String track_point_number = generateWaypointString(trackPoints);
		holder.measure.setText(track_consumedTime+"  "+track_distance+"  "+track_point_number);
		
		String startTime = (String)trackStartTimeList.get(position);
		String description = (String)trackDescriptionList.get(position);
		String result;
		if (description != null && !description.trim().equals(""))
			result = startTime + "\n" + description;
		else 
			result = startTime;
		holder.desc.setText(result);
		
		//holder.icon.setBackgroundColor(Color.GREEN);
		String track_source = (String)trackSourceList.get(position);
		if((track_source).equalsIgnoreCase("File")) {
			trackIcon=BitmapFactory.decodeResource(_ctx.getResources(),R.drawable.track_icon_flag_red );
			holder.source.setText(_ctx.getString(R.string.track_source_string_file));
		} else { // source:GPS
			trackIcon=BitmapFactory.decodeResource(_ctx.getResources(),R.drawable.track_icon_flag_blue );
			holder.source.setText(_ctx.getString(R.string.track_source_string_gps));
		}
		holder.icon.setImageBitmap(trackIcon);
		
		return convertView;
	}
	
	public void setTrackSource(String trackSource) {
		this.trackSource = trackSource;
	}
	
	public static String generateTimeString(long millisecond, Context context) {
		String timeString = MyTimeUtils.getTimeString(millisecond);
		return timeString;
	}
	
	public static String generateDistanceString(float distance, Context context) {
		String distanceString = "";
		NumberFormat formatter = new DecimalFormat("#.##");
		if(distance>1000) {
			distanceString = formatter.format(distance/1000) +" "+ context.getString(R.string.kilometer);
		} else {
			distanceString = formatter.format(distance) +" "+ context.getString(R.string.meter);
		}
		return distanceString;
	}
	
	public static String generateSpeedString(double speed,Context context) {
		String speedString = "";
		NumberFormat formatter = new DecimalFormat("#.##");
		speedString = formatter.format(speed) +" "+ context.getString(R.string.speed_unit);
		return speedString;
	}
	
	public static String generateAltitudeString(double altitude, Context context) {
		String altitudeString = "";
		NumberFormat formatter = new DecimalFormat("#.##");
		altitudeString = formatter.format(altitude) +" "+ context.getString(R.string.meter);
		return altitudeString;
	}
	
	private String generateWaypointString(long number) {
		String numberString = "";
		String point_number_unit = _ctx.getString(R.string.track_waypoints_string);
		numberString = number+" "+point_number_unit;
		return numberString;
	}

}
