package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

@RunWith(DefaultTestRunner.class)
public class DashboardTest {
    @Test
    public void shoulGoToStream() throws Exception {
        Dashboard db = createDashboard(Main.Tab.STREAM);
        expect(db.mListView).not.toBeNull();
    }

    @Test
    public void shoulGoToActivities() throws Exception {
        Dashboard db = createDashboard(Main.Tab.ACTIVITY);
        expect(db.mListView).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotThrowIfNoTabSpecified() throws Exception {
        new Dashboard().onCreate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfInvalidTabSpecified() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setIntent(new Intent().putExtra(Main.TAB_TAG, "unknown"));
        dashboard.onCreate(null);
    }

    private Dashboard createDashboard(Main.Tab tab) {
        Dashboard dashboard = new Dashboard();
        dashboard.setIntent(new Intent().putExtra(Main.TAB_TAG, tab.tag));
        dashboard.onCreate(null);
        return dashboard;
    }
}
