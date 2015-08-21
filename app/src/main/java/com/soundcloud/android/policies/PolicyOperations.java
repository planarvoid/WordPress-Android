package com.soundcloud.android.policies;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PolicyOperations {
    private static final String TAG = PolicyOperations.class.getSimpleName();

    public static final long POLICY_STALE_AGE_MILLISECONDS = TimeUnit.HOURS.toMillis(24);

    public static final Func1<ApiPolicyInfo, Urn> TO_TRACK_URN = new Func1<ApiPolicyInfo, Urn>() {
        @Override
        public Urn call(ApiPolicyInfo policy) {
            return policy.getUrn();
        }
    };

    public static final Func1<ApiPolicyInfo, Boolean> FILTER_MONETIZABLE = new Func1<ApiPolicyInfo, Boolean>() {
        @Override
        public Boolean call(ApiPolicyInfo policy) {
            return !policy.isMonetizable();
        }
    };

    private final FetchPoliciesCommand fetchPoliciesCommand;
    private final StorePoliciesCommand storePoliciesCommand;
    private final LoadTracksForPolicyUpdateCommand loadTracksForPolicyUpdateCommand;
    private final Scheduler scheduler;

    @Inject
    PolicyOperations(FetchPoliciesCommand fetchPoliciesCommand,
                     StorePoliciesCommand storePoliciesCommand,
                     LoadTracksForPolicyUpdateCommand loadTracksForPolicyUpdateCommand,
                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.fetchPoliciesCommand = fetchPoliciesCommand;
        this.storePoliciesCommand = storePoliciesCommand;
        this.scheduler = scheduler;
        this.loadTracksForPolicyUpdateCommand = loadTracksForPolicyUpdateCommand;
    }

    public Observable<Void> updatePolicies(Collection<Urn> urns) {
        return fetchAndStorePolicies(urns)
                .map(RxUtils.TO_VOID);
    }

    public Observable<List<Urn>> filterMonetizableTracks(Collection<Urn> urns) {
        return fetchAndStorePolicies(urns)
                .flatMap(RxUtils.<ApiPolicyInfo>emitCollectionItems())
                .filter(FILTER_MONETIZABLE)
                .map(TO_TRACK_URN)
                .toList();
    }

    public List<Urn> updateTrackPolicies() {
        try {
            final List<Urn> urns = loadTracksForPolicyUpdateCommand.call(null);
            final Collection<ApiPolicyInfo> policyInfos = fetchPoliciesCommand.with(urns).call();
            final WriteResult result = storePoliciesCommand.call(policyInfos);
            return result.success() ? urns : Collections.<Urn>emptyList();
        } catch (Exception ex) {
            Log.e(PolicyUpdateService.TAG, "Failed to update policies", ex);
            return Collections.emptyList();
        }
    }

    private Observable<Collection<ApiPolicyInfo>> fetchAndStorePolicies(Collection<Urn> urns) {
        return fetchPoliciesCommand.with(urns).toObservable()
                .subscribeOn(scheduler)
                .doOnNext(storePoliciesCommand.toAction());
    }

}
