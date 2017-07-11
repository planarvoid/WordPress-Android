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
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.users.User;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class StreamEntityToItemTransformerTest {

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
        when(trackRepository.fromUrns(anyList())).thenReturn(Single.just(Collections.emptyMap()));
        when(playlistRepository.withUrns(anyCollection())).thenReturn(Single.just(Collections.emptyMap()));
    }

    @Test
    public void enrichesTrackStreamItem() throws Exception {
        final TrackItem trackItem = ModelFixtures.trackItem(track);
        final StreamEntity streamEntity = builderFromImageResource(CREATED_AT, trackItem.getUrn(), trackItem).build();

        when(trackRepository.fromUrns(eq(Lists.newArrayList(track.urn())))).thenReturn(Single.just(Collections.singletonMap(track.urn(), track)));

        final TrackStreamItem trackStreamItem = TrackStreamItem.create(trackItem, streamEntity.createdAt(), streamEntity.avatarUrlTemplate());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(trackStreamItem);

        transformer.apply(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    @Test
    public void enrichesPromotedTrackStreamItem() throws Exception {
        final TrackItem promotedTrackItem = ModelFixtures.promotedTrackItem(track, promoter);
        final StreamEntity streamEntity = fromPromotedTrackItem(CREATED_AT, promotedTrackItem);

        when(trackRepository.fromUrns(eq(Lists.newArrayList(track.urn())))).thenReturn(Single.just(Collections.singletonMap(track.urn(), track)));

        final TrackStreamItem promotedTrackStreamItem = TrackStreamItem.create(promotedTrackItem, streamEntity.createdAt(), streamEntity.avatarUrlTemplate());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(promotedTrackStreamItem);

        transformer.apply(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    @Test
    public void enrichesPlaylistStreamItem() throws Exception {
        final PlaylistItem playlistItem = PlaylistItem.from(playlist);
        final StreamEntity streamEntity = builderFromImageResource(CREATED_AT, playlistItem.getUrn(), playlistItem).build();

        when(playlistRepository.withUrns(eq(Lists.newArrayList(playlist.urn())))).thenReturn(Single.just(Collections.singletonMap(playlist.urn(), playlist)));

        final PlaylistStreamItem playlistStreamItem = PlaylistStreamItem.create(playlistItem, streamEntity.createdAt(), streamEntity.avatarUrlTemplate());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(playlistStreamItem);

        transformer.apply(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    @Test
    public void enrichesPromotedPlaylistStreamItem() throws Exception {
        final PlaylistItem promotedPlaylistItem = ModelFixtures.promotedPlaylistItem(playlist, promoter);
        final StreamEntity streamEntity = fromPromotedPlaylistItem(CREATED_AT, promotedPlaylistItem);

        when(playlistRepository.withUrns(eq(Lists.newArrayList(playlist.urn())))).thenReturn(Single.just(Collections.singletonMap(playlist.urn(), playlist)));

        final PlaylistStreamItem promotedPlaylistStreamItem = PlaylistStreamItem.create(promotedPlaylistItem, streamEntity.createdAt(), streamEntity.avatarUrlTemplate());
        final ArrayList<StreamItem> expectedResult = Lists.newArrayList(promotedPlaylistStreamItem);

        transformer.apply(Lists.newArrayList(streamEntity)).test().assertValueCount(1).assertValue(expectedResult);
    }

    private StreamEntity.Builder builderFromImageResource(Date createdAt, Urn urn, PlayableItem trackItem) {
        return StreamEntity.builder(urn, createdAt).repostedProperties(trackItem.repostedProperties());
    }

    private StreamEntity fromPromotedTrackItem(Date createdAt, TrackItem trackItem) {
        return builderFromImageResource(createdAt, trackItem.getUrn(), trackItem).promotedProperties(trackItem.promotedProperties()).build();
    }

    private StreamEntity fromPromotedPlaylistItem(Date createdAt, PlaylistItem playlistItem) {
        return builderFromImageResource(createdAt, playlistItem.getUrn(), playlistItem).promotedProperties(playlistItem.promotedProperties()).build();
    }
}
