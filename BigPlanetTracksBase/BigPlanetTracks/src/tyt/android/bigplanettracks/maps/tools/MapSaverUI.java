package tyt.android.bigplanettracks.maps.tools;

import java.text.MessageFormat;
import java.util.Stack;
import java.util.Timer;

import tyt.android.bigplanettracks.R;
import tyt.android.bigplanettracks.maps.RawTile;
import tyt.android.bigplanettracks.maps.geoutils.GeoUtils;
import tyt.android.bigplanettracks.maps.providers.MapStrategyFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MapSaverUI {

	private final String DOWNLOAD_MESSAGE_PATTERN;

	private Context context;

	private int zoomLevel;

	private boolean alreadyCreated = false;

	private Point absoluteCenter;

	private MapSaver mapSaver;

	private Stack<RawTile> tiles = new Stack<RawTile>();

	private int sourceId;

	private int radius = 0;

	private TextView downloadInfo;

	private Timer timer = new Timer();

	public MapSaverUI(Context context, int zoomLevel, Point absoluteCenter,
			int sourceId) {
		this.context = context;
		this.absoluteCenter = absoluteCenter;
		this.zoomLevel = zoomLevel;
		this.sourceId = sourceId;
		DOWNLOAD_MESSAGE_PATTERN = getText(R.string.DOWNLOAD_MESSAGE_PATTERN);
	}

	public void show() {
		showParamsDialog();
	}

	private void showParamsDialog() {
		final Dialog paramsDialog = new Dialog(context);
		paramsDialog.setTitle(R.string.REGION_RADIUS_TITLE);
		paramsDialog.setCanceledOnTouchOutside(true);
		paramsDialog.setCancelable(true);

		View v = View.inflate(context, R.layout.savemap, null);

		downloadInfo = (TextView) v.findViewById(R.id.downloadInfo);

		updateLabels();
		final Button startButton = (Button) v.findViewById(R.id.startButton);
		startButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				getTiles(radius, false);
				paramsDialog.dismiss();
				showProgressDialog();
				// запуск загрузки, отображение индикатора
			}

		});

		final Button cancelButton = (Button) v.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				paramsDialog.dismiss();
			}

		});

		SeekBar radiusSeek = (SeekBar) v.findViewById(R.id.radiusSeekbar);
		radiusSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromTouch) {

				radius = (int) Math.pow(2, progress);
				updateLabels();
			}

		});
		paramsDialog.setContentView(v);
		paramsDialog.show();

	}

	private void updateLabels() {
		int tilesCount = getTiles(radius, true);
		String message = MessageFormat.format(
				getText(R.string.DOWNLOAD_PROGRESS_MESSAGE_PATTERN), radius,
				tilesCount,  Math.round(tilesCount * 1100l / 1024l));
		downloadInfo.setText(message);
	}

	private void showProgressDialog() {
		final ProgressDialog downloadDialog = new ProgressDialog(context);
		downloadDialog.setTitle(R.string.DOWNLOAD_IN_PROGRESS_MESSAGE);
		downloadDialog.setCancelable(true);

		downloadDialog.setButton(getText(R.string.CANCEL_LABEL),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						mapSaver.stopDownload();
						downloadDialog.dismiss();
						timer.cancel();
					}

				});

		downloadDialog.show();

		final Thread updateThread;
		mapSaver = new MapSaver(tiles,
				MapStrategyFactory.getStrategy(sourceId), new Handler() {

					@Override
					public void handleMessage(Message msg) {

						switch (msg.what) {
						case 0:
							if (!alreadyCreated
									&& mapSaver.getTotalSuccessful()
											+ mapSaver.getTotalUnsuccessful() == tiles
											.size()) {
								alreadyCreated = true;
								downloadDialog.dismiss();

								Builder completeDialog = new AlertDialog.Builder(
										context)
										.setTitle(
												R.string.DOWNLOAD_COMPLETE_TITLE)
										.setMessage(
												R.string.DOWNLOAD_COMPLETE_MESSAGE)
										.setPositiveButton(
												R.string.OK_LABEL,
												new DialogInterface.OnClickListener() {

													public void onClick(
															DialogInterface dialog,
															int which) {
														downloadDialog.cancel();
													}

												});
								completeDialog.show();
							}

							break;
						}
						super.handleMessage(msg);
					}

				});

		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {

				String message = MessageFormat.format(DOWNLOAD_MESSAGE_PATTERN,
						mapSaver.getTotalSuccessful(), tiles.size(), mapSaver
								.getTotalKB(), mapSaver.getTotalUnsuccessful());
				downloadDialog.setMessage(message);
			}

		};

		updateThread = new Thread() {

			@Override
			public void run() {

				while (downloadDialog != null && downloadDialog.isShowing()) {
					try {
						Thread.sleep(1000);
						handler.sendEmptyMessage(0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
			}

		};

		updateThread.start();

		String message = MessageFormat.format(DOWNLOAD_MESSAGE_PATTERN,
				mapSaver.getTotalSuccessful(), tiles.size(), mapSaver
						.getTotalKB(), mapSaver.getTotalUnsuccessful());
		downloadDialog.setMessage(message);
		mapSaver.download();
	}

	private String getText(int resourceId) {
		return context.getResources().getText(resourceId).toString();
	}

	private int getTiles(int radius, boolean onlyCount) {
		tyt.android.bigplanettracks.maps.geoutils.Point ppoint = GeoUtils.getLatLong(absoluteCenter.x / 256,
				absoluteCenter.y / 256, zoomLevel);
		
		Point latLon = new Point();
		latLon.x = (int) ppoint.x;
		latLon.y = (int) ppoint.y;
		
		double resolution = (Math.cos(latLon.x * Math.PI / 180) * 2 * Math.PI * 6378137)
				/ (256 * Math.pow(2, 17 - zoomLevel));

		// радиус в тайлах
		int dTile;
		dTile = (int) Math.rint((((radius * 1000) / resolution) / 256));
		// коордитаны центровой тайла
		int cx = absoluteCenter.x / 256;
		int cy = absoluteCenter.y / 256;

		int topX = cx - dTile;
		int topY = cy - dTile;

		tiles.clear();
		int count = 0;
		for (int i = topX; i <= topX + 2 * dTile; i++) {
			for (int j = topY; j <= topY + 2 * dTile; j++) {
				if (onlyCount) {
					count++;
				} else {
					RawTile tile = new RawTile(i, j, zoomLevel, sourceId);
					if (GeoUtils.isValid(tile)) {
						tiles.add(tile);
					}

				}
			}
		}
		return count;
	}

}
