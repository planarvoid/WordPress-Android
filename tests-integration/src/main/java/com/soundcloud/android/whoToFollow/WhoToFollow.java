package com.soundcloud.android.whoToFollow;

import com.soundcloud.android.activity.landing.SuggestedUsersActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import rx.concurrency.Schedulers;

public class WhoToFollow extends ActivityTestCase<SuggestedUsersActivity> {

    private SuggestedUsersScreen suggestedUsersScreen;
    private SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;

    public WhoToFollow() {
        super(SuggestedUsersActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAsDefault(getInstrumentation());
        super.setUp();
        suggestedUsersScreen = new SuggestedUsersScreen(solo);
        suggestedUsersCategoryScreen = new SuggestedUsersCategoryScreen(solo);
    }

    public void testCheckmarkSelection() throws Exception {
        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersScreen.clickCategory(1);
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());

        solo.goBack();
        suggestedUsersScreen.clickToggleCategoryCheckmark(1);
        suggestedUsersScreen.clickCategory(1);
        assertTrue(suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    public void testIndividualUserSelection() throws Exception {
        suggestedUsersScreen.clickCategory(1);
        String followed = suggestedUsersCategoryScreen.followRandomUser();
        solo.goBack();
        assertTrue(suggestedUsersScreen.subtextAtIndexEquals(1, followed));
    }

    public void testSelectDeselectToggle() throws Exception {
        suggestedUsersScreen.clickCategory(1);
        suggestedUsersCategoryScreen.waitForUsers();
        suggestedUsersCategoryScreen.selectAll();
        assertTrue(suggestedUsersCategoryScreen.hasAllUsersSelected());
        suggestedUsersCategoryScreen.deselectAll();
        assertTrue(suggestedUsersCategoryScreen.hasNoUsersSelected());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        final FollowingOperations followingOperations = new FollowingOperations(Schedulers.immediate());
        for (long userId : followingOperations.getFollowedUserIds()){
            followingOperations.removeFollowing(new User(userId));
        }

    }
}
