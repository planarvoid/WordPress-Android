package com.soundcloud.android.db;

import static com.soundcloud.android.coreutils.log.Logger.error;
import static com.soundcloud.android.coreutils.log.Logger.info;
import static com.soundcloud.android.coreutils.text.Strings.allInArrayAreBlank;
import static com.soundcloud.android.db.MigrationReader.MigrationFile;
import static com.soundcloud.android.db.MigrationReader.MigrationType;

import com.soundcloud.android.coreutils.io.IO;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Collection;

public abstract class MigrationEnabledSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "Migrator";

    private final Context context;
    private final String name;
    private final int version;
    private final MigrationReader migrationReader;
    private final IO io;


    public MigrationEnabledSQLiteOpenHelper(Context context, String name, int version,
                                               MigrationReader migrationReader, IO io) {
        super(context, name, null, version);
        this.context = context;
        this.name = name;
        this.version = version;
        this.migrationReader = migrationReader;
        this.io = io;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, version);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int fromVersion, int toVersion) {
        info(TAG, "Performing up migration from version %d to %d", fromVersion, toVersion);
        for(int nextVersion = fromVersion+1; nextVersion <= toVersion; nextVersion++){
            runMigrationsForVersion(db, nextVersion, MigrationType.UP);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int fromVersion, int toVersion) {
        info(TAG, "Performing down migration from version %d to %d", fromVersion, toVersion);
        for(int nextVersion = fromVersion; nextVersion > toVersion; nextVersion--){
            runMigrationsForVersion(db, nextVersion, MigrationType.DOWN);
        }
    }

    private void runMigrationsForVersion(SQLiteDatabase db, int nextVersion, MigrationType migrationType) {
        MigrationFile migration = migrationReader.getMigration(nextVersion);

        if(!migration.isValidMigrationFile()){
            throw new MigrationFileFormatException(nextVersion);
        }

        Collection<String> migrations = MigrationType.UP.equals(migrationType) ?
                migration.upMigrations() : migration.downMigrations();

        Log.d(TAG, String.format("Executing query %s", migration));
        for(String sql : migrations){
            db.execSQL(sql);
        }
    }

    public boolean requiresMigrationOfExistingDb(){
        //If we dont have a database file then onUpgrade/onDowngrade will not get called
        //Android will just call the onCreate method for completely new db's. So we return
        //true but will do the migration in onCreate
        if(allInArrayAreBlank(context.databaseList())){
            return true;
        }

        SQLiteDatabase db = null;
        try{
            db = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
            return db.getVersion() != version;
        } catch(SQLiteException e){
            error(TAG, "Cannot open db file to check for version", e);
            return true;
        } finally {
            io.closeQuietly(db);
        }

    }

}
