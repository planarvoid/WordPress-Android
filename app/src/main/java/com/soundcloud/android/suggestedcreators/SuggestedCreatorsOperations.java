package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.SoundStreamItem;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;

public class SuggestedCreatorsOperations {
    private final static int FOLLOWINGS_LIMIT = 5;

    private final FeatureFlags featureFlags;
    private final MyProfileOperations myProfileOperations;

    @Inject
    public SuggestedCreatorsOperations(FeatureFlags featureFlags,
                                       MyProfileOperations myProfileOperations) {
        this.featureFlags = featureFlags;
        this.myProfileOperations = myProfileOperations;
    }

    public Observable<SoundStreamItem> suggestedCreators() {
        if (featureFlags.isEnabled(Flag.SUGGESTED_CREATORS)) {
            return myProfileOperations.numberOfFollowings().flatMap(new Func1<Integer, Observable<SoundStreamItem>>() {
                @Override
                public Observable<SoundStreamItem> call(Integer integer) {
                    if (integer <= FOLLOWINGS_LIMIT) {
                        return Observable.just(SoundStreamItem.forSuggestedCreators());
                    }
                    return Observable.empty();
                }
            });
        }
        return Observable.empty();
    }
}
