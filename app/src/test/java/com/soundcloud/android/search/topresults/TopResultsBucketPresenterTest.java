package com.soundcloud.android.search.topresults;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import io.reactivex.subjects.BehaviorSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TopResultsBucketPresenterTest {
    private static final TopResultsBucketViewModel.Kind KIND = TopResultsBucketViewModel.Kind.GO_TRACKS;
    private static final Urn QUERY_URN = new Urn("soundcloud:query:urn");

    @Mock EventTracker eventTracker;
    @Mock ReferringEventProvider referringEventProvider;
    @Mock TopResultsBucketPresenter.TopResultsBucketView view;

    private final BehaviorSubject<Long> enterScreen = BehaviorSubject.create();

    private TopResultsBucketPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new TopResultsBucketPresenter(eventTracker, referringEventProvider);
        when(view.enterScreenTimestamp()).thenReturn(enterScreen);
    }

    @Test
    public void trackEnterScreen() throws Exception {
        when(view.getKind()).thenReturn(KIND);
        when(view.getQueryUrn()).thenReturn(Optional.of(QUERY_URN));
        final Optional<ReferringEvent> referringEventOptional = Optional.of(ReferringEvent.create("123", "kind"));
        when(referringEventProvider.getReferringEvent()).thenReturn(referringEventOptional);

        presenter.attachView(view);

        enterScreen.onNext(123L);

        verify(eventTracker).trackScreen(eq(ScreenEvent.create(Screen.SEARCH_TRACKS.get(), Optional.of(QUERY_URN))), eq(referringEventOptional));
    }
}
