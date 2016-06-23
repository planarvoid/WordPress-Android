package com.soundcloud.android.tests.widget;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Intent;

public class WidgetLinksNoUserTest extends ActivityTest<MainActivity> {

    public WidgetLinksNoUserTest() {
        super(MainActivity.class);
    }

    public void testOpenAppFromWidgetWithoutUserShowsLoginScreen() {
        setActivityIntent(new Intent(getInstrumentation().getContext(), MainActivity.class)
                                  .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                  .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, false));

        assertThat(new HomeScreen(solo), is(visible()));
    }

}
