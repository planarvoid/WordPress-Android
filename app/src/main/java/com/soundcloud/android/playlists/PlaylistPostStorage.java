package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.SoundView.CREATED_AT;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class PlaylistPostStorage {

    private final PropellerRx propellerRx;
    private final CurrentDateProvider dateProvider;
    private final RemovePlaylistCommand removePlaylistCommand;
    private final PlaylistAssociationMapper playlistAssociationMapper;

    @Inject
    public PlaylistPostStorage(PropellerRx propellerRx,
                               CurrentDateProvider dateProvider,
                               RemovePlaylistCommand removePlaylistCommand,
                               PlaylistAssociationMapperFactory mapperFactory) {
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
        this.removePlaylistCommand = removePlaylistCommand;
        this.playlistAssociationMapper = mapperFactory.create(Tables.Posts.CREATED_AT);
    }

    public Observable<List<PlaylistAssociation>> loadPostedPlaylists(int limit, long fromTimestamp, String filter) {
        final Query query = buildLoadPostedPlaylistsQuery(limit, fromTimestamp);
        PlaylistQueries.addPlaylistFilterToQuery(filter, query);
        return propellerRx.query(query)
                          .map(playlistAssociationMapper)
                          .toList();
    }

    protected Query buildLoadPostedPlaylistsQuery(int limit, long fromTimestamp) {
        return Query.from(Tables.PlaylistView.TABLE.name())
                    .select(Tables.PlaylistView.TABLE.name() + ".*", Tables.Posts.CREATED_AT)
                    .innerJoin(Tables.Posts.TABLE, on(Tables.Posts.TARGET_ID, Tables.PlaylistView.ID)
                    .whereEq(Tables.Posts.TYPE, "\"" + Tables.Posts.TYPE_POST + "\"")
                    .whereEq(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                    .whereLt(Tables.Posts.CREATED_AT, fromTimestamp))
                    .groupBy(Tables.PlaylistView.ID)
                    .order(CREATED_AT, DESC)
                    .limit(limit);
    }

    Observable<TxnResult> markPendingRemoval(final Urn urn) {
        return propellerRx.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.update(
                        Tables.Sounds.TABLE,
                        ContentValuesBuilder.values(1)
                                            .put(Tables.Sounds.REMOVED_AT, dateProvider.getCurrentTime())
                                            .get(),
                        filter().whereEq(Tables.Sounds._ID, urn.getNumericId())
                                .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                ));

                removePlaylistFromAssociatedViews(propeller);
            }

            private void removePlaylistFromAssociatedViews(PropellerDatabase propeller) {
                step(propeller.delete(Table.Activities, filter()
                        .whereEq(TableColumns.Activities.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.Activities.SOUND_ID, urn.getNumericId())));

                step(propeller.delete(Table.SoundStream, filter()
                        .whereEq(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(TableColumns.SoundStream.SOUND_ID, urn.getNumericId())));
            }
        });
    }

    Observable<TxnResult> remove(Urn urn) {
        return removePlaylistCommand.toObservable(urn);
    }
}
