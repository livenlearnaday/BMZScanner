/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.livenlearnaday.bmzscanner.scanning.ocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.snackbar.Snackbar;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.livenlearnaday.bmzscanner.R;
import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import io.github.livenlearnaday.bmzscanner.scanning.CodeListActivity;
import io.github.livenlearnaday.bmzscanner.scanning.ScannerViewModel;
import io.github.livenlearnaday.bmzscanner.scanning.adapter.CodeAdapter;
import io.github.livenlearnaday.bmzscanner.scanning.bulkqr.QrCodeDetectActivity;
import io.github.livenlearnaday.bmzscanner.scanning.zxing.ZXingScannerActivity;
import timber.log.Timber;

import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
import static io.github.livenlearnaday.bmzscanner.scanning.zxing.ZXingScannerActivity.uniqueCodeString;


/**
 * Activity for the multi-tracker app.  This app detects text and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and contents of each TextBlock.
 */
public final class OcrCaptureActivity extends AppCompatActivity {
    private static final String TAG = "OcrCaptureActivity";

    //for ocr qr text
    public static final int RC_HANDLE_CAMERA_PERM = 2; // Permission request codes need to be < 256
    public static final int REQUEST_HANDLE_GMS = 9001;


    ScannerViewModel scannerViewModel;

    // Intent request code to handle updating play services if needed.

    private Context context;
    static String mTextCaptured;
    private List<CodeDetail> mCodeDetailList;
    private String properCode = "";


    // Permission request codes need to be < 256
//    private static final int RC_HANDLE_CAMERA_PERM = 2; //added to constant util

    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;


    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private int uniqueCount = 0;
    private int oldValue = -1;


    @BindView(R.id.recycle_view_scanning)
    RecyclerView recyclerView;

    // Where the number of unique messages are listed
    @BindView(R.id.total_unique)
    TextView textUnqiueCount;
    @BindView(R.id.go_to_qrlist_button)
    Button goToQrlistButton;

    @BindView(R.id.bulkqr_button)
    Button bulkqrButton;
    @BindView(R.id.barcode_button)
    Button barcodeButton;
    @BindView(R.id.ocr_button)
    Button ocrButton;
    @BindView(R.id.flash_button)
    Button flashButton;


    public static final Map<String, String> uniqueStringOcr = new HashMap<String, String>();


    private CodeAdapter adapter1;

    private OcrGraphic graphic;
    private TextBlock text = null;


    private boolean mFocus = true; //start with focus on
    private boolean mFlash = false;
    private TextRecognizer textRecognizer;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_ocr_capture);
        ButterKnife.bind(this);


        //set scanning options
        ocrButton.setVisibility(View.GONE);


        scannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);


        mCodeDetailList = new ArrayList<>();


        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);

        // read parameters from the intent used to launch the activity.
        mFocus = getIntent().getBooleanExtra(AutoFocus, true);
        mFlash = getIntent().getBooleanExtra(UseFlash, false);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(mFocus, mFlash);
//         We have permission, so create the camerasource

        } else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(getApplicationContext(), new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());

//        Snackbar.make(mGraphicOverlay, "Tap to capture. Pinch/Stretch to zoom",
//                Snackbar.LENGTH_INDEFINITE)
//                .show();


//        mCodeDetailList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);

        adapter1 = new CodeAdapter();
        recyclerView.setAdapter(adapter1);

        scannerViewModel.getAllCodeDetail().observe(this, new Observer<List<CodeDetail>>() {
            @Override
            public void onChanged(List<CodeDetail> codeDetailList) {
                adapter1.submitList(codeDetailList);
                textUnqiueCount.setText(String.valueOf(codeDetailList.size()));


            }
        });


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                scannerViewModel.delete(adapter1.getCodeDetailAt(viewHolder.getAdapterPosition()));
                uniqueCodeString.remove(adapter1.getCodeDetailAt(viewHolder.getAdapterPosition()).getCodeString());
                Toast.makeText(OcrCaptureActivity.this, "code deleted", Toast.LENGTH_SHORT).show();

            }
        }).attachToRecyclerView(recyclerView);


    }


    @OnClick(R.id.bulkqr_button)
    public void bulkqrButtonClick(Button button) {

        Intent intent = new Intent(getApplicationContext(), QrCodeDetectActivity.class);
        finish();
        startActivity(intent);

    }


    @OnClick(R.id.barcode_button)
    public void barcodeButtonClick(Button button) {

        Intent intent = new Intent(getApplicationContext(), ZXingScannerActivity.class);
        finish();
        startActivity(intent);

    }


    @OnClick(R.id.flash_button)
    public void flashButtonClick(Button button) {

        if (mFlash) {
            mFlash = false;
            mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            flashButton.setBackground(getDrawable(R.drawable.ic_baseline_flash_off_24));
        } else {
            mFlash = true;
            mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            flashButton.setBackground(getDrawable(R.drawable.ic_baseline_flash_on_24));
        }


    }


    @OnClick(R.id.go_to_qrlist_button)
    public void goToQrlistButton(Button button) {

        Intent intent = new Intent(getApplicationContext(), CodeListActivity.class);
        finish();
        startActivity(intent);

    }


    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(OcrCaptureActivity.this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(OcrCaptureActivity.this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();

    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated processor instance
        // is set to receive the text recognition results and display graphics for each text block
        // on screen.
        textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay));

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(context, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.

        mCameraSource =
                new CameraSource.Builder(context, textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();


    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {

        if (mPreview != null) {
            mPreview.release();
        }
        super.onDestroy();

    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // We have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // Check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(OcrCaptureActivity.this, code, REQUEST_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);


            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    /**
     * onTap is called to capture the first TextBlock under the tap location and return it to
     * the Initializing Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private boolean onTap(float rawX, float rawY) {
        OcrGraphic graphic = mGraphicOverlay.getGraphicAtLocation(rawX, rawY);
        TextBlock text = null;
        if (graphic != null) {
            text = graphic.getTextBlock();


            if (text != null && text.getValue() != null) {


                if (!uniqueCodeString.containsKey(text.getValue())) {

                    uniqueCodeString.put(text.getValue(), text.getValue());

                    CodeDetail code = new CodeDetail(text.getValue(), String.valueOf(text.getValue().length()));

                    scannerViewModel.insert(code);

                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
                    tg.release();


                }

            } else {
                Log.d(TAG, "text data is null");
            }
        } else {
            Log.d(TAG, "no text detected");
        }
        return text != null;
    }


    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            scannerViewModel.getAllCodeDetail().observe(OcrCaptureActivity.this, new Observer<List<CodeDetail>>() {
                @Override
                public void onChanged(List<CodeDetail> codeDetailList) {
                    adapter1.submitList(codeDetailList);
                    textUnqiueCount.setText(String.valueOf(codeDetailList.size()));

                }
            });

            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);

        }

    }


    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(OcrCaptureActivity.this, ZXingScannerActivity.class));
        finish();

    }


}
