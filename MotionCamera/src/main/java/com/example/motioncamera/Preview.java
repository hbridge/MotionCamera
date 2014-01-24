package com.example.motioncamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by aseem on 1/22/14.
 */

class Preview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "Preview";

    SurfaceHolder mHolder;
    public Camera camera;

    Preview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        //camera = Camera.open();
        try {
            if (camera != null) {
                camera.setPreviewDisplay(holder);

               /* camera.setPreviewCallback(new Camera.PreviewCallback() {

                    public void onPreviewFrame(byte[] data, Camera arg1) {
                        FileOutputStream outStream = null;
                        try {
                            outStream = new FileOutputStream(String.format(
                                    "%d.jpg", System.currentTimeMillis()));
                            outStream.write(data);
                            outStream.close();
                            Log.d(TAG, "onPreviewFrame - wrote bytes: "
                                    + data.length);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                        }
                        Preview.this.invalidate();
                    }
                });*/
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error setting up preview display", e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        if (camera == null ) {
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        Size size = getBestSupportedSize(parameters.getSupportedPreviewSizes(), w, h);
        parameters.setPreviewSize(size.width, size.height); // preview size
        //camera.setDisplayOrientation(180);
        //parameters.setRotation(180);
        camera.setParameters(parameters);

        try {
            camera.startPreview();
        } catch (Exception e) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Paint p = new Paint(Color.RED);
        Log.d(TAG, "draw");
        canvas.drawText("PREVIEW", canvas.getWidth() / 2,
                canvas.getHeight() / 2, p);
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
}