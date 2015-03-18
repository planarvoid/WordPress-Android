package com.soundcloud.android.likes;

import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.WhereBuilder;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.content.ContentValues;
import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

class UpdateLikeCommand extends WriteStorageCommand<UpdateLikeCommand.UpdateLikeParams, WriteResult> {

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
                        new WhereBuilder()
                                .whereEq(TableColumns.Sounds._ID, params.targetUrn.getNumericId())
                                .whereEq(TableColumns.Sounds._TYPE, getSoundType(params.targetUrn))));
                step(propeller.upsert(Table.Likes, buildContentValuesForLike(params)));
            }
        });
    }

    private int obtainNewLikesCount(PropellerDatabase propeller, UpdateLikeParams params) {
        List<PropertySet> result = propeller.query(Query.from(Table.SoundView.name())
                .select(TableColumns.SoundView._ID, TableColumns.SoundView.LIKES_COUNT)
                .whereEq(TableColumns.SoundView._ID, params.targetUrn.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, getSoundType(params.targetUrn)))
                .toList(new LikeCountMapper());

        final int count = result.iterator().next().get(PlayableProperty.LIKES_COUNT);
        return params.addLike ? count + 1 : count - 1;
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

    int getUpdatedLikesCount() {
        return updatedLikesCount;
    }

    private static class LikeCountMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getLong(BaseColumns._ID)));
            propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));

            return propertySet;
        }
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

