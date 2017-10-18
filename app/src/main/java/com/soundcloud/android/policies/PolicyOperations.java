package com.soundcloud.android.policies;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PolicyUpdateFailureEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PolicyOperations {

    public static final long POLICY_STALE_AGE_MILLISECONDS = TimeUnit.HOURS.toMillis(24);

    private final ClearTableCommand clearTableCommand;
    private final UpdatePoliciesCommand updatePoliciesCommand;
    private final LoadPolicyUpdateTimeCommand loadPolicyUpdateTimeCommand;
    private final Scheduler scheduler;
    private final PolicyStorage policyStorage;
    private final EventBus eventBus;

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

    public Function<List<Urn>, Single<Map<Urn, Boolean>>> blockedStatuses() {
        return urns -> RxJava.toV2Single(policyStorage.loadBlockedStatuses(urns).subscribeOn(scheduler));
    }

    public Observable<Collection<ApiPolicyInfo>> updatePolicies(Collection<Urn> urns) {
        return fetchAndStorePolicies(urns);
    }

    List<Urn> updateTrackPolicies() {
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
                            .doOnNext(urns -> clearTableCommand.call(Tables.TrackPolicies.TABLE))
                            .flatMap(updatePoliciesCommand.toContinuation())
                            .flatMap(Observable::from)
                            .map(ApiPolicyInfo::getUrn)
                            .toList()
                            .doOnError(throwable -> handlePolicyUpdateFailure(throwable, false))
                            .subscribeOn(scheduler);
    }

    Observable<Long> getMostRecentPolicyUpdateTimestamp() {
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
