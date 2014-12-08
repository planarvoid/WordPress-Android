package com.soundcloud.android.db;

import static com.soundcloud.android.db.MigrationReader.MigrationFile;
import static edu.emory.mathcs.backport.java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.coreutils.io.IO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MigrationEnabledSQLiteOpenHelperTest {

    private static final int VERSION = 1;
    public static final String DB_NAME = "mydb";
    private MigrationEnabledSQLiteOpenHelper migrationEnabledSQLiteOpenHelper;

    @Mock private SQLiteDatabase database;
    @Mock private MigrationReader migrationReader;
    @Mock private Context context;
    @Mock private MigrationFile migrationFile;
    @Mock private IO io;

    @Before
    public void setUp() {
        initMocks(this);
        migrationEnabledSQLiteOpenHelper = new MigrationEnabledSQLiteOpenHelper(context, DB_NAME, VERSION, migrationReader, io){
            @Override
            public void onCreate(SQLiteDatabase db) {}
        };
    }

    @Test
    public void shouldPerformSingleUpMigrationFromOldVersionToNew(){
        when(migrationFile.isValidMigrationFile()).thenReturn(true);
        when(migrationReader.getMigration(1)).thenReturn(migrationFile);

        when(migrationFile.upMigrations()).thenReturn(singleton("sql1"));
        migrationEnabledSQLiteOpenHelper.onUpgrade(database, 0, 1);

        InOrder inOrder = inOrder(database);
        inOrder.verify(database).execSQL("sql1");
    }

    @Test
    public void shouldPerformMultipleUpMigrationFromOldVersionToNew(){
        when(migrationFile.isValidMigrationFile()).thenReturn(true);
        when(migrationReader.getMigration(2)).thenReturn(migrationFile);
        when(migrationReader.getMigration(3)).thenReturn(migrationFile);
        when(migrationReader.getMigration(4)).thenReturn(migrationFile);

        when(migrationFile.upMigrations()).thenReturn(singleton("sql2"), singleton("sql3"), singleton("sql4"));
        migrationEnabledSQLiteOpenHelper.onUpgrade(database, 1, 4);

        InOrder inOrder = inOrder(database);
        inOrder.verify(database).execSQL("sql2");
        inOrder.verify(database).execSQL("sql3");
        inOrder.verify(database).execSQL("sql4");
    }

    @Test
    public void shouldPerformSingleDownMigrationFromNewVersionToOld(){
        when(migrationFile.isValidMigrationFile()).thenReturn(true);
        when(migrationReader.getMigration(2)).thenReturn(migrationFile);

        when(migrationFile.downMigrations()).thenReturn(singleton("sql2"));
        migrationEnabledSQLiteOpenHelper.onDowngrade(database, 2, 1);

        InOrder inOrder = inOrder(database);
        inOrder.verify(database).execSQL("sql2");
    }

    @Test
    public void shouldPerformMultipleDownMigrationFromNewVersionToOld(){
        when(migrationFile.isValidMigrationFile()).thenReturn(true);
        when(migrationReader.getMigration(4)).thenReturn(migrationFile);
        when(migrationReader.getMigration(3)).thenReturn(migrationFile);
        when(migrationReader.getMigration(2)).thenReturn(migrationFile);

        when(migrationFile.downMigrations()).thenReturn(singleton("sql4"), singleton("sql3"), singleton("sql2"));
        migrationEnabledSQLiteOpenHelper.onDowngrade(database, 4, 1);

        InOrder inOrder = inOrder(database);
        inOrder.verify(database).execSQL("sql4");
        inOrder.verify(database).execSQL("sql3");
        inOrder.verify(database).execSQL("sql2");
    }

    @Test(expected = MigrationFileFormatException.class)
    public void shouldRaiseExceptionOnUpMigrationIfInvalidMigrationReturnedByReader(){
        when(migrationReader.getMigration(4)).thenReturn(MigrationReader.NO_OR_INVALID_MIGRATION);

        migrationEnabledSQLiteOpenHelper.onUpgrade(database, 3, 4);
    }

    @Test
    public void shouldRequireMigrationIfNoDatabasesExist(){
        when(context.databaseList()).thenReturn(null);
        assertThat(migrationEnabledSQLiteOpenHelper.requiresMigrationOfExistingDb(), is(true));
    }

    @Test
    public void shouldNotRequireMigrationIfDatabasesListEmpty(){
        when(context.databaseList()).thenReturn(new String[]{});
        assertThat(migrationEnabledSQLiteOpenHelper.requiresMigrationOfExistingDb(), is(true));
    }

    @Test
    public void shouldNotRequireMigrationIfDatabaseExistsButVersionIsTheSame(){
        when(context.databaseList()).thenReturn(new String[]{DB_NAME});
        when(context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)).thenReturn(database);
        when(database.getVersion()).thenReturn(VERSION);
        assertThat(migrationEnabledSQLiteOpenHelper.requiresMigrationOfExistingDb(), is(false));
    }

    @Test
    public void shouldRequireMigrationIfDatabaseExistsButVersionIsHigher(){
        when(context.databaseList()).thenReturn(new String[]{DB_NAME});
        when(context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)).thenReturn(database);
        when(database.getVersion()).thenReturn(VERSION+1);
        assertThat(migrationEnabledSQLiteOpenHelper.requiresMigrationOfExistingDb(), is(true));
    }

    @Test
    public void shouldRequireMigrationIfDatabaseExistsButVersionIsLower(){
        when(context.databaseList()).thenReturn(new String[]{DB_NAME});
        when(context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)).thenReturn(database);
        when(database.getVersion()).thenReturn(VERSION-1);
        assertThat(migrationEnabledSQLiteOpenHelper.requiresMigrationOfExistingDb(), is(true));
    }

    @Test
    public void shouldCloseDatabaseAfterOpeningIt(){
        when(context.databaseList()).thenReturn(new String[]{DB_NAME});
        when(context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)).thenReturn(database);
        migrationEnabledSQLiteOpenHelper.requiresMigrationOfExistingDb();
        verify(io).closeQuietly(database);
    }

    @Test
    public void shouldCloseDatabaseWhenExceptionIsThrown(){
        when(context.databaseList()).thenReturn(new String[]{DB_NAME});
        when(context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null)).thenReturn(database);
        when(database.getVersion()).thenThrow(SQLException.class);
        try{
            migrationEnabledSQLiteOpenHelper.requiresMigrationOfExistingDb();
            fail("We should have thrown an exception");
        }catch(Exception e){
            //Ignore expected exception
        }
        verify(io).closeQuietly(database);
    }

}
