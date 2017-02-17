package com.soundcloud.android.discovery.newforyou;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.newforyou.NewForYouItem.NewForYouHeaderItem;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NewForYouPresenterTest extends AndroidUnitTest {
    private static final Urn QUERY_URN = new Urn("my:fake:urn");
    private static final Date DATE = new TestDateProvider().getCurrentDate();
    private static final List<Track> TRACKS = ModelFixtures.tracks(1);
    private static final TrackItem TRACK_ITEM = TrackItem.from(TRACKS.get(0));
    private static final ArrayList<ImageResource> IMAGE_RESOURCES = newArrayList(TRACK_ITEM);
    private static final NewForYou NEW_FOR_YOU = NewForYou.create(DATE, QUERY_URN, TRACKS);
    public static final String DURATION = "duration";
    private static final String LAST_UPDATED = "last_updated";
    private static final NewForYouItem.NewForYouTrackItem NEW_FOR_YOU_TRACK_ITEM = NewForYouItem.NewForYouTrackItem.create(NEW_FOR_YOU, TRACK_ITEM);
    private static final NewForYouHeaderItem NEW_FOR_YOU_HEADER_ITEM = NewForYouHeaderItem.create(NEW_FOR_YOU, DURATION, LAST_UPDATED, IMAGE_RESOURCES);
    private static final ArrayList<NewForYouItem> ADAPTER_ITEMS = newArrayList(
            NEW_FOR_YOU_HEADER_ITEM,
            NEW_FOR_YOU_TRACK_ITEM);

    @Rule public final FragmentRule fragmentRule =
            new FragmentRule(R.layout.default_recyclerview_with_refresh, new Bundle());

    @Mock SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock NewForYouOperations newForYouOperations;
    @Mock NewForYouAdapterFactory newForYouAdapterFactory;
    @Mock NewForYouAdapter newForYouAdapter;
    @Mock PlaybackInitiator playbackInitiator;
    @Mock Resources resources;
    @Mock NewForYouHeaderRenderer.Listener headerListener;
    @Mock TrackItemRenderer.Listener trackListener;
    @Mock Fragment fragment;

    @Captor ArgumentCaptor<Iterable<NewForYouItem>> itemsCaptor;

    private final EventBus eventBus = new TestEventBus();

    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerSubscriber();

    private NewForYouPresenter presenter;

    @Before
    public void setUp() {
        when(newForYouAdapterFactory.create(any(NewForYouHeaderRenderer.Listener.class), any(TrackItemRenderer.Listener.class))).thenReturn(newForYouAdapter);
        when(newForYouOperations.newForYou()).thenReturn(Observable.just(NEW_FOR_YOU));
        when(resources.getString(eq(R.string.new_for_you_duration), any(Object.class), any(Object.class))).thenReturn(DURATION);
        when(resources.getQuantityString(eq(R.plurals.elapsed_seconds_ago), any(Integer.class), any(Integer.class))).thenReturn(LAST_UPDATED);
        when(playbackInitiator.playTracks(any(List.class), any(Integer.class), any(PlaySessionSource.class))).thenReturn(Observable.just(mock(PlaybackResult.class)));

        presenter = new NewForYouPresenter(swipeRefreshAttacher,
                                           newForYouOperations,
                                           newForYouAdapterFactory,
                                           playbackInitiator,
                                           expandPlayerSubscriberProvider,
                                           resources,
                                           eventBus);
    }

    @Test
    public void mapsNewForYouToViewModels() {
        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(newForYouAdapter).onNext(ADAPTER_ITEMS);
    }

    @Test
    public void trackItemClickedStartsPlaybackFromCorrectPosition() {
        final int position = 1;
        final int finalPosition = 0;
        final PlaySessionSource playSessionSource = PlaySessionSource.forNewForYou(Screen.NEW_FOR_YOU.get(),
                                                                                   finalPosition,
                                                                                   QUERY_URN);

        when(newForYouAdapter.getItems()).thenReturn(ADAPTER_ITEMS);
        when(newForYouAdapter.getItem(position)).thenReturn(NEW_FOR_YOU_TRACK_ITEM);
        when(newForYouAdapter.getItemCount()).thenReturn(ADAPTER_ITEMS.size());

        presenter.trackItemClicked(TRACKS.get(0).urn(), position);

        verify(playbackInitiator).playTracks(newArrayList(TRACKS.get(0).urn()), finalPosition, playSessionSource);
    }

    @Test
    public void playClickedStartsPlaybackFromBeginning() {
        final int adapterPosition = 0;
        final int playbackPosition = 0;
        final PlaySessionSource playSessionSource = PlaySessionSource.forNewForYou(Screen.NEW_FOR_YOU.get(),
                                                                                   playbackPosition,
                                                                                   QUERY_URN);

        when(newForYouAdapter.getItems()).thenReturn(ADAPTER_ITEMS);
        when(newForYouAdapter.getItem(adapterPosition)).thenReturn(NEW_FOR_YOU_TRACK_ITEM);
        when(newForYouAdapter.getItemCount()).thenReturn(ADAPTER_ITEMS.size());

        presenter.playClicked();

        verify(playbackInitiator).playTracks(newArrayList(TRACKS.get(0).urn()), playbackPosition, playSessionSource);
    }
}
