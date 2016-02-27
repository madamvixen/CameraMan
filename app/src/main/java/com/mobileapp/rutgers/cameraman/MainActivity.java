package com.mobileapp.rutgers.cameraman;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.provider.MediaStore.Files.FileColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements SensorEventListener{

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 2;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;


    ImageButton BtnCaptureImage;
    ImageButton BtnCaptureVideo;
//    Button BtnViewGallery;
    ImageView savedImageSurfaceView;
    Uri fileUri;
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    float[] LastPos = {0, 0, 0};
    float[] changePos = { 0, 0, 0};
    float vibThreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BtnCaptureImage = (ImageButton) findViewById(R.id.imageCaptureButton); // call capture image when clicked on the this particular button
        BtnCaptureVideo = (ImageButton) findViewById(R.id.videoCaptureButton); //Call capture video when clicked on this particular button
        savedImageSurfaceView = (ImageView) findViewById(R.id.displayLastImageTaken); // Display the last image/Video captured using application

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //Ontriggering of movement in sensor the video capture should begin'
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        vibThreshold = (float) (mAccelerometerSensor.getMaximumRange()/(1.5));
    }

    public void startImageCapture(View view) {

        Log.e("CAMERAMAN", "Inside the captureImage Method");
        Intent intent = new Intent(this,CameraActivity.class);
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
//        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

    }

    public void captureVideo(View view) {
        Log.e("CAMERAMAN", "Inside the captureVideo Method");
        Intent intent = new Intent()
                .setAction(MediaStore.ACTION_VIDEO_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
        // start the video capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);

    }
    MediaRecorder mMediaRecorder;

    public void captureVideo()
    {
        Log.e("CAMERAMAN", "INSIDE CAPTURE VIDEO");
        mMediaRecorder = new MediaRecorder();
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

//        CamcorderProfile cpHigh = CamcorderProfile
//                .get(CamcorderProfile.QUALITY_HIGH);
//        mMediaRecorder.setProfile(cpHigh);
        mMediaRecorder.setOutputFile(getOutputMediaFileUri(MEDIA_TYPE_VIDEO).getPath());
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mMediaRecorder.setMaxDuration(10000); // 10 seconds
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaRecorder.start();
        mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {

                    mMediaRecorder.stop();
                    saveVideoFile();
                }
            }
        });
//        mMediaRecorder.setMaxFileSize(5000000); // Approximately 5 megabytes
    }

    private void saveVideoFile() {
        Toast.makeText(this, "Video Captured!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Image Captured; Saved", Toast.LENGTH_SHORT).show();
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = null;
                String filename = (String) extras.get("IMAGE");
//                Log.e("CAMERAMAN", "filename is "+filename);
                try {
                    assert filename != null;
                    FileInputStream is = new FileInputStream(new File(filename));
                    imageBitmap = BitmapFactory.decodeStream(is);
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                savedImageSurfaceView.setImageBitmap(imageBitmap);

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
                Toast.makeText(this, "Image Capture Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // Image capture failed, advise user
                Toast.makeText(this, "Image Capture Failed, Exiting app...", Toast.LENGTH_SHORT).show();
                finishActivity(CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        }
        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent
                Toast.makeText(this, "Video Captured; Saved to "+data.getData(), Toast.LENGTH_SHORT).show();
                //capture the thumbnail of the Video captured

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
                Toast.makeText(this, "Video Capture Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // video capture failed, advise user
                Toast.makeText(this, "Video Capture Failed, Exiting app...", Toast.LENGTH_SHORT).show();
                finishActivity(CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
            }
        }
    }
    /** Create a file Uri for saving an image or video */
    private Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        Log.e("CameraMan", "inside get output media file");

        String state = Environment.getExternalStorageState();
        File mediaStorageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e("CAMERAMAN"," STORAGE MOUNTED");
            mediaStorageDir = new File(Environment.getExternalStorageDirectory() + "/MobileAppGallery/");
            if (!mediaStorageDir.exists()) {
                mediaStorageDir.mkdir();
            }

        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if(type == MEDIA_TYPE_VIDEO) {
//            Log.e("CameraMan", "media type is video");
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            Log.e("CameraMan", "media type is null");
            return null;
        }
        return mediaFile;
    }

    @Override
    public void onResume()
    {
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        //Log.e("CAMERAMAN","inside the sensor changed method "+event.values[2]);
        changePos[2] = Math.abs(LastPos[2] - event.values[2]);
        if(changePos[2] >vibThreshold) {
            Toast.makeText(this, "ACCELEROMETER ABOVE THRESHOLD: LAUNCH VIDEO RECORDER", Toast.LENGTH_SHORT).show();
            //start the video capture
//            Log.e("CAMERAMAN","Capturing video.. Above threshold");
            captureVideo();
        }else if(changePos[2] < vibThreshold) {
            changePos[2] = 0;
        }
        LastPos[0] = event.values[0];
        LastPos[1] = event.values[1];
        LastPos[2] = event.values[2];

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }

}
