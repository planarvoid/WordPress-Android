package com.soundcloud.android.settings.notifications;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.optional.Optional;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

public class NotificationPreferencesStorage {
    private static final boolean DEFAULT_VALUE = true;
    private static final String LAST_UPDATE = "last_update";
    private static final String PENDING_SYNC = "pending_sync";
    private static final String BACKUP_PREFIX = "backup_";

    private final SharedPreferences sharedPreferences;
    private final CurrentDateProvider dateProvider;

    @Inject
    public NotificationPreferencesStorage(@Named(StorageModule.NOTIFICATION_PREFERENCES) SharedPreferences sharedPreferences,
                                          CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    NotificationPreferences buildNotificationPreferences() {
        NotificationPreferences preferences = new NotificationPreferences();
        for (NotificationPreferenceType preference : NotificationPreferenceType.values()) {
            preferences.add(preference.getSettingKey(), get(preference));
        }
        return preferences;
    }

    void storeBackup(String key) {
        sharedPreferences.edit()
                         .putBoolean(backupKey(key), sharedPreferences.getBoolean(key, DEFAULT_VALUE))
                         .apply();
    }

    boolean getBackup(String key) {
        return sharedPreferences.getBoolean(backupKey(key), DEFAULT_VALUE);
    }

    boolean isPendingSync() {
        return sharedPreferences.getBoolean(PENDING_SYNC, false);
    }

    void setPendingSync(boolean value) {
        sharedPreferences.edit()
                         .putBoolean(PENDING_SYNC, value)
                         .apply();
    }

    long getLastUpdateAgo() {
        return dateProvider.getCurrentTime() - sharedPreferences.getLong(LAST_UPDATE, Consts.NOT_SET);
    }

    void setUpdated() {
        sharedPreferences.edit().putLong(LAST_UPDATE, dateProvider.getCurrentTime()).apply();
    }

    void update(NotificationPreferences preferences) {
        Map<String, NotificationPreference> properties = preferences.getProperties();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (Map.Entry<String, NotificationPreference> entry : properties.entrySet()) {
            Optional<NotificationPreferenceType> typeOpt = NotificationPreferenceType.from(entry.getKey());
            if (typeOpt.isPresent()) {
                NotificationPreferenceType type = typeOpt.get();
                NotificationPreference value = entry.getValue();

                if (type.mobileKey().isPresent()) {
                    editor.putBoolean(type.mobileKey().get(), value.isMobile());
                }
                if (type.mailKey().isPresent()) {
                    editor.putBoolean(type.mailKey().get(), value.isMail());
                }
            }
        }

        editor.apply();
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

    private NotificationPreference get(NotificationPreferenceType preference) {
        return new NotificationPreference(
                getFromOptionalKey(preference.mobileKey()),
                getFromOptionalKey(preference.mailKey()));
    }

    private boolean getFromOptionalKey(Optional<String> optKey) {
        return optKey.isPresent()
               ? sharedPreferences.getBoolean(optKey.get(), DEFAULT_VALUE)
               : DEFAULT_VALUE;
    }

    private String backupKey(String key) {
        return BACKUP_PREFIX + key;
    }

}
