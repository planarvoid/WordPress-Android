package com.soundcloud.android.db;

import static com.soundcloud.android.db.MigrationReader.MigrationFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.coreutils.io.IO;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class MigrationReaderTest {

    private MigrationReader migrationReader;
    @Mock private IO io;
    @Mock private File file;
    @Mock private InputStream in;
    @Mock private OutputStream out;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        migrationReader = new MigrationReader(io);
    }

    @Test
    public void shouldRetrieveMigrationsForValidMigrationFileWithNoNewlines() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/1.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("1.sql"));
        MigrationFile migrationFile = migrationReader.getMigration(1);
        assertThat(migrationFile.isValidMigrationFile(), is(true));
        Collection<String> upMigrations = migrationFile.upMigrations();
        Collection<String> downMigrations = migrationFile.downMigrations();
        assertThat(upMigrations.size(), is(1));
        assertThat(downMigrations.size(), is(1));
        assertThat(upMigrations.contains("goodupmigration"), is(true));
        assertThat(downMigrations.contains("gooddownmigration"), is(true));
    }

    @Test
    public void shouldStripNewLineCharsFromMigration() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/8.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("8.sql"));
        MigrationFile migrationFile = migrationReader.getMigration(8);
        assertThat(migrationFile.isValidMigrationFile(), is(true));
        Collection<String> upMigrations = migrationFile.upMigrations();
        Collection<String> downMigrations = migrationFile.downMigrations();
        assertThat(upMigrations.size(), is(1));
        assertThat(downMigrations.size(), is(2));
        assertThat(upMigrations.contains("migration1 migration2"), is(true));
        assertThat(downMigrations.contains("migration3"), is(true));
        assertThat(downMigrations.contains("migration4"), is(true));
    }

    @Test
    public void shouldRetrieveMigrationsForValidMigrationFileWithNewlines() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/2.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("2.sql"));
        MigrationFile migrationFile = migrationReader.getMigration(2);
        assertThat(migrationFile.isValidMigrationFile(), is(true));
        Collection<String> upMigrations = migrationFile.upMigrations();
        Collection<String> downMigrations = migrationFile.downMigrations();
        assertThat(upMigrations.size(), is(1));
        assertThat(downMigrations.size(), is(1));
        assertThat(upMigrations.contains("goodupmigration"), is(true));
        assertThat(downMigrations.contains("gooddownmigration"), is(true));
    }


    @Test
    public void shouldReturnInvalidMigrationFileThatDoesNotExist() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/99.sql")).thenThrow(IOException.class);
        MigrationFile migration = migrationReader.getMigration(99);
        assertThat(migration.isValidMigrationFile(), is(false));
    }

    @Test
    public void shouldReturnInvalidMigrationFileForMigrationWhichHasNoUpMigration() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/3.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("3.sql"));
        MigrationFile migration = migrationReader.getMigration(3);
        assertThat(migration.isValidMigrationFile(), is(false));
    }

    @Test
    public void shouldReturnInvalidMigrationFileForMigrationWhichHasNoDownMigration() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/4.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("4.sql"));
        MigrationFile migration = migrationReader.getMigration(4);
        assertThat(migration.isValidMigrationFile(), is(false));
    }

    @Test
    public void shouldReturnInvalidMigrationFileForMigrationWhichHasNoMigrations() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/5.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("5.sql"));
        MigrationFile migration = migrationReader.getMigration(5);
        assertThat(migration.isValidMigrationFile(), is(false));
    }

    @Test
    public void shouldReturnInvalidMigrationFileIfMigrationFileDoesNotContainDownMigrationHeader() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/6.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("6.sql"));
        MigrationFile migration = migrationReader.getMigration(6);
        assertThat(migration.isValidMigrationFile(), is(false));
    }

    @Test
    public void shouldReturnInvalidMigrationFileIfMigrationFileDoesNotContainUpMigrationHeader() throws IOException {
        when(io.inputStreamFromPrivateDirectory("migrations/7.sql")).thenReturn(in);
        when(io.toString(in)).thenReturn(getTestMigrationFileContents("7.sql"));
        MigrationFile migration = migrationReader.getMigration(7);
        assertThat(migration.isValidMigrationFile(), is(false));
    }

    @Test
    public void shouldCreateNewFileInPrivateFileDirectoryWhenCopyingMigrations() throws IOException {
        when(io.fileExistsInPrivateDirectory("migrations/1.sql")).thenReturn(false);
        migrationReader.copyMigrationFiles(1);
        verify(io).createFileInPrivateDirectory("migrations/1.sql");
    }

    @Test
    public void shouldDoNothingIfFileAlreadyExistsInPrivateDirectoryWhenCopyingMigrations() throws IOException {
        when(io.fileExistsInPrivateDirectory("migrations/1.sql")).thenReturn(true);
        migrationReader.copyMigrationFiles(1);
        verify(io).fileExistsInPrivateDirectory(anyString());
        verifyNoMoreInteractions(io);
    }

    @Test(expected = MigrationCopyException.class)
    public void shouldThrowExceptionIfCouldNotCreateNewFileInPrivateDirectoryWhenCopyingMigrations() throws IOException {
        when(io.fileExistsInPrivateDirectory("migrations/1.sql")).thenReturn(false);
        when(io.createFileInPrivateDirectory(anyString())).thenThrow(IOException.class);
        migrationReader.copyMigrationFiles(1);
    }

    @Test
    public void shouldCopyFileContentsOverToPrivateDirectoryWhenCopyingMigrations() throws IOException {
        when(io.fileExistsInPrivateDirectory("migrations/1.sql")).thenReturn(false);
        when(io.createFileInPrivateDirectory("migrations/1.sql")).thenReturn(file);
        when(io.inputStreamFromAsset("migrations/1.sql")).thenReturn(in);
        when(io.outputStreamForFile(file)).thenReturn(out);
        migrationReader.copyMigrationFiles(1);
        verify(io).copy(in, out);
    }

    @Test
    public void shouldCopyMultipleFileContentsOverToPrivateDirectoryWhenCopyingMigrations() throws IOException {
        when(io.fileExistsInPrivateDirectory(matches("migrations/[12].sql"))).thenReturn(false);
        when(io.createFileInPrivateDirectory(matches("migrations/[12].sql"))).thenReturn(file);
        when(io.inputStreamFromAsset(matches("migrations/[12].sql"))).thenReturn(in);
        when(io.outputStreamForFile(file)).thenReturn(out);
        migrationReader.copyMigrationFiles(2);
        verify(io, times(2)).copy(in, out);
    }

    @Test
    public void shouldCloseStreamsWhenCopyingMultipleMigrationFiles() throws IOException {
        when(io.fileExistsInPrivateDirectory(matches("migrations/[12].sql"))).thenReturn(false);
        when(io.createFileInPrivateDirectory(matches("migrations/[12].sql"))).thenReturn(file);
        when(io.inputStreamFromAsset(matches("migrations/[12].sql"))).thenReturn(in);
        when(io.outputStreamForFile(file)).thenReturn(out);
        migrationReader.copyMigrationFiles(2);
        verify(io, times(2)).closeQuietly(in, out);
    }

    @Test
    public void shouldCloseStreamsOnceFinishedCopyingFilesInMigrations() throws IOException {
        when(io.fileExistsInPrivateDirectory("migrations/1.sql")).thenReturn(false);
        when(io.createFileInPrivateDirectory("migrations/1.sql")).thenReturn(file);
        when(io.inputStreamFromAsset("migrations/1.sql")).thenReturn(in);
        when(io.outputStreamForFile(file)).thenReturn(out);
        migrationReader.copyMigrationFiles(1);
        verify(io).closeQuietly(in, out);
    }

    @Test(expected = MigrationCopyException.class)
    public void shouldThrowExceptionIfErrorOccursWhenCopyingMigrationFiles() throws IOException {
        when(io.fileExistsInPrivateDirectory("migrations/1.sql")).thenReturn(false);
        when(io.createFileInPrivateDirectory("migrations/1.sql")).thenReturn(file);
        when(io.inputStreamFromAsset("migrations/1.sql")).thenReturn(in);
        when(io.outputStreamForFile(file)).thenReturn(out);
        doThrow(IOException.class).when(io).copy(in, out);
        migrationReader.copyMigrationFiles(1);
    }

    @Test
    public void shouldCloseStreamsIfErrorOccursWhenCopyingFilesInMigrations() throws IOException {
        when(io.fileExistsInPrivateDirectory("migrations/1.sql")).thenReturn(false);
        when(io.createFileInPrivateDirectory("migrations/1.sql")).thenReturn(file);
        when(io.inputStreamFromAsset("migrations/1.sql")).thenReturn(in);
        when(io.outputStreamForFile(file)).thenReturn(out);
        doThrow(IOException.class).when(io).copy(in, out);
        try{
            migrationReader.copyMigrationFiles(1);
            fail("Should have thrown exception");
        } catch (MigrationCopyException e ){
            //Expected
        }

        verify(io).closeQuietly(in, out);
    }

    private String getTestMigrationFileContents(String filename) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(filename));
    }
}
