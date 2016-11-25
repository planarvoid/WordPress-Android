package com.soundcloud.android.likes;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Date;

class UpdateLikeCommand extends WriteStorageCommand<UpdateLikeCommand.UpdateLikeParams, WriteResult, Integer> {

    private int updatedLikesCount;

    @Inject
    UpdateLikeCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final UpdateLikeParams params) {
        updatedLikesCount = obtainNewLikesCount(propeller, params);

        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.update(Tables.Sounds.TABLE,
                                      ContentValuesBuilder.values()
                                                          .put(Tables.Sounds.LIKES_COUNT, updatedLikesCount)
                                                          .get(),
                                      filter().whereEq(Tables.Sounds._ID, params.targetUrn.getNumericId())
                                              .whereEq(Tables.Sounds._TYPE, getSoundType(params.targetUrn))));
                step(propeller.upsert(Tables.Likes.TABLE, buildContentValuesForLike(params)));
            }
        });
    }

    @Override
    protected Integer transform(WriteResult result) {
        return updatedLikesCount;
    }

    private int obtainNewLikesCount(PropellerDatabase propeller, UpdateLikeParams params) {
        int count = propeller.query(from(Table.SoundView.name())
                                            .select(SoundView.LIKES_COUNT)
                                            .whereEq(SoundView._ID, params.targetUrn.getNumericId())
                                            .whereEq(SoundView._TYPE, getSoundType(params.targetUrn)))
                             .first(Integer.class);

        if (count == Consts.NOT_SET) {
            return Consts.NOT_SET;
        } else {
            return params.addLike ? count + 1 : count - 1;
        }

    }

    private ContentValues buildContentValuesForLike(UpdateLikeParams params) {
        final Date now = new Date();
        final ContentValues cv = new ContentValues();
        final Urn targetUrn = params.targetUrn;
        cv.put(Tables.Likes._ID.name(), targetUrn.getNumericId());
        cv.put(Tables.Likes._TYPE.name(), getSoundType(targetUrn));
        cv.put(Tables.Likes.CREATED_AT.name(), now.getTime());
        if (params.addLike) {
            cv.put(Tables.Likes.ADDED_AT.name(), now.getTime());
            cv.putNull(Tables.Likes.REMOVED_AT.name());
        } else {
            cv.put(Tables.Likes.REMOVED_AT.name(), now.getTime());
            cv.putNull(Tables.Likes.ADDED_AT.name());
        }
        return cv;
    }

    private int getSoundType(Urn targetUrn) {
        return targetUrn.isTrack() ? Tables.Sounds.TYPE_TRACK : Tables.Sounds.TYPE_PLAYLIST;
    }

    static final class UpdateLikeParams {
        final boolean addLike;
        final Urn targetUrn;

        UpdateLikeParams(Urn targetUrn, boolean addLike) {
            this.addLike = addLike;
            this.targetUrn = targetUrn;
        }
    }
}

