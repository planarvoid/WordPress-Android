package com.soundcloud.android.search;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class LoadPlaylistLikedStatuses extends LegacyCommand<List<PropertySet>, List<PropertySet>, LoadPlaylistLikedStatuses> {

    private static final String COLUMN_IS_LIKED = "is_liked";

    private final PropellerDatabase propeller;

    @Inject
    public LoadPlaylistLikedStatuses(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return propeller.query(forLikes(input)).toList(new PlaylistLikeMapper());
    }

    private Query forLikes(List<PropertySet> input) {
        final Query isLiked = Query.from(Table.Likes.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, Table.Likes.name() + "." + TableColumns.Likes._ID)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(TableColumns.Likes.REMOVED_AT);

        return Query.from(Table.SoundView.name())
                .select(TableColumns.SoundView._ID, exists(isLiked).as(COLUMN_IS_LIKED))
                .whereIn(TableColumns.SoundView._ID, getPlaylistIds(input))
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private List<Long> getPlaylistIds(List<PropertySet> propertySets) {
        final List<Long> playlistIds = new ArrayList<>(propertySets.size());
        for (PropertySet set : propertySets) {
            final Urn urn = set.getOrElse(PlayableProperty.URN, Urn.NOT_SET);
            if (urn.isPlaylist()) {
                playlistIds.add(urn.getNumericId());
            }
        }
        return playlistIds;
    }

    private static final class PlaylistLikeMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader reader) {
            return PropertySet.from(
                    PlayableProperty.URN.bind(Urn.forPlaylist(reader.getLong(TableColumns.SoundView._ID))),
                    PlayableProperty.IS_LIKED.bind(reader.getBoolean(COLUMN_IS_LIKED)));
        }
    }
}
