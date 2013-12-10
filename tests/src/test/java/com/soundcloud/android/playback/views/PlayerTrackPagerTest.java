package com.soundcloud.android.playback.views;

import android.support.v4.view.PagerAdapter;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.playback.PlayerTrackPagerAdapter;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observer;
import rx.Subscription;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SoundCloudTestRunner.class)
public class PlayerTrackPagerTest {

    PlayerTrackPager playerTrackPager;
    @Mock
    PlayerTrackPager.OnPageChangeListener pageListener;
    @Mock
    Observer<String> observer;

    @Before
    public void setup(){
        playerTrackPager = new PlayerTrackPager(Robolectric.application, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfPlayerTrackPagerAdapterNotUsed(){
        playerTrackPager.setAdapter(Mockito.mock(PagerAdapter.class));
    }

    @Test
    public void shouldAllowNextOperationIfNotViewingLastItem(){
        final PlayerTrackPagerAdapter adapter = Mockito.mock(PlayerTrackPagerAdapter.class);
        when(adapter.getCount()).thenReturn(2);

        playerTrackPager.setAdapter(adapter);
        playerTrackPager.setCurrentItem(0);
        playerTrackPager.setOnPageChangeListener(pageListener);

        expect(playerTrackPager.next()).toBeTrue();
        verify(pageListener).onPageSelected(1);
    }

    @Test
    public void shouldNotAllowNextOperationIfViewingLastItem(){
        final PlayerTrackPagerAdapter adapter = Mockito.mock(PlayerTrackPagerAdapter.class);
        when(adapter.getCount()).thenReturn(1);

        playerTrackPager.setAdapter(adapter);
        playerTrackPager.setCurrentItem(0);
        playerTrackPager.setOnPageChangeListener(pageListener);

        expect(playerTrackPager.next()).toBeFalse();
        verifyZeroInteractions(pageListener);
    }

    @Test
    public void shouldPublishScreenEnteredEventWhenFlippingThroughTracks() {
        Subscription subscription = Event.SCREEN_ENTERED.subscribe(observer);

        playerTrackPager.setCurrentItem(1);
        verify(observer).onNext(eq("tracks:main"));

        subscription.unsubscribe();
    }
}
