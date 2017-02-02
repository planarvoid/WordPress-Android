package com.soundcloud.android.discovery.newforyou;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.JsonFileStorage;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class NewForYouStorage {

    private static final String FILE_NAME = "storage_newforyou";

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
        List<Urn> trackUrns = Lists.transform(storageItem.getTrackUrns(), urnString -> Urn.forTrack(Long.valueOf(urnString)));
        return trackRepository.trackListFromUrns(trackUrns)
                              .map(tracks -> NewForYou.create(storageItem.getLastUpdated(), Urn.forNewForYou(storageItem.getQueryUrn()), tracks));
    }

    private static class NewForYouStorageItem {
        private final Date lastUpdated;
        private final String queryUrn;
        private final List<String> trackUrns;

        @JsonCreator
        NewForYouStorageItem(@JsonProperty("lastUpdated") Date lastUpdated,
                             @JsonProperty("queryUrn") String queryUrn,
                             @JsonProperty("trackUrns") List<String> trackUrns) {
            this.lastUpdated = lastUpdated;
            this.queryUrn = queryUrn;
            this.trackUrns = trackUrns;
        }

        public Date getLastUpdated() {
            return lastUpdated;
        }

        public String getQueryUrn() {
            return queryUrn;
        }

        public List<String> getTrackUrns() {
            return trackUrns;
        }

        static NewForYouStorageItem fromApiNewForYou(ApiNewForYou apiNewForYou) {
            List<String> urns = Lists.transform(apiNewForYou.tracks().getCollection(), apiTrack -> apiTrack.getUrn().getStringId());
            return new NewForYouStorageItem(apiNewForYou.lastUpdate(), apiNewForYou.tracks().getQueryUrn().get().getStringId(), urns);
        }
    }
}
