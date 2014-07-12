package com.soundcloud.android.tracks;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

public class TrackStorage {

    private final DatabaseScheduler scheduler;

    @Inject
    public TrackStorage(DatabaseScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> track(final TrackUrn trackUrn, final UserUrn loggedInUserUrn) {
        final Query query = Query.from(Table.SOUND_VIEW.name)
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.PLAYBACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.WAVEFORM_URL,
                        TableColumns.SoundView.MONETIZABLE,
                        soundAssociationQuery(LIKE, loggedInUserUrn.numericId, TableColumns.SoundView.USER_LIKE),
                        soundAssociationQuery(REPOST, loggedInUserUrn.numericId, TableColumns.SoundView.USER_REPOST)
                ).whereEq(TableColumns.SoundView._ID, trackUrn.numericId);
        return scheduler.scheduleQuery(query).map(new TrackItemMapper());
    }

    /**
     * TODO: Duplicate code {@link com.soundcloud.android.stream.SoundStreamStorage#soundAssociationQuery(int, long, String)}
     */
    private Query soundAssociationQuery(int collectionType, long userId, String colName) {
        Query association = Query.from(Table.COLLECTION_ITEMS.name, Table.SOUNDS.name);
        association.joinOn(TableColumns.SoundView._ID, TableColumns.CollectionItems.ITEM_ID);
        association.joinOn(TableColumns.SoundView._TYPE, TableColumns.CollectionItems.RESOURCE_TYPE);
        association.whereEq(TableColumns.CollectionItems.COLLECTION_TYPE, collectionType);
        association.whereEq(TableColumns.CollectionItems.RESOURCE_TYPE, Playable.DB_TYPE_TRACK);
        association.whereEq(Table.COLLECTION_ITEMS.name + "." + TableColumns.CollectionItems.USER_ID, userId);
        return association.exists().as(colName);
    }

    private static final class TrackItemMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(TrackProperty.URN, readSoundUrn(cursorReader));
            propertySet.put(PlayableProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
            propertySet.put(PlayableProperty.DURATION, cursorReader.getInt(TableColumns.SoundView.DURATION));
            propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.SoundView.PLAYBACK_COUNT));
            propertySet.put(TrackProperty.WAVEFORM_URL, cursorReader.getString(TableColumns.SoundView.WAVEFORM_URL));
            propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(TableColumns.SoundView.LIKES_COUNT));
            propertySet.put(TrackProperty.MONETIZABLE, cursorReader.getBoolean(TableColumns.SoundView.MONETIZABLE));

            // synced tracks that might not have a user if they haven't been lazily updated yet
            final String creator = cursorReader.getString(TableColumns.SoundView.USERNAME);
            propertySet.put(PlayableProperty.CREATOR_NAME, creator == null ? ScTextUtils.EMPTY_STRING : creator);

            return propertySet;
        }

        private TrackUrn readSoundUrn(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getInt(TableColumns.SoundView._ID));
        }
    }
}
