package com.soundcloud.android.profile;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProfileScrollHelperTest {

    private ProfileScrollHelper profileScrollHelper;

    @Mock private ProfileScreen profileScreen1;
    @Mock private ProfileScreen profileScreen2;

    @Before
    public void setUp() throws Exception {
        profileScrollHelper = new ProfileScrollHelper();
    }

    @Test
    public void disablesSwipeToRefreshWhenOffsetLessThanZeroAfterRemovingRefreshableScreen() {
        profileScrollHelper.addProfileCollection(profileScreen1);
        profileScrollHelper.addProfileCollection(profileScreen2);
        profileScrollHelper.removeProfileScreen(profileScreen2);

        profileScrollHelper.setSwipeToRefreshEnabled(false);

        verify(profileScreen1).setSwipeToRefreshEnabled(false);
        verify(profileScreen2, never()).setSwipeToRefreshEnabled(false);
    }
}
