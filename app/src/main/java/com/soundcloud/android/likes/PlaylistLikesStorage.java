package com.soundcloud.android.likes;

import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistAssociationMapper;
import com.soundcloud.android.playlists.PlaylistAssociationMapperFactory;
import com.soundcloud.android.playlists.PlaylistQueries;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
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
        this.playlistAssociationMapper = mapperFactory.create(TableColumns.Likes.CREATED_AT);
    }

    public Observable<List<PlaylistAssociation>> loadLikedPlaylists(int limit, long fromTimestamp, String filter) {
        final Query query = Query.from(Tables.PlaylistView.TABLE.name())
                                 .select(Tables.PlaylistView.TABLE.name() + ".*",
                                         field(Likes.field(TableColumns.Likes.CREATED_AT)).as(TableColumns.Likes.CREATED_AT))
                                 .innerJoin(Table.Likes.name(),
                                            on(Table.Likes.field(TableColumns.Likes._ID), Tables.PlaylistView.ID.name())
                                                    .whereEq(Table.Likes.field(TableColumns.Likes._TYPE),
                                                             TableColumns.Sounds.TYPE_PLAYLIST))
                                 .whereLt(Table.Likes.field(TableColumns.Likes.CREATED_AT), fromTimestamp)
                                 .groupBy(Tables.PlaylistView.ID)
                                 .order(Likes.field(CREATED_AT), DESC)
                                 .limit(limit);

        PlaylistQueries.addPlaylistFilterToQuery(filter, query);
        return propellerRx.query(query).map(playlistAssociationMapper).toList();
    }
}
