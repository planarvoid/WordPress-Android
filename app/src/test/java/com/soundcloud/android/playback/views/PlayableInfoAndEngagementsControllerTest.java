package com.soundcloud.android.playback.views;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.UIEvent;
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
import android.widget.ToggleButton;

@RunWith(DefaultTestRunner.class)
public class PlayableInfoAndEngagementsControllerTest {

    private PlayableInfoAndEngagementsController controller;
    private ViewGroup rootView;

    @Before
    public void setup() {
        LayoutInflater inflater = (LayoutInflater) DefaultTestRunner.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = (ViewGroup) inflater.inflate(R.layout.player_action_bar, null);
        controller = new PlayableInfoAndEngagementsController(rootView, mock(PlayerTrackView.PlayerTrackViewListener.class), Screen.PLAYER_MAIN);
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
    public void shouldPublishUIEventWhenLikingPlayable() {
        controller.setTrack(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        EventBus.UI.subscribe(eventObserver);

        rootView.findViewById(R.id.toggle_like).performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.LIKE);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.PLAYER_MAIN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnlikingPlayable() {
        controller.setTrack(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        EventBus.UI.subscribe(eventObserver);

        ToggleButton likeToggle = (ToggleButton) rootView.findViewById(R.id.toggle_like);
        likeToggle.setChecked(true);
        likeToggle.performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.UNLIKE);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.PLAYER_MAIN.get());
    }

    @Test
    public void shouldPublishUIEventWhenRepostingPlayable() {
        controller.setTrack(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        EventBus.UI.subscribe(eventObserver);

        rootView.findViewById(R.id.toggle_repost).performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.REPOST);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.PLAYER_MAIN.get());
    }

    @Test
    public void shouldPublishUIEventWhenUnrepostingPlayable() {
        controller.setTrack(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        EventBus.UI.subscribe(eventObserver);

        ToggleButton repostToggle = (ToggleButton) rootView.findViewById(R.id.toggle_repost);
        repostToggle.setChecked(true);
        repostToggle.performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.UNREPOST);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.PLAYER_MAIN.get());
    }


    @Test
    public void shouldPublishUIEventWhenSharingPlayable() {
        controller.setTrack(new Track(1L));

        Observer<UIEvent> eventObserver = mock(Observer.class);
        EventBus.UI.subscribe(eventObserver);

        rootView.findViewById(R.id.btn_share).performClick();

        ArgumentCaptor<UIEvent> uiEvent = ArgumentCaptor.forClass(UIEvent.class);
        verify(eventObserver).onNext(uiEvent.capture());
        expect(uiEvent.getValue().getKind()).toBe(UIEvent.SHARE);
        expect(uiEvent.getValue().getAttributes().get("context")).toEqual(Screen.PLAYER_MAIN.get());
    }

    @Test
    public void shouldNotPublishUIEventWhenTrackIsNull() {
        Observer<UIEvent> eventObserver = mock(Observer.class);
        EventBus.UI.subscribe(eventObserver);

        rootView.findViewById(R.id.btn_share).performClick();

        verify(eventObserver, never()).onNext(any(UIEvent.class));
    }
}
