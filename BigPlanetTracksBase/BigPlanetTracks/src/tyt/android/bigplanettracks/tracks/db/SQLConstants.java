package tyt.android.bigplanettracks.tracks.db;


public class SQLConstants {
	
	private static String table = TrackDBAdapter.TRACKS_TABLE;

	public static final String SQL_UPDATE_1_1 = "DROP TABLE IF EXISTS '"+ table +"_2';";
	public static final String SQL_UPDATE_1_2 = "CREATE TABLE '"+ table +"_2' AS SELECT * FROM '"+ table +"';";
	public static final String SQL_UPDATE_1_3 = "DROP TABLE '"+ table +"';";
//	public static final String SQL_UPDATE_1_4 = TrackDBAdapter.TRACKS_TABLE_DDL;
	public static final String SQL_UPDATE_1_5 = "INSERT INTO '"+ table +"' (" +
			TrackDBAdapter.FIELD_trackid + ", " +
			TrackDBAdapter.FIELD_name + ", " +
			TrackDBAdapter.FIELD_description + ", " +
			TrackDBAdapter.FIELD_startTime + ", " +
			TrackDBAdapter.FIELD_totalTime + ", " +
			TrackDBAdapter.FIELD_totalDistance +" , " +
			TrackDBAdapter.FIELD_averageSpeed + ", " +
			TrackDBAdapter.FIELD_maximumSpeed + ", " +
			TrackDBAdapter.FIELD_trackPoints + ", " +
			TrackDBAdapter.FIELD_trackSource + ", " +
			TrackDBAdapter.FIELD_measureVersion +
			") SELECT " +
			TrackDBAdapter.FIELD_trackid + ", " +
			TrackDBAdapter.FIELD_name + ", " +
			TrackDBAdapter.FIELD_description + ", " +
			TrackDBAdapter.FIELD_startTime + ", " +
			TrackDBAdapter.FIELD_totalTime + ", " +
			TrackDBAdapter.FIELD_totalDistance +" , " +
			TrackDBAdapter.FIELD_averageSpeed + ", " +
			TrackDBAdapter.FIELD_maximumSpeed + ", " +
			TrackDBAdapter.FIELD_trackPoints + ", " +
			TrackDBAdapter.FIELD_trackSource + ", " +
			TrackDBAdapter.FIELD_measureVersion +
			" FROM '"+ table +"_2';";
	public static final String SQL_UPDATE_1_6 = "UPDATE '"+ table +"' SET "+TrackDBAdapter.FIELD_measureVersion+"=0;";
	public static final String SQL_UPDATE_1_7 = "DROP TABLE '"+ table +"_2';";
	
}
