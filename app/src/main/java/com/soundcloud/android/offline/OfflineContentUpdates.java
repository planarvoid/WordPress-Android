package com.soundcloud.android.offline;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class OfflineContentUpdates {
    public static Builder builder() {
        return new AutoValue_OfflineContentUpdates.Builder()
                .unavailableTracks(Collections.emptyList())
                .tracksToDownload(Collections.emptyList())
                .newTracksToDownload(Collections.emptyList())
                .tracksToRestore(Collections.emptyList())
                .tracksToRemove(Collections.emptyList())
                .userExpectedOfflineContent(ExpectedOfflineContent.EMPTY);
    }

    public abstract List<Urn> unavailableTracks();

    public abstract List<DownloadRequest> tracksToDownload();

    public abstract List<Urn> tracksToRestore();

    public abstract List<Urn> tracksToRemove();

    public abstract ExpectedOfflineContent userExpectedOfflineContent();

    // TODO Command stuff: useful ?
    public abstract List<Urn> newTracksToDownload();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder unavailableTracks(List<Urn> unavailableTracks);

        public abstract Builder tracksToDownload(List<DownloadRequest> tracksToDownload);

        public abstract Builder newTracksToDownload(List<Urn> newTracksToDownload);

        public abstract Builder tracksToRestore(List<Urn> tracksToRestore);

        public abstract Builder tracksToRemove(List<Urn> tracksToRemove);

        public abstract Builder userExpectedOfflineContent(ExpectedOfflineContent userExpectedOfflineContent);

        public abstract OfflineContentUpdates build();
    }
}
