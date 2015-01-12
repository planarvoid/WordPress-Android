package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.view.View;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;


@RunWith(SoundCloudTestRunner.class)
public class ShuffleViewControllerTest {

    @Mock private PlaybackOperations playbackOperations;

    private TestEventBus eventBus = new TestEventBus();
    private ShuffleViewController controller;

    @Before
    public void setUp() throws Exception {
        controller = new ShuffleViewController(TestSubscribers.expandPlayerSubscriber(), playbackOperations, eventBus);
        View view = mock(View.class);
        when(view.getContext()).thenReturn(Robolectric.application);
        controller.onViewCreated(view, null);
    }

    @Test
    public void hideShuffleButtonForEmptyTrackLikeList() {
        View shuffleButton = emitLikedTracksAndGetShuffleButton(Collections.<Urn>emptyList());
        expect(shuffleButton).toBeGone();
    }

    @Test
    public void disableShuffleButtonForEmptyTrackLikeList() {
        View shuffleButton = emitLikedTracksAndGetShuffleButton(Collections.<Urn>emptyList());
        expect(shuffleButton).not.toBeEnabled();
    }

    @Test
    public void hideShuffleButtonForOneTrackLike() {
        View shuffleButton = emitLikedTracksAndGetShuffleButton(Lists.newArrayList(Urn.forTrack(123L)));
        expect(shuffleButton).toBeGone();
    }

    @Test
    public void disableShuffleButtonForOneTrackLike() {
        View shuffleButton = emitLikedTracksAndGetShuffleButton(Lists.newArrayList(Urn.forTrack(123L)));
        expect(shuffleButton).not.toBeEnabled();
    }

    @Test
    public void displayShuffleButtonForTrackLikeList() {
        View shuffleButton = emitLikedTracksAndGetShuffleButton(Lists.newArrayList(Urn.forTrack(123L), Urn.forTrack(234L)));
        expect(shuffleButton).toBeVisible();
    }

    @Test
    public void enableShuffleButtonForTrackLikeList() {
        View shuffleButton = emitLikedTracksAndGetShuffleButton(Lists.newArrayList(Urn.forTrack(123L), Urn.forTrack(234L)));
        expect(shuffleButton).toBeEnabled();
    }

    @Test
    public void showNumberOfLikedTracksForEmptyTrackLikeList() {
        TextView shuffleTextView = emitLikedTracksAndGetShuffleText(Collections.<Urn>emptyList());
        expect(shuffleTextView.getText().toString()).toEqual(Robolectric.application.getResources()
                .getString(R.string.number_of_liked_tracks_you_liked_zero));
    }

    @Test
    public void showNumberOfLikedTracksForTrackLikeList() {
        TextView shuffleTextView = emitLikedTracksAndGetShuffleText(Lists.newArrayList(Urn.forTrack(123L)));
        expect(shuffleTextView.getText().toString()).toEqual(Robolectric.application.getResources()
                .getQuantityString(R.plurals.number_of_liked_tracks_you_liked, 1, 1));
    }

    @Test
    public void emitTrackingEventOnShuffleButtonClick() {
        when(playbackOperations.playTracksShuffled(anyList(), any(PlaySessionSource.class)))
                .thenReturn(Observable.<List<Urn>>empty());
        View buttonShuffle = controller.getHeaderView().findViewById(R.id.shuffle_btn);

        buttonShuffle.performClick();

        expect(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).toEqual(UIEvent.KIND_SHUFFLE_LIKES);
    }

    private View emitLikedTracksAndGetShuffleButton(List<Urn> urnList) {
        Observable observable = Observable.just(urnList);
        observable.subscribe(controller);
        return controller.getHeaderView().findViewById(R.id.shuffle_btn);
    }

    private TextView emitLikedTracksAndGetShuffleText(List<Urn> urnList) {
        Observable observable = Observable.just(urnList);
        observable.subscribe(controller);
        return (TextView)controller.getHeaderView().findViewById(R.id.shuffle_txt);
    }

}