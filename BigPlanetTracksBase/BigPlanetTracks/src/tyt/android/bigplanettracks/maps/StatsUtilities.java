/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package tyt.android.bigplanettracks.maps;

import android.app.Activity;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import tyt.android.bigplanettracks.R;
import tyt.android.bigplanettracks.tracks.MyTimeUtils;

/**
 * Various utility functions for views that display statistics information.
 *
 * @author Sandor Dornbush, (Modified by TYTung)
 */
public class StatsUtilities {

  private final Activity activity;
  private static final NumberFormat LAT_LONG_FORMAT =
      new DecimalFormat("##,###.00000");
  private static final NumberFormat ALTITUDE_FORMAT =
      new DecimalFormat("###,###");
  private static final NumberFormat SPEED_FORMAT =
      new DecimalFormat("#,###,###.00");

  public StatsUtilities(Activity a) {
    this.activity = a;
  }

  public void setUnknown(int id) {
    ((TextView) activity.findViewById(id)).setText(R.string.unknown);
  }

  public void setText(int id, double d, NumberFormat format) {
    if (!Double.isNaN(d) && !Double.isInfinite(d)) {
      setText(id, format.format(d));
    }
  }

  public void setText(int id, String s) {
    int lengthLimit = 8;
    String displayString = s.length() > lengthLimit
      ? s.substring(0, lengthLimit - 3) + "..."
      : s;
      ((TextView) activity.findViewById(id)).setText(displayString);
  }

  public void setLatLong(int id, double d) {
    TextView msgTextView = (TextView) activity.findViewById(id);
    msgTextView.setText(LAT_LONG_FORMAT.format(d));
  }

  public void setAltitude(int id, double d) {
    setText(id, d, ALTITUDE_FORMAT);
  }

  public void setDistance(int id, double d) {
    setText(id, d, SPEED_FORMAT);
  }

  public void setSpeed(int id, double d) {
    if (d == 0) {
      setUnknown(id);
      return;
    }
    double speed = d;
    setText(id, speed, SPEED_FORMAT);
  }

  public void setSpeedUnits(int unitLabelId, int unitLabelBottomId) {
    TextView unitTextView = (TextView) activity.findViewById(unitLabelId);
    unitTextView.setText(R.string.kilometer);

    unitTextView = (TextView) activity.findViewById(unitLabelBottomId);
    unitTextView.setText(R.string.hr);
  }

  public void setTime(int id, long l) {
    setText(id, MyTimeUtils.getTimeString(l));
  }

  /**
   * Updates the unit fields.
   */
  public void updateUnits() {
    setSpeedUnits(R.id.speed_unit_label_top, R.id.speed_unit_label_bottom);
    setSpeedUnits(R.id.max_speed_unit_label_top, R.id.max_speed_unit_label_bottom);
    setSpeedUnits(R.id.average_speed_unit_label_top, R.id.average_speed_unit_label_bottom);
  }

  /**
   * Sets all fields to "-" (unknown).
   */
  public void setAllToUnknown() {
    // "Instant" values:
    setUnknown(R.id.elevation_register);
    setUnknown(R.id.latitude_register);
    setUnknown(R.id.longitude_register);
    setUnknown(R.id.speed_register);
    // Values from provider:
    setUnknown(R.id.total_time_register);
    setUnknown(R.id.total_distance_register);
    setUnknown(R.id.average_speed_register);
    setUnknown(R.id.max_speed_register);
    setUnknown(R.id.min_elevation_register);
    setUnknown(R.id.max_elevation_register);
    setUnknown(R.id.elevation_gain_register);
  }

  public void setAllStats(double totalDistance,
      double averageSpeed, double maxSpeed,
      double minElevation, double maxElevation, double elevationGain) {
    setDistance(R.id.total_distance_register, totalDistance / 1000);
    setSpeed(R.id.average_speed_register, averageSpeed);
    setSpeed(R.id.max_speed_register, maxSpeed);
    setAltitude(R.id.min_elevation_register, minElevation);
    setAltitude(R.id.max_elevation_register, maxElevation);
    setAltitude(R.id.elevation_gain_register, elevationGain);
  }
  
}
