package com.soundcloud.android.widget;

import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.tests.ActivityTestCase;

import android.content.Intent;

public class WidgetLinksNoUser extends ActivityTestCase<MainActivity> {

    public WidgetLinksNoUser() {
        super(MainActivity.class);
    }

    public void testOpenAppFromWidgetWithoutUserShowsLoginScreen() {
        setActivityIntent(new Intent(getInstrumentation().getContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, false));

        assertThat(new HomeScreen(solo), is(Visible()));
    }

}
