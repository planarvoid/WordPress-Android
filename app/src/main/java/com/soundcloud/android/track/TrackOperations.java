package com.soundcloud.android.track;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackUrn;
import rx.Observable;

import javax.inject.Inject;

public class TrackOperations {

    @SuppressWarnings("UnusedDeclaration")
    private static final String LOG_TAG = "TrackOperations";

    private final TrackStorage trackStorage;
    private final AccountOperations accountOperations;

    @Inject
    public TrackOperations(TrackStorage trackStorage, AccountOperations accountOperations) {
        this.trackStorage = trackStorage;
        this.accountOperations = accountOperations;
    }

    public Observable<PropertySet> track(final TrackUrn trackUrn) {
        return trackStorage.track(trackUrn, accountOperations.getLoggedInUserUrn());
    }

}
