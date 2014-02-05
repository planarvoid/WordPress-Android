package com.soundcloud.android.explore;


import android.content.res.Resources;
import android.support.v4.view.ViewPager;
import android.view.View;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.injection.MockInjector;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.viewpagerindicator.TabPageIndicator;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Module;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.explore.ExploreFragment.ExplorePagerScreenListener;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SoundCloudTestRunner.class)
public class ExploreFragmentTest {


    @Module(injects = ExploreFragment.class, library = true)
    public class TestModule {
        @Provides
        public Resources provideResources() {
            return Robolectric.application.getResources();
        }

        @Provides
        public EventBus provideEventBus() {
            return eventBus;
        }
    }

    @Mock
    private View mockLayout;
    @Mock
    private TabPageIndicator mockIndicator;
    @Mock
    private ViewPager mockViewPager;
    @Mock
    private EventBus eventBus;

    private ExploreFragment mExploreFragment;
    private EventMonitor eventMonitor;

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.indicator)).thenReturn(mockIndicator);

        eventMonitor = EventMonitor.on(eventBus);
        mExploreFragment = new ExploreFragment(MockInjector.createInjector(new TestModule()));
        mExploreFragment.onCreate(null);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents(){
        when(mockLayout.findViewById(R.id.indicator)).thenReturn(mockIndicator);
        mExploreFragment.onViewCreated(mockLayout, null);
        verify(mockIndicator).setOnPageChangeListener(isA(ExplorePagerScreenListener.class));
    }

    @Test
    public void shouldTrackGenresScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(0);
        String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("explore:genres");
    }

    @Test
    public void shouldTrackTrendingMusicScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(1);
        String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("explore:trending_music");
    }

    @Test
    public void shouldTrackTrendingAudioScreenOnPageSelected() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(2);
        String screenTag = eventMonitor.verifyEventOn(EventQueue.SCREEN_ENTERED);
        expect(screenTag).toEqual("explore:trending_audio");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnknownScreenIsViewed() {
        ExplorePagerScreenListener explorePagerScreenListener = new ExplorePagerScreenListener(eventBus);
        explorePagerScreenListener.onPageSelected(3);
    }
}
