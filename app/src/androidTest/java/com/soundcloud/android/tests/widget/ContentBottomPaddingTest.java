package com.soundcloud.android.tests.widget;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ContentBottomPaddingTest extends ActivityTest<MainActivity> {

    public ContentBottomPaddingTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Ignore // FIXME https://soundcloud.atlassian.net/browse/DROID-1513
    public void testMainContainerIsPaddedProperlyOnPlayerVisible() throws Exception {
        MoreScreen moreScreen = mainNavHelper.goToMore();
        assertThat(moreScreen.appVersionText().isFullyOnScreen(), is(true));

        StreamScreen streamScreen = mainNavHelper.goToStream();
        streamScreen.scrollToFirstTrack()
                    .clickToPlay()
                    .pressCloseButton();

        moreScreen = mainNavHelper.goToMore();
        assertThat(moreScreen.appVersionText().isFullyOnScreen(), is(true));
    }
}
