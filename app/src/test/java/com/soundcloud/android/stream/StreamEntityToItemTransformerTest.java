package com.soundcloud.android.stream;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.users.User;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class StreamEntityToItemTransformerTest extends AndroidUnitTest {

    private static final Date CREATED_AT = new Date();
    private final User promoter = ModelFixtures.user();
    private final Track track = ModelFixtures.track();
    private final Playlist playlist = ModelFixtures.playlist();

    @Mock private TrackRepository trackRepository;
    @Mock private PlaylistRepository playlistRepository;
    private StreamEntityToItemTransformer transformer;

    @Before
    public void setUp() throws Exception {
        transformer = new StreamEntityToItemTransformer(trackRepository, playlistRepository, ModelFixtures.entityItemCreator());
        when(trackRepository.fromUrns(anyList())).thenReturn(Observable.just(Collections.emptyMap()));
        when(playlistRepository.withUrns(anyCollection())).thenReturn(Observable.just(Collections.emptyMap()));
    }

    @Test
    public void enrichesTrackStreamItem() throws Exception {
        final TrackItem trackItem = ModelFixtures.trackItem(track);
        final StreamEntity streamEntity = builderFromImageResource(CREATED_AT, trackItem.getUrn(), trackItem.avatarUrlTemplate()).build();

        when(trackRepository.fromUrns(eq(Lists.newArrayList(track.urn())))).thenReturn(Observable.just(Collections.singletonMap(track.urn(), track)));

        final TrackStreamItem trackStreamItem = TrackStreamItem.create(trackItem, streamEntity.createdAt());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(trackStreamItem);

        transformer.call(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    @Test
    public void enrichesPromotedTrackStreamItem() throws Exception {
        final TrackItem promotedTrackItem = ModelFixtures.promotedTrackItem(track, promoter);
        final StreamEntity streamEntity = fromPromotedTrackItem(CREATED_AT, promotedTrackItem);

        when(trackRepository.fromUrns(eq(Lists.newArrayList(track.urn())))).thenReturn(Observable.just(Collections.singletonMap(track.urn(), track)));

        final TrackStreamItem promotedTrackStreamItem = TrackStreamItem.create(promotedTrackItem, streamEntity.createdAt());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(promotedTrackStreamItem);

        transformer.call(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    @Test
    public void enrichesPlaylistStreamItem() throws Exception {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem(playlist);
        final StreamEntity streamEntity = builderFromImageResource(CREATED_AT, playlistItem.getUrn(), playlistItem.avatarUrlTemplate()).build();

        when(playlistRepository.withUrns(eq(Lists.newArrayList(playlist.urn())))).thenReturn(Observable.just(Collections.singletonMap(playlist.urn(), playlist)));

        final PlaylistStreamItem playlistStreamItem = PlaylistStreamItem.create(playlistItem, streamEntity.createdAt());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(playlistStreamItem);

        transformer.call(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    @Test
    public void enrichesPromotedPlaylistStreamItem() throws Exception {
        final PlaylistItem promotedPlaylistItem = ModelFixtures.promotedPlaylistItem(playlist, promoter);
        final StreamEntity streamEntity = fromPromotedPlaylistItem(CREATED_AT, promotedPlaylistItem);

        when(playlistRepository.withUrns(eq(Lists.newArrayList(playlist.urn())))).thenReturn(Observable.just(Collections.singletonMap(playlist.urn(), playlist)));

        final PlaylistStreamItem promotedPlaylistStreamItem = PlaylistStreamItem.create(promotedPlaylistItem, streamEntity.createdAt());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(promotedPlaylistStreamItem);

        transformer.call(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    private StreamEntity.Builder builderFromImageResource(Date createdAt, Urn urn, Optional<String> avatarUrlTemplate) {
        return StreamEntity.builder(urn, createdAt, Optional.absent(), Optional.absent(), avatarUrlTemplate);
    }

    private StreamEntity fromPromotedTrackItem(Date createdAt, TrackItem trackItem) {
        return builderFromImageResource(createdAt, trackItem.getUrn(), trackItem.avatarUrlTemplate()).promotedProperties(trackItem.promotedProperties()).build();
    }

    private StreamEntity fromPromotedPlaylistItem(Date createdAt, PlaylistItem playlistItem) {
        return builderFromImageResource(createdAt, playlistItem.getUrn(), playlistItem.avatarUrlTemplate()).promotedProperties(playlistItem.promotedProperties()).build();
    }
}
