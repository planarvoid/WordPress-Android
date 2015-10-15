package com.soundcloud.android.main;

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
    @Mock (extraInterfaces = {ScrollContent.class}) Fragment fragment1;
    @Mock Fragment fragment2;

    private MainPagerAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new MainPagerAdapter(fragmentManager, buildTestNavigationModel());
    }

    @Test
    public void resetsScrollOnFragmentThatImplementsScrollContent() {
        when(fragmentManager.findFragmentByTag("soundcloud:main:0")).thenReturn(fragment1);

        adapter.resetScroll(0);

        verify((ScrollContent) fragment1).resetScroll();
    }

    @Test
    public void resetScrollDoesNothingIfFragmentIsNotScrollContent() {
        when(fragmentManager.findFragmentByTag("soundcloud:main:1")).thenReturn(fragment2);

        adapter.resetScroll(1);

        verifyNoMoreInteractions(fragment2);
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

        BaseNavigationTarget page2 = new BaseNavigationTarget(R.string.tab_you, R.drawable.tab_you) {
            @Override
            public Fragment createFragment() {
                return fragment1;
            }

            @Override
            public Screen getScreen() {
                return Screen.YOU;
            }
        };

        return new NavigationModel(page1, page2);
    }

}
