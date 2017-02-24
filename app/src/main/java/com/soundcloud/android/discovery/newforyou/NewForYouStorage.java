package com.soundcloud.android.discovery.newforyou;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.JsonFileStorage;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class NewForYouStorage {

    @VisibleForTesting
    static final String FILE_NAME = "storage_newforyou";

    private final StoreTracksCommand storeTracksCommand;
    private final JsonFileStorage fileStorage;
    private final TrackRepository trackRepository;

    @Inject
    NewForYouStorage(TrackRepository trackRepository,
                     StoreTracksCommand storeTracksCommand,
                     JsonFileStorage fileStorage) {
        this.trackRepository = trackRepository;
        this.storeTracksCommand = storeTracksCommand;
        this.fileStorage = fileStorage;
    }

    boolean storeNewForYou(ApiNewForYou apiNewForYou) {
        ModelCollection<ApiTrack> tracks = apiNewForYou.tracks();
        WriteResult writeResult = storeTracksCommand.call(tracks);
        boolean fileStorageResult = fileStorage.writeToFile(FILE_NAME, NewForYouStorageItem.fromApiNewForYou(apiNewForYou));

        return writeResult.success() && fileStorageResult;
    }

    Observable<NewForYou> newForYou() {
        return fileStorage.readFromFile(FILE_NAME, TypeToken.of(NewForYouStorageItem.class))
                          .flatMap(this::toNewForYouItem);
    }

    private Observable<NewForYou> toNewForYouItem(NewForYouStorageItem storageItem) {
        List<Urn> trackUrns = Lists.transform(storageItem.trackUrns(), urnString -> Urn.forTrack(Long.valueOf(urnString)));
        return trackRepository.trackListFromUrns(trackUrns)
                              .map(tracks -> NewForYou.create(storageItem.lastUpdated(), Urn.forNewForYou(storageItem.queryUrn()), tracks));
    }

    @VisibleForTesting
    @AutoValue
    static abstract class NewForYouStorageItem {
        @JsonProperty("lastUpdated")
        public abstract Date lastUpdated();

        @JsonProperty("queryUrn")
        public abstract String queryUrn();

        @JsonProperty("trackUrns")
        public abstract List<String> trackUrns();

        @JsonCreator
        public static NewForYouStorageItem create(@JsonProperty("lastUpdated") Date lastUpdated,
                             @JsonProperty("queryUrn") String queryUrn,
                             @JsonProperty("trackUrns") List<String> trackUrns) {
            return new AutoValue_NewForYouStorage_NewForYouStorageItem(lastUpdated,queryUrn,trackUrns);
        }

        static NewForYouStorageItem fromApiNewForYou(ApiNewForYou apiNewForYou) {
            List<String> urns = Lists.transform(apiNewForYou.tracks().getCollection(), apiTrack -> apiTrack.getUrn().getStringId());
            return create(apiNewForYou.lastUpdate(), apiNewForYou.tracks().getQueryUrn().get().getStringId(), urns);
        }
    }
}
