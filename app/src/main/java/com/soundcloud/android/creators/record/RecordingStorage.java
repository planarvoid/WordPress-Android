package com.soundcloud.android.creators.record;

import static com.soundcloud.android.api.legacy.model.Recording.isAmplitudeFile;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.reader.WavReader;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.IOUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordingStorage {

    private final AccountOperations accountOperations;
    private final AudioDurationHelper durationHelper;

    @Inject
    RecordingStorage(AccountOperations accountOperations, AudioDurationHelper durationHelper) {
        this.accountOperations = accountOperations;
        this.durationHelper = durationHelper;
    }

    Observable<List<Recording>> cleanupRecordings(final Context context, final File recordingDir) {
        return Observable.create(observableEmitter -> {
            List<Recording> recordings = cleanupRecordings(context, recordingDir, accountOperations.getLoggedInUserUrn());
            observableEmitter.onNext(recordings);
            observableEmitter.onComplete();
        });
    }

    public static boolean delete(Context context, final Recording recording) {
        boolean deleted = false;
        if (!recording.external_upload || recording.isLegacyRecording(context) || recording.isUploadRecording(context)) {
            deleted = IOUtils.deleteFile(recording.audio_path);
        }
        IOUtils.deleteFile(recording.getEncodedFile());
        IOUtils.deleteFile(recording.getAmplitudeFile());
        IOUtils.deleteFile(recording.getImageFile(context));
        return deleted;
    }

    Completable deleteStaleUploads(final Context context, final File uploadsDirectory) {
        return Completable.create(observableEmitter -> {
            final File[] list = IOUtils.nullSafeListFiles(uploadsDirectory, null);
            for (File f : list) {
                RecordingStorage.delete(context, new Recording(f));
            }
            observableEmitter.onComplete();
        });
    }

    // this is poached from legacy code mostly. It's a bit ugly, but it is at least now tested
    private List<Recording> cleanupRecordings(Context context, File directory, Urn loggedInUserUrn) {

        Map<String, File> toCheck = new HashMap<>();
        final File[] list = IOUtils.nullSafeListFiles(directory, new Recording.RecordingFilter());
        Arrays.sort(list); // we want .wav files taking precedence, so make sure they appear last (alpha order)
        for (File f : list) {
            toCheck.put(IOUtils.removeExtension(f).getAbsolutePath(), f);
        }

        List<Recording> unsavedRecordings = new ArrayList<>();

        for (File f : toCheck.values()) {
            if (isAmplitudeFile(f.getName())) {
                IOUtils.deleteFile(f);
            } else {
                Recording recording = new Recording(f);
                recording.user_id = loggedInUserUrn.getNumericId();
                recording.duration = durationHelper.getDuration(f);

                if (recording.duration <= 0 || f.getName().contains(Recording.PROCESSED_APPEND)) {
                    delete(context, recording);
                } else {
                    unsavedRecordings.add(recording);
                }
            }
        }
        return unsavedRecordings;
    }

    public Observable<Recording> upload(final File uploadDir,
                                        final Uri stream,
                                        final String type,
                                        final ContentResolver resolver) {
        return Observable.create(observableEmitter -> {
            File file = IOUtils.getFromMediaUri(resolver, stream);
            String filename;

            if (file == null || !file.exists()) {
                try {
                    file = copyStreamToFile(uploadDir, stream, type, resolver);
                    filename = IOUtils.getFilenameFromUri(stream, resolver);
                } catch (IOException e) {
                    observableEmitter.onComplete();
                    return;
                }
            } else {
                filename = file.getName();
            }

            Recording recording = new Recording(file);
            recording.external_upload = true;
            recording.original_filename = filename;
            recording.duration = durationHelper.getDuration(file);

            observableEmitter.onNext(recording);
            observableEmitter.onComplete();
        });
    }

    @NotNull
    private File copyStreamToFile(File uploadDir,
                                  Uri stream,
                                  String type,
                                  ContentResolver resolver) throws IOException {
        File file;
        InputStream input = resolver.openInputStream(stream);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);

        if (extension == null) {
            extension = WavReader.EXTENSION;
        }

        file = new File(uploadDir, System.currentTimeMillis() + "." + extension);
        IOUtils.copy(input, file);
        return file;
    }
}
