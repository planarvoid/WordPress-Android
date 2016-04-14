package com.soundcloud.android.profile;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ProfileScrollHelperTest extends AndroidUnitTest {

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
