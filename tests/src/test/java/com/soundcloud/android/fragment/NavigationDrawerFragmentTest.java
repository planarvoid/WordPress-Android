package com.soundcloud.android.fragment;

import com.soundcloud.android.activity.MainActivity;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

@RunWith(DefaultTestRunner.class)
@DisableStrictI18n
public class NavigationDrawerFragmentTest {

    private NavigationDrawerFragment fragment;

    @Before
    public void setup() {
        fragment = new NavigationDrawerFragment();

        TestHelper.setUserId(1L);
    }

    @Test
    public void shouldUpdateUserProfileInfoWhenCreated() {
        Robolectric.shadowOf(fragment).setAttached(true);
        Robolectric.shadowOf(fragment).setAttached(true);
        final MainActivity activity = new MainActivity();
        Robolectric.shadowOf(activity).setIntent(new Intent());
        Robolectric.shadowOf(fragment).setActivity(activity);
        fragment.onCreate(null);

        View fragmentLayout = fragment.onCreateView(LayoutInflater.from(Robolectric.application), new FrameLayout(Robolectric.application), null);
        Robolectric.shadowOf(fragment).setView(fragmentLayout);
        fragment.onViewCreated(fragmentLayout, null);
    }
}
