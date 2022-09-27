/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;
//import com.google.android.gms.location.LocationListener;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 1000;
  private static final boolean TF_OD_API_IS_QUANTIZED = false;/*false?*/

  private static final String TF_OD_API_MODEL_FILE = "onlyplateaddpara.tflite";
  private static final String TF_OD_API_LABELS_FILE = "label.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;//640, 480
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  private static final String TAG = "gpoint";
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Detector detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  Date MDate;
  Long MTime;
  SQLiteDatabase db = null;
  static int count = 0;

  private SensorManager sensorManager;
  private Sensor accelerSensor;
  private SensorEventListener accLis;
  private float accX, accZ;

  double cur_lat;
  double cur_lon;

  ArrayList<Bitmap> bit_arr = new ArrayList<Bitmap>();





  /*private SensorManager sensorManager;
  private Sensor accelerSensor;
  private SensorEventListener accLis;
  private float accX, accY, accZ;

  Handler server_handler = new Handler();
  int bufferszie = 1024;

  Date MDate;
  Long MTime;

  TextView textView6;*/

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.tfe_od_activity_camera);












    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

    if(permissionCheck == PackageManager.PERMISSION_DENIED){ //포그라운드 위치 권한 확인

      //위치 권한 요청
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
    }


    int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

    if(permissionCheck == PackageManager.PERMISSION_DENIED){ //백그라운드 위치 권한 확인
      //위치 권한 요청
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
    }


    final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    Timer timer = new Timer();

    TimerTask TT = new TimerTask() {
      @Override
      public void run() {

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
          ActivityCompat.requestPermissions( DetectorActivity.this, new String[] {
                  android.Manifest.permission.ACCESS_FINE_LOCATION}, 0 );
        }
        else{
          // 가장최근 위치정보 가져오기
          Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
          if(location != null) {
            String provider = location.getProvider();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            double altitude = location.getAltitude();
            cur_lat = location.getLatitude();
            cur_lon = location.getLongitude();
            Log.e("lat", String.valueOf(cur_lat));

//            txtResult.setText("위치정보 : " + provider + "\n" +
//                    "위도 : " + longitude + "\n" +
//                    "경도 : " + latitude + "\n" +
//                    "고도  : " + altitude);
          }


        }
      }
    };
// 위치정보를 원하는 시간, 거리마다 갱신해준다.
    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
            1000,
            1,
            (android.location.LocationListener) gpsLocationListener);
    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
            1000,
            1,
            (android.location.LocationListener) gpsLocationListener);
    timer.schedule(TT, 0, 1000);








    //센서 등록
    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    accelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    accLis = new AccListener();

    if (db != null) {
      db.close();
      db = null;
    }
    initDB();


  }



  private void saveImageToGallery(ArrayList bitmaps) {
    //
    File appDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES) + File.separator + "Tensorflow");
    for (int i = 0; i < bitmaps.size(); i++) {
      if (!appDir.exists()) {
        appDir.mkdir();
      }
      String fileName = System.currentTimeMillis() + ".png";
      File file = new File(appDir, fileName);
      Log.e("test_sign", "     localFile = " + appDir.getAbsolutePath() + fileName);
      FileOutputStream out = null;
      try {
        out = new FileOutputStream(file);
        Bitmap bitmap = (Bitmap) bitmaps.get(i);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
      } catch (FileNotFoundException e) {
        //new Toast("");
        e.printStackTrace();
      } catch (IOException e) {
        //new Toast("       ！");
        e.printStackTrace();
      } finally {
        if (out != null) {
          try {
            out.close();
            //
            //bitmaps.get(i).recycle();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      //
      try {
        MediaStore.Images.Media.insertImage(this.getContentResolver(),
                file.getAbsolutePath(), fileName, null);
      } catch (FileNotFoundException e) {
        //showToast("       ！");
        e.printStackTrace();
      }
    }
    //showToast("        ！");
    //
    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
            Uri.fromFile(new File(appDir.getPath()))));
  }














  //센서동작
  @Override
  public void onResume() {
    super.onResume();
    sensorManager.registerListener(accLis, accelerSensor, SensorManager.SENSOR_DELAY_UI);
  }

  @Override
  public void onPause() {
    super.onPause();
    sensorManager.unregisterListener(accLis);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    saveImageToGallery(bit_arr);
    bit_arr.clear();
    sensorManager.unregisterListener(accLis);
  }

  //센서 리스너
  private class AccListener implements SensorEventListener {

    @Override
    public void onSensorChanged(SensorEvent event) {
      if (event.sensor == accelerSensor) {
        accX = event.values[0];
        accZ = event.values[2];

        //zeroback = 25
        if ((Math.pow(accX, 2) + Math.pow(accZ, 2) >= Math.pow(25, 2)) && (accX >= 0 && accZ >= 0)) {
          String TimeDate = getTime();
          Log.e("TimeDate", TimeDate);
          insertrecords(TimeDate, cur_lat, cur_lon, count);
          Log.e("COUNT", String.valueOf(count));
          count++;
        } else if ((Math.pow(accX, 2) + Math.pow(accZ, 2) >= Math.pow(25, 2)) && (accX <= 0 && accZ <= 0)) {
          String TimeDate = getTime();
          Log.e("TimeDate", TimeDate);
          insertrecords(TimeDate, cur_lat, cur_lon, count);
          Log.e("COUNT", String.valueOf(count));
          count++;
        }
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
  }

  private void initDB() {
    db = openOrCreateDatabase("TimeDate", Activity.MODE_PRIVATE, null);
    db.execSQL("drop table timedate");
    db.execSQL("create table if not exists timedate" + "("
            + " _id integer PRIMARY KEY autoincrement, "
            + " getTime String, "
            + " cur_lat double, "
            + " cur_lon double, "
            + " num integer);"
    );
    db.close();
  }

  private void insertrecords(String getTime, double cur_lat, double cur_lon, Integer num) {
    db = openOrCreateDatabase("TimeDate", Activity.MODE_PRIVATE, null);

    if (db != null) {
      String sql = "insert into timedate(getTime, cur_lat, cur_lon, num) values(?, ?, ?, ?)";
      Object[] params = {getTime, cur_lat, cur_lon, num};
      db.execSQL(sql, params);
    }
    db.close();
  }


  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
              TFLiteObjectDetectionAPIModel.create(
                      this,
                      TF_OD_API_MODEL_FILE,
                      TF_OD_API_LABELS_FILE,
                      TF_OD_API_INPUT_SIZE,
                      TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing Detector!");
      Toast toast =
              Toast.makeText(
                      getApplicationContext(), "Detector could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
            ImageUtils.getTransformationMatrix(
                    previewWidth, previewHeight,
                    cropSize, cropSize,
                    sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
            new DrawCallback() {
              @Override
              public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
                if (isDebug()) {
                  tracker.drawDebug(canvas);
                }
              }
            });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {

      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
            new Runnable() {
              @Override
              public void run() {
            /*sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            accelerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            accLis = new DetectorActivity.AccListener();*/

                Thread server = new Thread(new startServer());
                server.start();

                LOGGER.i("Running detection on image " + currTimestamp);
                final long startTime = SystemClock.uptimeMillis();
                final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);
                final Paint paint = new Paint();
                canvas.setBitmap(cropCopyBitmap);
                paint.setColor(Color.RED);
                paint.setStyle(Style.STROKE);
                paint.setStrokeWidth(2.0f);

                float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                switch (MODE) {
                  case TF_OD_API:
                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    break;
                }

                final List<Detector.Recognition> mappedRecognitions =
                        new ArrayList<Detector.Recognition>();

                for (final Detector.Recognition result : results) {
                  final RectF location = result.getLocation();
                  if (location != null && result.getConfidence() >= minimumConfidence) {
                    canvas.drawRect(location, paint);
                    String ss;
                    ss = getTime();
                    canvas.drawText(ss, 10, 20, paint);



                    cropToFrameTransform.mapRect(location);

                    result.setLocation(location);
                    mappedRecognitions.add(result);

                    bit_arr.add(cropCopyBitmap);





                  }
                }

                tracker.trackResults(mappedRecognitions, currTimestamp);
                trackingOverlay.postInvalidate();

                computingDetection = false;

                runOnUiThread(
                        new Runnable() {
                          @Override
                          public void run() {
//                            showFrameInfo(previewWidth + "x" + previewHeight);
//                            showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
//                            showInference(lastProcessingTimeMs + "ms");
                          }
                        });

              }
            });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(
            () -> {
              try {
                detector.setUseNNAPI(isChecked);
              } catch (UnsupportedOperationException e) {
                LOGGER.e(e, "Failed to set \"Use NNAPI\".");
                runOnUiThread(
                        () -> {
                          Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        });
              }
            });
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }


  public class startServer extends Thread {
    ServerSocket sock;

    @Override
    public void run() {
      try {
        sock = new ServerSocket(5698);
        while (true) {
          Socket socket = sock.accept();
          SocketServerProcessThread socketThread = new SocketServerProcessThread(socket);
          socketThread.run();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

  private class SocketServerProcessThread extends Thread {
    private Socket sock;
    DataInputStream instream;

    SocketServerProcessThread(Socket socket) {
      sock = socket;
    }

    @Override
    public void run() {

      try {
        instream = new DataInputStream(sock.getInputStream());
        byte[] in = new byte[1024];
        String TimeDate = getTime();
        Log.e("TimeDate", TimeDate);

        Log.e("lat", String.valueOf(cur_lat));
        Log.e("Lon", String.valueOf(cur_lon));















        insertrecords(TimeDate, cur_lat, cur_lon, count);
        Log.e("COUNT", String.valueOf(count));
        count++;

        /*SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        MTime = System.currentTimeMillis();
        MDate = new Date(MTime);
        timedate = mFormat.format(MDate);
        editText = timedate;
        Log.e("timedate",editText);
        Intent intent = new Intent(getBaseContext(), InfoActivity.class);
        intent.putExtra("timedate", editText);
        startActivity(intent);*/
        /*if(instream.read(in) != 0){

          Intent intent = new Intent(getBaseContext(), InfoActivity.class);
          intent.putExtra("timedate", editText);}*/
        /*server_handler.post(new Runnable() {
          @Override
          public void run() {
            txt6.setText("getTime()");
          }
        });*/
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }
  final LocationListener gpsLocationListener = new LocationListener() {
    public void onLocationChanged(Location location) {
      // 위치 리스너는 위치정보를 전달할 때 호출되므로 onLocationChanged()메소드 안에 위지청보를 처리를 작업을 구현 해야합니다.
      String provider = location.getProvider();  // 위치정보
      double longitude = location.getLongitude(); // 위도
      double latitude = location.getLatitude(); // 경도
      double altitude = location.getAltitude(); // 고도
      cur_lat = location.getLatitude();
      cur_lon = location.getLongitude();
      //txtResult.setText("위치정보 : " + provider + "\n" + "위도 : " + longitude + "\n" + "경도 : " + latitude + "\n" + "고도 : " + altitude);
    } public void onStatusChanged(String provider, int status, Bundle extras) {

    } public void onProviderEnabled(String provider) {

    } public void onProviderDisabled(String provider) {

    }
  };






  /*private class AccListener implements SensorEventListener {

    @Override
    public void onSensorChanged(SensorEvent event) {
      if (event.sensor == accelerSensor) {
        accX = event.values[0];
        accY = event.values[1];
        accZ = event.values[2];
        textView6 = findViewById(R.id.textView6);

        //zeroback = 25
        if ((Math.pow(accX, 2) + Math.pow(accX, 2) >= Math.pow(25, 2)) && (accX >= 0)) {
          //Safe_Acc.setText("Accident occur");
          textView6.setText(getTime());
        } else if ((Math.pow(accX, 2) + Math.pow(accX, 2) >= Math.pow(25, 2)) && (accX <= 0)) {
          //Safe_Acc.setText("Accident occur");
          textView6.setText(getTime());
        }
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
  }

  @Override
  public void onResume() {
    super.onResume();
    sensorManager.registerListener(accLis, accelerSensor, SensorManager.SENSOR_DELAY_NORMAL);
  }

  @Override
  public void onPause() {
    super.onPause();
    sensorManager.unregisterListener(accLis);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    sensorManager.unregisterListener(accLis);
  }



  public class startServer extends Thread {
    ServerSocket sock;

    @Override
    public void run() {
      try {
        sock = new ServerSocket(5698);
        while (true) {
          Socket socket = sock.accept();
          SocketServerProcessThread socketThread = new SocketServerProcessThread(socket);
          socketThread.run();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

  private class SocketServerProcessThread extends Thread {
    private Socket sock;
    DataInputStream instream;

    SocketServerProcessThread(Socket socket) {
      sock = socket;
    }

    @Override
    public void run() {
      try {
        instream = new DataInputStream(sock.getInputStream());
        byte[] in = new byte[bufferszie];
        server_handler.post(new Runnable() {
          @Override
          public void run() {
            textView6.setText(getTime());
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private String getTime() {
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    MTime = System.currentTimeMillis();
    MDate = new Date(MTime);
    return mFormat.format(MDate);
  }*/

  public String getTime() {
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    MTime = System.currentTimeMillis();
    MDate = new Date(MTime);
    return mFormat.format(MDate);
    //timedate = mFormat.format(MDate);
  }
}
