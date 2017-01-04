package com.soundcloud.android.associations;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.BaseRxResultMapper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Posts;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

class RepostStorage {

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    private final BaseRxResultMapper<Urn> repostsByUrnMapper = new BaseRxResultMapper<Urn>() {
        @Override
        public Urn map(CursorReader reader) {
            return readSoundUrn(reader, Posts.TARGET_ID, Posts.TARGET_TYPE);
        }
    };

    @Inject
    RepostStorage(PropellerDatabase propeller, PropellerRx propellerRx, CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    Observable<List<Urn>> loadReposts() {
        return propellerRx.query(Query.from(Posts.TABLE)
                                      .select(Posts.TARGET_TYPE, Posts.TARGET_ID)
                                      .whereEq(Posts.TYPE, Posts.TYPE_REPOST))
                          .map(repostsByUrnMapper).toList();
    }

    Command<Urn, Integer> addRepost() {
        return new WriteStorageCommand<Urn, WriteResult, Integer>(propeller) {
            private int updatedRepostCount;

            @Override
            public WriteResult write(PropellerDatabase propeller, final Urn urn) {
                updatedRepostCount = obtainNewRepostCount(propeller, urn, true);

                return propeller.runTransaction(new PropellerDatabase.Transaction() {
                    @Override
                    public void steps(PropellerDatabase propeller) {
                        step(updateRepostCount(propeller, urn, updatedRepostCount));
                        step(propeller.insert(Posts.TABLE, buildContentValuesForRepost(urn)));
                    }
                });
            }

            @Override
            protected Integer transform(WriteResult result) {
                return updatedRepostCount;
            }
        };
    }

    Command<Urn, Integer> removeRepost() {
        return new WriteStorageCommand<Urn, WriteResult, Integer>(propeller) {
            private int updatedRepostCount;

            @Override
            public WriteResult write(PropellerDatabase propeller, final Urn urn) {
                updatedRepostCount = obtainNewRepostCount(propeller, urn, false);

                final Where whereClause = filter()
                        .whereEq(Posts.TARGET_ID, urn.getNumericId())
                        .whereEq(Posts.TARGET_TYPE, urn.isTrack() ? Tables.Sounds.TYPE_TRACK : Tables.Sounds.TYPE_PLAYLIST)
                        .whereEq(Posts.TYPE, Posts.TYPE_REPOST);

                return propeller.runTransaction(new PropellerDatabase.Transaction() {
                    @Override
                    public void steps(PropellerDatabase propeller) {
                        step(updateRepostCount(propeller, urn, updatedRepostCount));
                        step(propeller.delete(Posts.TABLE, whereClause));
                    }
                });
            }

            @Override
            protected Integer transform(WriteResult result) {
                return updatedRepostCount;
            }
        };
    }

    private ChangeResult updateRepostCount(PropellerDatabase propeller, Urn urn, int repostCount) {
        return propeller.update(Tables.Sounds.TABLE, ContentValuesBuilder.values()
                                                                  .put(Tables.Sounds.REPOSTS_COUNT, repostCount).get(),
                                filter().whereEq(Tables.Sounds._ID, urn.getNumericId())
                                        .whereEq(Tables.Sounds._TYPE, getSoundType(urn)));
    }

    private ContentValues buildContentValuesForRepost(Urn urn) {
        final ContentValuesBuilder values = ContentValuesBuilder.values(4);
        values.put(Posts.TYPE, Posts.TYPE_REPOST);
        values.put(Posts.TARGET_TYPE, urn.isTrack() ? Tables.Sounds.TYPE_TRACK : Tables.Sounds.TYPE_PLAYLIST);
        values.put(Posts.TARGET_ID, urn.getNumericId());
        values.put(Posts.CREATED_AT, dateProvider.getCurrentDate().getTime());
        return values.get();
    }

    private int obtainNewRepostCount(PropellerDatabase propeller, Urn targetUrn, boolean addRepost) {
        int count = propeller.query(from(Table.SoundView.name())
                                            .select(SoundView.REPOSTS_COUNT)
                                            .whereEq(SoundView._ID, targetUrn.getNumericId())
                                            .whereEq(SoundView._TYPE, getSoundType(targetUrn)))
                             .first(Integer.class);
        return addRepost ? count + 1 : count - 1;
    }

    private int getSoundType(Urn targetUrn) {
        return targetUrn.isTrack() ? Tables.Sounds.TYPE_TRACK : Tables.Sounds.TYPE_PLAYLIST;
    }
}
