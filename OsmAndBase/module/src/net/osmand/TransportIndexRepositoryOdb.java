package net.osmand;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.database.Cursor;

public class TransportIndexRepositoryOdb extends BaseLocationIndexRepository<TransportStop> implements TransportIndexRepository {
	 private static final Log log = LogUtil.getLog(TransportIndexRepositoryOdb.class);

	 private final static String TRANSPORT_STOP_TABLE = IndexConstants.TRANSPORT_STOP_TABLE;
	 private final static String TRANSPORT_ROUTE_STOP_TABLE = IndexConstants.TRANSPORT_ROUTE_STOP_TABLE;
	 private final static String TRANSPORT_ROUTE_TABLE = IndexConstants.TRANSPORT_ROUTE_TABLE;

	public boolean initialize(final IProgress progress, File file) {
		return super.initialize(progress, file, IndexConstants.TRANSPORT_TABLE_VERSION, TRANSPORT_STOP_TABLE, false);
	}
	
	private final String[] columns = new String[]{"id", "latitude", "longitude", "name", "name_en"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	public List<TransportStop> searchTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int limit, List<TransportStop> stops){
		long now = System.currentTimeMillis();
		String squery = "? < latitude AND latitude < ? AND ? < longitude AND longitude < ?"; //$NON-NLS-1$
		
		if(limit != -1){
			squery += " ORDER BY RANDOM() LIMIT " +limit; //$NON-NLS-1$
		}
		Cursor query = db.query(TRANSPORT_STOP_TABLE, columns, squery,
				new String[]{Double.toString(bottomLatitude), 
				Double.toString(topLatitude), Double.toString(leftLongitude), Double.toString(rightLongitude)}, null, null, null);
		if(query.moveToFirst()){
			do {
				TransportStop st = new TransportStop();
				st.setId(query.getLong(0));
				st.setLocation(query.getDouble(1), 
							query.getDouble(2));
				st.setName(query.getString(3 ));
				st.setEnName(query.getString(4));
				stops.add(st);
				if(limit != -1 && stops.size() >= limit){
					break;
				}
			} while(query.moveToNext());
		}
		query.close();
		
		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for %s done in %s ms found %s.",  //$NON-NLS-1$
					topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, stops.size())); //$NON-NLS-1$
		}
		return stops;
	}
	
	
	
	private static String cacheSQLRouteDescriptions = null;
	/**
	 * 
	 * @param stop
	 * @param format {0} - ref, {1} - type, {2} - name, {3} - name_en
	 * @return
	 */
	public List<String> getRouteDescriptionsForStop(TransportStop stop, String format) {
		long now = System.currentTimeMillis();
		List<String> res = new ArrayList<String>();
		MessageFormat f = new MessageFormat(format);

		if (cacheSQLRouteDescriptions == null) {
			StringBuilder sql = new StringBuilder(200);
			sql.append("SELECT DISTINCT ref, type, name, name_en FROM ").append(TRANSPORT_ROUTE_TABLE).append(" JOIN ").append(TRANSPORT_ROUTE_STOP_TABLE); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" ON transport_route.id = transport_route_stop.route WHERE transport_route_stop.stop = ?"); //$NON-NLS-1$
			cacheSQLRouteDescriptions = sql.toString();
		}
		Cursor query = db.rawQuery(cacheSQLRouteDescriptions, new String[] { stop.getId() + "" }); //$NON-NLS-1$
		if (query.moveToFirst()) {
			do {
				res.add(f.format(new String[] { query.getString(0), query.getString(1), query.getString(2), query.getString(3) }));
			} while (query.moveToNext());
		}
		query.close();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for stop %s done in %s ms found %s.", //$NON-NLS-1$
					stop.getId() + "", System.currentTimeMillis() - now, res.size())); //$NON-NLS-1$
		}
		return res;
	}
	
	
	public void evaluateCachedTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, int limit,  List<TransportStop> toFill){
		cTopLatitude = topLatitude + (topLatitude -bottomLatitude);
		cBottomLatitude = bottomLatitude - (topLatitude -bottomLatitude);
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
		cZoom = zoom;
		// first of all put all entities in temp list in order to not freeze other read threads
		ArrayList<TransportStop> tempList = new ArrayList<TransportStop>();
		searchTransportStops(cTopLatitude, cLeftLongitude, cBottomLatitude, cRightLongitude, limit, tempList);
		synchronized (this) {
			cachedObjects.clear();
			cachedObjects.addAll(tempList);
		}
		
		checkCachedObjects(topLatitude, leftLongitude, bottomLatitude, rightLongitude, cZoom, toFill);
	}
	
	
	private static String cacheSQLRoutes = null;
	public List<RouteInfoLocation> searchTransportRouteStops(double latitude, double longitude, LatLon locationToGo, int zoom) {
		long now = System.currentTimeMillis();
		LatLon loc = new LatLon(latitude, longitude);
		double tileNumberX = MapUtils.getTileNumberX(zoom, longitude);
		double tileNumberY = MapUtils.getTileNumberY(zoom, latitude);
		double topLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY - 0.5);
		double bottomLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY + 0.5);
		double leftLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX - 0.5);
		double rightLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX + 0.5);
		if(cacheSQLRoutes == null){
			StringBuilder sql = new StringBuilder(200);
			sql.append("SELECT R.id, R.dist, R.name, R.name_en, R.ref, R.operator, R.type, "); //$NON-NLS-1$
			sql.append("T.id, T.name, T.name_en, T.latitude, T.longitude, TR.direction "); //$NON-NLS-1$
			sql.append(" FROM ").append(TRANSPORT_STOP_TABLE).append(" T "); //$NON-NLS-1$ //$NON-NLS-2$
			// join with stops table
			sql.append(" JOIN ").append(TRANSPORT_ROUTE_STOP_TABLE).append(" TR "); //$NON-NLS-1$ //$NON-NLS-2$ 
			sql.append(" ON T.id = TR.stop "); //$NON-NLS-1$
			// join with route table
			sql.append(" JOIN ").append(TRANSPORT_ROUTE_TABLE).append(" R "); //$NON-NLS-1$ //$NON-NLS-2$ 
			sql.append(" ON R.id = TR.route "); //$NON-NLS-1$
			sql.append(" WHERE ").append("? < latitude AND latitude < ? AND ? < longitude AND longitude < ?"); //$NON-NLS-1$ //$NON-NLS-2$
			cacheSQLRoutes = sql.toString();
		}
		Cursor query = db.rawQuery(cacheSQLRoutes, 
				new String[] {bottomLatitude + "" , topLatitude + "" , leftLongitude + "" , rightLongitude + "" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		Map<Long, RouteInfoLocation> registeredRoutes = new LinkedHashMap<Long, RouteInfoLocation>();
		if (query.moveToFirst()) {
			do {
				TransportRoute route = new TransportRoute();
				route.setId(query.getLong(0));
				route.setDistance(query.getInt(1));
				route.setName(query.getString(2));
				route.setEnName(query.getString(3));
				route.setRef(query.getString(4));
				route.setOperator(query.getString(5));
				route.setType(query.getString(6));
				TransportStop s = new TransportStop();
				s.setId(query.getLong(7));
				s.setName(query.getString(8));
				s.setEnName(query.getString(9));
				s.setLocation(query.getDouble(10),	query.getDouble(11));
				boolean direction = query.getInt(12) > 0; 
				long idToPut = route.getId() << 1 + (direction ? 1 : 0);
				if(registeredRoutes.containsKey(idToPut)){
					TransportStop st = registeredRoutes.get(idToPut).getStart();
					if(MapUtils.getDistance(loc, st.getLocation()) < MapUtils.getDistance(loc, s.getLocation())){
						continue;
					}
				}
				RouteInfoLocation r = new RouteInfoLocation();
				r.setRoute(route);
				r.setStart(s);
				r.setDirection(direction);
				registeredRoutes.put(idToPut, r);
				
				
			} while (query.moveToNext());
		}
		query.close();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for routes done in %s ms found %s.", //$NON-NLS-1$
					System.currentTimeMillis() - now, registeredRoutes.size())); 
		}
		
		List<RouteInfoLocation> list = preloadRouteStopsAndCalculateDistance(loc, locationToGo, registeredRoutes);
		return list;
		
	}
	
	@Override
	public boolean acceptTransportStop(TransportStop stop) {
		return checkContains(stop.getLocation().getLatitude(), stop.getLocation().getLongitude());
	}

	protected List<RouteInfoLocation> preloadRouteStopsAndCalculateDistance(final LatLon loc, LatLon locationToGo,
			Map<Long, RouteInfoLocation> registeredRoutes) {
		if(registeredRoutes.isEmpty()){
			return Collections.emptyList();
		}
		long now = System.currentTimeMillis();
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT T.id, T.latitude, T.longitude, T.name, T.name_en, "); //$NON-NLS-1$
		sql.append(" TR.route, TR.direction " ); //$NON-NLS-1$
		sql.append(" FROM ").append(TRANSPORT_STOP_TABLE).append(" T "); //$NON-NLS-1$ //$NON-NLS-2$
		// join with stops table
		sql.append(" JOIN ").append(TRANSPORT_ROUTE_STOP_TABLE).append(" TR "); //$NON-NLS-1$ //$NON-NLS-2$ 
		sql.append(" ON T.id = TR.stop "); //$NON-NLS-1$

		sql.append(" WHERE "); //$NON-NLS-1$
		boolean f = true;
		for (RouteInfoLocation il : registeredRoutes.values()) {
			if (f) {
				f = false;
			} else {
				sql.append(" OR "); //$NON-NLS-1$
			}
			sql.append("(TR.route"); //$NON-NLS-1$
			sql.append(" = ").append(il.getRoute().getId()); //$NON-NLS-1$
			sql.append(" AND TR.direction"); //$NON-NLS-1$
			sql.append(" = ").append(il.getDirection() ? 1 : 0); //$NON-NLS-1$
			sql.append(")"); //$NON-NLS-1$
		}
		sql.append(" ORDER BY TR.ord asc"); //$NON-NLS-1$


		Map<Long, TransportStop> distanceToLoc = new LinkedHashMap<Long, TransportStop>();

		Cursor query = db.rawQuery(sql.toString(), new String[] {}); 
		if (query.moveToFirst()) {
			// load only part of the route
			do {
				TransportStop st = null;

				long routeId = query.getLong(5);
				int direction = query.getInt(6);
				long id = routeId << 1 + direction;
				boolean found = distanceToLoc.containsKey(id);
				RouteInfoLocation i = registeredRoutes.get(id);
				if (found) {
					st = new TransportStop();
					st.setId(query.getLong(0));
					st.setLocation(query.getDouble(1), query.getDouble(2));
					st.setName(query.getString(3));
					st.setEnName(query.getString(4));
				} else if (query.getLong(0) == i.getStart().getId()) {
					st = i.getStart();
					found = true;
					distanceToLoc.put(id, st);
				}

				if (found) {
					if (locationToGo != null) {
						double d = MapUtils.getDistance(locationToGo, st.getLocation());
						double dbase = MapUtils.getDistance(locationToGo, distanceToLoc.get(id).getLocation());
						if (d < dbase) {
							distanceToLoc.put(id, st);
						}
					}
					if (i.getDirection()) {
						i.getRoute().getForwardStops().add(st);
					} else {
						i.getRoute().getBackwardStops().add(st);
					}
				}

			} while (query.moveToNext());
			query.close();

		}
		
		if (locationToGo != null) {
			for (Long l : registeredRoutes.keySet()) {
				Integer dist = (int) MapUtils.getDistance(locationToGo, distanceToLoc.get(l).getLocation());
				if (dist != null) {
					registeredRoutes.get(l).setDistToLocation(dist);
				}
				registeredRoutes.get(l).setStop(distanceToLoc.get(l));
			}
		}

		ArrayList<RouteInfoLocation> listRoutes = new ArrayList<RouteInfoLocation>(registeredRoutes.values());
		if (log.isDebugEnabled()) {
			log.debug(String.format("Loading routes done in %s ms for %s routes.", //$NON-NLS-1$
					System.currentTimeMillis() - now, listRoutes.size()));
		}

		if (locationToGo != null) {
			Collections.sort(listRoutes, new Comparator<RouteInfoLocation>() {
				@Override
				public int compare(RouteInfoLocation object1, RouteInfoLocation object2) {
					int x = (int) (MapUtils.getDistance(loc, object1.getStart().getLocation()) + object1.getDistToLocation());
					int y = (int) (MapUtils.getDistance(loc, object2.getStart().getLocation()) + object2.getDistToLocation());
					return  x - y;
				}

			});
		} else {
			Collections.sort(listRoutes, new Comparator<RouteInfoLocation>() {
				@Override
				public int compare(RouteInfoLocation object1, RouteInfoLocation object2) {
					return Double.compare(MapUtils.getDistance(loc, object1.getStart().getLocation()), MapUtils.getDistance(loc, object2
							.getStart().getLocation()));
				}

			});
		}
		return listRoutes;
	}

}
