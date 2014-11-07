package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.android.storage.TableColumns.SoundStreamView;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

class SoundStreamStorage implements ISoundStreamStorage {

    private final DatabaseScheduler scheduler;

    @Inject
    public SoundStreamStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> streamItemsBefore(final long timestamp, final Urn userUrn, final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                .select(
                        SoundStreamView.SOUND_ID,
                        SoundStreamView.SOUND_TYPE,
                        SoundView.TITLE,
                        SoundView.USERNAME,
                        SoundView.DURATION,
                        SoundView.PLAYBACK_COUNT,
                        SoundView.TRACK_COUNT,
                        SoundView.LIKES_COUNT,
                        SoundStreamView.CREATED_AT,
                        SoundStreamView.REPOSTER_USERNAME,
                        exists(soundAssociationQuery(LIKE, userUrn.getNumericId())).as(TableColumns.SoundView.USER_LIKE),
                        exists(soundAssociationQuery(REPOST, userUrn.getNumericId())).as(TableColumns.SoundView.USER_REPOST)
                )
                .whereLt(TableColumns.SoundStreamView.CREATED_AT, timestamp)
                        // TODO: poor man's check to remove orphaned tracks and playlists;
                        // We need to address this properly with a schema refactor, cf:
                        // https://github.com/soundcloud/SoundCloud-Android/issues/1524
                .where(TableColumns.SoundView.TITLE + " IS NOT NULL")
                .limit(limit);

        return scheduler.scheduleQuery(query).map(new StreamItemMapper());
    }

    public Observable<Urn> trackUrns() {
        Query query = Query.from(Table.SoundStreamView.name())
                .select(SoundStreamView.SOUND_ID)
                .whereEq(SoundStreamView.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        return scheduler.scheduleQuery(query).map(new TrackUrnMapper());
    }

    private Query soundAssociationQuery(int collectionType, long userId) {
        return Query.from(Table.CollectionItems.name(), Table.Sounds.name())
                .joinOn(TableColumns.SoundStreamView.SOUND_ID, TableColumns.CollectionItems.ITEM_ID)
                .joinOn(TableColumns.SoundStreamView.SOUND_TYPE, TableColumns.CollectionItems.RESOURCE_TYPE)
                .whereEq(TableColumns.CollectionItems.COLLECTION_TYPE, collectionType)
                .whereEq(Table.CollectionItems.name() + "." + TableColumns.CollectionItems.USER_ID, userId);
    }

    private static final class TrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(TableColumns.SoundStreamView.SOUND_ID));
        }
    }

    private static final class StreamItemMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(PlayableProperty.URN, readSoundUrn(cursorReader));
            addTitle(cursorReader, propertySet);
            propertySet.put(PlayableProperty.DURATION, cursorReader.getInt(TableColumns.SoundView.DURATION));
            propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
            propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundStreamView.CREATED_AT));
            addOptionalPlaylistLike(cursorReader, propertySet);
            addOptionalLikesCount(cursorReader, propertySet);
            addOptionalPlayCount(cursorReader, propertySet);
            addOptionalTrackCount(cursorReader, propertySet);
            addOptionalReposter(cursorReader, propertySet);

            return propertySet;
        }

        private void addTitle(CursorReader cursorReader, PropertySet propertySet) {
            final String string = cursorReader.getString(TableColumns.SoundView.TITLE);
            if (string == null){
                ErrorUtils.handleSilentException("urn : " + readSoundUrn(cursorReader),
                        new IllegalStateException("Unexpected null title in stream"));
                propertySet.put(PlayableProperty.TITLE, ScTextUtils.EMPTY_STRING);
            } else {
                propertySet.put(PlayableProperty.TITLE, string);
            }
        }


        private void addOptionalPlaylistLike(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(TableColumns.SoundView.USER_LIKE));
            }
        }

        private void addOptionalPlayCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_TRACK) {
                propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.SoundView.PLAYBACK_COUNT));
            }
        }

        private void addOptionalLikesCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
            }
        }

        private void addOptionalTrackCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlaylistProperty.TRACK_COUNT, cursorReader.getInt(TableColumns.SoundView.TRACK_COUNT));
            }
        }

        private void addOptionalReposter(CursorReader cursorReader, PropertySet propertySet) {
            final String reposter = cursorReader.getString(TableColumns.SoundStreamView.REPOSTER_USERNAME);
            if (ScTextUtils.isNotBlank(reposter)) {
                propertySet.put(PlayableProperty.REPOSTER, cursorReader.getString(TableColumns.SoundStreamView.REPOSTER_USERNAME));
            }
        }

        private Urn readSoundUrn(CursorReader cursorReader) {
            final int soundId = cursorReader.getInt(TableColumns.SoundStreamView.SOUND_ID);
            return getSoundType(cursorReader) == Playable.DB_TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
        }
    }

    private static int getSoundType(CursorReader cursorReader) {
        return cursorReader.getInt(TableColumns.SoundStreamView.SOUND_TYPE);
    }
}
