package com.soundcloud.android.settings.notifications;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.java.optional.Optional;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;

import javax.inject.Inject;
import java.util.Collection;

public class NotificationPreferencesFragment extends PreferenceFragment {
    private static final String MOBILE_GROUP_KEY = "all_mobile";
    private static final String MAIL_GROUP_KEY = "all_mail";
    private static final Collection<String> MOBILE_KEYS = NotificationPreferenceType.mobileKeys();
    private static final Collection<String> MAIL_KEYS = NotificationPreferenceType.mailKeys();

    @Inject NotificationPreferencesOperations operations;

    public NotificationPreferencesFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setup();
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
        if (preference.hasKey()) {
            final String key = preference.getKey();

            switch (key) {
                case MOBILE_GROUP_KEY:
                    onGroupUpdated(MOBILE_GROUP_KEY, MOBILE_KEYS);
                    break;
                case MAIL_GROUP_KEY:
                    onGroupUpdated(MAIL_GROUP_KEY, MAIL_KEYS);
                    break;
                default:
                    onToggleUpdated(key);
                    break;
            }
            fireAndForget(operations.sync());
        }
        return true;
    }

    private void setup() {
        getPreferenceManager().setSharedPreferencesName(StorageModule.PREFS_NOTIFICATION_PREFERENCES);
        addPreferencesFromResource(R.xml.notification_preferences);
        setupGroupToggles();
    }

    private void setupGroupToggles() {
        setChecked(MOBILE_GROUP_KEY, isAnyChecked(MOBILE_KEYS));
        setChecked(MAIL_GROUP_KEY, isAnyChecked(MAIL_KEYS));
    }

    private void onToggleUpdated(String key) {
        boolean checked = isChecked(key);

        if (MOBILE_KEYS.contains(key)) {
            updateToggleGroup(checked, MOBILE_GROUP_KEY, MOBILE_KEYS);
        } else if (MAIL_KEYS.contains(key)) {
            updateToggleGroup(checked, MAIL_GROUP_KEY, MAIL_KEYS);
        }
    }

    private void updateToggleGroup(boolean isToggleChecked, String groupKey, Collection<String> keys) {
        if (isToggleChecked && !isChecked(groupKey)) {
            setChecked(groupKey, true);
        } else if (!isAnyChecked(keys)) {
            backupSettings(keys);
            setChecked(groupKey, false);
        }
    }

    private void onGroupUpdated(String groupKey, Collection<String> keys) {
        if (isChecked(groupKey)) {
            restoreSettings(keys);
        } else {
            backupSettings(keys);
            setAllChecked(keys, false);
        }
    }

    private void backupSettings(Collection<String> keys) {
        for (String key : keys) {
            operations.backup(key);
        }
    }

    private void restoreSettings(Collection<String> keys) {
        for (String key : keys) {
            setChecked(key, operations.restore(key));
        }

        if (!isAnyChecked(keys)) {
            setAllChecked(keys, true);
        }
    }

    private Optional<TwoStatePreference> getPreferenceByKey(String key) {
        Preference preference = getPreferenceScreen().findPreference(key);
        return Optional.fromNullable((TwoStatePreference) preference);
    }

    private boolean isChecked(String key) {
        Optional<TwoStatePreference> preferenceByKey = getPreferenceByKey(key);
        return preferenceByKey.isPresent()
                && preferenceByKey.get().isChecked();
    }

    private void setChecked(String key, boolean checked) {
        Optional<TwoStatePreference> preferenceByKey = getPreferenceByKey(key);

        if (preferenceByKey.isPresent()) {
            preferenceByKey.get().setChecked(checked);
        }
    }

    private boolean isAnyChecked(Collection<String> keys) {
        for (String key : keys) {
            if (isChecked(key)) {
                return true;
            }
        }
        return false;
    }

    private void setAllChecked(Collection<String> keys, boolean checked) {
        for (String key : keys) {
            setChecked(key, checked);
        }
    }
}
