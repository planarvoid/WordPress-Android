package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class ProfilePagerRefreshHelperTest {

    private ProfilePagerRefreshHelper helper;

    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private RefreshableProfileItem refreshable;

    @Before
    public void setUp() throws Exception {
        helper = new ProfilePagerRefreshHelper(refreshLayout);
    }

    @Test
    public void attachesRefreshLayoutToExistingFragment() throws Exception {
        helper.addRefreshable(1, refreshable);

        helper.setRefreshablePage(1);

        verify(refreshable).attachRefreshLayout(refreshLayout);
    }

    @Test
    public void attachesRefreshLayoutToFragmentAfterCreation() throws Exception {
        helper.setRefreshablePage(1);

        helper.addRefreshable(1, refreshable);

        verify(refreshable).attachRefreshLayout(refreshLayout);
    }

    @Test
    public void removeDetachesRefreshLayout() throws Exception {
        helper.addRefreshable(1, refreshable);

        helper.removeFragment(1);

        verify(refreshable).detachRefreshLayout();
    }

    @Test
    public void detachesRefreshOnPreviousFragmentAfterSettingNewRefreshablePage() throws Exception {
        helper.addRefreshable(1, refreshable);

        helper.setRefreshablePage(1);

        helper.setRefreshablePage(2);

        verify(refreshable).detachRefreshLayout();
    }
}