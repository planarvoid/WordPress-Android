package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.TableColumns.PromotedTracks;
import static com.soundcloud.android.storage.TableColumns.SoundStreamView;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.android.storage.TableColumns.Sounds;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

class SoundStreamStorage {

    private static final Object[] STREAM_SELECTION = new Object[]{
            SoundStreamView.SOUND_ID,
            SoundStreamView.SOUND_TYPE,
            SoundView.TITLE,
            SoundView.USERNAME,
            SoundView.USER_ID,
            SoundView.DURATION,
            SoundView.PLAYBACK_COUNT,
            SoundView.TRACK_COUNT,
            SoundView.LIKES_COUNT,
            SoundView.REPOSTS_COUNT,
            SoundView.SHARING,
            field(Table.SoundStreamView.field(SoundStreamView.CREATED_AT)).as(SoundStreamView.CREATED_AT),
            SoundView.POLICIES_SUB_MID_TIER,
            SoundStreamView.REPOSTER_USERNAME,
            SoundStreamView.REPOSTER_ID,
            exists(likeQuery()).as(SoundView.USER_LIKE),
            exists(repostQuery()).as(SoundView.USER_REPOST),
    };

    private static final Object[] PROMOTED_EXTRAS = new Object[]{
            field(Table.PromotedTracks.field(PromotedTracks.AD_URN)).as(PromotedTracks.AD_URN),
            PromotedTracks.PROMOTER_ID,
            PromotedTracks.PROMOTER_NAME,
            PromotedTracks.TRACKING_TRACK_CLICKED_URLS,
            PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS,
            PromotedTracks.TRACKING_TRACK_PLAYED_URLS,
            PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS,
            PromotedTracks.TRACKING_PROFILE_CLICKED_URLS
    };

    private static final Object[] TRACKS_FOR_PLAYBACK_SELECTION = new Object[]{
            SoundStreamView.SOUND_ID,
            SoundStreamView.REPOSTER_ID
    };

    private static final Object[] PROMOTED_STREAM_SELECTION = buildPromotedSelection();

    private static Object[] buildPromotedSelection() {
        Object[] promotedSelection = new Object[STREAM_SELECTION.length + PROMOTED_EXTRAS.length];
        System.arraycopy(STREAM_SELECTION, 0, promotedSelection, 0, STREAM_SELECTION.length);
        System.arraycopy(PROMOTED_EXTRAS, 0, promotedSelection, STREAM_SELECTION.length, PROMOTED_EXTRAS.length);
        return promotedSelection;
    }

    private final PropellerRx propellerRx;
    private final PropellerDatabase database;

    @Inject
    public SoundStreamStorage(PropellerRx propellerRx, PropellerDatabase database) {
        this.propellerRx = propellerRx;
        this.database = database;
    }

    public Observable<PropertySet> initialStreamItems(final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                .select(PROMOTED_STREAM_SELECTION)
                .leftJoin(Table.PromotedTracks.name(),
                        Table.PromotedTracks.field(PromotedTracks._ID),
                        TableColumns.SoundStream.PROMOTED_ID)
                .whereLe(Table.SoundStreamView.field(SoundStreamView.CREATED_AT), Long.MAX_VALUE)
                .whereNotNull(SoundView.TITLE)
                .limit(limit);

        return propellerRx.query(query).map(new PromotedStreamItemMapper());
    }

    public Observable<PropertySet> streamItemsBefore(final long timestamp, final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                .select(STREAM_SELECTION)
                .whereLt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                .whereNull(SoundStreamView.PROMOTED_ID)
                .whereNotNull(SoundView.TITLE)
                .limit(limit);

        return propellerRx.query(query).map(new StreamItemMapper());
    }

    public List<PropertySet> loadStreamItemsSince(final long timestamp, final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                .select(STREAM_SELECTION)
                .whereGt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                .whereNull(SoundStreamView.PROMOTED_ID)
                .whereNotNull(SoundView.TITLE)
                .limit(limit);

        return database.query(query).toList(new StreamItemMapper());
    }

    public Observable<PropertySet> tracksForPlayback() {
        Query query = Query.from(Table.SoundStreamView.name())
                .select(TRACKS_FOR_PLAYBACK_SELECTION)
                .whereEq(SoundStreamView.SOUND_TYPE, Sounds.TYPE_TRACK);
        return propellerRx.query(query).map(new TrackForPlaybackMapper());
    }

    private static class StreamItemMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());

            propertySet.put(PlayableProperty.URN, readSoundUrn(cursorReader));
            addTitle(cursorReader, propertySet);
            propertySet.put(PlayableProperty.DURATION, cursorReader.getLong(SoundView.DURATION));
            propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(SoundView.USERNAME));
            propertySet.put(PlayableProperty.CREATOR_URN, Urn.forUser(cursorReader.getInt(SoundView.USER_ID)));
            propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(SoundStreamView.CREATED_AT));
            propertySet.put(PlayableProperty.IS_PRIVATE,
                    Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));

            addUserLike(cursorReader, propertySet);
            addUserRepost(cursorReader, propertySet);

            addOptionalPlayCount(cursorReader, propertySet);
            addOptionalTrackCount(cursorReader, propertySet);
            addOptionalReposter(cursorReader, propertySet);

            if (cursorReader.isNotNull(SoundView.POLICIES_SUB_MID_TIER)) {
                propertySet.put(TrackProperty.SUB_MID_TIER, cursorReader.getBoolean(SoundView.POLICIES_SUB_MID_TIER));
            }

            return propertySet;
        }

        private void addTitle(CursorReader cursorReader, PropertySet propertySet) {
            final String string = cursorReader.getString(SoundView.TITLE);
            if (string == null) {
                ErrorUtils.handleSilentException("urn : " + readSoundUrn(cursorReader),
                        new IllegalStateException("Unexpected null title in stream"));
                propertySet.put(PlayableProperty.TITLE, ScTextUtils.EMPTY_STRING);
            } else {
                propertySet.put(PlayableProperty.TITLE, string);
            }
        }

        private void addUserLike(CursorReader cursorReader, PropertySet propertySet) {
            propertySet.put(PlayableProperty.IS_LIKED, cursorReader.getBoolean(SoundView.USER_LIKE));
            propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(SoundView.LIKES_COUNT));
        }

        private void addUserRepost(CursorReader cursorReader, PropertySet propertySet) {
            propertySet.put(PlayableProperty.IS_REPOSTED, cursorReader.getBoolean(SoundView.USER_REPOST));
            propertySet.put(PlayableProperty.REPOSTS_COUNT, cursorReader.getInt(SoundView.REPOSTS_COUNT));
        }

        private void addOptionalPlayCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Sounds.TYPE_TRACK) {
                propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(SoundView.PLAYBACK_COUNT));
            }
        }

        private void addOptionalTrackCount(CursorReader cursorReader, PropertySet propertySet) {
            if (getSoundType(cursorReader) == Sounds.TYPE_PLAYLIST) {
                propertySet.put(PlaylistProperty.TRACK_COUNT, cursorReader.getInt(SoundView.TRACK_COUNT));
            }
        }

        private void addOptionalReposter(CursorReader cursorReader, PropertySet propertySet) {
            final String reposter = cursorReader.getString(SoundStreamView.REPOSTER_USERNAME);
            if (Strings.isNotBlank(reposter)) {
                propertySet.put(PostProperty.REPOSTER, cursorReader.getString(SoundStreamView.REPOSTER_USERNAME));
                propertySet.put(PostProperty.REPOSTER_URN, Urn.forUser(cursorReader.getInt(SoundStreamView.REPOSTER_ID)));
            }
        }

        private Urn readSoundUrn(CursorReader cursorReader) {
            final int soundId = cursorReader.getInt(SoundStreamView.SOUND_ID);
            return getSoundType(cursorReader) == Sounds.TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
        }

        private static int getSoundType(CursorReader cursorReader) {
            return cursorReader.getInt(SoundStreamView.SOUND_TYPE);
        }
    }

    private static final class TrackForPlaybackMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.from(
                    TrackProperty.URN.bind(Urn.forTrack(cursorReader.getLong(SoundStreamView.SOUND_ID)))
            );
            if (cursorReader.isNotNull(SoundStreamView.REPOSTER_ID)) {
                propertySet.put(PostProperty.REPOSTER_URN, Urn.forUser(cursorReader.getLong(SoundStreamView.REPOSTER_ID)));
            }
            return propertySet;
        }
    }

    private static class PromotedStreamItemMapper extends StreamItemMapper {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            PropertySet propertySet = super.map(cursorReader);
            addOptionalPromotedProperties(cursorReader, propertySet);
            return propertySet;
        }

        private void addOptionalPromotedProperties(CursorReader cursorReader, PropertySet propertySet) {
            if (cursorReader.isNotNull(PromotedTracks.AD_URN)) {
                propertySet.put(PromotedItemProperty.AD_URN, cursorReader.getString(PromotedTracks.AD_URN));
                propertySet.put(PromotedItemProperty.TRACK_CLICKED_URLS, splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_CLICKED_URLS)));
                propertySet.put(PromotedItemProperty.TRACK_IMPRESSION_URLS, splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS)));
                propertySet.put(PromotedItemProperty.TRACK_PLAYED_URLS, splitUrls(cursorReader.getString(PromotedTracks.TRACKING_TRACK_PLAYED_URLS)));
                propertySet.put(PromotedItemProperty.PROMOTER_CLICKED_URLS, splitUrls(cursorReader.getString(PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS)));
                addOptionalPromoter(cursorReader, propertySet);
            }
        }

        private void addOptionalPromoter(CursorReader cursorReader, PropertySet propertySet) {
            if (cursorReader.isNotNull(PromotedTracks.PROMOTER_ID)) {
                propertySet.put(PromotedItemProperty.PROMOTER_URN, Optional.of(Urn.forUser(cursorReader.getLong(PromotedTracks.PROMOTER_ID))));
                propertySet.put(PromotedItemProperty.PROMOTER_NAME, Optional.of(cursorReader.getString(PromotedTracks.PROMOTER_NAME)));
            } else {
                propertySet.put(PromotedItemProperty.PROMOTER_URN, Optional.<Urn>absent());
                propertySet.put(PromotedItemProperty.PROMOTER_NAME, Optional.<String>absent());
            }
        }

        private List<String> splitUrls(String urls) {
            return newArrayList(urls.split(" "));
        }
    }

    private static Query likeQuery() {
        return Query.from(Table.Likes.name(), Table.Sounds.name())
                .joinOn(SoundStreamView.SOUND_ID, Table.Likes.field(TableColumns.Likes._ID))
                .joinOn(SoundStreamView.SOUND_TYPE, Table.Likes.field(TableColumns.Likes._TYPE))
                .whereNull(TableColumns.Likes.REMOVED_AT);
    }

    private static Query repostQuery() {
        return Query.from(Table.Posts.name(), Table.Sounds.name())
                .joinOn(SoundStreamView.SOUND_ID, Table.Posts.field(TableColumns.Posts.TARGET_ID))
                .joinOn(SoundStreamView.SOUND_TYPE, Table.Posts.field(TableColumns.Posts.TARGET_TYPE))
                .whereEq(Table.Posts.field(TableColumns.Posts.TYPE), TableColumns.Posts.TYPE_REPOST);
    }

}
