package io.github.livenlearnaday.bmzscanner.data.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.List;

import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import io.github.livenlearnaday.bmzscanner.data.source.local.AppDatabase;
import io.github.livenlearnaday.bmzscanner.data.source.local.CodeDetailDao;

public class CodeDetailRepository {

    private final CodeDetailDao codeDetailDao;
    private final LiveData<List<CodeDetail>> allCodeDetail;


    public CodeDetailRepository(Application application) {

        AppDatabase database = AppDatabase.getInstance(application);
        codeDetailDao = database.codeDetailDao();
        allCodeDetail = codeDetailDao.getAllCodeDetail();

    }


    //apis repository exposed to other classes, creating abstraction layer
    public void insert(CodeDetail codeDetail) {
        new InsertCodeDetailAsyncTask(codeDetailDao).execute(codeDetail);
    }

    public void update(CodeDetail codeDetail) {
        new UpdateCodeDetailAsyncTask(codeDetailDao).execute(codeDetail);

    }

    public void delete(CodeDetail codeDetail) {
        new DeleteCodeDetailAsyncTask(codeDetailDao).execute(codeDetail);
    }

    public void deleteAllCodeDetail() {
        new DeleteAllCodeDetailAsyncTask(codeDetailDao).execute();
    }


    public LiveData<List<CodeDetail>> getAllCodeDetail() {
        return allCodeDetail;

    }


    private static class InsertCodeDetailAsyncTask extends AsyncTask<CodeDetail, Void, Void> {

        private final CodeDetailDao codeDetailDao;

        private InsertCodeDetailAsyncTask(CodeDetailDao codeDetailDao) {
            this.codeDetailDao = codeDetailDao;
        }

        @Nullable
        @Override
        public Void doInBackground(CodeDetail... codeDetails) {
            codeDetailDao.insert(codeDetails[0]);
            return null;
        }


    }


    private static class UpdateCodeDetailAsyncTask extends AsyncTask<CodeDetail, Void, Void> {

        private final CodeDetailDao codeDetailDao;

        private UpdateCodeDetailAsyncTask(CodeDetailDao codeDetailDao) {
            this.codeDetailDao = codeDetailDao;
        }

        @Nullable
        @Override
        public Void doInBackground(CodeDetail... codeDetails) {
            codeDetailDao.update(codeDetails[0]);
            return null;
        }


    }


    private static class DeleteCodeDetailAsyncTask extends AsyncTask<CodeDetail, Void, Void> {

        private final CodeDetailDao codeDetailDao;

        private DeleteCodeDetailAsyncTask(CodeDetailDao codeDetailDao) {
            this.codeDetailDao = codeDetailDao;
        }

        @Nullable
        @Override
        public Void doInBackground(CodeDetail... codeDetails) {
            codeDetailDao.delete(codeDetails[0]);
            return null;
        }


    }


    private static class DeleteAllCodeDetailAsyncTask extends AsyncTask<Void, Void, Void> {

        private final CodeDetailDao codeDetailDao;

        private DeleteAllCodeDetailAsyncTask(CodeDetailDao codeDetailDao) {
            this.codeDetailDao = codeDetailDao;
        }

        @Nullable
        @Override
        public Void doInBackground(Void... voids) {
            codeDetailDao.deleteAllCodeDetail();
            return null;
        }


    }


}
