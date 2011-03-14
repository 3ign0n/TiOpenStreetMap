package net.osmand.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.OsmandSettings;
import net.osmand.PoiFilter;
import net.osmand.R;
import net.osmand.ResourceManager;
import net.osmand.activities.EditingPOIActivity;
import net.osmand.data.Amenity;
import net.osmand.osm.LatLon;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class POIMapLayer implements OsmandMapLayer, ContextMenuLayer.IContextMenuProvider {
	private static final int startZoom = 10;
	public static final int LIMIT_POI = 200;
	public static final org.apache.commons.logging.Log log = LogUtil.getLog(POIMapLayer.class);
	
	
	private Paint pointAltUI;
	private Paint point;
	private OsmandMapTileView view;
	private List<Amenity> objects = new ArrayList<Amenity>();

	private ResourceManager resourceManager;
	private PoiFilter filter;
	private DisplayMetrics dm;
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}
	
	public PoiFilter getFilter() {
		return filter;
	}
	
	public void setFilter(PoiFilter filter) {
		this.filter = filter;
	}
	
	public Amenity getAmenityFromPoint(PointF point){
		Amenity result = null;
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity n = objects.get(i);
					int x = view.getRotatedMapXForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						radius = Math.max(Math.abs(x - ex), Math.abs(y - ey));
						result = n;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
		return result;
	}
	

	@Override
	public boolean onTouchEvent(PointF point) {
		Amenity n = getAmenityFromPoint(point);
		if(n != null){
			String format = n.getSimpleFormat(OsmandSettings.usingEnglishNames(view.getSettings()));
			if(n.getOpeningHours() != null){
				format += "\n" + view.getContext().getString(R.string.opening_hours) +" : "+ n.getOpeningHours(); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if(n.getPhone() != null){
				format += "\n" + view.getContext().getString(R.string.phone) +" : "+ n.getPhone(); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if(n.getSite() != null){
				format += "\n" + view.getContext().getString(R.string.website) +" : "+ n.getSite(); //$NON-NLS-1$ //$NON-NLS-2$
			}
			Toast.makeText(view.getContext(), format, Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}
	
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		pointAltUI = new Paint();
		pointAltUI.setColor(Color.rgb(255, 128, 0));
		pointAltUI.setAlpha(160);
		pointAltUI.setStyle(Style.FILL);
		
		point = new Paint();
		point.setColor(Color.GRAY);
		point.setAntiAlias(true);
		point.setStyle(Style.STROKE);
		resourceManager = view.getApplication().getResourceManager();
	}
	
	public int getRadiusPoi(int zoom){
		int r = 0;
		if(zoom < startZoom){
			r = 0;
		} else if(zoom <= 15){
			r = 10;
		} else if(zoom == 16){
			r = 14;
		} else if(zoom == 17){
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * dm.density);
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, boolean nightMode) {
		if (view.getZoom() >= startZoom) {

			objects.clear();
			resourceManager.searchAmenitiesAsync(latLonBounds.top, latLonBounds.left, latLonBounds.bottom, latLonBounds.right, view.getZoom(), filter, objects);
			int r = getRadiusPoi(view.getZoom());
			for (Amenity o : objects) {
				int x = view.getMapXForPoint(o.getLocation().getLongitude());
				int y = view.getMapYForPoint(o.getLocation().getLatitude());
				
				canvas.drawCircle(x, y, r, pointAltUI);
				canvas.drawCircle(x, y, r, point);
			}

		}
	}

	@Override
	public void destroyLayer() {
		
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public OnClickListener getActionListener(List<String> actionsList, Object o) {
		final Amenity a = (Amenity) o;
		actionsList.add(this.view.getResources().getString(R.string.poi_context_menu_modify));
		actionsList.add(this.view.getResources().getString(R.string.poi_context_menu_delete));
		int ind = 2;
		final int phoneIndex = a.getPhone() != null ? ind++ : -1;
		final int siteIndex = a.getSite() != null ? ind++ : -1;
		if(a.getPhone() != null){
			actionsList.add(this.view.getResources().getString(R.string.poi_context_menu_call));
		}
		if(a.getSite() != null){
			actionsList.add(this.view.getResources().getString(R.string.poi_context_menu_website));
		}
		final EditingPOIActivity edit = new EditingPOIActivity(view.getContext(), view.getApplication(), view);
		return new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					edit.showEditDialog(a);
				} else if(which == 1) {
					edit.showDeleteDialog(a);
				} else if (which == phoneIndex) {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse("tel:"+a.getPhone())); //$NON-NLS-1$
						view.getContext().startActivity(intent);
					} catch (RuntimeException e) {
						log.error("Failed to invoke call", e); //$NON-NLS-1$
						Toast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				} else if (which == siteIndex) {
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(a.getSite())); 
						view.getContext().startActivity(intent);
					} catch (RuntimeException e) {
						log.error("Failed to invoke call", e); //$NON-NLS-1$
						Toast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
					}
				} else {
				}
			}
		};
	}

	@Override
	public String getObjectDescription(Object o) {
		if(o instanceof Amenity){
			return ((Amenity)o).getSimpleFormat(OsmandSettings.usingEnglishNames(view.getSettings()));
		}
		return null;
	}

	@Override
	public Object getPointObject(PointF point) {
		return getAmenityFromPoint(point);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof Amenity){
			return ((Amenity)o).getLocation();
		}
		return null;
	}

}
