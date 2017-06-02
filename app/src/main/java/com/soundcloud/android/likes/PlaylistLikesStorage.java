package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistAssociationMapper;
import com.soundcloud.android.playlists.PlaylistAssociationMapperFactory;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Observable;

import javax.inject.Inject;
import java.util.List;

public class PlaylistLikesStorage {

    private final PropellerRxV2 propellerRx;
    private final PlaylistAssociationMapper playlistAssociationMapper;

    @Inject
    PlaylistLikesStorage(PropellerRxV2 propellerRx,
                         PlaylistAssociationMapperFactory mapperFactory) {
        this.propellerRx = propellerRx;
        this.playlistAssociationMapper = mapperFactory.create(Tables.Likes.CREATED_AT);
    }

    public Observable<List<PlaylistAssociation>> loadLikedPlaylists(int limit, long fromTimestamp) {
        final Query query = Query.from(Tables.PlaylistView.TABLE.name())
                                 .select(Tables.PlaylistView.TABLE.name() + ".*", Tables.Likes.CREATED_AT)
                                 .innerJoin(Tables.Likes.TABLE,
                                            on(Tables.Likes._ID.name(), Tables.PlaylistView.ID.name())
                                                    .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                                                    .whereLt(Tables.Likes.CREATED_AT, fromTimestamp))
                                 .groupBy(Tables.PlaylistView.ID)
                                 .order(Tables.Likes.CREATED_AT, DESC)
                                 .limit(limit);

        return propellerRx.queryResult(query)
                          .map(result -> result.toList(playlistAssociationMapper));
    }
}
