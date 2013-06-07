package com.soundcloud.android.api;


import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import rx.Observable;

import android.content.Context;


public class SuggestedUsersOperations extends ScheduledOperations {

    private RxHttpClient mRxHttpClient;

    public SuggestedUsersOperations(Context context){
        this(new SoundCloudRxHttpClient(context));
    }

    @VisibleForTesting
    protected SuggestedUsersOperations(RxHttpClient rxHttpClient) {
        this.mRxHttpClient = rxHttpClient;
    }

    public Observable<CategoryGroup> getCategories(){
        return mRxHttpClient.getResources(null);
    }

}
