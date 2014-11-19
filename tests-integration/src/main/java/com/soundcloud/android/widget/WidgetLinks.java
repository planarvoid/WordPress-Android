package com.soundcloud.android.widget;

import static com.soundcloud.android.tests.TestUser.defaultUser;
import static com.soundcloud.android.tests.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

import android.content.Intent;

public class WidgetLinks extends ActivityTestCase<MainActivity> {

    public WidgetLinks() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        defaultUser.logIn(getInstrumentation().getTargetContext());
        assertNotNull(AccountAssistant.getAccount(getInstrumentation().getTargetContext()));
        super.setUp();
    }

    public void testOpenAppFromWidgetWithUserShowsStreamScreen() {
        setActivityIntent(new Intent(getInstrumentation().getContext(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, false));

        assertThat(new StreamScreen(solo), is(visible()));
    }

}
