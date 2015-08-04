package com.soundcloud.android.tests.tablet;

import static com.soundcloud.android.framework.TestUser.followedUser;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MenuScreenTablet;
import com.soundcloud.android.screens.MyProfileScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.annotation.TabletTest;

import android.support.v4.view.ViewPager;
import android.widget.ListView;

@TabletTest
public class ProfileTest extends ActivityTest<MainActivity> {
    private ProfileScreen profileScreen;
    private MyProfileScreen myProfileScreen;

    public ProfileTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        followedUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
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
    }

    private void navigateToFollower() {
        for (int i = 0; i < 4; i++) {
            myProfileScreen.swipeLeft();
        }

        solo.sleep(1000);

        ViewPager pager = solo.getSolo().getCurrentViews(ViewPager.class).get(0);
        ViewElement list = solo.findElement(With.className(ListView.class));

        ViewElement item = list.findElement(With.id(R.id.username));

        item.click();
        profileScreen = new ProfileScreen(solo);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
