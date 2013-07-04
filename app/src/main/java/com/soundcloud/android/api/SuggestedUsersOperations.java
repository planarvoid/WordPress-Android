package com.soundcloud.android.api;


import static com.google.common.collect.Collections2.filter;
import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.Collection;
import java.util.List;


public class SuggestedUsersOperations extends ScheduledOperations {

    private static final Func1<Exception, CategoryGroup> EMPTY_FACEBOOK_GROUP = new Func1<Exception, CategoryGroup>() {
        @Override
        public CategoryGroup call(Exception e) {
            return CategoryGroup.createErrorGroup(CategoryGroup.KEY_FACEBOOK);
        }
    };

    private final RxHttpClient mRxHttpClient;

    public SuggestedUsersOperations() {
        this(new SoundCloudRxHttpClient());
    }

    @VisibleForTesting
    public SuggestedUsersOperations(SoundCloudRxHttpClient rxHttpClient) {
        mRxHttpClient = rxHttpClient;
    }

    public Observable<CategoryGroup> getMusicAndSoundsSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new TypeToken<List<CategoryGroup>>() {})
                .build();
        return schedule(mRxHttpClient.<CategoryGroup>executeAPIRequest(request));
    }

    public Observable<CategoryGroup> getFacebookSuggestions() {
        APIRequest<List<CategoryGroup>> request = RequestBuilder.<List<CategoryGroup>>get(APIEndpoints.SUGGESTED_USER_FACEBOOK_CATEGORIES.path())
                .forPrivateAPI(1)
                .forResource(new TypeToken<List<CategoryGroup>>() {})
                .build();
        return schedule(mRxHttpClient.<CategoryGroup>executeAPIRequest(request).onErrorReturn(EMPTY_FACEBOOK_GROUP));
    }

    public Observable<CategoryGroup> getCategoryGroups() {
        return schedule(Observable.merge(getMusicAndSoundsSuggestions(), getFacebookSuggestions()));
    }

    public Observable<Void> bulkFollowAssociations(final Collection<UserAssociation> userAssociations) {
        return createApiRequestObservable(userAssociations).flatMap(new Func1<APIRequest<Void>, Observable<Void>>() {
            @Override
            public Observable<Void> call(APIRequest<Void> request) {
                return mRxHttpClient.executeAPIRequest(request);
            }
        });
    }

    private Observable<APIRequest<Void>> createApiRequestObservable(final Collection<UserAssociation> userAssociations) {
        return Observable.create(new Func1<Observer<APIRequest<Void>>, Subscription>() {
            @Override
            public Subscription call(Observer<APIRequest<Void>> apiRequestObserver) {
                final Collection<UserAssociation> associationsWithTokens = filter(userAssociations, UserAssociation.HAS_TOKEN_PREDICATE);
                final Collection<String> tokens = Collections2.transform(associationsWithTokens, UserAssociation.TO_TOKEN_FUNCTION);
                if (!tokens.isEmpty()) {
                    APIRequest<Void> request = RequestBuilder.<Void>post(APIEndpoints.BULK_FOLLOW_USERS.path())
                            .forPublicAPI()
                            .withContent(new BulkFollowingsHolder(tokens))
                            .build();

                    apiRequestObserver.onNext(request);
                }
                apiRequestObserver.onCompleted();
                return Subscriptions.empty();
            }
        });
    }

    @VisibleForTesting
    public static class BulkFollowingsHolder {
        public BulkFollowingsHolder(Collection<String> tokens) {
            this.tokens = tokens;
        }
        @JsonProperty
        Collection<String> tokens;
    }

}
