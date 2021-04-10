package io.github.livenlearnaday.bmzscanner.scanning.zxing;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.Result;

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
import io.github.livenlearnaday.bmzscanner.scanning.ocr.OcrCaptureActivity;

import static io.github.livenlearnaday.bmzscanner.util.ConstantUtils.PERMISSION_REQUEST_CODE;


public class ZXingScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private ZXingScannerView mScannerView;

    private static final String FLASH_STATE = "FLASH_STATE";

    private static final String FOCUS_STATE = "FOCUS_STATE";


    // List of unique qr codes
    public static final Map<String, String> uniqueCodeString = new HashMap<>();


    private CodeAdapter adapter1;
    private boolean mFlash = false;
    private final boolean mFocus = true;

    private List<CodeDetail> mCodeDetailList;

    private ViewGroup contentFrame;

    private final int uniqueCount = 0;
    private final int oldValue = -1;


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


    ScannerViewModel scannerViewModel;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_zxing_simple_scanner);

        ButterKnife.bind(this);


        //set scanning options
        barcodeButton.setVisibility(View.GONE);


        scannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);


        contentFrame = findViewById(R.id.content_frame);


        if (checkPermission()) {
            //main logic or main code

            // . write your main code to execute, It will execute if the permission is already given.

            loadView();


        } else {
            requestPermission();
        }


    }


    @OnClick(R.id.ocr_button)
    public void ocrButtonClick(Button button) {

        Intent intent = new Intent(getApplicationContext(), OcrCaptureActivity.class);
        finish();
        startActivity(intent);


    }

    @OnClick(R.id.bulkqr_button)
    public void bulkqrButtonClick(Button button) {

        Intent intent = new Intent(getApplicationContext(), QrCodeDetectActivity.class);
        finish();
        startActivity(intent);

    }


    @OnClick(R.id.flash_button)
    public void flashButtonClick(Button button) {

        if (mFlash) {
            mFlash = false;
            flashButton.setBackground(getDrawable(R.drawable.ic_baseline_flash_off_24));

        } else {
            mFlash = true;
            flashButton.setBackground(getDrawable(R.drawable.ic_baseline_flash_on_24));

        }

        mScannerView.setFlash(mFlash);

    }


    @OnClick(R.id.go_to_qrlist_button)
    public void goToQrlistButton(Button button) {

        Intent intent = new Intent(getApplicationContext(), CodeListActivity.class);
        startActivity(intent);


    }


    @Override
    public void onResume() {
        super.onResume();

        if (mScannerView != null) {


            mScannerView.setResultHandler(ZXingScannerActivity.this);
            // You can optionally set aspect ratio tolerance level
            // that is used in calculating the optimal Camera preview size
            mScannerView.setAspectTolerance(0.2f);
            mScannerView.startCamera();


        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null) {
            mScannerView.stopCamera();
        }


    }

    @Override
    public void handleResult(Result rawResult) {

        String codeString = rawResult.getText();


        if (!uniqueCodeString.containsKey(codeString)) {

            uniqueCodeString.put(codeString, codeString);

            CodeDetail code = new CodeDetail(codeString, String.valueOf(codeString.length()));

            scannerViewModel.insert(code);

            final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            tg.startTone(ToneGenerator.TONE_PROP_PROMPT);
            tg.release();


        }



        mScannerView.resumeCameraPreview(ZXingScannerActivity.this); //resume immediately


    }




    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        outState.putBoolean(FLASH_STATE, mFlash);
        outState.putBoolean(FOCUS_STATE, mFocus);
    }


    private boolean checkPermission() {
        // Permission is not granted
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(ZXingScannerActivity.this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    loadView();


                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(ZXingScannerActivity.this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ZXingScannerActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    private void loadView() {

        mCodeDetailList = new ArrayList<>();

        mScannerView = new ZXingScannerView(this);


        contentFrame.addView(mScannerView);

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
                Toast.makeText(ZXingScannerActivity.this, "code deleted", Toast.LENGTH_SHORT).show();

            }
        }).attachToRecyclerView(recyclerView);


    }



    @Override
    protected void onDestroy() {

        mScannerView.stopCamera();
        super.onDestroy();

    }




}




