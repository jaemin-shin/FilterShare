package team16.filtershare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by shinjaemin on 2016. 7. 6..
 */
public class MainActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    public static int cameraId=-1;
    public static final int MEDIA_TYPE_IMAGE = 1;
    //This app doesn't use VIDEO but I left it just in case.
    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final int GALLERY_INTENT = 0;
    private static  final int FOCUS_AREA_SIZE= 300;

    private static int isAf =0;



    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getTmpOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d("PictureCallback", "Error creating media file");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile, false);
                //fos.write(data);
                //fos.close();

                fos = new FileOutputStream(pictureFile, false);

                Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);
                //Log.d("absolute", pictureFile.getAbsolutePath());
                //Log.d("tostring", pictureFile.toString());
                realImage= rotate(realImage, 90);
                Uri.fromFile(pictureFile).getPath();
                Log.d("Uri", Uri.fromFile(pictureFile).getPath());
                realImage=rotate_image(Uri.fromFile(pictureFile).getPath(),realImage);
                //realImage= rotate(realImage, 270);

                realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();



            } catch (FileNotFoundException e) {
                Log.d("PictureCallback", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("PictureCallback", "Error accessing file: " + e.getMessage());
            }

            GlobalVariables mApp = ((GlobalVariables)getApplicationContext());
            mApp.set_picture_path(pictureFile.getAbsolutePath());
            /*
            Intent intent = new Intent(MainActivity.this, PhotoConfirmActivity.class);
            startActivity(intent);
            */
            finish();

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an instance of Camera
        if(checkCameraHardware(this)) {
            Log.d("Ok", "It has camera");
            mCamera = getCameraInstance();


            //set camera to continually auto-focus
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);

            if (mCamera==null)
                Log.e("Fail", "no Camera Instance");
        }
        else
            Log.e("NO", "checkCameraHardware failed");

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        final FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        final AutofocusRect mAutofocusRect = (AutofocusRect) findViewById(R.id.af_rect);
        mAutofocusRect.setParentInfo(mPreview.getWidth(),mPreview.getHeight());

        preview. setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mCamera != null) {
                    isAf=1;
                    Camera camera = mCamera;
                    camera.cancelAutoFocus();
                    Rect focusRect = calculateFocusArea(event.getX(), event.getY());
                    Log.d("Rect", focusRect.toString());
                    Log.d("Rect_ctr", "x: " + focusRect.centerX() +"y:"+ focusRect.centerY());
                    Log.d("event_ctr", "x: " + event.getX() +"y:"+ event.getX());

                    mAutofocusRect.setLocation(event.getX(), event.getY());
                    mAutofocusRect.showStart();


                    Camera.Parameters parameters = camera.getParameters();
                    if (parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    if (parameters.getMaxNumFocusAreas() > 0) {
                        List<Camera.Area> mylist = new ArrayList<Camera.Area>();
                        mylist.add(new Camera.Area(focusRect, 1000));
                        parameters.setFocusAreas(mylist);
                    }

                    try {
                        camera.cancelAutoFocus();
                        camera.setParameters(parameters);
                        camera.startPreview();

                        camera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                                    Camera.Parameters parameters = camera.getParameters();
                                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                    if (parameters.getMaxNumFocusAreas() > 0) {
                                        parameters.setFocusAreas(null);
                                    }
                                    camera.setParameters(parameters);
                                    mAutofocusRect.clear();
                                    isAf=0;
                                    camera.startPreview();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });

        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        //mCamera.takePicture(null, null, mPicture);
                        Log.d("isAf", ""+isAf);
                        if(isAf==1) {
                            isAf=0;
                            mAutofocusRect.clear();
                            mCamera.takePicture(null, null, mPicture);

                            return;
                        }

                        mAutofocusRect.setLocation(mPreview.getWidth()/2, mPreview.getHeight()/2);
                        mAutofocusRect.showStart();
                        mCamera.cancelAutoFocus();
                        mCamera.startPreview();
                        mCamera.autoFocus (new Camera.AutoFocusCallback() {
                            public void onAutoFocus(boolean success, Camera camera) {
                                mAutofocusRect.clear();
                                mCamera.takePicture(null, null, mPicture);

                            }

                        });

                    }
                }
        );

        Button galleryButton = (Button) findViewById(R.id.button_gallery);
        galleryButton.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, GALLERY_INTENT);

                    }
                }
        );
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_INTENT) {
                // Get the filepath and display on imageview.
                String filepath = getGalleryImagePath(data);
                // Check if the specified image exists.
                if (!new File(filepath).exists()) {
                    Toast.makeText(this, "Image does not exist.", Toast.LENGTH_SHORT).show();
                }
                else {
                    GlobalVariables mApp = ((GlobalVariables)getApplicationContext());
                    Log.d("filepath_gal", filepath);
                    mApp.set_picture_path(filepath);
                    /*
                    Intent intent = new Intent(MainActivity.this, PhotoConfirmActivity.class);
                    startActivity(intent);
                    */
                    finish();


                }
            }
        }
    }

    public String getGalleryImagePath(Intent data) {
        Uri imgUri = data.getData();
        String filePath = "";
        if (data.getType() == null) {
            // For getting images from default gallery app.
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(imgUri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } else if (data.getType().equals("image/jpeg") || data.getType().equals("image/png")) {
            // For getting images from dropbox or any other gallery apps.
            filePath = imgUri.getPath();
        }
        return filePath;
    }



    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
    }
    @Override
    protected void onStop(){
        super.onStop();
        releaseCamera();
    }


    @Override
    protected void onResume() {
        super.onResume();  // Always call the superclass method first
        Log.d("Resume", "Camera resumes");

        if(checkCameraHardware(this)) {
            Log.d("Ok", "It has camera");
            mCamera = getCameraInstance();
            if (mCamera==null)
                Log.e("Fail", "no Camera Instance");
        }
        else
            Log.e("NO", "checkCameraHardware failed");

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

    }


    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            Log.d("CameraNum", "Num: "+ Camera.getNumberOfCameras());
            cameraId=findFrontFacingCamera();
            c = Camera.open(cameraId); // attempt to get a Camera instance

        } catch (Exception e) {
            Log.e("NoCamera", "Camera " +cameraId+ " is no available");
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    private void releaseCamera(){
        if (mCamera != null){
            Log.d("Camera Release", "Camera is released");
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();        // release the camera for other applications
            mCamera = null;

        }
    }


    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private static File getTmpOutputMediaFile(int type){
        File mediaStorageFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaStorageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "latest_picture.jpg");
        }
        else{
            mediaStorageFile=null;
        }
        return mediaStorageFile;
    }


    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public static Bitmap rotate_image(String pathname, Bitmap realImage) throws IOException {
        ExifInterface exif=new ExifInterface(pathname);
        Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));



        // refer http://sylvana.net/jpegcrop/exif_orientation.html to understand to code below
        if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")){
            realImage= rotate(realImage, 90);
        } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")){
            realImage= rotate(realImage, 270);
        } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")){
            realImage= rotate(realImage, 180);
        } /*else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")){
            realImage= rotate(realImage, 90);
        } */
        return realImage;

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        releaseCamera();
        Intent intent = new Intent(MainActivity.this, PhotoConfirmActivity.class);
        startActivity(intent);


    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / mPreview.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / mPreview.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }

    private static int findFrontFacingCamera() {

        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;

                break;
            }
        }
        return cameraId;
    }








}