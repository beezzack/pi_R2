package com.dji.mediaManagerDemo;


import com.wikitude.architect.ArchitectStartupConfiguration;
import com.wikitude.architect.ArchitectView;
import com.dji.mediaManagerDemo.util.SampleData;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * This Activity is (almost) the least amount of code required to use the
 * basic functionality for Image-/Instant- and Object Tracking.
 *
 * This Activity needs Manifest.permission.CAMERA permissions because the
 * ArchitectView will try to start the camera.
 */
public class WikitudeArActivity extends AppCompatActivity {
    public static final String INTENT_EXTRAS_KEY_SAMPLE = "sampleData";

    protected static final String TAG = WikitudeArActivity.class.getSimpleName();

    /** Root directory of the sample AR-Experiences in the assets dir. */
    private static final String SAMPLES_ROOT = "samples/";

    /**
     * The ArchitectView is the core of the AR functionality, it is the main
     * interface to the Wikitude SDK.
     * The ArchitectView has its own lifecycle which is very similar to the
     * Activity lifecycle.
     * To ensure that the ArchitectView is functioning properly the following
     * methods have to be called:
     *      - onCreate(ArchitectStartupConfiguration)
     *      - onPostCreate()
     *      - onResume()
     *      - onPause()
     *      - onDestroy()
     * Those methods are preferably called in the corresponding Activity lifecycle callbacks.
     */
    protected ArchitectView architectView;

    /** The path to the AR-Experience. This is usually the path to its index.html. */
    private String arExperience;
    /** record video */
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    ImageButton btn_record, btn_picture;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 1920;
    private static final int DISPLAY_HEIGHT = 1080;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSION_KEY = 1;
    boolean isRecording = false;
    private String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    private File videoFile;


    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);



        // Used to enabled remote debugging of the ArExperience with google chrome https://developers.google.com/web/tools/chrome-devtools/remote-debugging
        WebView.setWebContentsDebuggingEnabled(true);

        /*
         * The following code is used to run different configurations of the SimpleArActivity,
         * it is not required to use the ArchitectView but is used to simplify the Sample App.
         *
         * Because of this the Activity has to be started with correct intent extras.
         * e.g.:
         *  SampleData sampleData = new SampleData.Builder("SAMPLE_NAME", "PATH_TO_AR_EXPERIENCE")
         *              .arFeatures(ArchitectStartupConfiguration.Features.ImageTracking)
         *              .cameraFocusMode(CameraSettings.CameraFocusMode.CONTINUOUS)
         *              .cameraPosition(CameraSettings.CameraPosition.BACK)
         *              .cameraResolution(CameraSettings.CameraResolution.HD_1280x720)
         *              .camera2Enabled(false)
         *              .build();
         *
         * Intent intent = new Intent(this, SimpleArActivity.class);
         * intent.putExtra(UrlLauncherStorageActivity.URL_LAUNCHER_SAMPLE_CATEGORY, category);
         * startActivity(intent);
         */
        final Intent intent = getIntent();
        if (!intent.hasExtra(INTENT_EXTRAS_KEY_SAMPLE)) {
            throw new IllegalStateException(getClass().getSimpleName() +
                    " can not be created without valid SampleData as intent extra for key " + INTENT_EXTRAS_KEY_SAMPLE + ".");
        }

        final SampleData sampleData = (SampleData) intent.getSerializableExtra(INTENT_EXTRAS_KEY_SAMPLE);
        arExperience = sampleData.getPath();

        /*
         * The ArchitectStartupConfiguration is required to call architectView.onCreate.
         * It controls the startup of the ArchitectView which includes camera settings,
         * the required device features to run the ArchitectView and the LicenseKey which
         * has to be set to enable an AR-Experience.
         */
        final ArchitectStartupConfiguration config = new ArchitectStartupConfiguration(); // Creates a config with its default values.
        config.setLicenseKey(getString(R.string.wikitude_license_key)); // Has to be set, to get a trial license key visit http://www.wikitude.com/developer/licenses.
        config.setCameraPosition(sampleData.getCameraPosition());       // The default camera is the first camera available for the system.
        config.setCameraResolution(sampleData.getCameraResolution());   // The default resolution is 640x480.
        config.setCameraFocusMode(sampleData.getCameraFocusMode());     // The default focus mode is continuous focusing.
        config.setCamera2Enabled(sampleData.isCamera2Enabled());        // The camera2 api is disabled by default (old camera api is used).
        config.setFeatures(sampleData.getArFeatures());                 // This tells the ArchitectView which AR-features it is going to use, the default is all of them.

        setContentView(R.layout.activity_wikitude_ar);
        architectView = new ArchitectView(this);
        this.architectView = (ArchitectView)findViewById( R.id.architectView );
        architectView.onCreate(config); // create ArchitectView with configuration
        //record
        if (!RecordHelpFunction.hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
        }



        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        btn_record = (ImageButton) findViewById(R.id.btn_record);
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onToggleScreenShare();

            }
        });
        btn_picture = (ImageButton) findViewById(R.id.btn_picture);
        btn_picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureScreenshot();
                Toast toast = Toast.makeText(WikitudeArActivity.this,
                        "拍照成功", Toast.LENGTH_SHORT);
                toast.show();

            }
        });
        /*create MediaManagerDemo folder to save video and picture*/
        String folder = "MediaManagerDemo";

        File f = new File(Environment.getExternalStorageDirectory(), folder);
        if (!f.exists()) {
            f.mkdirs();
        }

    }

    public void actionBtnReload() {
        if (isRecording) {
            btn_record.setBackgroundResource(R.drawable.end);
        } else {
            btn_record.setBackgroundResource(R.drawable.recording);
        }

    }


    public void onToggleScreenShare() {
        if (!isRecording) {
            initRecorder();
            shareScreen();
        } else {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            stopScreenSharing();
        }
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        isRecording = true;
        actionBtnReload();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    private void initRecorder() {
        try {
            String videoPath = Environment.getExternalStorageDirectory() + "/MediaManagerDemo/ARVideo" + getCurSysDate() + ".mp4";
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //THREE_GPP
            mMediaRecorder.setOutputFile(videoPath);
            mMediaRecorder.setVideoFrameRate(60 ); // 30
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);

            mMediaRecorder.setVideoEncodingBitRate(3000000);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
            videoFile = new File(videoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        destroyMediaProjection();
        isRecording = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent mediaScanIntent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(videoFile); //out is your output file
            mediaScanIntent.setData(contentUri);
            WikitudeArActivity.this.sendBroadcast(mediaScanIntent);
        } else {
            sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://"
                            + Environment.getExternalStorageDirectory() + "MediaManagerDemo")));
        }
        Toast toast = Toast.makeText(WikitudeArActivity.this,
                "錄製成功", Toast.LENGTH_SHORT);
        toast.show();
        actionBtnReload();
    }



    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    public String getCurSysDate() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            isRecording = false;
            actionBtnReload();
            return;
        }
        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        isRecording = true;
//        Toast toast = Toast.makeText(WikitudeArActivity.this,
//                "開始錄製", Toast.LENGTH_SHORT);
//        toast.show();

        actionBtnReload();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_KEY:
            {
                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    onToggleScreenShare();
                } else {
                    isRecording = false;
                    actionBtnReload();
                    Snackbar.make(findViewById(android.R.id.content), "Please enable Microphone and Storage permissions.",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                return;
            }
        }
    }





    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording) {
                isRecording = false;
                actionBtnReload();
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        architectView.onPostCreate();

        try {
            /*
             * Loads the AR-Experience, it may be a relative path from assets,
             * an absolute path (file://) or a server url.
             *
             * To get notified once the AR-Experience is fully loaded,
             * an ArchitectWorldLoadedListener can be registered.
             */
            architectView.load(SAMPLES_ROOT + arExperience);


        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_loading_ar_experience), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception while loading arExperience " + arExperience + ".", e);
        }
    }
    private void captureScreenshot() {
        try {
            // image saving sd card path
            String mPath = Environment.getExternalStorageDirectory().toString() + "/MediaManagerDemo/ARPicture" + getCurSysDate() + ".jpg";

            // create bitmap screen capture
            File imageFile = new File(mPath);

            savePic(takeScreenShot(this), mPath);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Intent mediaScanIntent = new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imageFile); //out is your output file
                mediaScanIntent.setData(contentUri);
                WikitudeArActivity.this.sendBroadcast(mediaScanIntent);
            } else {
                sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_MOUNTED,
                        Uri.parse("file://"
                                + Environment.getExternalStorageDirectory() + "MediaManagerDemo")));
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        architectView.onResume(); // Mandatory ArchitectView lifecycle call
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        architectView.onPause(); // Mandatory ArchitectView lifecycle call
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*
         * Deletes all cached files of this instance of the ArchitectView.
         * This guarantees that internal storage for this instance of the ArchitectView
         * is cleaned and app-memory does not grow each session.
         *
         * This should be called before architectView.onDestroy
         */
        architectView.clearCache();
        architectView.onDestroy(); // Mandatory ArchitectView lifecycle call

        destroyMediaProjection();
    }
    @Override
    public void onBackPressed() {
        if (isRecording) {
            Snackbar.make(findViewById(android.R.id.content), "Wanna Stop recording and exit?",
                    Snackbar.LENGTH_INDEFINITE).setAction("Stop",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mMediaRecorder.stop();
                            mMediaRecorder.reset();
                            Log.v(TAG, "Stopping Recording");
                            stopScreenSharing();
                            finish();
                        }
                    }).show();
        } else {
            finish();
        }
    }
    public static Bitmap takeScreenShot(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;

        //Find the screen dimensions to create bitmap in the same size.
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int height = dm.heightPixels;
        int width = dm.widthPixels + 40;


        Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height - statusBarHeight);
        view.destroyDrawingCache();
        return b;
    }

    public static void savePic(Bitmap b, String strFileName) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(strFileName);
            b.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

