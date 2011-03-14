package net.osmand.views;

import java.util.List;

import net.osmand.OsmandSettings;
import net.osmand.TransportIndexRepository.RouteInfoLocation;
import net.osmand.activities.TransportRouteHelper;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.LatLon;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

public class TransportInfoLayer implements OsmandMapLayer {
	
	private final TransportRouteHelper routeHelper;
	private OsmandMapTileView view;
	private Paint paintInt;
	private Paint paintEnd;
	private boolean visible = true;
	private DisplayMetrics dm;
	
	public TransportInfoLayer(TransportRouteHelper routeHelper){
		this.routeHelper = routeHelper;
	}
	
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);

		paintInt = new Paint();
		paintInt.setColor(Color.rgb(50, 200, 50));
		paintInt.setAlpha(150);
		paintInt.setAntiAlias(true);
		paintEnd = new Paint();
		paintEnd.setColor(Color.rgb(255, 0, 0));
		paintEnd.setAlpha(150);
		paintEnd.setAntiAlias(true);
	}
	
	public boolean isVisible() {
		return visible;
	}
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public int getRadius(){
		return (int) (dm.density * 8);
	}

	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, boolean nightMode) {
		if(routeHelper.routeIsCalculated() && visible){
			List<RouteInfoLocation> list = routeHelper.getRoute();
			for(RouteInfoLocation l : list){
				if(l == null){
					// once l is null in list
					continue;
				}
				TransportRoute route = l.getRoute();
				boolean start = false;
				boolean end = false;
				List<TransportStop> stops = l.getDirection() ? route.getForwardStops() : route.getBackwardStops();
				for(int i=0; i<stops.size() && !end;  i++){
					Paint toShow = paintInt;
					TransportStop st = stops.get(i);
					if(!start){
						if(st == l.getStart()){
							start = true;
							toShow = paintEnd;
						}
					} else {
						if(st == l.getStop()){
							end = true;
							toShow = paintEnd;
						}
					}
					if(start){
						LatLon location = st.getLocation();
						if (location.getLatitude() >= latLonBounds.bottom && location.getLatitude() <= latLonBounds.top  && location.getLongitude() >= latLonBounds.left 
								&& location.getLongitude() <= latLonBounds.right ) {
							int x = view.getRotatedMapXForPoint(location.getLatitude(), location.getLongitude());
							int y = view.getRotatedMapYForPoint(location.getLatitude(), location.getLongitude());
							canvas.drawRect(x - getRadius(), y - getRadius(), x + getRadius(), y + getRadius(), toShow);
						}
					}
				}
				
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
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		if (visible && !routeHelper.getRoute().isEmpty()) {
			for (RouteInfoLocation l : routeHelper.getRoute()) {
				if(l == null){
					// once l is null in list
					continue;
				}
				TransportRoute route = l.getRoute();
				boolean start = false;
				boolean end = false;
				List<TransportStop> stops = l.getDirection() ? route.getForwardStops() : route.getBackwardStops();
				for (int i = 0; i < stops.size() && !end; i++) {
					TransportStop st = stops.get(i);
					if (!start) {
						if (st == l.getStart()) {
							start = true;
						}
					} else {
						if (st == l.getStop()) {
							end = true;
						}
					}
					if (start) {
						LatLon location = st.getLocation();
						int x = view.getRotatedMapXForPoint(location.getLatitude(), location.getLongitude());
						int y = view.getRotatedMapYForPoint(location.getLatitude(), location.getLongitude());
						if (Math.abs(x - ex) < getRadius() * 3 /2 && Math.abs(y - ey) < getRadius() * 3 /2) {
							Toast.makeText(view.getContext(), st.getName(OsmandSettings.usingEnglishNames(view.getSettings())) + " : " + //$NON-NLS-1$
									route.getType() + " " + route.getRef() //$NON-NLS-1$
							, Toast.LENGTH_LONG).show();
							return true;
						}
					}
				}

			}
		}
		return false;
	}


	
	
}
