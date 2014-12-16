package com.soundcloud.android.db;

import static com.soundcloud.android.db.MigrationReader.MigrationFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public class MigrationFileTest {

    private MigrationFile migrationFile;

    @Test
    public void shouldAllowForSingleMigrationStrings() {
        migrationFile = new MigrationFile("up", "down", 1);
        assertThat(migrationFile.upMigrations().size(), is(1));
        assertThat(migrationFile.upMigrations().contains("up"), is(true));
        assertThat(migrationFile.downMigrations().size(), is(1));
        assertThat(migrationFile.downMigrations().contains("down"), is(true));
    }

    @Test
    public void shouldAllowForMultipleMigrationStrings() {
        migrationFile = new MigrationFile("up1;up2", "down1;down2", 1);
        assertThat(migrationFile.upMigrations().size(), is(2));
        assertThat(migrationFile.upMigrations().contains("up1"), is(true));
        assertThat(migrationFile.upMigrations().contains("up2"), is(true));
        assertThat(migrationFile.downMigrations().size(), is(2));
        assertThat(migrationFile.downMigrations().contains("down1"), is(true));
        assertThat(migrationFile.downMigrations().contains("down2"), is(true));
    }

    @Test
    public void shouldTrimWhiteSpacesInMigrations() {
        migrationFile = new MigrationFile("  up1 ;  up2", "down1  ;  down2", 1);
        assertThat(migrationFile.upMigrations().contains("up1"), is(true));
        assertThat(migrationFile.upMigrations().contains("up2"), is(true));
        assertThat(migrationFile.downMigrations().contains("down1"), is(true));
        assertThat(migrationFile.downMigrations().contains("down2"), is(true));
    }

    @Test
    public void shouldSpecifyValidMigrationFile() {
        migrationFile = new MigrationFile("up", "down", 1);
        assertThat(migrationFile.isValidMigrationFile(), is(true));
    }

    @Test
    public void shouldSpecifyInvalidMigrationFile() {
        migrationFile = new MigrationFile();
        assertThat(migrationFile.isValidMigrationFile(), is(false));
    }

    @Test
    public void shouldNotRecreateMigrationCollectionsWhenRequestingThemSecondTimeAround() {
        migrationFile = new MigrationFile("up", "down", 1);
        assertThat(migrationFile.upMigrations() == migrationFile.upMigrations(), is(true));
        assertThat(migrationFile.downMigrations() == migrationFile.downMigrations(), is(true));

    }
}
