package com.soundcloud.android.policies;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

public class PolicyOperations {

    private final FetchPoliciesCommand fetchPoliciesCommand;
    private final StorePoliciesCommand storePoliciesCommand;
    private final Scheduler scheduler;

    @Inject
    public PolicyOperations(FetchPoliciesCommand fetchPoliciesCommand,
                            StorePoliciesCommand storePoliciesCommand,
                            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.fetchPoliciesCommand = fetchPoliciesCommand;
        this.storePoliciesCommand = storePoliciesCommand;
        this.scheduler = scheduler;
    }

    public Observable<Void> fetchAndStorePolicies(Collection<Urn> urns) {
        return fetchPoliciesCommand.with(urns).toObservable()
                .subscribeOn(scheduler)
                .flatMap(storePoliciesCommand)
                .map(RxUtils.TO_VOID);
    }

}
