package com.soundcloud.android.tests.widget;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.playback.ui.SlidingPlayerController.EXTRA_EXPAND_PLAYER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.Intent;

public class WidgetLinksTest extends ActivityTest<MainActivity> {

    public WidgetLinksTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testOpenAppFromWidgetWithUserShowsStreamScreen() throws Exception {
        setActivityIntent(new Intent(getInstrumentation().getContext(), MainActivity.class)
                                  .addFlags(FLAG_ACTIVITY_CLEAR_TOP)
                                  .putExtra(EXTRA_EXPAND_PLAYER, false));

        assertThat(new StreamScreen(solo), is(visible()));
    }

}
