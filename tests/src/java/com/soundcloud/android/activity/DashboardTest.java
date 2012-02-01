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
        Dashboard db = createDashboard(Dashboard.Tab.STREAM);
        expect(db.mListView).not.toBeNull();
    }

    @Test
    public void shoulGoToActivities() throws Exception {
        Dashboard db = createDashboard(Dashboard.Tab.ACTIVITY);
        expect(db.mListView).not.toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfNoTabSpecified() throws Exception {
        new Dashboard().onCreate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfInvalidTabSpecified() throws Exception {
        Dashboard dashboard = new Dashboard();
        dashboard.setIntent(new Intent().putExtra("tab", "unknown"));
        dashboard.onCreate(null);
    }

    private Dashboard createDashboard(Dashboard.Tab tab) {
        Dashboard dashboard = new Dashboard();
        dashboard.setIntent(new Intent().putExtra("tab", tab.tag));
        dashboard.onCreate(null);
        return dashboard;
    }
}
