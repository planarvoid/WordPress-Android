package com.soundcloud.android.discovery.newforyou;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class NewForYouAdapterTest extends AndroidUnitTest {
    private static final List<Track> TRACKS = ModelFixtures.tracks(3);
    private static final NewForYou NEW_FOR_YOU = NewForYou.create(new TestDateProvider().getCurrentDate(), new Urn("fake:query:urn"), TRACKS);
    private static final NewForYouItem.NewForYouHeaderItem HEADER = NewForYouItem.NewForYouHeaderItem.create(NEW_FOR_YOU, "duration", "last_updated", Optional.absent());
    private static final NewForYouItem.NewForYouTrackItem FIRST = NewForYouItem.NewForYouTrackItem.create(NEW_FOR_YOU, ModelFixtures.trackItem(TRACKS.get(0)));
    private static final NewForYouItem.NewForYouTrackItem SECOND = NewForYouItem.NewForYouTrackItem.create(NEW_FOR_YOU, ModelFixtures.trackItem(TRACKS.get(1)));
    private static final NewForYouItem.NewForYouTrackItem THIRD = NewForYouItem.NewForYouTrackItem.create(NEW_FOR_YOU, ModelFixtures.trackItem(TRACKS.get(2)));

    @Mock private NewForYouHeaderRenderer.Listener headerItemListener;
    @Mock private TrackItemRenderer.Listener trackItemListener;
    @Mock private NewForYouHeaderRendererFactory headerRendererFactory;
    @Mock private NewForYouTrackRendererFactory trackRendererFactory;
    private NewForYouAdapter newForYouAdapter;

    @Before
    public void setUp() {
        this.newForYouAdapter = new NewForYouAdapter(headerItemListener, trackItemListener, headerRendererFactory, trackRendererFactory);
        newForYouAdapter.addItem(HEADER);
        newForYouAdapter.addItem(FIRST);
        newForYouAdapter.addItem(SECOND);
        newForYouAdapter.addItem(THIRD);
    }

    @Test
    public void updatesPlayingStateForCurrentPlayingTrack() {
        newForYouAdapter.updateNowPlaying(SECOND.track().getUrn());
        assertThat(((NewForYouItem.NewForYouTrackItem) newForYouAdapter.getItem(1)).track().isPlaying()).isFalse();
        assertThat(((NewForYouItem.NewForYouTrackItem) newForYouAdapter.getItem(2)).track().isPlaying()).isTrue();
        assertThat(((NewForYouItem.NewForYouTrackItem) newForYouAdapter.getItem(3)).track().isPlaying()).isFalse();
    }
}
