package com.soundcloud.android.policies;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class PolicyOperations {

    private final FetchPoliciesCommand fetchPoliciesCommand;
    private final StorePoliciesCommand storePoliciesCommand;

    @Inject
    public PolicyOperations(FetchPoliciesCommand fetchPoliciesCommand, StorePoliciesCommand storePoliciesCommand) {
        this.fetchPoliciesCommand = fetchPoliciesCommand;
        this.storePoliciesCommand = storePoliciesCommand;
    }

    public Observable<Void> fetchAndStorePolicies(List<Urn> urns) {
        return fetchPoliciesCommand.with(urns).toObservable()
                .flatMap(storePoliciesCommand)
                .map(RxUtils.TO_VOID);
    }

}
