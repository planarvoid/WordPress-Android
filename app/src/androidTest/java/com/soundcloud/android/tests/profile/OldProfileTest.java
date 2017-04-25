package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

public class OldProfileTest extends ActivityTest<ResolveActivity> {
    private static final String OTHER_PROFILE_PAGEVIEW_EVENTS = "specs/other_profile_pageview_events.spec";

    private FeatureFlagsHelper featureFlagsHelper;

    private ProfileScreen profileScreen;

    public OldProfileTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return profileEntryUser;
    }

    @Override
    protected void setUp() throws Exception {
        featureFlagsHelper = FeatureFlagsHelper.create(getInstrumentation().getTargetContext());
        featureFlagsHelper.disable(Flag.ALIGNED_USER_INFO);

        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.OTHER_PROFILE_USER_URI));
        super.setUp();

        profileScreen = new ProfileScreen(solo);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        featureFlagsHelper.reset(Flag.ALIGNED_USER_INFO);
    }

    public void testOldPageViewEvents() throws Exception {
        mrLocalLocal.startEventTracking();

        profileScreen.touchInfoTab();
        profileScreen.touchSoundsTab();
        profileScreen.touchFollowersTab();
        profileScreen.touchFollowingsTab();

        mrLocalLocal.verify(OTHER_PROFILE_PAGEVIEW_EVENTS);
    }

    public void testFollowersClickOpensProfilePage() {
        profileScreen.touchFollowersTab();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testClickFollowingsLoadsProfile() {
        profileScreen.touchFollowingsTab();

        final UserItemElement expectedUser = profileScreen
                .getUsers()
                .get(0);

        String targetUsername = expectedUser.getUsername();
        assertEquals(expectedUser.click().getUserName(), targetUsername);
    }
}
