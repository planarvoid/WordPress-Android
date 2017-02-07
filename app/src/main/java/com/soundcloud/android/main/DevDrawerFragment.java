package com.soundcloud.android.main;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.cast.CastConfigStorage;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.gcm.GcmDebugDialogFragment;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayOperations;
import com.soundcloud.android.playback.ConcurrentPlaybackOperations;
import com.soundcloud.android.policies.DailyUpdateService;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;
import java.util.Locale;

@SuppressLint("ValidFragment")
public class DevDrawerFragment extends PreferenceFragment implements IntroductoryOverlayOperations.OnIntroductoryOverlayStateChangedListener {

    private static final String DEVICE_CONFIG_SETTINGS = "device_config_settings";
    private static final String KEY_LAST_CONFIG_CHECK_TIME = "last_config_check_time";

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject FeatureFlags featureFlags;
    @Inject AccountOperations accountOperations;
    @Inject DevDrawerExperimentsHelper drawerExperimentsHelper;
    @Inject ConfigurationManager configurationManager;
    @Inject Navigator navigator;
    @Inject ConcurrentPlaybackOperations concurrentPlaybackOperations;
    @Inject CastConfigStorage castConfigStorage;
    @Inject EventBus eventBus;
    @Inject IntroductoryOverlayOperations introductoryOverlayOperations;

    public DevDrawerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.dev_drawer_prefs);
        addActions();
        addFeatureToggles();
        addIntroductoryOverlaysControls();
        drawerExperimentsHelper.addExperiments(getPreferenceScreen());
        subscription = subscribeToPlaybackStateEvent();
    }

    @Override
    public void onDestroy() {
        introductoryOverlayOperations.unregisterOnStateChangedListener(this);
        subscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onIntroductoryOverlayStateChanged(String introductoryOverlayKey) {
        updateIntroductoryOverlayPreference(introductoryOverlayKey, introductoryOverlayOperations.wasOverlayShown(introductoryOverlayKey));
    }

    private Subscription subscribeToPlaybackStateEvent() {
        return eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED).subscribe(event -> {
            updatePlayerInformation(event.getPlayerType());
        });
    }

    private void updatePlayerInformation(@Nullable String player) {
        final String name = player == null ? "None" : player;
        getPreferenceScreen()
                .findPreference(getString(R.string.dev_drawer_player_key))
                .setTitle("Current player: " + name);
    }

    private void addFeatureToggles() {
        final PreferenceScreen screen = this.getPreferenceScreen();
        final PreferenceCategory category = new PreferenceCategory(screen.getContext());
        category.setTitle(getString(R.string.dev_drawer_section_build_features));
        screen.addPreference(category);
        for (Flag flag : Flag.features()) {
            category.addPreference(new FeatureFlagCheckBoxPreference(screen.getContext(), featureFlags, flag));
        }
    }

    private void addActions() {
        final PreferenceScreen screen = this.getPreferenceScreen();

        screen.findPreference(getString(R.string.dev_drawer_action_get_oauth_token_to_clipboard_key))
              .setOnPreferenceClickListener(preference -> {
                  copyTokenToClipboard();
                  Toast.makeText(getActivity(), R.string.dev_oauth_token_copied, Toast.LENGTH_LONG).show();
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_show_remote_debug_key))
              .setOnPreferenceClickListener(preference -> {
                  GcmDebugDialogFragment dialogFragment = new GcmDebugDialogFragment();
                  dialogFragment.show(getFragmentManager(), "gcm_debug");
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_kill_app_key))
              .setOnPreferenceClickListener(preference -> {
                  navigator.restartApp(getActivity());
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_upgrade_ht))
              .setOnPreferenceClickListener(preference -> {
                  launchFakeUpgrade(Plan.HIGH_TIER);
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_upgrade_mt))
              .setOnPreferenceClickListener(preference -> {
                  launchFakeUpgrade(Plan.MID_TIER);
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_downgrade_mt))
              .setOnPreferenceClickListener(preference -> {
                  launchFakeDowngrade(Plan.MID_TIER);
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_downgrade_free))
              .setOnPreferenceClickListener(preference -> {
                  launchFakeDowngrade(Plan.FREE_TIER);
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_reset_flags_key))
              .setOnPreferenceClickListener(preference -> {
                  for (Flag flag : Flag.features()) {
                      final String preferenceKey = featureFlags.getRuntimeFeatureFlagKey(flag);
                      final CheckBoxPreference chkPreference =
                              (CheckBoxPreference) screen.findPreference(preferenceKey);
                      chkPreference.setChecked(featureFlags.resetRuntimeFlagValue(flag));
                  }
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_policy_sync_key))
              .setOnPreferenceClickListener(preference -> {
                  DailyUpdateService.start(getActivity().getApplicationContext());
                  return true;
              });


        screen.findPreference(getString(R.string.dev_drawer_action_crash_key))
              .setOnPreferenceClickListener(preference -> {
                  if (!AndroidUtils.isUserAMonkey()) {
                      throw new RuntimeException("Developer requested crash");
                  }
                  return true;
              });

        screen.findPreference(getString(R.string.dev_drawer_action_concurrent_key))
              .setOnPreferenceClickListener(preference -> {
                  concurrentPlaybackOperations.pauseIfPlaying();
                  return true;
              });

        setupForceConfigUpdatePref(screen);
        setupCastReceiverIdPref(screen);
    }

    private void launchFakeUpgrade(Plan plan) {
        getActivity().getSharedPreferences("device_config_settings", Context.MODE_PRIVATE)
                     .edit()
                     .putString("pending_plan_upgrade", plan.planId)
                     .apply();
        navigator.resetForAccountUpgrade(getActivity());
    }

    private void launchFakeDowngrade(Plan plan) {
        getActivity().getSharedPreferences("device_config_settings", Context.MODE_PRIVATE)
                     .edit()
                     .putString("pending_plan_downgrade", plan.planId)
                     .apply();
        navigator.resetForAccountDowngrade(getActivity());
    }

    private void addIntroductoryOverlaysControls() {
        final PreferenceScreen screen = getPreferenceScreen();
        final PreferenceCategory category = getIntroductoryOverlaysPreferenceCategory();
        category.removeAll();
        for (String key : IntroductoryOverlayKey.ALL_KEYS) {
            category.addPreference(new IntroductoryOverlayCheckBoxPreference(screen.getContext(), introductoryOverlayOperations, key));
        }

        introductoryOverlayOperations.registerOnStateChangedListener(this);
    }

    private PreferenceCategory getIntroductoryOverlaysPreferenceCategory() {
        return (PreferenceCategory) getPreferenceScreen().findPreference(getString(R.string.dev_drawer_introductory_overlays_key));
    }

    private void updateIntroductoryOverlayPreference(String key, boolean isChecked) {
        IntroductoryOverlayCheckBoxPreference preference = (IntroductoryOverlayCheckBoxPreference) getIntroductoryOverlaysPreferenceCategory().findPreference(key);
        preference.setChecked(isChecked);
    }

    private void setupCastReceiverIdPref(PreferenceScreen screen) {
        final Preference castIdPref = screen.findPreference(getString(R.string.dev_drawer_action_cast_id_key));
        castIdPref.setSummary(castConfigStorage.getReceiverID());
        castIdPref.setOnPreferenceClickListener(preference -> {
            showCastIDInputDialog(preference);
            return true;
        });
    }

    private void showCastIDInputDialog(Preference preference) {
        final View dialogView = View.inflate(getActivity(), R.layout.comment_input, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.custom_dialog_title);
        final EditText input = (EditText) dialogView.findViewById(R.id.comment_input);

        title.setText(R.string.dev_drawer_dialog_cast_id_title);
        input.setHint(castConfigStorage.getReceiverID());

        new AlertDialog.Builder(preference.getContext())
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    final String newID = input.getText().toString().toUpperCase(Locale.US);
                    if (Strings.isNotBlank(newID)) {
                        castConfigStorage.saveReceiverIDOverride(newID);
                        dialog.dismiss();
                        navigator.restartApp(getActivity());
                    }
                })
                .setNeutralButton(R.string.dev_drawer_dialog_cast_id_reset, (dialog, which) -> {
                    castConfigStorage.reset();
                    dialog.dismiss();
                    navigator.restartApp(getActivity());
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
                .show();
    }

    private void setupForceConfigUpdatePref(PreferenceScreen screen) {
        final Preference updateConfigPref = screen.findPreference(getString(R.string.dev_drawer_action_config_update_key));
        updateConfigPref.setOnPreferenceClickListener(preference -> {
            configurationManager.forceConfigurationUpdate();
            return true;
        });
        final SharedPreferences sharedPrefs = getActivity().getSharedPreferences(DEVICE_CONFIG_SETTINGS,
                                                                                 Context.MODE_PRIVATE);
        sharedPrefs.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (KEY_LAST_CONFIG_CHECK_TIME.equals(key)) {
                DevDrawerFragment.this.updateLastConfigUpdateText(updateConfigPref, sharedPreferences);
            }
        });
        updateLastConfigUpdateText(updateConfigPref, sharedPrefs);
    }

    private void updateLastConfigUpdateText(Preference preference, SharedPreferences sharedPreferences) {
        final long lastUpdatedTs = sharedPreferences.getLong(KEY_LAST_CONFIG_CHECK_TIME, 0);
        final String lastUpdateTime = ScTextUtils.formatTimeElapsedSince(preference.getContext().getResources(),
                                                                         lastUpdatedTs,
                                                                         true);
        preference.setSummary("last updated " + lastUpdateTime);
    }

    private void copyTokenToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("oauth_token", accountOperations.getSoundCloudToken().getAccessToken());
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.white));
        return view;
    }

    /**
     * {@link CheckBoxPreference} class for Feature Flags which
     * uses a custom obfuscated shared preferences file.
     */
    private static class FeatureFlagCheckBoxPreference extends CheckBoxPreference {

        private final FeatureFlags featureFlags;
        private final Flag flag;

        FeatureFlagCheckBoxPreference(Context context, FeatureFlags featureFlags, Flag flag) {
            super(context);
            this.featureFlags = featureFlags;
            this.flag = flag;
            this.initialize();
        }

        private void initialize() {
            setTitle(ScTextUtils.fromSnakeCaseToCamelCase(flag.name()));
            setKey(featureFlags.getRuntimeFeatureFlagKey(flag));
            setChecked(featureFlags.isEnabled(flag));
            setOnPreferenceClickListener(preference -> {
                featureFlags.setRuntimeFeatureFlagValue(flag, ((CheckBoxPreference) preference).isChecked());
                return false;
            });
        }
    }

    private static class IntroductoryOverlayCheckBoxPreference extends CheckBoxPreference {

        private final IntroductoryOverlayOperations introductoryOverlayOperations;
        private final String key;

        IntroductoryOverlayCheckBoxPreference(Context context, IntroductoryOverlayOperations introductoryOverlayOperations, String key) {
            super(context);
            this.introductoryOverlayOperations = introductoryOverlayOperations;
            this.key = key;
            initialize();
        }

        private void initialize() {
            setTitle(ScTextUtils.fromSnakeCaseToCamelCase(key));
            setKey(key);
            setChecked(introductoryOverlayOperations.wasOverlayShown(key));
            setOnPreferenceClickListener(preference -> {
                introductoryOverlayOperations.setOverlayShown(key, ((CheckBoxPreference) preference).isChecked());
                return true;
            });
        }
    }
}
