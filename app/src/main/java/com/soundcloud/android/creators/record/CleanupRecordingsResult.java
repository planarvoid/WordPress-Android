package com.soundcloud.android.creators.record;

import com.soundcloud.android.api.legacy.model.Recording;

import java.util.List;

public class CleanupRecordingsResult {
    final int amplitudeFilesRemoved;
    final int invalidRecordingsRemoved;
    final List<Recording> unsavedRecordings;

    public CleanupRecordingsResult(List<Recording> unsavedRecordings, int amplitudeFilesRemoved, int invalidRecordingsRemoved) {
        this.amplitudeFilesRemoved = amplitudeFilesRemoved;
        this.invalidRecordingsRemoved = invalidRecordingsRemoved;
        this.unsavedRecordings = unsavedRecordings;
    }

    @Override
    public String toString() {
        return "CleanupRecordingsResult{" +
                "amplitudeFilesRemoved=" + amplitudeFilesRemoved +
                ", invalidRecordingsRemoved=" + invalidRecordingsRemoved +
                ", unsavedRecordings=" + unsavedRecordings +
                '}';
    }
}
