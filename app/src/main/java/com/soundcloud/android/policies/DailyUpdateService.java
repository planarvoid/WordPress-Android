package com.soundcloud.android.policies;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.configuration.ConfigurationManager;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.List;

public class DailyUpdateService extends IntentService {

    public static final String ACTION_START = "action_start_update";
    public static final String TAG = "DailyUpdate";

    @Inject PolicyOperations policyOperations;
    @Inject PolicySettingsStorage policySettingsStorage;
    @Inject ConfigurationManager configurationManager;
    @Inject AdIdHelper adIdHelper;
    @Inject EventBus eventBus;

    @SuppressWarnings("unused")
    public DailyUpdateService() {
        super(TAG);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    DailyUpdateService(PolicyOperations policyOperations,
                       PolicySettingsStorage policySettingsStorage,
                       ConfigurationManager configurationManager,
                       AdIdHelper adIdHelper, EventBus eventBus) {
        super(TAG);
        this.policyOperations = policyOperations;
        this.policySettingsStorage = policySettingsStorage;
        this.configurationManager = configurationManager;
        this.adIdHelper = adIdHelper;
        this.eventBus = eventBus;
    }

    public static void start(Context context) {
        context.startService(createIntent(context, ACTION_START));
    }

    private static Intent createIntent(Context context, String action) {
        final Intent intent = new Intent(context, DailyUpdateService.class);
        intent.setAction(action);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_START.equals(intent.getAction())) {
            updateTrackPolicies();
            configurationManager.forceConfigurationUpdate();
            adIdHelper.init();
        }
    }

    private void updateTrackPolicies() {
        Log.d(TAG, "Update track policies start");
        final List<Urn> updatedTracks = policyOperations.updateTrackPolicies();
        if (!updatedTracks.isEmpty()) {
            Log.d(TAG, "Successful policy update for " + updatedTracks.size() + " tracks");
            eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.create(updatedTracks));
        } else {
            Log.d(TAG, "No policy update received");
        }
    }
}
