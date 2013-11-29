package com.soundcloud.android.explore;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.Event;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksFragmentTest {

    private ExploreTracksFragment fragment;

    @Mock
    private ExploreTracksAdapter adapter;
    @Mock
    private FragmentActivity activity;
    @Mock
    private Observer<String> screenTrackingObserver;

    @Before
    public void setUp() throws Exception {
        fragment = new ExploreTracksFragment();
        Robolectric.shadowOf(fragment).setActivity(activity);
    }

    @Ignore("TODO: breaks while inflating the PTR GridView")
    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWhenStarted() {
        fragment.onViewCreated(View.inflate(Robolectric.application, R.layout.suggested_tracks_fragment, null), null);
    }

    @Test
    public void shouldPublishScreenEnteredEventWithGivenTrackingTagInOnResume() {
        Subscription subscription = Event.SCREEN_ENTERED.subscribe(screenTrackingObserver);

        Bundle arguments = new Bundle();
        arguments.putString(ExploreTracksFragment.SCREEN_TRACKING_TAG_EXTRA, "explore:trending_music");
        fragment.setArguments(arguments);
        fragment.onResume();
        verify(screenTrackingObserver).onNext(eq("explore:trending_music"));

        verifyNoMoreInteractions(screenTrackingObserver);
        subscription.unsubscribe();
    }
}
