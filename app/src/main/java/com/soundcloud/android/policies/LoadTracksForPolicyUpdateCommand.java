package com.soundcloud.android.policies;

import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class LoadTracksForPolicyUpdateCommand extends Command<Void, List<Urn>> {

    private final PropellerDatabase propeller;
    private final FeatureFlags featureFlags;

    @Inject
    public LoadTracksForPolicyUpdateCommand(PropellerDatabase database, FeatureFlags featureFlags) {
        this.propeller = database;
        this.featureFlags = featureFlags;
    }

    @Override
    public List<Urn> call(Void input) {
        if (featureFlags.isEnabled(Flag.OFFLINE_SYNC)) {
            return loadAllTracks();
        } else {
            Set<Urn> tracks = new HashSet<>();
            tracks.addAll(loadLikedTracks());
            tracks.addAll(loadPlaylistTracks());
            return new ArrayList<>(tracks);
        }
    }

    private List<Urn> loadAllTracks() {
        final Query query = Query.from(Sounds).whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
        return propeller.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> loadLikedTracks() {
        final Where whereTrackDataExists = filter()
                .whereEq(Likes.field(TableColumns.Likes._ID), Sounds.field(TableColumns.Sounds._ID))
                .whereEq(Likes.field(TableColumns.Likes._TYPE), Sounds.field(TableColumns.Sounds._TYPE));

        final Query query = Query.from(Likes.name())
                .select(field(Likes.field(TableColumns.Likes._ID)).as(BaseColumns._ID))
                .innerJoin(Sounds.name(), whereTrackDataExists)
                .whereEq(Likes.field(TableColumns.Likes._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Likes.field(TableColumns.Likes.REMOVED_AT));

        return propeller.query(query).toList(new TrackUrnMapper());
    }

    private List<Urn> loadPlaylistTracks() {
        final Query query = Query.from(PlaylistTracks.name())
                .select(field(PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID)).as(BaseColumns._ID))
                .whereNull(PlaylistTracks.field(TableColumns.PlaylistTracks.REMOVED_AT));

        return propeller.query(query).toList(new TrackUrnMapper());
    }
}
