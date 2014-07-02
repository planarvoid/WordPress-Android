package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.android.storage.TableColumns.ActivityView;
import static com.soundcloud.android.storage.TableColumns.CollectionItems;
import static com.soundcloud.android.storage.TableColumns.SoundView;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PlaylistProperty;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

class SoundStreamStorage {

    private final DatabaseScheduler scheduler;

    @Inject
    public SoundStreamStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> streamItemsBefore(final long timestamp, final Urn userUrn, final int limit) {
        final Query query = Query.from(Table.ACTIVITY_VIEW.name)
                .select(
                        ActivityView.SOUND_ID,
                        ActivityView.SOUND_TYPE,
                        SoundView.TITLE,
                        SoundView.USERNAME,
                        SoundView.DURATION,
                        SoundView.PLAYBACK_COUNT,
                        SoundView.TRACK_COUNT,
                        SoundView.LIKES_COUNT,
                        ActivityView.CREATED_AT,
                        ActivityView.TYPE,
                        ActivityView.USER_USERNAME,
                        soundAssociationQuery(LIKE, userUrn.numericId, SoundView.USER_LIKE),
                        soundAssociationQuery(REPOST, userUrn.numericId, SoundView.USER_REPOST)
                )
                .whereEq(ActivityView.CONTENT_ID, Content.ME_SOUND_STREAM.id)
                .whereLt(ActivityView.CREATED_AT, timestamp)
                .limit(limit);

        return scheduler.scheduleQuery(query).map(new StreamItemMapper());
    }

    public Observable<TrackUrn> trackUrns() {
        Query query = Query.from(Table.ACTIVITY_VIEW.name)
                .select(ActivityView.SOUND_ID)
                .whereEq(ActivityView.CONTENT_ID, Content.ME_SOUND_STREAM.id)
                .whereEq(ActivityView.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        return scheduler.scheduleQuery(query).map(new TrackUrnMapper());
    }

    private Query soundAssociationQuery(int collectionType, long userId, String colName) {
        Query association = Query.from(Table.COLLECTION_ITEMS.name, Table.SOUNDS.name);
        association.joinOn(ActivityView.SOUND_ID, CollectionItems.ITEM_ID);
        association.joinOn(ActivityView.SOUND_TYPE, CollectionItems.RESOURCE_TYPE);
        association.whereEq(CollectionItems.COLLECTION_TYPE, collectionType);
        association.whereEq(Table.COLLECTION_ITEMS.name + "." + CollectionItems.USER_ID, userId);
        return association.exists().as(colName);
    }

    private static final class TrackUrnMapper extends RxResultMapper<TrackUrn> {
        @Override
        public TrackUrn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(ActivityView.SOUND_ID));
        }
    }

    private static final class StreamItemMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(PlayableProperty.URN, readSoundUrn(cursorReader));
            propertySet.put(PlayableProperty.TITLE, cursorReader.getString(SoundView.TITLE));
            propertySet.put(PlayableProperty.DURATION, cursorReader.getInt(SoundView.DURATION));
            propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(SoundView.USERNAME));
            propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(ActivityView.CREATED_AT));
            addOptionalPlaylistLike(cursorReader, propertySet);
            addOptionalLikesCount(cursorReader, propertySet);
            addOptionalPlayCount(cursorReader, propertySet);
            addOptionalTrackCount(cursorReader, propertySet);
            addOptionalReposter(cursorReader, propertySet);

            return propertySet;
        }

        private void addOptionalPlaylistLike(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(SoundView.USER_LIKE));
            }
        }

        private void addOptionalPlayCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_TRACK) {
                propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(SoundView.PLAYBACK_COUNT));
            }
        }

        private void addOptionalLikesCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(SoundView.LIKES_COUNT));
            }
        }

        private void addOptionalTrackCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlaylistProperty.TRACK_COUNT, cursorReader.getInt(SoundView.TRACK_COUNT));
            }
        }

        private void addOptionalReposter(CursorReader cursorReader, PropertySet propertySet) {
            if (cursorReader.getString(ActivityView.TYPE).endsWith("-repost")) {
                propertySet.put(PlayableProperty.REPOSTER, cursorReader.getString(ActivityView.USER_USERNAME));
            }
        }

        private Urn readSoundUrn(CursorReader cursorReader) {
            final int soundId = cursorReader.getInt(ActivityView.SOUND_ID);
            return getSoundType(cursorReader) == Playable.DB_TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
        }
    }

    private static int getSoundType(CursorReader cursorReader) {
        return cursorReader.getInt(ActivityView.SOUND_TYPE);
    }
}
