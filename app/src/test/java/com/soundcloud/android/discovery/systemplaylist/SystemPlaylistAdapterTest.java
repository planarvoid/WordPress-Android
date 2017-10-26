package com.soundcloud.android.discovery.systemplaylist;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;
import java.util.List;

public class SystemPlaylistAdapterTest extends AndroidUnitTest {
    private static final Optional<Urn> QUERY_URN = Optional.of(new Urn("my:fake:systemPlaylistUrn"));
    private static final Optional<Date> DATE = Optional.of(new TestDateProvider().getCurrentDate());
    private static final List<Track> TRACKS = TrackFixtures.tracks(3);
    private static final TrackItem FIRST_TRACK_ITEM = TrackFixtures.trackItem(TRACKS.get(0));
    private static final TrackItem SECOND_TRACK_ITEM = TrackFixtures.trackItem(TRACKS.get(1));
    private static final TrackItem THIRD_TRACK_ITEM = TrackFixtures.trackItem(TRACKS.get(2));
    private static final Urn URN = Urn.forSystemPlaylist("123");
    private static final Optional<String> TITLE = Optional.of("Title");
    private static final Optional<String> DESCRIPTION = Optional.of("Description");
    private static final Optional<String> ARTWORK_URL = Optional.of("https://cool.artwork/url.jpg");
    private static final Optional<String> TRACKING_FEATURE_NAME = Optional.of("The Upload");
    private static final SystemPlaylist SYSTEM_PLAYLIST = SystemPlaylist.create(URN, QUERY_URN, TITLE, DESCRIPTION, TRACKS, DATE, ARTWORK_URL, TRACKING_FEATURE_NAME);
    private static final String METADATA = "duration";
    private static final Optional<String> LAST_UPDATED = Optional.of("last_updated");

    private static final SystemPlaylistItem.Header HEADER = SystemPlaylistItem.Header.create(URN,
                                                                                             TITLE,
                                                                                             DESCRIPTION,
                                                                                             METADATA,
                                                                                             LAST_UPDATED,
                                                                                             SYSTEM_PLAYLIST.imageResource(),
                                                                                             QUERY_URN,
                                                                                             TRACKING_FEATURE_NAME,
                                                                                             true);

    private static final SystemPlaylistItem.Track FIRST = SystemPlaylistItem.Track.create(URN, FIRST_TRACK_ITEM, QUERY_URN, TRACKING_FEATURE_NAME);
    private static final SystemPlaylistItem.Track SECOND = SystemPlaylistItem.Track.create(URN, SECOND_TRACK_ITEM, QUERY_URN, TRACKING_FEATURE_NAME);
    private static final SystemPlaylistItem.Track THIRD = SystemPlaylistItem.Track.create(URN, THIRD_TRACK_ITEM, QUERY_URN, TRACKING_FEATURE_NAME);

    @Mock private SystemPlaylistHeaderRenderer.Listener headerItemListener;
    @Mock private TrackItemRenderer.Listener trackItemListener;
    @Mock private SystemPlaylistHeaderRendererFactory headerRendererFactory;
    @Mock private SystemPlaylistTrackRendererFactory trackRendererFactory;
    private SystemPlaylistAdapter systemPlaylistAdapter;

    @Before
    public void setUp() {
        this.systemPlaylistAdapter = new SystemPlaylistAdapter(headerItemListener, trackItemListener, headerRendererFactory, trackRendererFactory);
        systemPlaylistAdapter.addItem(HEADER);
        systemPlaylistAdapter.addItem(FIRST);
        systemPlaylistAdapter.addItem(SECOND);
        systemPlaylistAdapter.addItem(THIRD);
    }

    @Test
    public void updatesPlayingStateForCurrentPlayingTrack() {
        systemPlaylistAdapter.updateNowPlaying(SECOND.track().getUrn());
        assertThat(((SystemPlaylistItem.Track) systemPlaylistAdapter.getItem(1)).track().isPlaying()).isFalse();
        assertThat(((SystemPlaylistItem.Track) systemPlaylistAdapter.getItem(2)).track().isPlaying()).isTrue();
        assertThat(((SystemPlaylistItem.Track) systemPlaylistAdapter.getItem(3)).track().isPlaying()).isFalse();
    }
}
