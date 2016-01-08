package com.soundcloud.android.sync;

import static com.soundcloud.android.Consts.PrefKeys.NOTIFICATIONS_FREQUENCY;
import static com.soundcloud.android.sync.SyncConfig.DEFAULT_NOTIFICATIONS_FREQUENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SharedPreferences;

public class SyncConfigTest extends AndroidUnitTest {

    public static final String TEST_KEY = "test_key";
    @Mock SharedPreferences sharedPreferences;

    private SyncConfig syncConfig;

    @Before
    public void setUp() throws Exception {
        syncConfig = new SyncConfig(sharedPreferences, new TestDateProvider(10000L), context());
    }

    @Test
    public void testGetNotificationsFrequencyDefault() throws Exception {
        when(sharedPreferences.contains(NOTIFICATIONS_FREQUENCY)).thenReturn(false);

        long notificationsFrequency = syncConfig.getNotificationsFrequency();

        assertThat(notificationsFrequency).isEqualTo(DEFAULT_NOTIFICATIONS_FREQUENCY);
    }

    @Test
    public void testGetNotificationsFrequency() throws Exception {
        when(sharedPreferences.contains(NOTIFICATIONS_FREQUENCY)).thenReturn(true);
        when(sharedPreferences.getString(eq(NOTIFICATIONS_FREQUENCY), any(String.class))).thenReturn("1000");

        long notificationsFrequency = syncConfig.getNotificationsFrequency();

        assertThat(notificationsFrequency).isEqualTo(1000L);
    }

    @Test
    public void testShouldSync() throws Exception {
        when(sharedPreferences.getLong(eq(TEST_KEY), any(Long.class))).thenReturn(5000L);

        boolean shouldSync = syncConfig.shouldSync(TEST_KEY, 3000L);

        assertThat(shouldSync).isTrue();
    }

    @Test
    public void testShouldSyncReturnsFalseWhenTooEarly() throws Exception {
        when(sharedPreferences.getLong(eq(TEST_KEY), any(Long.class))).thenReturn(5000L);

        boolean shouldSync = syncConfig.shouldSync(TEST_KEY, 7000L);

        assertThat(shouldSync).isFalse();
    }

}
