package com.waitertwelve.politipic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhotoIntentActivity extends Activity {

    private static final int ACTION_TAKE_PHOTO_B = 1;

    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private ImageView mImageView;
    private Bitmap mImageBitmap;

    public static String mCurrentPhotoPath;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

    /* Photo album for this application */
    private String getAlbumName() {
        return getString(R.string.album_name);
    }


    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        Log.d("createImageFile", "Temp file should be created");
        return imageF;
    }

    private File setUpPhotoFile() throws IOException {

        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
		
		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		/* Associate the Bitmap to the ImageView */
        mImageView.setImageBitmap(bitmap);
        mImageView.setVisibility(View.VISIBLE);
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchTakePictureIntent(int actionCode) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        switch (actionCode) {
            case ACTION_TAKE_PHOTO_B:
                File f = null;

                try {
                    f = setUpPhotoFile();
                    mCurrentPhotoPath = f.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                } catch (IOException e) {
                    e.printStackTrace();
                    f = null;
                    mCurrentPhotoPath = null;
                }
                break;

            default:
                break;
        }
        startActivityForResult(takePictureIntent, actionCode);

    }

    private void handleBigCameraPhoto() {

        if (mCurrentPhotoPath != null) {
            setPic();
//            DetectLandmark jeff = new DetectLandmark(vision);
//			try {
//                String TAG = "MyActivity";
//                Log.d(TAG, "right before findlandmakr");
////                System.out.print("right before findlandmark");
//                findLandmark();
//            } catch (GeneralSecurityException e){
//                System.out.print("General Security Exception");
//            } catch (IOException e){
//                System.out.print("IOException");
//            }
            galleryAddPic();
            uploadImage();

        }

    }


    Button.OnClickListener mTakePicOnClickListener =
            new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
                }
            };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("onCreate", "Fucking hello?");
        mImageView = (ImageView) findViewById(R.id.imageView1);
        FloatingActionButton mfab = (FloatingActionButton) findViewById(R.id.fab);

//        mVideoView = (VideoView) findViewById(R.id.videoView1);
        mImageBitmap = null;

//        FloatingActionButton picBtn = (FloatingActionButton) findViewById(R.id.fab);
        setBtnListenerOrDisable(
                mfab,
                mTakePicOnClickListener,
                MediaStore.ACTION_IMAGE_CAPTURE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {
                    try {
                        handleBigCameraPhoto();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ;
                }
                break;
            }
        } // switch
    }

    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
//		outState.putParcelable(VIDEO_STORAGE_KEY, mVideoUri);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
//		outState.putBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY, (mVideoUri != null) );
        super.onSaveInstanceState(outState);
    }


    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list =
                packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void setBtnListenerOrDisable(
            FloatingActionButton btn,
            Button.OnClickListener onClickListener,
            String intentName
    ) {
        Log.d("setBtnListener", "can you see me??");
        if (isIntentAvailable(this, intentName)) {
        btn.setOnClickListener(onClickListener);
      }
    }

    //    if (myImg){};
// Must compress the Image to reduce image size to make upload easy
//    String fileNameSegments[] = mCurrentPhotoPath.split("/");
//    String fileName = fileNameSegments[fileNameSegments.length - 1];


    //    RequestQueue rq = Volley.newRequestQueue(this);
    String url = "http://gordons.byteprojects.co/appcall";

    public void uploadImage() {
        Log.d("photo path", mCurrentPhotoPath);
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        ;

        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, stream);
        byte[] byte_arr = stream.toByteArray();

//        bitmap.compress()
//    byte array[] = stream.toByteArray();
// Encode Image to String
        final String encodedString = Base64.encodeToString(byte_arr, 8);
        Log.d("compressed", String.valueOf(encodedString));


        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.e("RESPONSE", response);
                    JSONObject json = new JSONObject(response);
                    Toast.makeText(getBaseContext(),
                            json.getString("econ"), Toast.LENGTH_SHORT)
                            .show();
                    Toast.makeText(getBaseContext(),
                            json.getString("social"), Toast.LENGTH_SHORT)
                            .show();
                } catch (JSONException e) {
                    Log.d("JSON Exception", e.toString());
                    Toast.makeText(getBaseContext(),
                            "Error while loadin data!",
                            Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ERROR", "Error [" + error + "]");
                Toast.makeText(getBaseContext(),
                        "Cannot connect to server", Toast.LENGTH_LONG)
                        .show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("image", encodedString);
//                params.put("filename", fileName);
                return params;
            }
        };


        Volley.newRequestQueue(this).add(stringRequest);
    }
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//// Dismiss the progress bar when application is closed
//        if (prgDialog != null) {
//            prgDialog.dismiss();
//        }
//    }
//    RequestQueue requestQueue = Volley.newRequestQueue(this);
//    int array[] = new int[2];
//	String url = "http://httpbin.org/post";
//	StringRequest postRequest();
};
