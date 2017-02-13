package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.TableColumns.PromotedTracks;
import static com.soundcloud.android.storage.TableColumns.SoundStreamView;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.timeline.TimelineStorage;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class StreamStorage implements TimelineStorage<StreamEntity> {

    private static final Object[] STREAM_SELECTION = new Object[]{
            SoundStreamView.SOUND_ID,
            SoundStreamView.SOUND_TYPE,
            TableColumns.SoundView.USER_AVATAR_URL,
            field(Table.SoundStreamView.field(SoundStreamView.CREATED_AT)).as(SoundStreamView.CREATED_AT),
            SoundStreamView.REPOSTER_USERNAME,
            SoundStreamView.REPOSTER_ID,
            SoundStreamView.REPOSTER_AVATAR_URL,
    };

    private static final Object[] PROMOTED_EXTRAS = new Object[]{
            PromotedTracks.AD_URN,
            Tables.Users.AVATAR_URL.as(SoundStreamView.PROMOTER_AVATAR_URL),
            PromotedTracks.PROMOTER_ID,
            PromotedTracks.PROMOTER_NAME,
            PromotedTracks.TRACKING_TRACK_CLICKED_URLS,
            PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS,
            PromotedTracks.TRACKING_TRACK_PLAYED_URLS,
            PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS
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
    public Observable<StreamEntity> timelineItems(final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                                 .select(PROMOTED_STREAM_SELECTION)
                                 .leftJoin(Table.PromotedTracks.name(),
                                           Table.PromotedTracks.field(PromotedTracks._ID),
                                           TableColumns.SoundStream.PROMOTED_ID)
                                 .leftJoin(Tables.Users.TABLE.name(), Tables.Users._ID.qualifiedName(), Table.PromotedTracks.field(PromotedTracks.PROMOTER_ID))
                                 .whereLe(Table.SoundStreamView.field(SoundStreamView.CREATED_AT), Long.MAX_VALUE)
                                 .whereNotNull(SoundStreamView.SOUND_ID)
                                 .limit(limit);

        return propellerRx.query(query)
                          .map(StreamEntityMapper.getPromotedMapper());
    }

    @Override
    public Observable<StreamEntity> timelineItemsBefore(final long timestamp, final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                                 .select(STREAM_SELECTION)
                                 .whereLt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                                 .whereNull(SoundStreamView.PROMOTED_ID)
                                 .limit(limit);

        return propellerRx.query(query).map(StreamEntityMapper.getMapper());
    }

    @Override
    public List<StreamEntity> timelineItemsSince(final long timestamp, final int limit) {
        final Query query = Query.from(Table.SoundStreamView.name())
                                 .select(STREAM_SELECTION)
                                 .whereGt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                                 .whereNull(SoundStreamView.PROMOTED_ID)
                                 .limit(limit);

        return propeller.query(query).toList(StreamEntityMapper.getMapper());
    }

    public Observable<Integer> timelineItemCountSince(final long timestamp) {
        Query query = Query.count(Table.SoundStreamView.name())
                           .whereGt((Table.SoundStreamView.field(SoundStreamView.CREATED_AT)), timestamp)
                           .whereNull(SoundStreamView.PROMOTED_ID)
                           .whereNotNull(SoundStreamView.SOUND_ID);

        return propellerRx.query(query).map(scalar(Integer.class));
    }

    public Observable<PlayableWithReposter> playbackItems() {
        Query query = Query.from(Table.SoundStreamView.name())
                           .select(PLAYBACK_ITEMS_SELECTION);
        return propellerRx.query(query).map(new ItemsForPlayback());
    }


    private static final class ItemsForPlayback extends RxResultMapper<PlayableWithReposter> {
        @Override
        public PlayableWithReposter map(CursorReader cursorReader) {
            final Urn urn = readSoundUrn(cursorReader);
            Optional<Urn> reposterUrn = Optional.absent();
            if (cursorReader.isNotNull(SoundStreamView.REPOSTER_ID)) {
                reposterUrn = Optional.of(Urn.forUser(cursorReader.getLong(SoundStreamView.REPOSTER_ID)));
            }
            return PlayableWithReposter.create(urn, reposterUrn);
        }
    }

    private static Urn readSoundUrn(CursorReader cursorReader) {
        final int soundId = cursorReader.getInt(SoundStreamView.SOUND_ID);
        return getSoundType(cursorReader) == Tables.Sounds.TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
    }

    private static int getSoundType(CursorReader cursorReader) {
        return cursorReader.getInt(SoundStreamView.SOUND_TYPE);
    }
}
