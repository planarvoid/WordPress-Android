package com.soundcloud.android.db;

import static com.soundcloud.android.coreutils.log.Log.error;
import static com.soundcloud.android.coreutils.log.Log.info;
import static com.soundcloud.android.coreutils.text.Strings.allInArrayAreBlank;
import static com.soundcloud.android.db.MigrationReader.MigrationFile;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.coreutils.io.IO;
import com.soundcloud.android.coreutils.log.Log;
import org.slf4j.Logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class MigrationEnabledSQLiteOpenHelper extends SQLiteOpenHelper {
    private static final Logger LOG = Log.getLogger();

    private final Context context;
    private String name;
    private final int version;
    private MigrationReader migrationReader;
    private IO io;

    public MigrationEnabledSQLiteOpenHelper(Context context, String name, int version) {
        this(context, name, version, new MigrationReader(context), new IO(context));
    }

    @VisibleForTesting
    protected MigrationEnabledSQLiteOpenHelper(Context context, String name, int version,
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
        info(LOG, "Performing up migration from version %d to %d", fromVersion, toVersion);
        for(int nextVersion = fromVersion+1; nextVersion <= toVersion; nextVersion++){

            MigrationFile migration = migrationReader.getMigration(nextVersion);

            if(!migration.isValidMigrationFile()){
                throw new MigrationFileFormatException(nextVersion);
            }

            info(LOG, "Executing query %s", migration);
            for(String sql : migration.upMigrations()){
                db.execSQL(sql);
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int fromVersion, int toVersion) {
        info(LOG, "Performing down migration from version %d to %d", fromVersion, toVersion);
        for(int nextVersion = fromVersion; nextVersion > toVersion; nextVersion--){
            MigrationFile migration = migrationReader.getMigration(nextVersion);

            if(!migration.isValidMigrationFile()){
                throw new MigrationFileFormatException(nextVersion);
            }

            info(LOG, "Executing query %s", migration);
            for(String sql : migration.downMigrations()){
                db.execSQL(sql);
            }
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
            error(LOG, "Cannot open db file to check for version",e);
            return true;
        } finally {
            io.closeQuietly(db);
        }

    }

}
