package com.mobileapp.rutgers.cameraman;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import android.content.Intent;

public class CameraActivity extends Activity {

    public final static int LABEL_START_CAMERA = 0;
    private static final int RESULT_REQUEST_CODE = 1;
    TextureView mTextureView;
    ImageButton BtnCaptureImage;
    Button BtnChangeCamera;
    CameraManager manager;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCameraPreviewBuilder;
    private CameraCaptureSession mCameraPreviewSession;
    private CameraCharacteristics characteristics;
    private CaptureRequest mCaptureRequest;
    String cameraId;
    String mPresentCameraId = null;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private TotalCaptureResult mCaptureResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if(savedInstanceState!=null)
            mPresentCameraId = savedInstanceState.getString("CAMERAID");

        //Initilise UI elements
        mTextureView = (TextureView) findViewById(R.id.previewTexture);
        mTextureView.setSurfaceTextureListener(thisSurfaceTextureListener);

        BtnCaptureImage = (ImageButton) findViewById(R.id.captureImage); // call capture image when clicked on the this particular button
//        BtnViewGallery = (Button) findViewById(R.id.viewPictureGallery);

    }

    TextureView.SurfaceTextureListener thisSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e("EMCAMERA", " Inside onSurfaceTextureAvailable, width=" + width + ",height=" + height);

            if (isCameraAvailable()) {
                if(mPresentCameraId==null)
                    openCamera();
                else
                    openCamera(mPresentCameraId);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.e("EMCAMERA", "Inside onSurfaceTextureChanged, width=" + width + ",height=" + height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//            Log.e("EMCAMERA", "Inside onSurfaceTextureUpdated Method");
        }
    };



    private boolean isCameraAvailable() {
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG)
                    .show();
            return false;
        }
        return true;
    }

    private void openCamera() {
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        openCamera(cameraId);
    }

    private void openCamera(String cameraId) {
        Log.e("CAMERAMAN", "Inside openCamera - begin");
        try {
            characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            assert map != null;
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];


            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e("CAMERAMAN", "NO PERMISSION");

                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{permission.CAMERA}, RESULT_REQUEST_CODE);
                return;
            }
            manager.openCamera(cameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e("CAMERAMAN", "Inside openCamera - end of method");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RESULT_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera(mPresentCameraId);

                } else {
                    finish();
                }
                return;
            }
        }
    }

    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //Launched when the cameradevice is opened
            //Start preview when successfully opened
            Log.e("CAMERAMAN", "Inside On Opened");
            cameraDevice = camera;
            startCapturePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            //Launched when the camera device is disconnected
            Log.e("CAMERAMAN", "Camera disconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            //Launched when there is an error in opening the Camera device
            //close camera and end activity
            Log.e("CAMERAMAN", "Camera OnError");
        }
    };

    Surface surface;
    private void startCapturePreview() {
        if(cameraDevice == null || !mTextureView.isAvailable() ||mPreviewSize == null) {
            Log.e("CAMERAMAN", "startPreview failed - ");
            return;
        }

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if(null == texture) {
            Log.e("CAMERAMAN","texture is null");
            return;
        }

        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        surface = new Surface(texture);

        try {
            mCameraPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mCameraPreviewBuilder.addTarget(surface);

        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                mCameraPreviewSession = session;
                updatePreview();
            }
            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                Toast.makeText(getApplicationContext(), "onConfigureFailed", Toast.LENGTH_LONG).show();
            }
        }, null);
        } catch (CameraAccessException e) {

            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(cameraDevice == null) {
            Log.e("CAMERAMAN", "updatePreview error, return");
        }

        mCameraPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            mCameraPreviewSession.setRepeatingRequest(mCameraPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    File file;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    ImageReader reader;
    byte[] bytes;
    //Called when the capture image button is clicked
    public void captureImage(View view) {
        Log.e("CAMERAMAN", "Inside Capture Image");
        if(cameraDevice==null) {
            Log.e("EMCAMERA", "mCameraDevice is null, return");
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());

            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            File storageDirectory=new File(Environment.getExternalStorageDirectory() + "/MobileAppGallery/");
            if (!storageDirectory.exists()) {
                storageDirectory.mkdir();
            }

            Random randomNo = new Random();
            final String mImageFileName  = (new SimpleDateFormat("yyyy_MM_dd_HHmmss")).format(new Date())+randomNo.nextInt(100) + ".jpg";

            file = new File(storageDirectory,mImageFileName);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    FileOutputStream output = null;
                    try {
                        verifyStoragePermissions(CameraActivity.this);
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                        bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                       //                        save(bytes);
                        output = new FileOutputStream(file);
//                            DngCreator dngImageCreator=new DngCreator(characteristics,mCaptureResult);
//                            dngImageCreator.writeImage(output, image);
//                            dngImageCreator.close();
                        output.write(bytes);
                        output.flush();
                        if (null != output) {
                            output.close();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

            };

            HandlerThread thread = new HandlerThread("CameraPicture");
            thread.start();
            final Handler captureListenerHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(readerListener, captureListenerHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request, TotalCaptureResult result) {

                    super.onCaptureCompleted(session, request, result);
                    mCaptureResult = result;
                    Toast.makeText(getApplicationContext(), "Saved: "+ file, Toast.LENGTH_SHORT).show();
                    //Update the surface view in the UI thread - in Main activity

                    Intent intent = new Intent();
                    intent.putExtra("IMAGE", file.getAbsolutePath());
                    setResult(RESULT_OK, intent);
                    finish();
//                    startCapturePreview();
                }
            };


            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, captureListenerHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, captureListenerHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private File saveImage(CameraCharacteristics cameraCharacteristics,TotalCaptureResult totalCaptureResult,ImageReader imageReader) throws IOException{
//        File storageDirectory=new File(Environment.getExternalStorageDirectory() + "/MobileAppGallery/");
//        if (!storageDirectory.exists()) {
//            storageDirectory.mkdir();
//        }
//
//        Random randomNo = new Random();
//        String mImageFileName  = (new SimpleDateFormat("YY_MM_dd_HHmmss")).format(new Date())+randomNo.nextInt(100) + ".dng";
//
//        File imageFile = new File(storageDirectory,mImageFileName);
//
        Log.e("CAMERAMAN",file.toString());

        try {
            FileOutputStream imageFileStream=new FileOutputStream(file);
            DngCreator dngImageCreator=new DngCreator(cameraCharacteristics,totalCaptureResult);
            dngImageCreator.writeImage(imageFileStream,imageReader.acquireNextImage());
            dngImageCreator.close();
            imageFileStream.flush();
            imageFileStream.close();
        }
        catch (  FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public void onPause()
    {
        closeCamera();
         super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putString("CAMERAID", mPresentCameraId);

    }

    private void closeCamera() {
        if (null != mCameraPreviewSession) {
            mCameraPreviewSession.close();
            mCameraPreviewSession = null;
        }
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != reader) {
            reader.close();
            reader = null;
        }
    }


    public void ChangeCameras(View view) throws CameraAccessException {
        cameraId = mPresentCameraId;
        //switch between front and back cameras
        String[] CameraIDArray = new String[2];
        CameraIDArray = manager.getCameraIdList();

        Log.e("CAMERAMAN", "The present camera id is: "+ cameraId + "The ids are "+ CameraIDArray[0] +";;" + CameraIDArray[1]);
        if(cameraId !=null) {
            //check which camera is active now.
            if (cameraId == CameraIDArray[0]) {
                cameraId = CameraIDArray[1];
            } else if (cameraId == CameraIDArray[1])
                cameraId = CameraIDArray[0];


            // startCapturePreview();
        }
        else
            cameraId = manager.getCameraIdList()[0];

        mPresentCameraId = cameraId;
    }
}

