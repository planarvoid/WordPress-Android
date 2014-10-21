package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.leaveBehindForPlayer;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.leaveBehindForPlayerWithDisplayMetaData;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LeaveBehindEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class LeaveBehindPresenterTest {

    private PropertySet properties;
    private LeaveBehindPresenter presenter;
    AdOverlayPresenter adOverlayPresenter;

    @Mock AdOverlayPresenter.Listener listener;
    @Mock View trackView;
    @Mock private View overlay;
    @Mock private View closeStub;
    @Mock private ImageView imageStub;
    @Mock private ViewStub overlayStub;
    private TestEventBus eventBus;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        when(trackView.findViewById(R.id.leave_behind_stub)).thenReturn(overlayStub);
        when(overlayStub.inflate()).thenReturn(overlay);
        when(overlay.findViewById(R.id.leave_behind_close)).thenReturn(closeStub);
        when(overlay.findViewById(R.id.leave_behind_image)).thenReturn(imageStub);

        presenter = new LeaveBehindPresenter(trackView, listener, eventBus);
    }

    @Test
    public void createsLeaveBehindPresenterFromLeaveBehindPropertySet() throws Exception {
        adOverlayPresenter = AdOverlayPresenter.create(TestPropertySets.leaveBehindForPlayer(), trackView, listener, eventBus);
        expect(adOverlayPresenter).toBeInstanceOf(LeaveBehindPresenter.class);
    }

    @Test
    public void shouldNotShowLeaveBehindWhenAdWasClicked() {
        properties = leaveBehindForPlayer().put(LeaveBehindProperty.META_AD_CLICKED, true);

        expect(presenter.shouldDisplayOverlay(properties, true, true, true)).toBeFalse();
    }

    @Test
    public void shouldNotShowLeaveBehindWhenAdWasNotCompleted() {
        properties = leaveBehindForPlayer().put(LeaveBehindProperty.META_AD_COMPLETED, false);

        expect(presenter.shouldDisplayOverlay(properties, true, true, true)).toBeFalse();
    }

    @Test
    public void shouldNotShowTheLeaveBehindIfAlreadyShown() {
        properties = leaveBehindForPlayerWithDisplayMetaData().put(LeaveBehindProperty.META_AD_COMPLETED, false);

        expect(presenter.shouldDisplayOverlay(properties, true, true, true)).toBeFalse();
    }

    @Test
    public void showsLeaveBehindWhenAdWasCompletedAndNotClicked() {
        properties = leaveBehindForPlayerWithDisplayMetaData();

        expect(presenter.shouldDisplayOverlay(properties, true, true, true)).toBeTrue();
    }

    @Test
    public void sendAnEventWhenVisible() {
        presenter.setVisible();

        expect(eventBus.eventsOn(EventQueue.LEAVE_BEHIND)).toNumber(1);
        expect(eventBus.lastEventOn(EventQueue.LEAVE_BEHIND).getKind()).toEqual(LeaveBehindEvent.SHOWN);
    }

    @Test
    public void sendAnEventWhenInVisible() {
        presenter.setInvisible();

        expect(eventBus.eventsOn(EventQueue.LEAVE_BEHIND)).toNumber(1);
        expect(eventBus.lastEventOn(EventQueue.LEAVE_BEHIND).getKind()).toEqual(LeaveBehindEvent.HIDDEN);
    }

    @Test
    public void isNotFullScreen() throws Exception {
        expect(presenter.isFullScreen()).toBeFalse();
    }
}