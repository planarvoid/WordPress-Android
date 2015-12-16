package com.soundcloud.android.likes;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
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
                step(propeller.update(Table.Sounds, ContentValuesBuilder.values().put(TableColumns.Sounds.LIKES_COUNT, updatedLikesCount).get(),
                        filter().whereEq(TableColumns.Sounds._ID, params.targetUrn.getNumericId())
                                .whereEq(TableColumns.Sounds._TYPE, getSoundType(params.targetUrn))));
                step(propeller.upsert(Table.Likes, buildContentValuesForLike(params)));
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
        cv.put(TableColumns.Likes._ID, targetUrn.getNumericId());
        cv.put(TableColumns.Likes._TYPE, getSoundType(targetUrn));
        cv.put(TableColumns.Likes.CREATED_AT, now.getTime());
        if (params.addLike) {
            cv.put(TableColumns.Likes.ADDED_AT, now.getTime());
            cv.putNull(TableColumns.Likes.REMOVED_AT);
        } else {
            cv.put(TableColumns.Likes.REMOVED_AT, now.getTime());
            cv.putNull(TableColumns.Likes.ADDED_AT);
        }
        return cv;
    }

    private int getSoundType(Urn targetUrn) {
        return targetUrn.isTrack() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST;
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

