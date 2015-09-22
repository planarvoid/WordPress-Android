package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.transform;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.ApiDateFormat;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;

import java.util.Collections;
import java.util.List;

class StationsSyncPostBody {
    private final RecentStations recent;
    private final SavedStations saved;

    StationsSyncPostBody(List<PropertySet> recentStationsToSync) {
        this.recent = new RecentStations(recentStationsToSync);
        this.saved = new SavedStations();
    }

    public RecentStations getRecent() {
        return recent;
    }

    public SavedStations getSaved() {
        return saved;
    }

    static class RecentStations {
        private final Function<PropertySet, RecentStation> toCollection = new Function<PropertySet, RecentStation>() {
            @Override
            public RecentStation apply(PropertySet recentStation) {
                return new RecentStation(
                        recentStation.get(StationProperty.URN),
                        recentStation.get(StationProperty.UPDATED_LOCALLY_AT)
                );
            }
        };

        private List<RecentStation> collection;

        public RecentStations(List<PropertySet> recentStationsToSync) {
            this.collection = transform(recentStationsToSync, toCollection);
        }

        public List<RecentStation> getCollection() {
            return collection;
        }

        class RecentStation {
            private final String urn;
            @JsonProperty("last_played") private final String lastPlayed;

            public RecentStation(Urn urn, long lastPlayed) {
                this.urn = urn.toString();
                this.lastPlayed = ApiDateFormat.formatDate(lastPlayed);
            }

            public String getUrn() {
                return urn;
            }

            public String getLastPlayed() {
                return lastPlayed;
            }
        }
    }

    // For the stations soft launch, we won't be syncing Saved Stations. However, the
    // backend requires a JSON representation that includes: {"saved": {"collection": []}}
    static class SavedStations {
        private List<SavedStation> collection = Collections.emptyList();

        public List<SavedStation> getCollection() {
            return collection;
        }

        class SavedStation {}
    }
}
