/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.io.IOException;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.OsmandSettings;
import net.osmand.activities.MapActivity;
import net.osmand.activities.RoutingHelper;
import net.osmand.activities.SavingTrackHelper;
import net.osmand.activities.TransportRouteHelper;
import net.osmand.map.IMapLocationListener;
import net.osmand.osm.LatLon;
import net.osmand.render.RendererLayer;
import net.osmand.views.ContextMenuLayer;
import net.osmand.views.FavoritesLayer;
import net.osmand.views.GPXLayer;
import net.osmand.views.MapInfoLayer;
import net.osmand.views.OsmBugsLayer;
import net.osmand.views.OsmandMapTileView;
import net.osmand.views.POIMapLayer;
import net.osmand.views.PointLocationLayer;
import net.osmand.views.PointNavigationLayer;
import net.osmand.views.RouteInfoLayer;
import net.osmand.views.RouteLayer;
import net.osmand.views.TransportInfoLayer;
import net.osmand.views.TransportStopsLayer;
import net.osmand.views.YandexTrafficLayer;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

public class TiUIOpenStreetMap extends TiUIView
{
	private static final String LCAT = "TiUIOpenStreetMap";
	private static final boolean DBG = TiConfig.LOGD;

	// the order of layer should be preserved ! when you are inserting new layer
	private RendererLayer rendererLayer;
	private GPXLayer gpxLayer;
	private RouteLayer routeLayer;
	private YandexTrafficLayer trafficLayer;
	private OsmBugsLayer osmBugsLayer;
	private POIMapLayer poiMapLayer;
	private FavoritesLayer favoritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private TransportInfoLayer transportInfoLayer;
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private MapInfoLayer mapInfoLayer;
	private ContextMenuLayer contextMenuLayer;
	private RouteInfoLayer routeInfoLayer;

	private SavingTrackHelper savingTrackHelper;
	private RoutingHelper routingHelper;

	private SharedPreferences settings;

	private Context context;
	
	public TiUIOpenStreetMap(final TiViewProxy proxy) {
		super(proxy);
		if (DBG) {
			Log.d(LCAT, "Creating a Open Street Map");
		}
		context = proxy.getContext();

		OsmandMapTileView mapView = new OsmandMapTileView(proxy.getContext());
		mapView.setMapLocationListener(new IMapLocationListener(){
			public void locationChanged(double newLatitude,
					double newLongitude, Object source) {
				locationChanged(newLatitude, newLongitude, source);
			}
		});
		//routingHelper = ((OsmandApplication) getApplication()).getRoutingHelper();
		
		// 0.5 layer
		rendererLayer = new RendererLayer();
		mapView.addLayer(rendererLayer, 0.5f);
		
		// 0.6 gpx layer
		gpxLayer = new GPXLayer();
		mapView.addLayer(gpxLayer, 0.6f);
		
		// 1. route layer
//		routeLayer = new RouteLayer(routingHelper);
//		mapView.addLayer(routeLayer, 1);
		
		// 1.5. traffic layer
		trafficLayer = new YandexTrafficLayer();
		mapView.addLayer(trafficLayer, 1.5f);
		
		
		// 2. osm bugs layer
//		osmBugsLayer = new OsmBugsLayer(proxy.getContext());   <- the first argument is Activity
		// 3. poi layer
		poiMapLayer = new POIMapLayer();
		// 4. favorites layer
		favoritesLayer = new FavoritesLayer();
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5.5 transport info layer 
		transportInfoLayer = new TransportInfoLayer(TransportRouteHelper.getInstance());
		mapView.addLayer(transportInfoLayer, 5.5f);
		// 6. point navigation layer
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer, 6);
		// 7. point location layer 
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer, 7);
		// 8. map info layer
//		mapInfoLayer = new MapInfoLayer(proxy.getContext(), routeLayer);  <- the first argument is Activity
//		mapView.addLayer(mapInfoLayer, 8);
		// 9. context menu layer 
//		contextMenuLayer = new ContextMenuLayer(proxy.getContext());  <- the first argument is Activity
//		mapView.addLayer(contextMenuLayer, 9);
		// 10. route info layer
//		routeInfoLayer = new RouteInfoLayer(routingHelper, (LinearLayout) findViewById(R.id.RouteLayout));
//		mapView.addLayer(routeInfoLayer, 10);
		
		savingTrackHelper = new SavingTrackHelper(proxy.getContext());
		
		LatLon pointToNavigate = OsmandSettings.getPointToNavigate(settings);
		
		// This situtation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if(!Algoritms.objectEquals(routingHelper.getFinalLocation(), pointToNavigate)){
			routingHelper.setFollowingMode(false);
			routingHelper.setFinalAndCurrentLocation(pointToNavigate, null);

		}

		setNativeView(mapView);
	}
	
	
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user start dragging 
		if(locationLayer.getLastKnownLocation() != null){
			if(isMapLinkedToLocation()){
				OsmandSettings.setSyncMapToGpsLocation(context, false);
			}
		}
	}
	
	private boolean isMapLinkedToLocation(){
		return OsmandSettings.isMapSyncToGpsLocation(settings);
	}
	
	/* What's this method for? */
	public void processProperties(KrollDict d) {
		super.processProperties(d);
	}

	/* What's this method for? */
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		super.propertyChanged(key, oldValue, newValue, proxy);
	}
	
	/* What's this method for? */
	public void setOpacity(float opacity) {
//		TiUIHelper.setPaintOpacity(((TiUIOpenStreetMap)getNativeView()).getPaint(), opacity); <- Activity?
		super.setOpacity(opacity);
	}
	
	public void clearOpacity(View view) {
		super.clearOpacity(view);
//		((TiUIOpenStreetMap)getNativeView()).getPaint().setColorFilter(null);  <- Activity?
	}
}
