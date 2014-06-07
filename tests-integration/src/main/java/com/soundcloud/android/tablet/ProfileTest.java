package com.soundcloud.android.tablet;

import android.support.v4.view.ViewPager;
import android.view.View;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.ScListView;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreenTablet;
import com.soundcloud.android.screens.MyProfileScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TabletTest;
import com.soundcloud.android.tests.ViewElement;
import com.soundcloud.android.tests.with.With;

import static com.soundcloud.android.tests.TestUser.followedUser;

@TabletTest
public class ProfileTest extends ActivityTestCase<MainActivity> {
    private ProfileScreen profileScreen;
    private MyProfileScreen myProfileScreen;

    public ProfileTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        AccountAssistant.loginAs(getInstrumentation(), followedUser.getUsername(), followedUser.getPassword());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
        myProfileScreen = new MenuScreenTablet(solo).clickUserProfile();
        navigateToFollower();
    }

    public void ignoreFollowButtonIsVisibleOnOtherProfiles() {
        assertTrue(profileScreen.isFollowButtonVisible());
    }

    public void ignoreFollowingMessageUpdatedWhenFollowButtonToggled() {
        String initialMessage = profileScreen.getFollowersMessage();
        profileScreen.clickFollowToggle();
        assertEquals("Following message changes when FOLLOW button is toggled", false, initialMessage.equals(profileScreen.getFollowersMessage()));

        followedUser.unfollowAll(solo.getCurrentActivity());
    }

    private void navigateToFollower() {
        for (int i = 0; i < 4; i++) {
            myProfileScreen.swipeLeft();
        }

        solo.sleep(1000);

        ViewPager pager = (ViewPager)(solo.getSolo().getCurrentViews(ViewPager.class).get(0));
        ViewElement list = solo.findElement(With.className(ScListView.class));

        ViewElement item = list.findElement(With.id(R.id.username));

        item.click();
        profileScreen = new ProfileScreen(solo);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
