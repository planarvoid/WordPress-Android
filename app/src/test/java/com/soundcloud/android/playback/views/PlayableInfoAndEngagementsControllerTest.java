package com.soundcloud.android.playback.views;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import rx.Observer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

@RunWith(DefaultTestRunner.class)
public class PlayableInfoAndEngagementsControllerTest {

    private PlayableInfoAndEngagementsController controller;
    private ViewGroup rootView;

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) DefaultTestRunner.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (ViewGroup) inflater.inflate(R.layout.player_action_bar, null);
        controller = new PlayableInfoAndEngagementsController(rootView, mock(PlayerTrackView.PlayerTrackViewListener.class));
    }

    @Test
    public void shouldShortenCountsOnToggleButtons() {
        expect(controller.labelForCount(999)).toEqual("999");
        expect(controller.labelForCount(1000)).toEqual("1k+");
        expect(controller.labelForCount(1999)).toEqual("1k+");
        expect(controller.labelForCount(2000)).toEqual("2k+");
        expect(controller.labelForCount(9999)).toEqual("9k+");
        expect(controller.labelForCount(10000)).toEqual("9k+"); // 4 chars would make the text spill over again
    }

    @Test
    public void shouldPublishSocialEventWhenSharingPlayable() {
        controller.setTrack(new Track(1L));

        Observer<SocialEvent> eventObserver = mock(Observer.class);
        EventBus.SOCIAL.subscribe(eventObserver);

        rootView.findViewById(R.id.btn_share).performClick();

        ArgumentCaptor<SocialEvent> socialEvent = ArgumentCaptor.forClass(SocialEvent.class);
        verify(eventObserver).onNext(socialEvent.capture());
        expect(socialEvent.getValue().getKind()).toBe(SocialEvent.SHARE);
        //TODO: no idea how to obtain this
        //expect(socialEvent.getValue().getAttributes().screenTag).toEqual("screen_tag");
    }

    @Test
    public void shouldNotPublishSocialEventWhenTrackIsNull() {
        Observer<SocialEvent> eventObserver = mock(Observer.class);
        EventBus.SOCIAL.subscribe(eventObserver);

        rootView.findViewById(R.id.btn_share).performClick();

        verify(eventObserver, never()).onNext(any(SocialEvent.class));
    }
}
