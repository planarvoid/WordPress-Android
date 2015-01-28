package com.soundcloud.android.likes;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.WhereBuilder;
import com.soundcloud.propeller.rx.RxResultMapper;

import android.content.ContentValues;
import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

class UpdateLikeCommand extends Command<PropertySet, PropertySet, UpdateLikeCommand> {

    private final PropellerDatabase database;

    @Inject
    UpdateLikeCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public PropertySet call() throws Exception {
        final boolean addLike = input.get(PlayableProperty.IS_LIKED);
        final Urn urn = input.get(LikeProperty.TARGET_URN);
        final int updatedLikesCount = getUpdatedLikesCount(urn, addLike);

        updateLikesCount(urn, updatedLikesCount);
        updateLikes(addLike);

        return PropertySet.from(PlayableProperty.URN.bind(urn), PlayableProperty.LIKES_COUNT.bind(updatedLikesCount),
                PlayableProperty.IS_LIKED.bind(addLike));
    }

    private void updateLikes(boolean addLike) {
        database.upsert(Table.Likes, buildContentValuesForLike(input, addLike));
    }

    private int getUpdatedLikesCount(Urn urn, boolean addLike) {
        final int count = readLikesCount(urn);
        return addLike ? count + 1 : count - 1;
    }

    private void updateLikesCount(Urn urn, int updatedLikesCount) {
        database.update(Table.Sounds, ContentValuesBuilder.values().put(TableColumns.Sounds.LIKES_COUNT, updatedLikesCount).get(),
                new WhereBuilder()
                        .whereEq(TableColumns.Sounds._ID, urn.getNumericId())
                        .whereEq(TableColumns.Sounds._TYPE, getSoundType(urn)));
    }

    private int readLikesCount(Urn targetUrn) {
        List<PropertySet> result = database.query(Query.from(Table.SoundView.name())
                .select(TableColumns.SoundView._ID, TableColumns.SoundView.LIKES_COUNT)
                .whereEq(TableColumns.SoundView._ID, targetUrn.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, getSoundType(targetUrn)))
                .toList(new LikeCountMapper());

        return result.iterator().next().get(PlayableProperty.LIKES_COUNT);
    }

    private ContentValues buildContentValuesForLike(PropertySet like, boolean addLike) {
        final ContentValues cv = new ContentValues();
        final Urn targetUrn = like.get(LikeProperty.TARGET_URN);
        cv.put(TableColumns.Likes._ID, targetUrn.getNumericId());
        cv.put(TableColumns.Likes._TYPE, getSoundType(targetUrn));
        cv.put(TableColumns.Likes.CREATED_AT, like.get(LikeProperty.CREATED_AT).getTime());
        if (addLike) {
            cv.put(TableColumns.Likes.ADDED_AT, like.get(LikeProperty.ADDED_AT).getTime());
            cv.putNull(TableColumns.Likes.REMOVED_AT);
        } else {
            cv.put(TableColumns.Likes.REMOVED_AT, like.get(LikeProperty.REMOVED_AT).getTime());
            cv.putNull(TableColumns.Likes.ADDED_AT);
        }
        return cv;
    }

    private int getSoundType(Urn targetUrn) {
        return targetUrn.isTrack() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST;
    }

    private static class LikeCountMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getInt(BaseColumns._ID)));
            propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));

            return propertySet;
        }
    }
}

