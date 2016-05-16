package com.tnpxu.droneeyes;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.fpvtutorial.R;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.tnpxu.droneeyes.api.ResData;
import com.tnpxu.droneeyes.api.SendingPhotoApi;
import com.tnpxu.droneeyes.api.SendingPhotoData;
import com.tnpxu.droneeyes.api.ServiceGenerator;
import com.tnpxu.droneeyes.checkmarker.PointsTransForm;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dji.log.DJILogHelper;
import dji.sdk.AirLink.DJILBAirLink.DJIOnReceivedVideoCallback;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.Camera.DJICameraSettingsDef.CameraMode;
import dji.sdk.Camera.DJICameraSettingsDef.CameraShootPhotoMode;
import dji.sdk.Camera.DJIMedia;
import dji.sdk.Camera.DJIMediaManager;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent.DJICompletionCallback;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.Model;
import dji.sdk.base.DJIError;
import dji.sdk.Camera.DJIPlaybackManager.CameraFileDownloadCallback;
import dji.sdk.Camera.DJIMediaManager.*;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class FPVTutorialActivity extends Activity implements SurfaceTextureListener, OnClickListener {


    // handler case
    private final static int SHOWDIALOG = 0;
    private final static int SHOWTOAST = 1;
    private final static int FETCHALLMEDIALIST = 2;
    private final static int ENTERMULTIPLEEDIT = 3;
    private final static int SELECTFIRSTFILE = 4;
    private final static int DOWNLOADIT = 5;
    private final static int SHOWDOWNLOADDIALOG = 6;
    private final static int CLOSEDOWNLOADDIALOG = 7;

    private static final String TAG = FPVTutorialActivity.class.getName();

    private static final int INTERVAL_LOG = 300;
    private static long mLastTime = 0l;

    protected CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJIOnReceivedVideoCallback mOnReceivedVideoCallback = null;

    private DJIBaseProduct mProduct = null;
    private DJICamera mCamera = null;
    private DJICamera fCamera = null;
    private DJIFlightController mFlight = null;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextView mConnectStatusTextView;
    protected TextView mGpsSignalTextView;
    protected TextView mSettelliteCountTextView;
    protected TextView mHeightTextView;
    //Video Preview
    protected TextureView mVideoSurface = null;
    private Button captureAction, downloadAction, sendAction, updateStatus;
    private TextView viewTimer;
    private int i = 0;
    private int TIME = 1000;

    //////********** NOW CHECKING
    public static boolean isChecking = false;
    //count detect
    private int countDetect = 0;

    private PointsTransForm mTransform;

    public int limitShowToastSteamH264 = 1;
    public int limitShowToastSteamAirlink = 1;
    public int limitShowToastCheckMark = 1;
    public boolean captured = false;
    public boolean onChangeSteamMat = false;

    private ProgressDialog mProgressDialog;

    private CameraFileDownloadCallback mFileDownloadCallBack;
    private CameraDownloadListener<String> mCameraDownloadListener;
    public ArrayList<DJIMedia> resDJIMedia = null;

    public String fileName;

    public EditText tokenText;
    public CheckBox registerCheckBox;

    // The OpenCV loader callback.
    private BaseLoaderCallback mLoaderCallback =
            new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(final int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.d(TAG, "OpenCV loaded successfully");
                            mTransform = new PointsTransForm();
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };

    public Handler handlerFlightController = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            updateStatusBar();
            handlerFlightController.sendEmptyMessageDelayed(1,200);

            return false;
        }
    });

    public float textStream = 0.1f;

    public Handler handlerStatusCameraCommand = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1: updateStatus.setText("Status: Fetching");
                    break;
                case 2: updateStatus.setText("Status: Fetching Success!");
                    break;
                case 3: updateStatus.setText("Status: Downloading");
                    break;
                case 4:
                    updateStatus.setText("Status: Downloading Success!");
                    clearScreen();
                    break;
                case 5: updateStatus.setText("Status: Sending Photo");
                    break;
                case 6: updateStatus.setText("Status: Fetching Success!");
                    break;
                case 99:
                    textStream += 0.00001;
                    mGpsSignalTextView.setText(String.valueOf(textStream));
                    break;
                default:
                    break;
            }

            return false;
        }
    });

    public Handler handlerFetching = new Handler (new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    mCamera = mProduct.getCamera();

                    showToast("handler stage 1");
//                    mCamera = mProduct.getCamera();
                    //stopshootingmode
                    mCamera.stopShootPhoto(new DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                            if (djiError == null) {
                                handlerFetching.sendEmptyMessageDelayed(2,2000);
                            } else {
                                showToast(djiError.getDescription() + "from stoptaking");
                            }
                        }

                    });
                    break;
                case 2:
                    //showToast("handler stage 2");
                    if(mCamera.isMediaDownloadModeSupported ())
                        fetchPhoto();
                    else
                        showToast("DownloadModeNotReady");
                    break;
                case 3:
                    //showToast("handler stage 3");
                    downloadMedia();
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    public void checkPermission() {
        int hasStoragePermission = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE);

        if (hasStoragePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);
            return;
        }
    }

    private GoogleApiClient client;

        @Override
        protected void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_fpvtutorial);

            initUI();







        /**********************************************************************/
        /**********************************************************************/
        /**********************************************************************/
        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {
            //preprocessing
            @Override
            public void onResult(final byte[] videoBuffer, int size) {


//                /***********************************/
//                /******* BYTE TO MAT ***************/
//                /***********************************/
//
//
//                AsyncTask<String, Void, String> asyncBackground = new AsyncTask<String, Void, String>() {
//
//
//                    @Override
//                    protected void onPreExecute() {
//
//
//                    }
//
//                    @Override
//                    protected String doInBackground(String... params) {
//
//                        byte[] rawData = videoBuffer;
//                        Mat matData = new Mat();
//                        matData.put(0, 0, rawData);
//
//
//                        return "SUCCESS";
//
//                    }
//
//                    @Override
//                    protected void onPostExecute(String res) {
//
//                        onChangeSteamMat = false;
//
//                    }
//
//                };
//
//                if (!onChangeSteamMat) {
//                    if (Build.VERSION.SDK_INT >= 11) {
//                        onChangeSteamMat = true;
//                        asyncBackground.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                    } else {
//                        onChangeSteamMat = true;
//                        asyncBackground.execute();
//                    }
//                }
//
//                /*************************************************************************/

                if (mCodecManager != null) {
                    //test stream data
                    //handlerStatusCameraCommand.sendEmptyMessage(99);

                    // Send the raw H264 video data to codec manager for decoding
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                    //detect marker here

                } else {
                    Log.e(TAG, "mCodecManager is null");
                }

                //sendToCheck(matData);
            }
        };

        // The callback for receiving the raw video data from Airlink
        mOnReceivedVideoCallback = new DJIOnReceivedVideoCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {

                if (mCodecManager != null) {


                    // Send the raw H264 video data to codec manager for decoding
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                    // detect marker here

                }

                //sendToCheck(matData);
            }
        };

        /**********************************************************************/
        /**********************************************************************/
        /**********************************************************************/

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVTutorialApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11,
//                this, mLoaderCallback);
        initPreviewer();
        updateTitleBar();
        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }

    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "FPVTutorial Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.tnpxu.droneeyes/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();

        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private void initUI() {
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
        mGpsSignalTextView = (TextView) findViewById(R.id.GpsSignalTextView);
        mSettelliteCountTextView = (TextView) findViewById(R.id.SatelliteCountTextView);
        mHeightTextView = (TextView) findViewById(R.id.HeightTextView);
        // init mVideoSurface
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);

        viewTimer = (TextView) findViewById(R.id.timer);
        captureAction = (Button) findViewById(R.id.button1);
        downloadAction = (Button) findViewById(R.id.button2);
        sendAction = (Button) findViewById(R.id.button3);
        updateStatus = (Button)findViewById(R.id.button4);

        tokenText = (EditText) findViewById(R.id.tagText);
        registerCheckBox = (CheckBox) findViewById(R.id.registerModeCheckBox);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
        captureAction.setOnClickListener(this);
        downloadAction.setOnClickListener(this);
        sendAction.setOnClickListener(this);

    }

    private Handler handlerTimer = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // handler
            try {

                handlerTimer.postDelayed(this, TIME);
                viewTimer.setText(Integer.toString(i++));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void initPreviewer() {
        try {
            mProduct = FPVTutorialApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }

        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
            showToast(getString(R.string.disconnected));
        } else {


            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }

            if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
                mCamera = mProduct.getCamera();
                if (mCamera != null) {
                    // Set the callback
                    mCamera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            } else {
                if (null != mProduct.getAirLink()) {
                    if (null != mProduct.getAirLink().getLBAirLink()) {
                        // Set the callback
                        mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(mOnReceivedVideoCallback);
                    }
                }

                showToast("come to else shouldwork");
            }
        }
    }

    private void uninitPreviewer() {
        try {
            mProduct = FPVTutorialApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }

        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
            showToast(getString(R.string.disconnected));
        } else {
            if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
                mCamera = mProduct.getCamera();
                if (mCamera != null) {
                    // Set the callback
                    mCamera.setDJICameraReceivedVideoDataCallback(null);

                }
            } else {
                if (null != mProduct.getAirLink()) {
                    if (null != mProduct.getAirLink().getLBAirLink()) {
                        // Set the callback
                        mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(null);
                    }
                }
            }
        }
    }

    //
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            Log.e(TAG, "mCodecManager is null 2");
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    //
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    //
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    //
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureUpdated");
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
            onProductChange();
        }

    };


    private void updateTitleBar() {
        if (mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = FPVTutorialApplication.getProductInstance();
        if (product != null) {

            if (product.isConnected()) {
                //The product is connected
//                mConnectStatusTextView.setText(FPVTutorialApplication.getProductInstance().getModel() + "Connected");
                mConnectStatusTextView.setText("Connected");
                //initailize flightcontroller
                handlerFlightController.sendEmptyMessage(1);
                ret = true;
            } else {

                if (product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft) product;
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if (!ret) {
            // The product or the remote controller are not connected.
            mConnectStatusTextView.setText("Disconnected");
        }
    }

    protected void onProductChange() {
        initPreviewer();

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            final long current = System.currentTimeMillis();
            if (current - mLastTime < INTERVAL_LOG) {
                Log.d("", "click double");
                mLastTime = 0;
            } else {
                mLastTime = current;
                Log.d("", "click single");
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(FPVTutorialActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onClick(View v) {

        try {
            mProduct = FPVTutorialApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }

        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
            showToast(getString(R.string.disconnected));
            return;
        }

        switch (v.getId()) {
            case R.id.button1: {
                captureAction();
                break;
            }
            case R.id.button2: {
                fetchPhoto();
                break;
            }

            case R.id.button3:{
                sendPhoto();
                break;
            }

            case R.id.button4:{
                updateStatusBar();
                break;
            }
            default:
                break;
        }
    }

    // function for taking photo
    private void captureAction() {

        CameraMode cameraMode = CameraMode.ShootPhoto;

        mCamera = mProduct.getCamera();

        mCamera.setCameraMode(cameraMode, new DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {

                if (error == null) {
                    CameraShootPhotoMode photoMode = CameraShootPhotoMode.Single; // Set the camera capture mode as Single mode

                    mCamera.startShootPhoto(photoMode, new DJICompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                showToast("take photo: success");

//                                handlerFetching.sendEmptyMessageDelayed(2, 5000);
//                                showToast("post taking success");

                            } else {
                                showToast(error.getDescription());
                            }
                        }

                    }); // Execute the startShootPhoto API
                } else {
                    showToast(error.getDescription() + "from capture");
                }

            }

        });

    }

    private void clearScreen() {
        CameraMode cameraMode = CameraMode.ShootPhoto;

        mCamera = mProduct.getCamera();

        mCamera.setCameraMode(cameraMode, new DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {

                if (error == null) {
                } else {
                    showToast("clear screen error capture will auto clear screen");
                }

            }

        });
    }

    public void sendToCheck(final Mat checkMat) {
        if (!isChecking) {
            if (limitShowToastCheckMark == 1) {
                showToast("detecting");
                limitShowToastCheckMark++;
            }

            /***********************************/
            /******* Detect to Capture **********/
            /***********************************/

            AsyncTask<String, Void, String> asyncBackground = new AsyncTask<String, Void, String>() {

//                private Mat checkMat = new Mat();

                @Override
                protected void onPreExecute() {
                    isChecking = true;
//                    rgba.copyTo(checkMat);
                }

                @Override
                protected String doInBackground(String... params) {
                    if (mTransform.checkTransform(checkMat)) {
                        countDetect++;
                        if (countDetect == 10) {
                            return "T";
                        } else {
                            return "F1";
                        }
                    }

                    return "F2";

                }

                @Override
                protected void onPostExecute(String res) {
                    if (res.equals("T")) {
//                        takePhoto(checkMat);
                        /******** takePhoto here *********/
                        if (!captured) {
                            captureAction();
                            captured = true;
                        }

                        countDetect = 0;

                    } else if (res.equals("F1")) {
                        isChecking = false;
                    } else {
                        countDetect = 0;
                        isChecking = false;
                    }
//                    frameBuffer.clear();
                }

            };


            if (Build.VERSION.SDK_INT >= 11)
                asyncBackground.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                asyncBackground.execute();


            /*************************************************************************/
        }
    }

    public void fetchPhoto() {

        CameraMode cameraMode = CameraMode.MediaDownload;

        mCamera = mProduct.getCamera();

        mCamera.setCameraMode(cameraMode, new DJICompletionCallback() {

            @Override
            public void onResult(DJIError error) {

                if (error == null) {

                    showToast("onResult setCamMediaDownload");

                    mCamera.getMediaManager().fetchMediaList(new CameraDownloadListener<ArrayList<DJIMedia>>() {
                        @Override
                        public void onStart() {

                            handlerStatusCameraCommand.sendEmptyMessage(1);
                        }

                        @Override
                        public void onRateUpdate(long l, long l1, long l2) {

                        }

                        @Override
                        public void onProgress(long l, long l1) {

                        }

                        @Override
                        public void onSuccess(ArrayList<DJIMedia> djiMedias) {
                            handlerStatusCameraCommand.sendEmptyMessage(2);
                            resDJIMedia = djiMedias;
                            handlerFetching.sendEmptyMessageDelayed(3, 2000);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                        }
                    });

                } else {
                    showToast(error.getDescription() + "from fetch");
                }

            }

        });




    }

    public void downloadMedia(){
        int lastIndexMedia = resDJIMedia.size();

        if(lastIndexMedia != 0) {

            final DJIMedia getMedia = resDJIMedia.get(lastIndexMedia - 1);

            File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DroneEyes-Photo/");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            fileName = Environment.getExternalStorageDirectory().getPath() + "/DroneEyes-Photo/" +
                    getMedia.getFileName().substring(0,8) + "--" + getMedia.getCreatedDate() + ".jpg";

            getMedia.fetchMediaData(destDir, getMedia.getFileName().substring(0,8) + "--" + getMedia.getCreatedDate(), new CameraDownloadListener<String>() {

                ProgressDialog dDialog;
                @Override
                public void onStart() {

                    //dDialog = ProgressDialog.show(FPVTutorialActivity.this, "Fetching", "Please wait");

                    handlerStatusCameraCommand.sendEmptyMessage(3);

                }

                @Override
                public void onRateUpdate(long l, long l1, long l2) {

                }

                @Override
                public void onProgress(long l, long l1) {

                }

                @Override
                public void onSuccess(String strSuccess) {

//                    fileName = getMedia.getFileName().substring(0,8) + "--" + getMedia.getCreatedDate();
                    //dDialog.dismiss();

                    handlerStatusCameraCommand.sendEmptyMessage(4);

                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast("onFailure downloadMedial");
                    //dDialog.dismiss();

                }

            });
        }
    }

    public void sendPhoto() {

        //take photo before sendPhoto
        if(fileName == null){
            showToast("filename == null");
            return;
        }

        final File ph​oto = new File(fileName);

        final ProgressDialog pDialog;
        pDialog = ProgressDialog.show(FPVTutorialActivity.this, "Sending", "Please wait");

        String description = "Hello, Team alpha this is Drone eiei";
        SendingPhotoApi service = ServiceGenerator.createService(SendingPhotoApi.class);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), ph​oto);
        //get token id
        SendingPhotoData sendingPhotoData = new SendingPhotoData();
        String sendToken = tokenText.getText().toString();
        sendingPhotoData.setToken(sendToken);

        Call<ResData> call;
        //register mode or service mode
        if(registerCheckBox.isChecked()) {
            call = service.uploadRegister(requestBody, description, sendToken,sendingPhotoData);
        } else {
            call = service.uploadService(requestBody, description, sendToken,sendingPhotoData);
        }

        call.enqueue(new Callback<ResData>() {
            @Override
            public void onResponse(Response<ResData> response, Retrofit retrofit) {
                Log.v("Upload", "success");

                showToast("Upload Status : Success");
                pDialog.dismiss();
                //CameraActivity.isChecking = false;
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("Upload", t.getMessage());
                showToast("Upload Status : Failed");
                pDialog.dismiss();
                //CameraActivity.isChecking = false;
            }
        });


    }


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "FPVTutorial Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.tnpxu.droneeyes/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    private void updateStatusBar() {

        if (mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = FPVTutorialApplication.getProductInstance();
        if (product != null) {

            if (product.isConnected()) {

                initFlightControllerValue(product);

                ret = true;
            } else {

                if (product instanceof DJIAircraft) {

                    initFlightControllerValue(product);
                }
            }
        }

        if (!ret) {
            // The product or the remote controller are not connected.

        }

    }

    private void initFlightControllerValue(DJIBaseProduct product) {
        DJIAircraft aircraft = (DJIAircraft) product;
        mFlight = aircraft.getFlightController();

        String gpsText = "GPS signal: ";
        switch (mFlight.getCurrentState().getGpsSignalStatus().value()) {
            case 0:
                gpsText += "very bad";
                break;
            case 1:
                gpsText += "very weak";
                break;
            case 2:
                gpsText += "weak";
                break;
            case 3:
                gpsText += "good";
                break;
            case 4:
                gpsText += "very good";
                break;
            case 5:
                gpsText += "very strong";
                break;
            default:
                gpsText += "none";
                break;
        }

        mGpsSignalTextView.setText(gpsText);
        String settelliteText = "Satellite: " + String.valueOf(mFlight.getCurrentState().getSatelliteCount());
        mSettelliteCountTextView.setText(settelliteText);
        String heightText = String.format("Height: %1$.2f m.",mFlight.getCurrentState().getUltrasonicHeight());
        mHeightTextView.setText(heightText);
    }
}
