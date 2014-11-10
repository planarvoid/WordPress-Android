package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.android.storage.TableColumns.ActivityView;
import static com.soundcloud.android.storage.TableColumns.CollectionItems;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

class LegacySoundStreamStorage implements ISoundStreamStorage {

    private final DatabaseScheduler scheduler;
    private final PropellerDatabase database;

    @Inject
    public LegacySoundStreamStorage(DatabaseScheduler scheduler, PropellerDatabase database) {
        this.scheduler = scheduler;
        this.database = database;
    }

    public Observable<PropertySet> streamItemsBefore(final long timestamp, final Urn userUrn, final int limit) {
        final Query query = Query.from(Table.ActivityView.name())
                .select(soundStreamSelection(userUrn))
                .whereEq(ActivityView.CONTENT_ID, Content.ME_SOUND_STREAM.id)
                .whereLt(ActivityView.CREATED_AT, timestamp)
                // TODO: poor man's check to remove orphaned tracks and playlists;
                // We need to address this properly with a schema refactor, cf:
                // https://github.com/soundcloud/SoundCloud-Android/issues/1524
                .where(SoundView.TITLE + " IS NOT NULL")
                .limit(limit);

        return scheduler.scheduleQuery(query).map(new StreamItemMapper());
    }

    @Override
    public List<PropertySet> loadStreamItemsSince(final long timestamp, final Urn userUrn, final int limit) {
        final Query query = Query.from(Table.ActivityView.name())
                .select(soundStreamSelection(userUrn))
                .whereGt(ActivityView.CREATED_AT, timestamp)
                .where(SoundView.TITLE + " IS NOT NULL")
                .limit(limit);

        return database.query(query).toList(new StreamItemMapper());
    }

    private Object[] soundStreamSelection(Urn userUrn) {
        return new Object[]{ActivityView.SOUND_ID,
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
                    exists(soundAssociationQuery(LIKE, userUrn.getNumericId())).as(SoundView.USER_LIKE),
                    exists(soundAssociationQuery(REPOST, userUrn.getNumericId())).as(SoundView.USER_REPOST)};
    }

    public Observable<Urn> trackUrns() {
        Query query = Query.from(Table.ActivityView.name())
                .select(ActivityView.SOUND_ID)
                .whereEq(ActivityView.CONTENT_ID, Content.ME_SOUND_STREAM.id)
                .whereEq(ActivityView.SOUND_TYPE, Playable.DB_TYPE_TRACK);
        return scheduler.scheduleQuery(query).map(new TrackUrnMapper());
    }

    private Query soundAssociationQuery(int collectionType, long userId) {
        return Query.from(Table.CollectionItems.name(), Table.Sounds.name())
                .joinOn(ActivityView.SOUND_ID, CollectionItems.ITEM_ID)
                .joinOn(ActivityView.SOUND_TYPE, CollectionItems.RESOURCE_TYPE)
                .whereEq(CollectionItems.COLLECTION_TYPE, collectionType)
                .whereEq(Table.CollectionItems.name() + "." + CollectionItems.USER_ID, userId);
    }

    private static final class TrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(ActivityView.SOUND_ID));
        }
    }

    private static final class StreamItemMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(PlayableProperty.URN, readSoundUrn(cursorReader));
            addTitle(cursorReader, propertySet);
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

        private void addTitle(CursorReader cursorReader, PropertySet propertySet) {
            final String string = cursorReader.getString(SoundView.TITLE);
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
