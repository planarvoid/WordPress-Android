package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PlaySessionOriginScreenProviderTest {
    private final TestEventBus eventBus = new TestEventBus();
    private final Screen fallbackScreen = Screen.LIKES;
    private final String fallbackScreenName = fallbackScreen.get();
    private final Urn URN = Urn.forTrack(123L);
    private PlaySessionOriginScreenProvider screenProvider;

    @Mock private ScreenProvider fallbackProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(fallbackProvider.getLastScreen()).thenReturn(fallbackScreen);
        screenProvider = new PlaySessionOriginScreenProvider(eventBus, fallbackProvider);
    }

    @Test
    public void returnsValueFromFallbackProviderWhenNoScreen() {
        screenProvider.subscribe();

        assertThat(screenProvider.getOriginScreen()).isEqualTo(fallbackScreenName);
    }

    @Test
    public void ignoresQueueWhenNotSubscribedAndReturnValueFromFallbackProvider() {
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forFilter(PlaylistsOptions.SHOW_ALL));

        assertThat(screenProvider.getOriginScreen()).isEqualTo(fallbackScreenName);
    }

    @Test
    public void returnsCollectionPageNameIfTheUserNavigatedToCollectionsAndThenOpenedPlaylistFromTheRecentlyPlayedBucket() {
        screenProvider.subscribe();

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(URN, Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.PLAYLISTS));

        assertThat(screenProvider.getOriginScreen()).isEqualTo(Screen.COLLECTIONS.get());
    }

    @Test
    public void returnsCollectionPageNameIfTheUserNavigatedToCollectionsAndThenOpenedStationFromTheRecentlyPlayedBucket() {
        screenProvider.subscribe();

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(URN, Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.STATIONS_INFO));

        assertThat(screenProvider.getOriginScreen()).isEqualTo(Screen.COLLECTIONS.get());
    }

    @Test
    public void returnsCollectionPageNameIfTheUserNavigatedToCollectionsAndThenOpenedProfileFromTheRecentlyPlayedBucket() {
        screenProvider.subscribe();

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(URN, Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.USER_MAIN));

        assertThat(screenProvider.getOriginScreen()).isEqualTo(Screen.COLLECTIONS.get());
    }

    @Test
    public void returnsFallbackPageNameIfTheUserNavigateAwayFromCollections() {
        screenProvider.subscribe();

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(URN, Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.ACTIVITIES));

        assertThat(screenProvider.getOriginScreen()).isEqualTo(fallbackScreenName);
    }

    @Test
    public void returnsFallbackPageNameIfTheUserCameBackAndNavigatedToSomewhereElse() {
        screenProvider.subscribe();

        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(URN, Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.COLLECTIONS));
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.USER_MAIN));

        assertThat(screenProvider.getOriginScreen()).isEqualTo(fallbackScreenName);
    }
}
