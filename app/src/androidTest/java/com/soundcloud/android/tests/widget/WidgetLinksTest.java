package com.soundcloud.android.tests.widget;

import static com.soundcloud.android.framework.TestUser.defaultUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;

public class WidgetLinksTest extends ActivityTest<MainActivity> {

    public WidgetLinksTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testOpenAppFromWidgetWithUserShowsStreamScreen() {
        setActivityIntent(new Intent(getInstrumentation().getContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, false));

        assertThat(new StreamScreen(solo), is(visible()));
    }

}
