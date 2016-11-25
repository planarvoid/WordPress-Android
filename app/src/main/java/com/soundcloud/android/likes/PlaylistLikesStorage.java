package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistAssociationMapper;
import com.soundcloud.android.playlists.PlaylistAssociationMapperFactory;
import com.soundcloud.android.playlists.PlaylistQueries;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class PlaylistLikesStorage {

    private final PropellerRx propellerRx;
    private final PlaylistAssociationMapper playlistAssociationMapper;

    @Inject
    public PlaylistLikesStorage(PropellerRx propellerRx,
                                PlaylistAssociationMapperFactory mapperFactory) {
        this.propellerRx = propellerRx;
        this.playlistAssociationMapper = mapperFactory.create(Tables.Likes.CREATED_AT);
    }

    public Observable<List<PlaylistAssociation>> loadLikedPlaylists(int limit, long fromTimestamp, String filter) {
        final Query query = Query.from(Tables.PlaylistView.TABLE.name())
                                 .select(Tables.PlaylistView.TABLE.name() + ".*", Tables.Likes.CREATED_AT)
                                 .innerJoin(Tables.Likes.TABLE,
                                            on(Tables.Likes._ID.name(), Tables.PlaylistView.ID.name())
                                                    .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                                 .whereLt(Tables.Likes.CREATED_AT, fromTimestamp))
                                 .groupBy(Tables.PlaylistView.ID)
                                 .order(Tables.Likes.CREATED_AT, DESC)
                                 .limit(limit);

        PlaylistQueries.addPlaylistFilterToQuery(filter, query);
        return propellerRx.query(query).map(playlistAssociationMapper).toList();
    }
}
