package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ProfileInfoTest extends ActivityTest<LauncherActivity> {

    private static final String EXPECTED_DESCRIPTION = "Hashtag literally PBR art party, blog master cleanse taxidermy 3 wolf moon 8-bit squid fingerstache Austin. Flannel American Apparel deep v photo booth, locavore fixie meditation banjo pickled. IPhone fap Wes Anderson, organic distillery health goth tilde fingerstache heirloom biodiesel asymmetrical. Readymade 90's leggings cred banh mi McSweeney's, Williamsburg tattooed cardigan stumptown cliche Brooklyn Thundercats raw denim. Gluten-free heirloom gastropub freegan ennui, pork belly Helvetica pop-up swag post-ironic dreamcatcher. Farm-to-table listicle four loko Pitchfork, salvia wayfarers skateboard sartorial hashtag chillwave street art McSweeney's food truck aesthetic. Hoodie drinking vinegar kogi, Shoreditch Portland single-origin coffee direct trade brunch mustache.\n" +
            "\n" +
            "Fanny pack gentrify banh mi Etsy, actually crucifix jean shorts post-ironic roof party salvia. Neutra dreamcatcher single-origin coffee sriracha, stumptown normcore umami art party blog lo-fi seitan keytar tilde Kickstarter kogi. Bicycle rights distillery tote bag swag flannel. Tofu forage plaid brunch shabby chic. Viral health goth Vice, blog leggings sriracha selvage bitters lo-fi drinking vinegar master cleanse. IPhone tofu freegan narwhal, cronut Godard chia street art Etsy keytar meggings jean shorts lomo try-hard. Hoodie stumptown freegan Schlitz, tote bag whatever gentrify tousled ethical.";

    private ProfileScreen screen;

    public ProfileInfoTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.profileUser;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        screen = mainNavHelper.goToMyProfile();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testShowsInfo() {
        screen.touchInfoTab();

        assertThat(screen.description().getText(), is(equalTo(EXPECTED_DESCRIPTION)));
        assertThat(screen.website().getText(), is(equalTo("Google")));
        assertThat(screen.discogs().getText(), is(equalTo("Discogs")));
        assertThat(screen.myspace().getText(), is(equalTo("Myspace")));
    }

    public void testShowsExpandedImage() {
        assertThat(screen.touchProfileImage(), is(visible()));
    }

    public void ignore_testReflectsFollowingChanges() {
        screen.touchLegacyFollowingsTab();

        assertTrue(screen.emptyView().isVisible());

        screen = screen.touchLegacyFollowersTab().clickUserAt(0);
        String followerName = screen.getUserName();
        screen.clickFollowToggle();

        solo.goBack();

        screen = new ProfileScreen(solo).touchLegacyFollowingsTab();
        screen = screen.clickUserAt(0);
        assertEquals(screen.getUserName(), followerName);

        screen.clickFollowToggle();

        solo.goBack();
        screen = new ProfileScreen(solo).touchLegacyFollowingsTab();
        assertTrue(screen.emptyView().isVisible());
    }
}
