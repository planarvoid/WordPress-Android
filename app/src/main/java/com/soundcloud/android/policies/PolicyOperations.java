package com.soundcloud.android.policies;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateFailureEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PolicyOperations {
    public static final long POLICY_STALE_AGE_MILLISECONDS = TimeUnit.HOURS.toMillis(24);

    private static final Func1<ApiPolicyInfo, Urn> TO_TRACK_URN = new Func1<ApiPolicyInfo, Urn>() {
        @Override
        public Urn call(ApiPolicyInfo policy) {
            return policy.getUrn();
        }
    };

    private static final Func1<ApiPolicyInfo, Boolean> FILTER_MONETIZABLE = new Func1<ApiPolicyInfo, Boolean>() {
        @Override
        public Boolean call(ApiPolicyInfo policy) {
            return !policy.isMonetizable();
        }
    };

    private final ClearTableCommand clearTableCommand;
    private final UpdatePoliciesCommand updatePoliciesCommand;
    private final LoadPolicyUpdateTimeCommand loadPolicyUpdateTimeCommand;
    private final Scheduler scheduler;
    private final PolicyStorage policyStorage;
    private final EventBus eventBus;
    private final Action1<List<Urn>> clearTrackPolicies = new Action1<List<Urn>>() {
        @Override
        public void call(List<Urn> urns) {
            clearTableCommand.call(Tables.TrackPolicies.TABLE);
        }
    };
    private final Action1<Throwable> handleErrorAction = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            handlePolicyUpdateFailure(throwable, false);
        }
    };

    @Inject
    PolicyOperations(ClearTableCommand clearTableCommand, UpdatePoliciesCommand updatePoliciesCommand,
                     LoadPolicyUpdateTimeCommand loadPolicyUpdateTimeCommand,
                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                     PolicyStorage policyStorage, EventBus eventBus) {
        this.clearTableCommand = clearTableCommand;
        this.scheduler = scheduler;
        this.updatePoliciesCommand = updatePoliciesCommand;
        this.loadPolicyUpdateTimeCommand = loadPolicyUpdateTimeCommand;
        this.policyStorage = policyStorage;
        this.eventBus = eventBus;
    }

    public Observable<Map<Urn, Boolean>> blockedStatuses(List<Urn> urns) {
        return policyStorage.loadBlockedStatuses(urns).subscribeOn(scheduler);
    }

    public Observable<Collection<ApiPolicyInfo>> updatePolicies(Collection<Urn> urns) {
        return fetchAndStorePolicies(urns);
    }

    public Observable<List<Urn>> filterMonetizableTracks(Collection<Urn> urns) {
        return fetchAndStorePolicies(urns)
                .flatMap(RxUtils.<ApiPolicyInfo>iterableToObservable())
                .filter(FILTER_MONETIZABLE)
                .map(TO_TRACK_URN)
                .toList();
    }

    public List<Urn> updateTrackPolicies() {
        try {
            final List<Urn> urns = policyStorage.loadTracksForPolicyUpdate();
            updatePoliciesCommand.call(urns);
            return urns;
        } catch (Exception ex) {
            handlePolicyUpdateFailure(ex, true);
            return Collections.emptyList();
        }
    }

    public Observable<List<Urn>> refreshedTrackPolicies() {
        return policyStorage.tracksForPolicyUpdate()
                            .doOnNext(clearTrackPolicies)
                            .flatMap(updatePoliciesCommand.toContinuation())
                            .flatMap(RxUtils.<ApiPolicyInfo>iterableToObservable())
                            .map(TO_TRACK_URN)
                            .toList()
                            .doOnError(handleErrorAction)
                            .subscribeOn(scheduler);
    }

    public Observable<Long> getMostRecentPolicyUpdateTimestamp() {
        return loadPolicyUpdateTimeCommand.toObservable(null)
                                          .subscribeOn(scheduler);
    }

    private Observable<Collection<ApiPolicyInfo>> fetchAndStorePolicies(Collection<Urn> urns) {
        return updatePoliciesCommand.toObservable(urns)
                                    .subscribeOn(scheduler);
    }

    private void handlePolicyUpdateFailure(Throwable error, boolean isBackgroundUpdate) {
        if (ErrorUtils.isNetworkError(error.getCause())) {
            String context = isBackgroundUpdate ? "background" : "foreground";
            ErrorUtils.handleSilentException("Policy update failed: " + context, error);
        }
        Throwable cause = error instanceof PolicyUpdateFailure ? error.getCause() : error;
        final PolicyUpdateFailureEvent failureEvent =
                cause instanceof PropellerWriteException
                ? PolicyUpdateFailureEvent.insertFailed(isBackgroundUpdate)
                : PolicyUpdateFailureEvent.fetchFailed(isBackgroundUpdate);
        eventBus.publish(EventQueue.TRACKING, failureEvent);
    }
}
