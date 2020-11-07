package io.github.livenlearnaday.bmzscanner.scanning;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.livenlearnaday.bmzscanner.R;
import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import io.github.livenlearnaday.bmzscanner.data.repository.CodeDetailRepository;

public class ScannerViewModel extends AndroidViewModel {
    private CodeDetailRepository repository;
    private LiveData<List<CodeDetail>> allCodeDetail;


    public ScannerViewModel(@NonNull Application application) {
        super(application);
        repository = new CodeDetailRepository(application);
        allCodeDetail = repository.getAllCodeDetail();


    }


    public void insert(CodeDetail codeDetail) {
        repository.insert(codeDetail);

    }


    public void update(CodeDetail codeDetail) {
        repository.update(codeDetail);

    }

    public void delete(CodeDetail codeDetail) {
        repository.delete(codeDetail);

    }


    public void deleteAllCodeDetail() {
        repository.deleteAllCodeDetail();

    }


    public LiveData<List<CodeDetail>> getAllCodeDetail() {
        return allCodeDetail;

    }



}
