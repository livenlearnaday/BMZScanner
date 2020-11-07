package io.github.livenlearnaday.bmzscanner.data.source.local;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import io.github.livenlearnaday.bmzscanner.util.converter.MapTypeConverter;

@Database(entities = {CodeDetail.class}, version = 1)
@TypeConverters({MapTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract CodeDetailDao codeDetailDao();


    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "app_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallBack)
                    .build();

        }
        return instance;
    }


    private static RoomDatabase.Callback roomCallBack = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            new PopulateDbAsyncTask(instance).execute();
        }
    };

    private static class PopulateDbAsyncTask extends AsyncTask<Void, Void, Void> {
        private CodeDetailDao codeDetailDao;

        private PopulateDbAsyncTask(AppDatabase db) {
            codeDetailDao = db.codeDetailDao();
        }


        @Override
        protected Void doInBackground(Void... voids) {

            return null;
        }
    }


}
