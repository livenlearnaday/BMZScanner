package io.github.livenlearnaday.bmzscanner.scanning;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import io.github.livenlearnaday.bmzscanner.data.repository.CodeDetailRepository;

public class ScannerViewModel extends AndroidViewModel {
    private final CodeDetailRepository repository;
    private final LiveData<List<CodeDetail>> allCodeDetail;


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
