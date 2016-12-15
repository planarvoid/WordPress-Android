package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.TableColumns.PromotedTracks;
import static com.soundcloud.android.storage.TableColumns.SoundStreamView;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.timeline.TimelineStorage;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class StreamStorage implements TimelineStorage<StreamPlayable> {

    private static final Object[] STREAM_SELECTION = new Object[]{
            SoundStreamView.SOUND_ID,
            SoundStreamView.SOUND_TYPE,
            SoundView.TITLE,
            SoundView.USERNAME,
            SoundView.USER_ID,
            SoundView.DURATION,
            SoundView.FULL_DURATION,
            SoundView.SNIPPET_DURATION,
            SoundView.PLAYBACK_COUNT,
            field(Table.SoundStreamView.field(SoundView.TRACK_COUNT)).as(SoundView.TRACK_COUNT),
            SoundView.LIKES_COUNT,
            SoundView.REPOSTS_COUNT,
            SoundView.SHARING,
            SoundView.ARTWORK_URL,
            SoundView.USER_AVATAR_URL,
            SoundView.SET_TYPE,
            SoundView.IS_ALBUM,
            field(Table.SoundStreamView.field(SoundStreamView.CREATED_AT)).as(SoundStreamView.CREATED_AT),
            SoundView.POLICIES_SNIPPED,
            SoundView.POLICIES_SUB_HIGH_TIER,
            SoundView.GENRE,
            SoundStreamView.REPOSTER_USERNAME,
            SoundStreamView.REPOSTER_ID,
            SoundStreamView.REPOSTER_AVATAR_URL,
            exists(likeQuery()).as(SoundView.USER_LIKE),
            exists(repostQuery()).as(SoundView.USER_REPOST),
    };

    private static final Object[] PROMOTED_EXTRAS = new Object[]{
            field(Table.PromotedTracks.field(PromotedTracks.AD_URN)).as(PromotedTracks.AD_URN),
            Tables.Users.AVATAR_URL.as(SoundStreamView.PROMOTER_AVATAR_URL),
            PromotedTracks.PROMOTER_ID,
            PromotedTracks.PROMOTER_NAME,
            PromotedTracks.TRACKING_TRACK_CLICKED_URLS,
            PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS,
            PromotedTracks.TRACKING_TRACK_PLAYED_URLS,
            PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS,
            PromotedTracks.TRACKING_PROFILE_CLICKED_URLS
    };

    private static final Object[] PLAYBACK_ITEMS_SELECTION = new Object[]{
            SoundStreamView.SOUND_ID,
            SoundStreamView.SOUND_TYPE,
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
    private final PropellerDatabase propeller;

    @Inject
    public StreamStorage(PropellerDatabase propeller) {
        this.propellerRx = new PropellerRx(propeller);
        this.propeller = propeller;
    }

    @Override
    public Observable<StreamPlayable> timelineItems(final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                                 .select(PROMOTED_STREAM_SELECTION)
                                 .leftJoin(Table.PromotedTracks.name(),
                                           Table.PromotedTracks.field(PromotedTracks._ID),
                                           TableColumns.SoundStream.PROMOTED_ID)
                                 .leftJoin(Tables.Users.TABLE.name(), Tables.Users._ID.qualifiedName(), Table.PromotedTracks.field(PromotedTracks.PROMOTER_ID))
                                 .whereLe(Table.SoundStreamView.field(SoundStreamView.CREATED_AT), Long.MAX_VALUE)
                                 .whereNotNull(SoundView.TITLE)
                                 .limit(limit);

        return propellerRx.query(query)
                          .map(StreamItemMapper.getPromotedMapper());
    }

    @Override
    public Observable<StreamPlayable> timelineItemsBefore(final long timestamp, final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                                 .select(STREAM_SELECTION)
                                 .whereLt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                                 .whereNull(SoundStreamView.PROMOTED_ID)
                                 .limit(limit);

        return propellerRx.query(query).map(StreamItemMapper.getMapper());
    }

    @Override
    public List<StreamPlayable> timelineItemsSince(final long timestamp, final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                                 .select(STREAM_SELECTION)
                                 .whereGt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                                 .whereNull(SoundStreamView.PROMOTED_ID)
                                 .limit(limit);

        return propeller.query(query).toList(StreamItemMapper.getMapper());
    }

    public Observable<Integer> timelineItemCountSince(final long timestamp) {
        Query query = Query.count(Table.SoundStreamView.name())
                           .whereGt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                           .whereNull(SoundStreamView.PROMOTED_ID)
                           .whereNotNull(SoundView.TITLE);

        return propellerRx.query(query).map(scalar(Integer.class));
    }

    public Observable<PropertySet> playbackItems() {
        Query query = Query.from(Table.SoundStreamView.name())
                           .select(PLAYBACK_ITEMS_SELECTION);
        return propellerRx.query(query).map(new ItemsForPlayback());
    }


    private static final class ItemsForPlayback extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.from(
                    EntityProperty.URN.bind(readSoundUrn(cursorReader))
            );
            if (cursorReader.isNotNull(SoundStreamView.REPOSTER_ID)) {
                propertySet.put(PostProperty.REPOSTER_URN,
                                Urn.forUser(cursorReader.getLong(SoundStreamView.REPOSTER_ID)));
            }
            return propertySet;
        }
    }

    private static Urn readSoundUrn(CursorReader cursorReader) {
        final int soundId = cursorReader.getInt(SoundStreamView.SOUND_ID);
        return getSoundType(cursorReader) == Tables.Sounds.TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
    }

    private static int getSoundType(CursorReader cursorReader) {
        return cursorReader.getInt(SoundStreamView.SOUND_TYPE);
    }

    private static Query likeQuery() {
        return Query.from(Tables.Likes.TABLE, Tables.Sounds.TABLE)
                    .joinOn(SoundStreamView.SOUND_ID, Tables.Likes._ID.qualifiedName())
                    .joinOn(SoundStreamView.SOUND_TYPE, Tables.Likes._TYPE.qualifiedName())
                    .whereNull(Tables.Likes.REMOVED_AT);
    }

    private static Query repostQuery() {
        return Query.from(Tables.Posts.TABLE, Tables.Sounds.TABLE)
                    .joinOn(SoundStreamView.SOUND_ID, Tables.Posts.TARGET_ID.qualifiedName())
                    .joinOn(SoundStreamView.SOUND_TYPE, Tables.Posts.TARGET_TYPE.qualifiedName())
                    .whereEq(Tables.Posts.TYPE, Tables.Posts.TYPE_REPOST);
    }

}
