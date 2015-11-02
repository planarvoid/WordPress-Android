package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.Context;
import android.net.Uri;

import javax.inject.Provider;

public class SuggestionsHelperTest extends AndroidUnitTest {

    private static final Urn SUGGESTED_TRACK_URN = Urn.forTrack(4L);
    private static final Urn SUGGESTED_USER_URN = Urn.forUser(5L);

    private SuggestionsHelper suggestionsHelper;

    @Mock private Navigator navigator;
    @Mock private EventBus eventBus;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private SuggestionsAdapter adapter;

    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);

    @Before
    public void setUp() {
        when(adapter.getItemIntentData(anyInt())).thenReturn(Uri.EMPTY);
        when(adapter.getQueryUrn(anyInt())).thenReturn(Urn.NOT_SET);
        suggestionsHelper = new SuggestionsHelper(navigator, eventBus, expandPlayerSubscriberProvider,
                playbackInitiator, adapter);
    }

    @Test
    public void shouldPlayTrackFromSearchResults() {
        when(adapter.getUrn(anyInt())).thenReturn(SUGGESTED_TRACK_URN);
        when(playbackInitiator.startPlaybackWithRecommendations(eq(SUGGESTED_TRACK_URN), eq(Screen.SEARCH_SUGGESTIONS),
                any(SearchQuerySourceInfo.class))).thenReturn(Observable.just(PlaybackResult.success()));

        suggestionsHelper.launchSuggestion(context(), 2);

        verify(playbackInitiator).startPlaybackWithRecommendations(eq(SUGGESTED_TRACK_URN),
                eq(Screen.SEARCH_SUGGESTIONS), any(SearchQuerySourceInfo.class));
    }

    @Test
    public void shouldLaunchSearchSuggestionUser() {
        when(adapter.getUrn(anyInt())).thenReturn(SUGGESTED_USER_URN);
        final Context context = context();

        suggestionsHelper.launchSuggestion(context, 2);

        verify(navigator).launchSearchSuggestion(eq(context), eq(SUGGESTED_USER_URN), any(SearchQuerySourceInfo.class),
                eq(Uri.EMPTY));
    }
}