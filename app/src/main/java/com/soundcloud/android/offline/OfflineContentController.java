package com.soundcloud.android.offline;

import rx.subscriptions.CompositeSubscription;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OfflineContentController {

    private final Context context;
    private final OfflineContentOperations operations;

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    public OfflineContentController(OfflineContentOperations operations, Context context) {
        this.operations = operations;
        this.context = context;
    }

    public void subscribe() {
        subscription = new CompositeSubscription(
                operations.startOfflineContentSyncing().subscribe(new StartOfflineSyncServiceSubscriber(context, true)),
                operations.stopOfflineContentSyncing().subscribe(new StartOfflineSyncServiceSubscriber(context, false))
        );
    }

    public void unsubscribe() {
        subscription.unsubscribe();
    }
}
