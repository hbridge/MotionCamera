package com.example.motioncamera;

import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;


import java.io.FileOutputStream;
import java.io.File;

public class MainActivity extends ActionBarActivity implements SensorEventListener {
    private static final String TAG = "MotionCamera";

    protected TextView shotCounterTextView;
    protected Preview preview;
    protected PreviewVideo previewVideo;
    protected Button calibrateClosedButton;
    protected Button calibrateOpenButton;

    private SensorManager sensorMan;
    private Sensor accelerometer;
    private float[] mGravityCurrent;
    private float[] mGravityLast;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    protected Handler handler = new Handler();
    protected MotionEndTask motionEndTask;

    private boolean USE_CAMERA = true;
    private boolean USE_VIDEO = false;
    static boolean photoInProgress;
    protected int shotCount;
    protected Handler vHandler = new Handler();
    protected VideoEndTask videoEndTask;
    public static final String EXTRA_PHOTO_FILENAME = "MotionCamera.filename";

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
        this.calibrateClosedButton = (Button) findViewById(R.id.buttonCalClosed);
        this.calibrateOpenButton = (Button) findViewById(R.id.buttonCalOpen);

        if (USE_CAMERA) {
            preview = new Preview(this);
            ((FrameLayout) findViewById(R.id.preview)).addView(preview);
        }
        else if (USE_VIDEO){
            previewVideo = new PreviewVideo(this);
            ((FrameLayout) findViewById(R.id.preview)).addView(previewVideo);
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
        }
    };

    /** Handles data for jpeg picture */
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            String root = Environment.getExternalStorageDirectory().toString();
            File myDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            myDir.mkdirs();
            //create a filename
            String fileName = String.format(
                    "%d.jpg", System.currentTimeMillis());
            // save the jpeg data to disk
            FileOutputStream os = null;
            File file = null;
            boolean success = true;
            try {

                file = new File (myDir, fileName);
                os = new FileOutputStream(file);
                os.write(data);
                os.flush();
                Log.e(TAG, "Wrote data");
            } catch (Exception e) {
                Log.e(TAG, "Error writing to file " + fileName, e);
                success = false;
            } finally {
                Log.e(TAG, "Inside finally tag");
                try {
                    if (os != null)
                        os.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing file " + fileName, e);
                    success = false;
                }
            }

            if (success) {
                // set the photo filename on the result intent
                if (success) {
                    Log.i(TAG, "JPEG saved at " + fileName);
                    addFileToGallery(file.toString());
                }
            }



            Log.d(TAG, "onPictureTaken - jpeg");
            photoInProgress = false;
            if (preview != null && preview.camera != null) {
                preview.camera.startPreview();
            }
        }
    };

    public void addFileToGallery(String file){
        MediaScannerConnection.scanFile(MainActivity.this,
                new String[]{file}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.e("ExternalStorage", "Scanned " + path + ":");
                        Log.e("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_UI);
        if (USE_CAMERA) {
            preview.camera = Camera.open(0);
        }
        else if (USE_VIDEO) {
            //previewVideo.prepareRecorder();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorMan.unregisterListener(this);
        if (preview != null && preview.camera != null) {
            preview.camera.release();
            preview.camera = null;
            System.err.println("OnPause - camera released");
        }


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravityLast = mGravityCurrent;
            mGravityCurrent = event.values.clone();
            // Shake detection
            float x = mGravityCurrent[0];
            float y = mGravityCurrent[1];
            float z = mGravityCurrent[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = FloatMath.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            // Make this higher or lower according to how much
            // motion you want to detect
            if(mAccel > 1){
                if (motionEndTask != null) handler.removeCallbacks(motionEndTask);
                motionEndTask = new MotionEndTask();
                handler.postDelayed(motionEndTask, 1000);

                if (USE_VIDEO) {
                    previewVideo.startRecording();
                    Log.e(TAG, "Started recording");
                    if (videoEndTask != null) {
                        vHandler.removeCallbacks(videoEndTask);
                    }
                    videoEndTask = new VideoEndTask();
                    vHandler.postDelayed(videoEndTask, 3000);

                }
            }
        }

    }

    protected class MotionEndTask implements Runnable {
        public void run() {
            shotCounterTextView.setText(Integer.toString(++shotCount));
            if (USE_CAMERA) {
                if (!photoInProgress && preview.camera != null) {
                    photoInProgress = true;
                    preview.camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                }
            }
        }
    }

    protected class VideoEndTask implements Runnable {
        public void run() {
            if (USE_VIDEO) {
                if (previewVideo.recording) {
                    previewVideo.stopRecording();
                    addFileToGallery(previewVideo.mediaFile.toString());
                    Log.e(TAG, "Stopped Recording");
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // required method
    }



    public void calibrateCloseClicked(View view) {
        System.out.println("Close calibrated at: x:" + mGravityCurrent[0] + " y:" + mGravityCurrent[1] + " z:" + mGravityCurrent[2]);

    }

    public void calibrateOpenClicked(View view) {
        System.out.println("Open calibrated at: x:" + mGravityCurrent[0] + " y:" + mGravityCurrent[1] + " z:" + mGravityCurrent[2]);
    }


}
