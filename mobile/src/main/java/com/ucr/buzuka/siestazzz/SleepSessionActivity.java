package com.ucr.buzuka.siestazzz;

import static java.lang.Math.abs;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.ucr.buzuka.siestazzz.database.AppDatabase;
import com.ucr.buzuka.siestazzz.model.Journal;
import com.ucr.buzuka.siestazzz.model.JournalEntry;
import com.ucr.buzuka.siestazzz.model.SensorData;
import com.ucr.buzuka.siestazzz.model.SensorReadout;
import com.ucr.buzuka.siestazzz.model.Session;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class SleepSessionActivity extends AppCompatActivity implements SensorEventListener {

  private static final int M_SENSOR_DELAY = 0;
  private static int STORAGE_LIMITER = 0;
  private static final String TAG = "SleepSessionActivity";
  //private Queue<Float> sensorLog;
  public ArrayList<SensorData> sensorDataList = new ArrayList<>();
  //sensor manager and accelerometer
  private SensorManager sensorManager;
  private Sensor sensorAccelerometer;
  //local variable for sensor data
  private long lastUpdate = 0;
  private float last_x, last_y, last_z; //last position
  private float SENSOR_THRESHOLD = 0.02f;
  private float MAX_SPEED = Float.NEGATIVE_INFINITY;
  long curTime = 0;
  long diffTime = 0;
  float speed = 0;
  String sessionID;
  public static final String SENSOR_ACCEL = "accelerometer_toggle";
  public static final String SENSOR_AUDIO = "audio_toggle";

  private static final String LOG_TAG = "AudioRecordTest";
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
  private static String mFileName = null;
  private File myDir;
  private MediaRecorder mRecorder = null;
  private MediaPlayer mPlayer = null;
  private Date mDate = new Date();
  private String fDate;
  private int volume = 0;

  private static boolean toggle_accel;
  private static boolean toggle_audio;
  private String volumeValues = "";
  private String volumeTimes = "";
  private String speedValues = "";
  private String speedTimes = "";
  private String journalPath = "";

  private void stopRecording() {
    mRecorder.stop();
    mRecorder.release();
    mRecorder = null;
  }

  private void startRecording() {
    mFileName = getExternalCacheDir().getAbsolutePath();
    long yourmilliseconds = System.currentTimeMillis();
    SimpleDateFormat sdf = new SimpleDateFormat("MMMddyyyHHmm");
    Date resultdate = new Date(yourmilliseconds);
    mFileName += "/";
    mFileName += fDate;
    journalPath = mFileName;
    mFileName += "/";
    mFileName += "recording.3gp";

    mRecorder = new MediaRecorder();
    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    mRecorder.setOutputFile(mFileName);
    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

    try {
      mRecorder.prepare();
    } catch (IOException e) {
      Log.e(LOG_TAG, "prepare() failed");
    }

    mRecorder.start();
    Log.d("RECORD", "Start Recording is being executed");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sleep_session);

    SharedPreferences prefs = getSharedPreferences(MainActivity.GLOBAL_PREFS, MODE_PRIVATE);
    toggle_accel = Boolean.parseBoolean(prefs.getString(String.valueOf(SENSOR_ACCEL), ""));
    toggle_audio = Boolean.parseBoolean(prefs.getString(String.valueOf(SENSOR_AUDIO), ""));
    Log.i(TAG, "accel: " + String.valueOf(toggle_accel));
    Log.i(TAG, "audio: " + String.valueOf(toggle_audio));

    DateFormat df = new SimpleDateFormat("M_d_h_m");
    fDate = df.format(mDate);

    long yourmilliseconds = System.currentTimeMillis();
    SimpleDateFormat sdf = new SimpleDateFormat("MMMddyyyHHmm");
    Date resultdate = new Date(yourmilliseconds);

    myDir = new File(getExternalCacheDir().getAbsolutePath(), fDate);
    if (!myDir.exists()) {
      myDir.mkdir();
    }
    Log.d("RECORD", myDir.getPath());
    myDir = new File(getExternalCacheDir().getAbsolutePath(), fDate);

    if (toggle_accel) {
      sensorManager = (SensorManager) getSystemService(
          Context.SENSOR_SERVICE); // get an instance of system sensor
      assert sensorManager != null;
      sensorAccelerometer = sensorManager
          .getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // get accelerometer
      sensorManager.registerListener(this, sensorAccelerometer, M_SENSOR_DELAY);
    }

//        create a new session id
    sessionID = UUID.randomUUID().toString();
    //sensorReadoutList.add(sensorReadout);

    // Logs a list of readouts
    SensorReadout sensorReadout = new SensorReadout(sessionID, System.currentTimeMillis(), 0, 0);
    Session session = new Session(sessionID, myDir.getPath());
    Log.d("DATABASE", "Session:" + session);
    Log.d("RECORD", "OnCreate Completed");
  }

  /**
   * Put sensor to sleep when app not in use, will need to comment out in production. will need to
   * make sensor polling into a service
   */
  protected void onPause() {
    super.onPause();
    if (toggle_accel) {
      sensorManager.unregisterListener(this);
    }
  }

  /**
   * Re-register sensor for app, will not need this in production
   */
  protected void onResume() {
    super.onResume();
    if (toggle_accel) {
      sensorManager.registerListener(this, sensorAccelerometer, M_SENSOR_DELAY);
    }
    if (toggle_audio) {
      startRecording();
    }
  }

  protected void onStop() {
    super.onStop();
    if (toggle_audio) {
      stopRecording();

      try {
        String pathTemp =
            getExternalCacheDir().getAbsolutePath() + "/" + fDate + "/volumeValues.txt";
        File vVals = new File(pathTemp);
        vVals.createNewFile();
        FileWriter vWrite = new FileWriter(vVals);
        vWrite.write(volumeValues);
        vWrite.flush();
        vWrite.close();
        pathTemp = getExternalCacheDir().getAbsolutePath() + "/" + fDate + "/volumeTimes.txt";
        vVals = new File(pathTemp);
        vVals.createNewFile();
        vWrite = new FileWriter(vVals);
        vWrite.write(volumeTimes);
        vWrite.flush();
        vWrite.close();
      } catch (IOException e) {
        Log.d("RECORD", "OOPS");
      }
    }
    if (toggle_accel) {
      sensorManager.unregisterListener(this);
      try {
        String pathTemp =
            getExternalCacheDir().getAbsolutePath() + "/" + fDate + "/speedValues.txt";
        File sVals = new File(pathTemp);
        sVals.createNewFile();
        FileWriter sWrite = new FileWriter(sVals);
        sWrite.write(speedValues);
        sWrite.flush();
        sWrite.close();
        pathTemp = getExternalCacheDir().getAbsolutePath() + "/" + fDate + "/speedTimes.txt";
        sVals = new File(pathTemp);
        sVals.createNewFile();
        sWrite = new FileWriter(sVals);
        sWrite.write(speedTimes);
        sWrite.flush();
        sWrite.close();
      } catch (IOException e) {
        Log.d("RECORD", "OOPS");
      }
    }
    // TODO Create JournalEntryObject
    JournalEntry journalEntry = new JournalEntry();
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (toggle_audio) {
      volume = mRecorder.getMaxAmplitude();
    }
    curTime = System.currentTimeMillis();

    if (toggle_accel) {
      Sensor sensor = sensorEvent.sensor;

      if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        if (STORAGE_LIMITER == 0) {
          STORAGE_LIMITER = 150;
          diffTime = (curTime - lastUpdate) / 100;
          lastUpdate = curTime;
          speed = abs(x + y + z - last_x - last_y - last_z) / diffTime;
        }
        last_x = x;
        last_y = y;
        last_z = z;
        STORAGE_LIMITER--;
      }
    }

    if (!(speed > SENSOR_THRESHOLD)) {
      speed = 0;
    }
    if (volume > 5000) {
      volume = 0;
    }
    speedValues += Float.toString(speed);
    speedValues += " ";
    speedTimes += curTime;
    speedTimes += " ";

    volumeValues += Float.toString(volume);
    volumeValues += " ";
    volumeTimes += curTime;
    volumeTimes += " ";
    Log.d("CONVERT", "Speed:" + speed);

    if (speed != Float.POSITIVE_INFINITY) {
      MAX_SPEED = speed;
    }
//        }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }

  //  onClick listener for going back to MainActivity to end session
  public void GoHome(View view) {

    Date wakeTime = new Date();

    JournalEntry journalEntry = new JournalEntry();
    journalEntry.setSleepDateAndTime(mDate);
    journalEntry.setWakeDateAndTime(wakeTime);

    if (toggle_audio) {
      journalEntry.setSoundDataPath(journalPath);
      Log.d("AUDIO", "Path=" + journalPath);
    } else {
      journalEntry.setSoundDataPath(null);
    }

    Journal.get(this).addJournalEntry(journalEntry);

    finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (toggle_accel) {
      sensorManager.unregisterListener(this);
      AppDatabase.destroyInstance();
    }
  }
}
