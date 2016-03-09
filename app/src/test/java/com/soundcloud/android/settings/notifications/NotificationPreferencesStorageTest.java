package com.soundcloud.android.settings.notifications;

import static com.soundcloud.android.settings.notifications.NotificationPreferenceType.NEWSLETTERS;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.concurrent.TimeUnit;

public class NotificationPreferencesStorageTest extends AndroidUnitTest {

    private TestDateProvider dateProvider = new TestDateProvider();
    private NotificationPreferencesStorage storage;
    private SharedPreferences prefs;

    @Before
    public void setUp() throws Exception {
        prefs = PreferenceManager.getDefaultSharedPreferences(context());
        storage = new NotificationPreferencesStorage(prefs, dateProvider);
    }

    @Test
    public void isPendingSyncIsFalseByDefault() {
        assertThat(storage.isPendingSync()).isEqualTo(false);
    }

    @Test
    public void updatePendingSyncValue() {
        storage.setPendingSync(true);

        assertThat(storage.isPendingSync()).isEqualTo(true);
    }

    @Test
    public void shouldUpdateFromNotificationPreferences() {
        storage.update(buildTestPreferences());

        assertThat(prefs.getBoolean(NEWSLETTERS.mailKey().get(), true)).isFalse();
    }

    @Test
    public void shouldBuildNotificationPreferencesWithCurrentValues() {
        storage.update(buildTestPreferences());

        NotificationPreferences preferences = storage.buildNotificationPreferences();

        NotificationPreference newslettersPreference = preferences.getProperties().get(NEWSLETTERS.getName());
        assertThat(newslettersPreference.isMobile()).isTrue();
        assertThat(newslettersPreference.isMail()).isFalse();
    }

    @Test
    public void shouldBackupAndRestoreValue() {
        setPreference(NEWSLETTERS.mailKey().get(), false);

        storage.storeBackup(NEWSLETTERS.mailKey().get());
        setPreference(NEWSLETTERS.mailKey().get(), true);

        assertThat(storage.getBackup(NEWSLETTERS.mailKey().get())).isFalse();
    }

    @Test
    public void shouldStoreAndRestoreLastSyncDate() {
        dateProvider.setTime(1000, TimeUnit.SECONDS);

        storage.setSynced();
        dateProvider.advanceBy(500, TimeUnit.SECONDS);

        assertThat(storage.getLastSyncAgo()).isEqualTo(500000);
    }

    @Test
    public void shouldClearPreferences() {
        setPreference(NEWSLETTERS.mailKey().get(), false);

        storage.clear();

        assertThat(prefs.getBoolean(NEWSLETTERS.mailKey().get(), true)).isTrue();
    }

    private void setPreference(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private NotificationPreferences buildTestPreferences() {
        NotificationPreferences notificationPreferences = new NotificationPreferences();
        notificationPreferences.add(NEWSLETTERS.getName(), new NotificationPreference(true, false));
        return notificationPreferences;
    }
}
