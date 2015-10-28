package com.soundcloud.android.associations;

import static com.soundcloud.android.storage.TableColumns.Posts;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.android.storage.TableColumns.Sounds;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;

class RepostStorage {

    private final PropellerDatabase propeller;
    private final DateProvider dateProvider;

    @Inject
    RepostStorage(PropellerDatabase propeller, CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.dateProvider = dateProvider;
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
                        step(propeller.insert(Table.Posts, buildContentValuesForRepost(urn)));
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
                        .whereEq(Posts.TARGET_TYPE, urn.isTrack() ? Sounds.TYPE_TRACK : Sounds.TYPE_PLAYLIST)
                        .whereEq(Posts.TYPE, Posts.TYPE_REPOST);

                return propeller.runTransaction(new PropellerDatabase.Transaction() {
                    @Override
                    public void steps(PropellerDatabase propeller) {
                        step(updateRepostCount(propeller, urn, updatedRepostCount));
                        step(propeller.delete(Table.Posts, whereClause));
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
        return propeller.update(Table.Sounds, ContentValuesBuilder.values()
                        .put(Sounds.REPOSTS_COUNT, repostCount).get(),
                filter().whereEq(Sounds._ID, urn.getNumericId())
                        .whereEq(Sounds._TYPE, getSoundType(urn)));
    }

    private ContentValues buildContentValuesForRepost(Urn urn) {
        final ContentValues values = new ContentValues();
        values.put(Posts.TYPE, Posts.TYPE_REPOST);
        values.put(Posts.TARGET_TYPE, urn.isTrack() ? Sounds.TYPE_TRACK : Sounds.TYPE_PLAYLIST);
        values.put(Posts.TARGET_ID, urn.getNumericId());
        values.put(Posts.CREATED_AT, dateProvider.getCurrentDate().getTime());
        return values;
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
        return targetUrn.isTrack() ? Sounds.TYPE_TRACK : Sounds.TYPE_PLAYLIST;
    }
}
