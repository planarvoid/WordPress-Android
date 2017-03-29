package com.soundcloud.android.main;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class MainPagerAdapterTest extends AndroidUnitTest {

    @Mock FragmentManager fragmentManager;
    @Mock(extraInterfaces = {MainPagerAdapter.ScrollContent.class, MainPagerAdapter.FocusListener.class}) Fragment fragment1;
    @Mock Fragment fragment2;

    private MainPagerAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new MainPagerAdapter(context(), fragmentManager, buildTestNavigationModel());
    }

    @Test
    public void resetsScrollOnFragmentThatImplementsScrollContent() {
        when(fragmentManager.findFragmentByTag("soundcloud:main:0")).thenReturn(fragment1);

        adapter.resetScroll(0);

        verify((MainPagerAdapter.ScrollContent) fragment1).resetScroll();
    }

    @Test
    public void resetScrollDoesNothingIfFragmentIsNotScrollContent() {
        when(fragmentManager.findFragmentByTag("soundcloud:main:1")).thenReturn(fragment2);

        adapter.resetScroll(1);

        verifyNoMoreInteractions(fragment2);
    }

    @Test
    public void focusUpdateSentIfFragmentImplementsFocusListenerOnFirstSet() {
        when(fragmentManager.findFragmentByTag("soundcloud:main:0")).thenReturn(fragment1);

        adapter.setPrimaryItem(null, 0, fragment1);

        verify((MainPagerAdapter.FocusListener) fragment1, times(1)).onFocusChange(true);
    }

    @Test
    public void focusUpdateSentIfFragmentImplementsFocusListenerOnSubsequentSetViaSetCurrentFragmentFocused() {
        when(fragmentManager.findFragmentByTag("soundcloud:main:0")).thenReturn(fragment1);

        adapter.setPrimaryItem(null, 0, fragment2);
        adapter.setPrimaryItem(null, 0, fragment1);
        adapter.setCurrentFragmentFocused();

        verify((MainPagerAdapter.FocusListener) fragment1, times(1)).onFocusChange(true);
    }

    @Test
    public void unFocusUpdateSentIfFragmentImplementsFocusListener() {
        when(fragmentManager.findFragmentByTag("soundcloud:main:0")).thenReturn(fragment1);

        adapter.setPrimaryItem(null, 0, fragment1);
        adapter.setPrimaryItem(null, 0, fragment2);

        verify((MainPagerAdapter.FocusListener) fragment1).onFocusChange(false);
    }

    private NavigationModel buildTestNavigationModel() {
        BaseNavigationTarget page1 = new BaseNavigationTarget(R.string.tab_home, R.drawable.tab_home) {
            @Override
            public Fragment createFragment() {
                return fragment1;
            }

            @Override
            public Screen getScreen() {
                return Screen.STREAM;
            }
        };

        BaseNavigationTarget page2 = new BaseNavigationTarget(R.string.tab_more, R.drawable.tab_more) {
            @Override
            public Fragment createFragment() {
                return fragment1;
            }

            @Override
            public Screen getScreen() {
                return Screen.MORE;
            }
        };

        return new NavigationModel(page1, page2);
    }

}
