package com.soundcloud.android.db;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.soundcloud.android.coreutils.log.Log.error;
import static com.soundcloud.android.coreutils.text.Strings.isBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.soundcloud.android.coreutils.io.IO;
import com.soundcloud.android.coreutils.log.Log;
import org.slf4j.Logger;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads migration files from the private files directory. When wanting to go to version n of the DB,
 * one should request the migration file for version n, obtain the up migration and have it executed.
 * If one wishes to downgrade to version n, then one should request the migration file
 * for version n+1, obtain the down migration and execute it.
 * <p/>
 * Important that one copies all migrations over from the apk's assets directory
 * onto the device disk before executing any migrations as in the event of a downgrade,
 * the n+1 migration will not exist in the APK. So important to copy all over
 * all migrations to support backwards migrations. Migration files are expected to be under a
 * 'migrations' directory in the assets folder in the format of #.sql
 * where # represents the version of the DB. They will be copied into the private
 * files directory under the same folder name.
 */
class MigrationReader {

    private static final String MIGRATION_FILENAME_FORMAT = "migrations/%d.sql";
    protected static final MigrationFile NO_OR_INVALID_MIGRATION = new MigrationFile();

    enum MigrationType {
        UP(1),
        DOWN(2);
        private int groupPosition;

        MigrationType(int groupPosition) {
            this.groupPosition = groupPosition;
        }

        public int groupPosition() {
            return groupPosition;
        }
    }

    private static final Logger LOG = Log.getLogger();
    private static final Pattern MIGRATION_PATTERN = Pattern.compile("\\n*--!UP!\\n(.+?)--!DOWN!\\n(.+?)", Pattern.DOTALL);

    private IO io;

    public MigrationReader(Context context) {
        this(new IO(context));
    }

    @VisibleForTesting
    protected MigrationReader(IO io) {
        this.io = io;
    }


    /**
     * Returns a MigrationFile which contains the up and down migration for a given DB version.
     * This method will never return null and in the event of an error of any kind or invalid migration file
     * the validity itself will be specified within the MigrationFile. So one must always check the
     * validity by calling isValidMigrationFile before doing any operations
     *
     * @param version The version of the migration being requested
     * @return A MigrationFile containing the up and down contents of the specified version
     */
    public MigrationFile getMigration(int version) {

        Matcher matcher = MIGRATION_PATTERN.matcher(migrationFileContents(version));

        if (!matcher.matches()) {
            error(LOG, "Migration file contents are not valid for version %d", version);
            return NO_OR_INVALID_MIGRATION;
        }

        if (matcher.groupCount() != 2) {
            error(LOG, "Both up and down migration could not be found in file for version %d", version);
            return NO_OR_INVALID_MIGRATION;
        }

        String upMigration = matcher.group(MigrationType.UP.groupPosition()).replaceAll("\n", " ").trim();
        String downMigration = matcher.group(MigrationType.DOWN.groupPosition()).replace("\n", " ").trim();
        return isBlank(upMigration) || isBlank(downMigration) ? NO_OR_INVALID_MIGRATION :
                new MigrationFile(upMigration, downMigration, version);
    }

    private CharSequence migrationFileContents(int version) {
        String migrationFilePath = String.format(MIGRATION_FILENAME_FORMAT, version);
        InputStream inputStream;
        try {
            inputStream = io.inputStreamFromPrivateDirectory(migrationFilePath);
        } catch (IOException e) {
            error(LOG, e, "Could not find migration file for ", migrationFilePath);
            return "";
        }

        try {
            return io.toString(inputStream);
        } catch (IOException e) {
            error(LOG, e, "Problem when reading in migration ", migrationFilePath);
            return "";
        } finally {
            io.closeQuietly(inputStream);
        }
    }

    /**
     * Copies migrations files located in the APKs assets directory over to the ondevice private
     * directory. This is necessary to support backwards migrations as the down migration needs to be
     * obtained from the currentVersion+1 migration file. The migration file will not be copied over
     * if one already exists in the private directory.
     *
     * @param currentVersion The version of the last file to be copied over. Must be >= 1
     * @throws MigrationCopyException If the output file in the private directory could not
     *                                be created or an error occured during copying.
     */
    public void copyMigrationFiles(int currentVersion) throws MigrationCopyException {
        checkArgument(currentVersion >= 1, "Current db version value is smaller than 1");
        for (int version = 1; version <= currentVersion; version++) {

            String migrationFilePath = String.format(MIGRATION_FILENAME_FORMAT, version);


            if (io.fileExistsInPrivateDirectory(migrationFilePath)) {
                continue;
            }

            File migrationFile;
            try {
                migrationFile = io.createFileInPrivateDirectory(migrationFilePath);
            } catch (IOException e) {
                throw new MigrationCopyException("Could not create migration file in private directory", e);
            }

            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = io.inputStreamFromAsset(migrationFilePath);
                outputStream = io.outputStreamForFile(migrationFile);
                io.copy(inputStream, outputStream);
            } catch (IOException e) {
                throw new MigrationCopyException("Problem when copying migration files", e);
            } finally {
                io.closeQuietly(inputStream, outputStream);
            }

        }
    }

    protected static class MigrationFile {
        private static final int NO_MIGRATION = -1;
        public static final Function<String, String> TRIM_FUNCTION = new Function<String, String>() {
            @Override
            public String apply(String input) {
                return input.trim();
            }
        };
        private String upMigration;
        private String downMigration;
        private Collection<String> upMigrations;
        private Collection<String> downMigrations;
        private final int version;

        protected MigrationFile(String upMigration, String downMigration, int version) {
            checkArgument(version >= 0);
            this.upMigration = upMigration;
            this.downMigration = downMigration;
            this.version = version;
        }

        protected MigrationFile() {
            version = NO_MIGRATION;
        }

        /**
         * Lazily create the collections to save memory (e.g very unlikely we will do down migrations)
         */
        public Collection<String> upMigrations() {
            checkState(isValidMigrationFile(), "Migration File is not valid");
            if (upMigrations == null) {
                upMigrations = transform(newArrayList(upMigration.split(";")), TRIM_FUNCTION);
                upMigrations = Collections.unmodifiableCollection(upMigrations);
            }
            return upMigrations;
        }

        public Collection<String> downMigrations() {
            checkState(isValidMigrationFile(), "Migration File is not valid");
            if (downMigrations == null) {
                downMigrations = transform(newArrayList(downMigration.split(";")), TRIM_FUNCTION);
                downMigrations = Collections.unmodifiableCollection(downMigrations);
            }
            return downMigrations;
        }

        public boolean isValidMigrationFile() {
            return version != NO_MIGRATION;
        }
    }

}
