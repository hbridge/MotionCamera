package com.example.motioncamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by aseem on 1/27/14.
 */

class PreviewVideo extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "Preview";

    SurfaceHolder mHolder;
    MediaRecorder recorder;
    boolean recording = false;
    File mediaFile = null;

    PreviewVideo(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        recorder = new MediaRecorder();
        initRecorder();
    }

    /** a simple algorithm to get the largest size available. For a more
     * robust version, see CameraPreview.java in the ApiDemos
     * sample app from Android. */
    private Size getBestSupportedSize(List<Size> sizes, int width, int height) {
        Size bestSize = sizes.get(0);
        int largestArea = bestSize.width * bestSize.height;
        for (Size s : sizes) {
            int area = s.width * s.height;
            if (area > largestArea) {
                bestSize = s;
                largestArea = area;
            }
        }
        return bestSize;
    }

    private void initRecorder() {
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        myDir.mkdirs();
        //create a filename
        String fileName = String.format(
                "video" + "%d.mp4", System.currentTimeMillis());

        mediaFile = new File(myDir, fileName);


        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);
        recorder.setOutputFile(mediaFile.toString());
        //recorder.setOutputFile(fileName);
        recorder.setMaxDuration(5000); // 5 seconds
        recorder.setMaxFileSize(50000000); // Approximately 50MB
        recorder.setOrientationHint(180);
    }

    public void prepareRecorder() {
        recorder.setPreviewDisplay(mHolder.getSurface());

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            //finish();

        } catch (IOException e) {
            e.printStackTrace();
            //finish();
        }
    }

    public void startRecording() {
        if (!recording) {
            recording = true;
            recorder.start();
        }
    }

    public void stopRecording(){
        if (recording){
            recorder.stop();
            recording = false;

            initRecorder();
            prepareRecorder();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        prepareRecorder();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        //finish();
    }
}