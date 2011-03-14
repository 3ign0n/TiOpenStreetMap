package tyt.android.bigplanettracks.tracks.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import tyt.android.bigplanettracks.tracks.TrackAnalyzer;
import tyt.android.bigplanettracks.tracks.TrackTabViewActivity;
import tyt.android.bigplanettracks.tracks.listview.ExtendedCheckBox;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author taiyuchen, TYTung
 * @version 0.1
 */
public class MyParseThread extends Thread {

	private Handler mainThreadHandler;
	private List<ExtendedCheckBox> fileParsingList;
	private String GPXFilePath;
	private ArrayList<File> failedFileList;
	public static final int ALL_Files_PARSE_SUCCESS=1;
	public static final int Some_Files_PARSE_FAIL=0;
	public int count;
	private static String trackSourceString;

	public MyParseThread(String threadName, List<ExtendedCheckBox> fileParsingList) {
		super(threadName);
		this.fileParsingList = fileParsingList;
		mainThreadHandler = null;
		failedFileList = new ArrayList<File>();
		count = 0;
	}
	
	public void setTrackSourceString(String source) {
		MyParseThread.trackSourceString = source;
	}
	
	public  void setMainThreadHandler(Handler mainHandler) {
		this.mainThreadHandler = mainHandler;
	}
	
	public void setGPXFileDirectory(String myGPXFilePath) {
		this.GPXFilePath = myGPXFilePath;
	}
	
	public void run() {

		Message m = null;
		try {
			/* Parse all selected tracks*/  
			for (ExtendedCheckBox extendedCheckBox:fileParsingList) {
				if (extendedCheckBox.getChecked()) {
					String GPX_file_path = this.GPXFilePath+File.separator+extendedCheckBox.getText()+".gpx";
					parsingGPXFile(new File(GPX_file_path));
				}
			}
			
			/*transfer the results of file parsing */
			if (this.getFailedFileList().size()>0) {
				StringBuffer obj = new StringBuffer();
				for (File file: this.getFailedFileList()) {
					obj.append(file.getName()+" ");
				}
				m = mainThreadHandler.obtainMessage(Some_Files_PARSE_FAIL, 0, 1, obj.toString());
			} else {
				String obj = "Success!";
				m = mainThreadHandler.obtainMessage(ALL_Files_PARSE_SUCCESS, 1, 1, obj);
			}
			
			if (mainThreadHandler != null)
				mainThreadHandler.sendMessage(m);
			else
				throw new Error("mainHandler is Null");
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			TrackTabViewActivity.myGPXParseDialog.dismiss();
		}
	}
	
	private void parsingGPXFile(File gpxFile) {
		FileInputStream fis;
		Reader reader;
		boolean isError = false;
		String errorMsg = null;
			
		try {
			fis = new FileInputStream(gpxFile);
			reader = new InputStreamReader(fis);
			Log.i("Message:", "GPXFile="+gpxFile);
//			Log.i("Message:","Parsing XML begins...");
			/* Get a SAXParser from the SAXPArserFactory. */ 
			SAXParserFactory spf = SAXParserFactory.newInstance(); 
			SAXParser sp = spf.newSAXParser();
			/* Get the XMLReader of the SAXParser we created. */ 
			XMLReader xr;
			xr = sp.getXMLReader();
			/* Create a new ContentHandler and apply it to the XML-Reader*/ 
			GpxHandler myGpxHandler = new GpxHandler(); 
			xr.setContentHandler(myGpxHandler);
			/* Parse the xml-data from our URL. */
			InputSource inputSource = new InputSource(reader);
			inputSource.setEncoding("UTF-8");
			//sp.parse(inputSource, myExampleHandler);
			xr.parse(inputSource);
			/* Parsing has finished. */
//			Log.i("Message:","Parsing has finished...");
			reader.close();
			fis.close();
				
			/* Save the parsedData into sqlite DB */  
			ParsedDataSet parsedDataSet = myGpxHandler.getParsedData();
			ArrayList<Location> locationList = parsedDataSet.getLocationList();
			String trackName = parsedDataSet.getTrackName();
			String trackDescription = parsedDataSet.getTrackDescription();
			String trackStartGMTTime = parsedDataSet.getTrackStartGMTTime();
			
			TrackAnalyzer analyzer = new TrackAnalyzer(trackName, trackDescription, trackStartGMTTime, locationList, trackSourceString);
			analyzer.analyzeAndSave();

		} catch (FileNotFoundException e){
			e.printStackTrace();
			isError = true;
			errorMsg = e.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			isError = true;
			errorMsg = e.toString();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			isError = true;
			errorMsg = e.toString();
		} catch (SAXException e) {
			e.printStackTrace();
			isError = true;
			errorMsg = e.toString();
		} catch (IOException e) {
			e.printStackTrace();
			isError = true;
			errorMsg = e.toString();
		} finally {
			if (isError) {
				Log.i("error", errorMsg);
				Log.i("Message","Add to failedList");
				failedFileList.add(gpxFile);
			}
		}
	}
	
	public ArrayList<File> getFailedFileList() {
		return this.failedFileList;
	}
	
}
