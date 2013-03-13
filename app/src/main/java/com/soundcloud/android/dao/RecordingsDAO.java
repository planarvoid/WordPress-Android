package com.soundcloud.android.dao;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;

public class RecordingsDAO {

    public static Uri insert(Recording recording, ContentResolver contentResolver) {
        return insert(recording, contentResolver, true);
    }

    public static Uri insert(Recording recording, ContentResolver contentResolver, boolean fullValues) {
        recording.insertDependencies(contentResolver);
        // insert parent resource, with possible partial values
        return contentResolver.insert(recording.toUri(), fullValues ? recording.buildContentValues() : recording.buildBaseContentValues());
    }

    public static boolean updateStatus(Recording recording, ContentResolver resolver) {
        if (recording.id > 0) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.Recordings.UPLOAD_STATUS, recording.upload_status);
            cv.put(DBHelper.Recordings.AUDIO_PATH, recording.audio_path.getAbsolutePath());
            return resolver.update(recording.toUri(), cv, null, null) > 0;
        } else {
            return false;
        }
    }

    public static boolean delete(Recording recording, @Nullable ContentResolver resolver) {
        boolean deleted = false;
        if (!recording.external_upload || recording.isLegacyRecording()) {
            deleted = IOUtils.deleteFile(recording.audio_path);
        }
        IOUtils.deleteFile(recording.getEncodedFile());
        IOUtils.deleteFile(recording.getAmplitudeFile());
        if (recording.id > 0 && resolver != null) resolver.delete(recording.toUri(), null, null);
        return deleted;
    }

    public static @Nullable Recording getRecordingByUri(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        Recording recording = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            recording = new Recording(cursor);
        }
        if (cursor != null) cursor.close();
        return recording;
    }

    public static @Nullable Recording getRecordingByPath(ContentResolver resolver, File file) {
        Cursor cursor = resolver.query(Content.RECORDINGS.uri,
                null,
                DBHelper.Recordings.AUDIO_PATH + " LIKE ?",
                new String[]{ IOUtils.removeExtension(file).getAbsolutePath() + "%" },
                null);

        Recording recording = null;
        if (cursor != null && cursor.moveToFirst()) {
            recording = new Recording(cursor);
        }
        if (cursor != null) cursor.close();

        return recording;
    }
}
