package tyt.android.bigplanettracks.tracks.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tyt.android.bigplanettracks.maps.storage.SQLLocalStorage;
import tyt.android.bigplanettracks.tracks.listview.ExtendedCheckBox;

import android.os.Handler;

/**
 * @author taiyuchen
 * @version 0.1
 */
public class FileImporter {

	public static String GPXFileImportPath;
	private List<String> items = null;

	public FileImporter() {
		GPXFileImportPath = SQLLocalStorage.TRACK_IMPORT_PATH;
		items = new ArrayList<String>();
	}

	public List<String> findAllGPXFiles() {
		this.items = null;
		this.items = new ArrayList<String>();
		File file = new File(FileImporter.GPXFileImportPath);
		if (!file.exists())
			file.mkdir();
		File[] files = file.listFiles();
		for (File f : files) {
//			System.out.println("file: " + f.getName());
			if (f.getName().endsWith(".gpx")) {
				this.items.add(f.getName().replace(".gpx", ""));
			}
		}

//		System.out.println("gpx files found: " + items.toString());
		return items;
	}

	public void parseGPXFile(List<ExtendedCheckBox> extendedCheckBoxList,
			Handler mainHandler, String trackSource) {
		MyParseThread myParseThread = new MyParseThread("GPXParseThread", extendedCheckBoxList);
		myParseThread.setMainThreadHandler(mainHandler);
		myParseThread.setGPXFileDirectory(GPXFileImportPath);
		myParseThread.setTrackSourceString(trackSource);
		myParseThread.start();
	}

}
