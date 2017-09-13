package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Observable;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.List;

public class PostsStorage {

    private final PropellerRxV2 propellerRx;
    private final CurrentDateProvider dateProvider;
    private final RemovePlaylistCommand removePlaylistCommand;

    @Inject
    public PostsStorage(PropellerRxV2 propellerRx,
                        CurrentDateProvider dateProvider,
                        RemovePlaylistCommand removePlaylistCommand) {
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
        this.removePlaylistCommand = removePlaylistCommand;
    }

    public Single<List<Association>> loadPostedPlaylists(int limit, long fromTimestamp) {
        return propellerRx.queryResult(playlistPostQuery(limit, fromTimestamp))
                          .map(queryResult -> queryResult.toList(cursorReader -> new Association(Urn.forPlaylist(cursorReader.getLong(Tables.Posts.TARGET_ID)),
                                                                                                              cursorReader.getDateFromTimestamp(Tables.Posts.CREATED_AT)))).singleOrError();
    }

    private static Query playlistPostQuery(int limit, long fromTimestamp) {
        return Query.from(Tables.Posts.TABLE)
                    .select(Tables.Posts.TARGET_ID, Tables.Posts.CREATED_AT)
                    .whereNull(Tables.Posts.REMOVED_AT)
                    .whereEq(Tables.Posts.TYPE, Tables.Posts.TYPE_POST)
                    .whereEq(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                    .whereLt(Tables.Posts.CREATED_AT, fromTimestamp)
                    .order(Tables.Posts.CREATED_AT, Query.Order.DESC)
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

                step(propeller.delete(Tables.Posts.TABLE,filter()
                             .whereEq(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST)
                             .whereEq(Tables.Posts.TARGET_ID, urn.getNumericId())));

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
        return RxJava.toV2Observable(removePlaylistCommand.toObservable(urn));
    }
}
