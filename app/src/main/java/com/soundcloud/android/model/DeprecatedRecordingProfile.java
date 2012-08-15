package com.soundcloud.android.model;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.IOUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public enum DeprecatedRecordingProfile {
    UNKNOWN(-1, null),
    ENCODED_LOW(0, "ogg"),
    ENCODED_HIGH(1, "ogg"),
    RAW(2, "wav");

    final int id;
    final String updatedExtension;

    DeprecatedRecordingProfile(int id, String updatedExtension){
        this.id = id;
        this.updatedExtension = updatedExtension;
    }

    public static DeprecatedRecordingProfile getProfile(File f) {
        if (f != null) {
            try {
                final int profile = Integer.parseInt(IOUtils.extension(f));
                for (DeprecatedRecordingProfile p : DeprecatedRecordingProfile.values()) {
                    if (p.id == profile) return p;
                }
            } catch (NumberFormatException ignore) {
            }
        }
        return UNKNOWN;
    }

    public String getExtension() {
        return "." + id;
    }

    public static boolean migrateRecordings(List<Recording> recordings, final ContentResolver resolver) {
        final List<Recording> migrate = new ArrayList<Recording>();
        for (Recording r : recordings) {
            if (needsMigration(r)) migrate.add(r);
        }

        if (!migrate.isEmpty()) {
            new Thread() {
                @Override
                public void run() {
                    Log.i(SoundCloudApplication.TAG, "Deprecated recordings found, trying to migrate " + migrate.size() + " recordings");
                    ContentValues[] cv = new ContentValues[migrate.size()];
                    int i = 0;
                    for (Recording r : migrate) {
                        cv[i] = migrate(r);
                        i++;
                    }
                    int updated = resolver.bulkInsert(Content.RECORDINGS.uri, cv);
                    Log.i(SoundCloudApplication.TAG,"Finished migrating " + updated + " recordings");
                }
            }.start();
            return true;
        } else {
            return false;
        }

    }

    /**
     * Rename files, return CV for a bulk insert
     */
    public static ContentValues migrate(Recording r) {
        final DeprecatedRecordingProfile profile = DeprecatedRecordingProfile.getProfile(r.audio_path);
        if (profile != DeprecatedRecordingProfile.UNKNOWN) {
            final File newPath = IOUtils.changeExtension(r.audio_path, profile.updatedExtension);
            final long lastMod = r.audio_path.lastModified();
            if (r.audio_path.renameTo(newPath)){
                newPath.setLastModified(lastMod);
                r.audio_path = newPath;
                r.external_upload = true;

                // return content values for bulk migration
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.Recordings._ID, r.id);
                cv.put(DBHelper.Recordings.EXTERNAL_UPLOAD, r.external_upload);
                cv.put(DBHelper.Recordings.AUDIO_PATH, r.audio_path.getAbsolutePath());
                return cv;
            }
        }
        return null;
    }

    public static boolean needsMigration(Recording r) {
        if (!r.external_upload) {
            final DeprecatedRecordingProfile profile = getProfile(r.audio_path);
            return (profile != DeprecatedRecordingProfile.UNKNOWN);
        }
        return false;
    }
}
