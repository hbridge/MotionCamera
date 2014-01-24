package com.example.motioncamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends ActionBarActivity implements SensorEventListener {

    // Start with some variables
    private static final String TAG = "MotionCamera";
    public static final String EXTRA_PHOTO_FILENAME = "MotionCamera.filename";
    private SensorManager sensorMan;
    private Sensor accelerometer;
    Preview preview;
    static boolean photoInProgress;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    private boolean USE_CAMERA = true;

    protected Handler handler = new Handler();
    protected MotionEndTask motionEndTask;
    protected int shotCount;
    protected TextView shotCounterTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }*/


        sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        photoInProgress = false;
        this.shotCounterTextView = (TextView) findViewById(R.id.shotCounter);

        if (USE_CAMERA) {
            preview = new Preview(this);
            ((FrameLayout) findViewById(R.id.preview)).addView(preview);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }


    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.d(TAG, "onShutter'd");

        }
    };

    /** Handles data for raw picture */
    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken - raw");
            photoInProgress = false;
            //preview.camera.startPreview();
        }
    };

    /** Handles data for jpeg picture */
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            //create a filename
            String filename = String.format(
                    "%d.jpg", System.currentTimeMillis());
            // save the jpeg data to disk
            FileOutputStream os = null;
            boolean success = true;
            try {
                os = MainActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
                os.write(data);
            } catch (Exception e) {
                Log.e(TAG, "Error writing to file " + filename, e);
                success = false;
            } finally {
                try {
                    if (os != null)
                        os.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing file " + filename, e);
                    success = false;
                }
            }

            if (success) {
                // set the photo filename on the result intent
                if (success) {
                    Intent i = new Intent();
                    i.putExtra("", filename);
                    MainActivity.this.setResult(Activity.RESULT_OK, i);
                } else {
                    MainActivity.this.setResult(Activity.RESULT_CANCELED);
                }
            }



            /*
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(String.format(
                        "/temp/%d.jpg", System.currentTimeMillis()));
                outStream.write(data);
                outStream.close();
                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.err.println("Error thrown");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            */
            Log.d(TAG, "onPictureTaken - jpeg");
            photoInProgress = false;
            preview.camera.startPreview();

        }
    };

    @Override
    public void onResume() {
        super.onResume();
        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_UI);
        preview.camera = Camera.open(0);
        System.err.println("Camera opened");


    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorMan.unregisterListener(this);
        if (preview.camera != null) {
            preview.camera.release();
            preview.camera = null;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = FloatMath.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            // Make this higher or lower according to how much
            // motion you want to detect
            if(mAccel > 3){
                if (motionEndTask != null) handler.removeCallbacks(motionEndTask);
                motionEndTask = new MotionEndTask();
                handler.postDelayed(motionEndTask, 500);
            }
        }

    }

    protected class MotionEndTask implements Runnable {
        public void run() {
            shotCounterTextView.setText(Integer.toString(shotCount++));
            if (USE_CAMERA) {
                if (!photoInProgress && preview.camera != null) {
                    photoInProgress = true;
                    preview.camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required method
    }


}
