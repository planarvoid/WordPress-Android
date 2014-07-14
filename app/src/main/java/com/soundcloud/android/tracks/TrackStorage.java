package com.soundcloud.android.tracks;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
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
                        SoundView._ID,
                        SoundView.TITLE,
                        SoundView.USERNAME,
                        SoundView.DURATION,
                        SoundView.PLAYBACK_COUNT,
                        SoundView.LIKES_COUNT,
                        SoundView.WAVEFORM_URL,
                        SoundView.MONETIZABLE,
                        exists(soundAssociationQuery(LIKE, loggedInUserUrn.numericId)).as(SoundView.USER_LIKE),
                        exists(soundAssociationQuery(REPOST, loggedInUserUrn.numericId)).as(SoundView.USER_REPOST)
                ).whereEq(SoundView._ID, trackUrn.numericId);
        return scheduler.scheduleQuery(query).map(new TrackItemMapper());
    }

    private Query soundAssociationQuery(int collectionType, long userId) {
        return Query.from(Table.COLLECTION_ITEMS.name, Table.SOUNDS.name)
                .joinOn(SoundView._ID, TableColumns.CollectionItems.ITEM_ID)
                .joinOn(SoundView._TYPE, TableColumns.CollectionItems.RESOURCE_TYPE)
                .whereEq(TableColumns.CollectionItems.COLLECTION_TYPE, collectionType)
                .whereEq(TableColumns.CollectionItems.RESOURCE_TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(Table.COLLECTION_ITEMS.name + "." + TableColumns.CollectionItems.USER_ID, userId);
    }

    private static final class TrackItemMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(TrackProperty.URN, readSoundUrn(cursorReader));
            propertySet.put(PlayableProperty.TITLE, cursorReader.getString(SoundView.TITLE));
            propertySet.put(PlayableProperty.DURATION, cursorReader.getInt(SoundView.DURATION));
            propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(SoundView.PLAYBACK_COUNT));
            propertySet.put(TrackProperty.WAVEFORM_URL, cursorReader.getString(SoundView.WAVEFORM_URL));
            propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(SoundView.LIKES_COUNT));
            propertySet.put(TrackProperty.MONETIZABLE, cursorReader.getBoolean(SoundView.MONETIZABLE));

            // synced tracks that might not have a user if they haven't been lazily updated yet
            final String creator = cursorReader.getString(SoundView.USERNAME);
            propertySet.put(PlayableProperty.CREATOR_NAME, creator == null ? ScTextUtils.EMPTY_STRING : creator);

            return propertySet;
        }

        private TrackUrn readSoundUrn(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getInt(SoundView._ID));
        }
    }
}
