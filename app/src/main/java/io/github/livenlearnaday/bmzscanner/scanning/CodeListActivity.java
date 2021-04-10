package io.github.livenlearnaday.bmzscanner.scanning;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.livenlearnaday.bmzscanner.R;
import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import io.github.livenlearnaday.bmzscanner.scanning.adapter.CodeAdapter;
import io.github.livenlearnaday.bmzscanner.scanning.bulkqr.QrCodeDetectActivity;
import io.github.livenlearnaday.bmzscanner.scanning.ocr.OcrCaptureActivity;
import io.github.livenlearnaday.bmzscanner.scanning.zxing.ZXingScannerActivity;

import static io.github.livenlearnaday.bmzscanner.scanning.zxing.ZXingScannerActivity.uniqueCodeString;


public class CodeListActivity extends AppCompatActivity {

    @BindView(R.id.total_unique)
    TextView textUnqiueCount;

    @BindView(R.id.list_view)
    RecyclerView recyclerView;

    @BindView(R.id.bulkqr_button) Button bulkqrButton;
    @BindView(R.id.barcode_button) Button barcodeButton;
    @BindView(R.id.ocr_button) Button ocrButton;
    @BindView(R.id.flash_button) Button flashButton;


    @BindView(R.id.clear_list_button) Button clearListButton;


    CodeAdapter adapter1;

    ScannerViewModel scannerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.qrcode_list);
        ButterKnife.bind(this);


        flashButton.setVisibility(View.GONE);


        scannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);

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
                Toast.makeText(CodeListActivity.this, "code deleted", Toast.LENGTH_SHORT).show();

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

    @OnClick(R.id.ocr_button)
    public void ocrButtonClick(Button button) {


        Intent intent = new Intent(getApplicationContext(), OcrCaptureActivity.class);
        finish();
        startActivity(intent);

    }



    @OnClick(R.id.clear_list_button)
    public void clearListButtonClick(Button button) {

        uniqueCodeString.clear();
        scannerViewModel.deleteAllCodeDetail();

    }



    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        startActivity(new Intent(CodeListActivity.this, ZXingScannerActivity.class));
        finish();

    }




}
