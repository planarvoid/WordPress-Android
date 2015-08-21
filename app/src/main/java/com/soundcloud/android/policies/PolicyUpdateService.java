package com.soundcloud.android.policies;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PolicyUpdateService extends IntentService {

    public static final String ACTION_START = "action_start_update";
    public static final String TAG = "PolicyUpdate";

    @Inject PolicyOperations policyOperations;
    @Inject PolicySettingsStorage policySettingsStorage;
    @Inject DateProvider dateProvider;
    @Inject EventBus eventBus;

    public PolicyUpdateService() {
        super("PolicyUpdateService");
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    PolicyUpdateService(PolicyOperations policyOperations, PolicySettingsStorage policySettingsStorage,
                        DateProvider dateProvider, EventBus eventBus){
        super("PolicyUpdateService");
        this.policyOperations = policyOperations;
        this.policySettingsStorage = policySettingsStorage;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
    }

    public static void start(Context context) {
        context.startService(createIntent(context, ACTION_START));
    }

    private static Intent createIntent(Context context, String action) {
        final Intent intent = new Intent(context, PolicyUpdateService.class);
        intent.setAction(action);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final List<Urn> updatedTracks = policyOperations.updateTrackPolicies();

        if (!updatedTracks.isEmpty()) {
            Log.d(TAG, "Successfull policy update");
            policySettingsStorage.setPolicyUpdateTime(dateProvider.getCurrentTime());
            eventBus.publish(EventQueue.POLICY_UPDATES, PolicyUpdateEvent.success(updatedTracks));
        } else {
            long policyUpdateInDays = TimeUnit.MILLISECONDS.toDays(dateProvider.getCurrentTime() - policySettingsStorage.getPolicyUpdateTime());
            Log.d(TAG, "Last successful policy update was " + policyUpdateInDays + " days ago");
        }
    }
}
