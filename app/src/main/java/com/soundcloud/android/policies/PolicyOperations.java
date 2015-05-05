package com.soundcloud.android.policies;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PolicyOperations {

    public static final long POLICY_STALE_AGE_MILISECONDS = TimeUnit.HOURS.toMillis(24);

    public static final Func1<PolicyInfo, Urn> TO_TRACK_URN = new Func1<PolicyInfo, Urn>() {
        @Override
        public Urn call(PolicyInfo policy) {
            return policy.getTrackUrn();
        }
    };

    public static final Func1<PolicyInfo, Boolean> FILTER_MONETIZABLE = new Func1<PolicyInfo, Boolean>() {
        @Override
        public Boolean call(PolicyInfo policy) {
            return !policy.isMonetizable();
        }
    };

    private final FetchPoliciesCommand fetchPoliciesCommand;
    private final StorePoliciesCommand storePoliciesCommand;
    private final Scheduler scheduler;

    private final Action1<Collection<PolicyInfo>> storePolicies = new Action1<Collection<PolicyInfo>>() {
        @Override
        public void call(Collection<PolicyInfo> policies) {
            storePoliciesCommand.with(policies).call();
        }
    };

    @Inject
    public PolicyOperations(FetchPoliciesCommand fetchPoliciesCommand,
                            StorePoliciesCommand storePoliciesCommand,
                            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.fetchPoliciesCommand = fetchPoliciesCommand;
        this.storePoliciesCommand = storePoliciesCommand;
        this.scheduler = scheduler;
    }

    public Observable<Void> updatePolicies(Collection<Urn> urns) {
        return fetchAndStorePolicies(urns)
                .map(RxUtils.TO_VOID);
    }

    public Observable<List<Urn>> filterMonetizableTracks(Collection<Urn> urns) {
        return fetchAndStorePolicies(urns)
                .flatMap(RxUtils.<PolicyInfo>emitCollectionItems())
                .filter(FILTER_MONETIZABLE)
                .map(TO_TRACK_URN)
                .toList();
    }

    private Observable<Collection<PolicyInfo>> fetchAndStorePolicies(Collection<Urn> urns) {
        return fetchPoliciesCommand.with(urns).toObservable()
                .subscribeOn(scheduler)
                .doOnNext(storePolicies);
    }

}
