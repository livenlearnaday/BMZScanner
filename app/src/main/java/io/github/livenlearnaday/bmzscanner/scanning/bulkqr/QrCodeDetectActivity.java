package io.github.livenlearnaday.bmzscanner.scanning.bulkqr;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Bundle;

import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import org.ddogleg.struct.FastQueue;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import boofcv.abst.fiducial.QrCodeDetectorPnP;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayU8;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import io.github.livenlearnaday.bmzscanner.MainApplication;
import io.github.livenlearnaday.bmzscanner.R;
import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import io.github.livenlearnaday.bmzscanner.scanning.CodeListActivity;
import io.github.livenlearnaday.bmzscanner.scanning.ScannerViewModel;
import io.github.livenlearnaday.bmzscanner.scanning.adapter.CodeAdapter;
import io.github.livenlearnaday.bmzscanner.scanning.misc.MiscUtil;
import io.github.livenlearnaday.bmzscanner.scanning.misc.RenderCube3D;
import io.github.livenlearnaday.bmzscanner.scanning.ocr.OcrCaptureActivity;
import io.github.livenlearnaday.bmzscanner.scanning.zxing.ZXingScannerActivity;
import timber.log.Timber;

import static io.github.livenlearnaday.bmzscanner.scanning.zxing.ZXingScannerActivity.uniqueCodeString;


/**
 * Used to detect and read information from QR codes
 */
public class QrCodeDetectActivity extends DemoCamera2Activity {
    private static final String TAG = "QrCodeDetect";
    boolean waitingCameraPermissions = true;


    // Switches what information is displayed
    Mode mode = Mode.NORMAL;
    // Does it render failed detections too?
    boolean showFailures = false;

    private boolean mFlash = false;


    // Where the number of unique messages are listed;
    // List of unique qr codes
    public static final Object uniqueLock = new Object();
    public static final Map<String, String> uniqueStringBulkQr = new HashMap<>();
    // qr which has been selected and should be viewed
    public static String selectedQR = null;
    // TODO don't use a static method and forget detection if the activity is exited by the user

    // Location in image coordinates that the user is touching
    Point2D_F64 touched = new Point2D_F64();
    boolean touching = false;
    boolean touchProcessed = false;
    private CodeDetail codeDetail;


    private List<QrCode> qrcodes;
    private List<CodeDetail> mCodeDetailList;
    int index = 0;
    CodeAdapter adapter1;
    private boolean failedQRDetection = false;
    private FrameLayout surfaceLayout;


    @BindView(R.id.recycle_view_scanning)
    RecyclerView recyclerView;

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


    // Which standard configuration to use
    Detector detectorType = Detector.STANDARD;
    Spinner spinnerDetector;
    private ViewGroup contentFrame;

    ScannerViewModel scannerViewModel;

    public QrCodeDetectActivity() {
        super(Resolution.HIGH);
        super.changeResolutionOnSlow = true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.standard_camera2);

        ButterKnife.bind(this);


        //set scanning options
        bulkqrButton.setVisibility(View.GONE);
        flashButton.setVisibility(View.GONE);


        surfaceLayout = findViewById(R.id.camera_frame_layout);

        scannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);


        app = (MainApplication) getApplication();

        if (app == null)
            throw new RuntimeException("App is null!");

        try {

            loadCameraSpecs();


        } catch (NoClassDefFoundError e) {
            // Some people like trying to run this app on really old versions of android and
            // seem to enjoy crashing and reporting the errors.
            e.printStackTrace();
            abortDialog("Camera2 API Required");
            return;
        }


// Create the list of QR Codes
        qrcodes = new ArrayList<>();
        mCodeDetailList = new ArrayList<>();


        adapter1 = new CodeAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
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
                Toast.makeText(QrCodeDetectActivity.this, "code deleted", Toast.LENGTH_SHORT).show();

            }
        }).attachToRecyclerView(recyclerView);


        startCamera(surfaceLayout, null);


        displayView.setOnTouchListener(new TouchListener());


    }


    @OnClick(R.id.ocr_button)
    public void ocrButtonClick(Button button) {

        Intent intent = new Intent(getApplicationContext(), OcrCaptureActivity.class);
        finish();
        startActivity(intent);

    }


    @OnClick(R.id.barcode_button)
    public void barcodeButtonClick(Button button) {

        Intent intent = new Intent(getApplicationContext(), ZXingScannerActivity.class);
        finish();
        startActivity(intent);

    }


    @OnClick(R.id.go_to_qrlist_button)
    public void pressedListViewButton(View view) {
        Log.v("Logging ", String.format("Enter pressedListView QrCodeDetectActivity, mCodeDetailList size %4d", mCodeDetailList.size()));
        Intent intent = new Intent(this, CodeListActivity.class);
        finish();
        startActivity(intent);
    }


    private class TouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            Log.v("Logging ", "Enter TouchListener onTouch");
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    touching = true;
                }
                break;

                case MotionEvent.ACTION_UP: {
                    touching = false;
                }
                break;
            }

            if (touching) {
                applyToPoint(viewToImage, motionEvent.getX(), motionEvent.getY(), touched);
            }

            return true;
        }
    }

    @Override
    public void createNewProcessor() {
        Log.v("Logging ", "Enter createNewProcessor");
        setProcessing(new QrCodeProcessing());


    }


    protected class QrCodeProcessing extends DemoProcessingAbstract<GrayU8> {

        QrCodeDetectorPnP<GrayU8> detector;

        FastQueue<QrCode> detected = new FastQueue<>(QrCode::new);
        FastQueue<QrCode> failures = new FastQueue<>(QrCode::new);

        Paint colorDetected = new Paint();
        Paint colorFailed = new Paint();
        Path path = new Path();

        private int uniqueCount = 0;
        private int oldValue = -1;


        final FastQueue<Se3_F64> listPose = new FastQueue<>(Se3_F64::new);
        RenderCube3D renderCube = new RenderCube3D();
        CameraPinholeBrown intrinsic;

        public QrCodeProcessing() {
            super(GrayU8.class);

            Log.v("Logging ", "Enter QrCodeProcessing ");

            ConfigQrCode config;

            switch (detectorType) {
                case FAST: {
                    config = ConfigQrCode.fast();
                }
                break;

                default: {
                    config = new ConfigQrCode();
                }
            }

            detector = FactoryFiducial.qrcode3D(config, GrayU8.class);

            colorDetected.setARGB(0, 0, 0, 0);
            colorDetected.setStyle(Paint.Style.STROKE);
            colorFailed.setARGB(0, 0, 0, 0);
            colorFailed.setStyle(Paint.Style.STROKE);

//            colorDetected.setARGB(0xA0, 0, 0xFF, 0);
//            colorDetected.setStyle(Paint.Style.FILL);
//            colorFailed.setARGB(0xA0, 0xFF, 0x11, 0x11);
//            colorFailed.setStyle(Paint.Style.FILL);
        }


        @Override
        public void initialize(int imageWidth, int imageHeight, int sensorOrientation) {

            Timber.v("Enter initialize ");

            touchProcessed = false;
            selectedQR = null;
            touching = false;

            renderCube.initialize(cameraToDisplayDensity);
            intrinsic = lookupIntrinsics();
            detector.setLensDistortion(LensDistortionFactory.narrow(intrinsic), imageWidth, imageHeight);


        }

        @Override
        public void onDraw(Canvas canvas, Matrix imageToView) {

            Log.v("Logging ", "Enter onDraw ");

            canvas.concat(imageToView);

            synchronized (lockGui) {

                switch (mode) {
                    case NORMAL: {
                        for (int i = 0; i < detected.size; i++) {

                            QrCode qr = detected.get(i);
                            MiscUtil.renderPolygon(qr.bounds, path, canvas, colorDetected);

                            if (touching && Intersection2D_F64.containConvex(qr.bounds, touched)) {
                                selectedQR = qr.message;
                            }

                        }


                        for (int i = 0; showFailures && i < failures.size; i++) {
                            QrCode qr = failures.get(i);
                            MiscUtil.renderPolygon(qr.bounds, path, canvas, colorFailed);


                        }


                    }
                    break;

                    case GRAPH: {
                        // TODO implement this in the future
                    }
                    break;
                }

                for (int i = 0; i < listPose.size; i++) {
                    renderCube.drawCube("", listPose.get(i), intrinsic, 1, canvas);
                }
            }


            // touchProcessed is needed to prevent multiple intent from being sent
            if (selectedQR != null && !touchProcessed) {
                touchProcessed = true;
                Intent intent = new Intent(QrCodeDetectActivity.this, CodeListActivity.class);
                startActivity(intent);
            }


        }

        @Override
        public void process(GrayU8 input) {


            detector.detect(input);

            Log.v("Logging ", "Enter process ");

            synchronized (uniqueLock) {

                for (QrCode qr : detector.getDetector().getDetections()) {
                    if (qr.message == null) {
                        Log.e(TAG, "qr with null message?!?");
                    }


                    String codeString = MiscUtil.properCodeStringAfterFormatting(qr.message);


                    if (!uniqueCodeString.containsKey(codeString)) {

                        uniqueCodeString.put(codeString, codeString);

                        Timber.tag("Logging ").v("synchronized enter loop");

                        qrcodes.add(qr);

                        CodeDetail code = new CodeDetail(codeString, String.valueOf(codeString.length()));

                        scannerViewModel.insert(code);

                        final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                        tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
                        tg.release();


                    }

                }


            }


            synchronized (lockGui) {
                detected.reset();


                for (QrCode qr : detector.getDetector().getDetections()) {
                    detected.grow().set(qr);

                }

                failures.reset();
                for (QrCode qr : detector.getDetector().getFailures()) {
                    if (qr.failureCause.ordinal() >= QrCode.Failure.ERROR_CORRECTION.ordinal()) {
                        failures.grow().set(qr);


                    }

                }

                listPose.reset();
                for (int i = 0; i < detector.totalFound(); i++) {
                    detector.getFiducialToCamera(i, listPose.grow());
                }
            }


        }
    }

    public static CameraSpecs defaultCameraSpecs(MainApplication app) {
        for (int i = 0; i < app.specs.size(); i++) {
            CameraSpecs s = app.specs.get(i);
            if (s.deviceId.equals(app.preference.cameraId))
                return s;
        }
        throw new RuntimeException("Can't find default camera");
    }

    private void setDefaultPreferences() {
        app.preference.showSpeed = false;
        app.preference.autoReduce = true;

        // There are no cameras.  This is possible due to the hardware camera setting being set to false
        // which was a work around a bad design decision where front facing cameras wouldn't be accepted as hardware
        // which is an issue on tablets with only front facing cameras
        if (app.specs.size() == 0) {
            dialogNoCamera();
        }
        // select a front facing camera as the default
        for (int i = 0; i < app.specs.size(); i++) {
            CameraSpecs c = app.specs.get(i);

            app.preference.cameraId = c.deviceId;
            if (c.facingBack) {
                break;
            }
        }

    }


    private void loadCameraSpecs() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    0);
        } else {
            waitingCameraPermissions = false;

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (manager == null)
                throw new RuntimeException("No cameras?!");
            try {
                String[] cameras = manager.getCameraIdList();

                for (String cameraId : cameras) {
                    CameraSpecs c = new CameraSpecs();
                    app.specs.add(c);
                    c.deviceId = cameraId;
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    c.facingBack = facing != null && facing == CameraCharacteristics.LENS_FACING_BACK;
                    StreamConfigurationMap map = characteristics.
                            get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }
                    Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
                    if (sizes == null)
                        continue;
                    c.sizes.addAll(Arrays.asList(sizes));
                }
            } catch (CameraAccessException e) {
                throw new RuntimeException("No camera access??? Wasn't it just granted?");
            }

            // Now that it can read the camera set the default settings
            setDefaultPreferences();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadCameraSpecs();

                } else {
                    dialogNoCameraPermission();
                }
                return;
            }
        }
    }


    private void dialogNoCamera() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your device has no cameras!")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void dialogNoCameraPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Denied access to the camera! Exiting.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    /**
     * Displays a warning dialog and then exits the activity
     */
    private void abortDialog(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(QrCodeDetectActivity.this).create();
        alertDialog.setTitle("Fatal error");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> {
                    dialog.dismiss();
                    QrCodeDetectActivity.this.finish();
                });
        alertDialog.show();
    }


    enum Mode {
        NORMAL,
        GRAPH
    }

    enum Detector {
        STANDARD,
        FAST
    }


    @Override
    protected void onDestroy() {
        this.processor.stop();
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        super.onPause();
        synchronized (lockProcessor) {
            if (processor != null)
                processor.stop();
        }

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(QrCodeDetectActivity.this, ZXingScannerActivity.class));
        finish();

    }


}







