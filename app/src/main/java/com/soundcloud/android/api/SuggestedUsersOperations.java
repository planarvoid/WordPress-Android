package com.soundcloud.android.api;


import static com.google.common.collect.Collections2.filter;
import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ScSuccessObserver;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.utils.Log;
import rx.Observable;
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
    private final UserAssociationStorage mUserAssociationStorage;

    public SuggestedUsersOperations() {
        this(new SoundCloudRxHttpClient(ScSchedulers.API_SCHEDULER));
    }

    public SuggestedUsersOperations(SoundCloudRxHttpClient soundCloudRxHttpClient) {
        this(soundCloudRxHttpClient, new UserAssociationStorage());
    }

    @VisibleForTesting
    protected SuggestedUsersOperations(RxHttpClient rxHttpClient, UserAssociationStorage userAssociationStorage) {
        mRxHttpClient = rxHttpClient;
        mUserAssociationStorage = userAssociationStorage;
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

    public boolean bulkFollowAssociations(Collection<UserAssociation> userAssociations) {
        Collection<UserAssociation> associationsWithTokens = filter(userAssociations, UserAssociation.HAS_TOKEN_PREDICATE);
        Collection<String> tokens = Collections2.transform(associationsWithTokens, UserAssociation.TO_TOKEN_FUNCTION);

        if(tokens.isEmpty()){
            return true;
        }

        try {
            final String bulkFollowJsonContent = new ObjectMapper().writeValueAsString(new BulkFollowingsJsonCreator(tokens));
            APIRequest<Void> request = RequestBuilder.<Void>post(APIEndpoints.BULK_FOLLOW_USERS.path())
                    .forPublicAPI()
                    .withJsonContent(bulkFollowJsonContent)
                    .build();

            ScSuccessObserver<Void> successObserver = new BulkFollowObserver(associationsWithTokens, mUserAssociationStorage);
            mRxHttpClient.executeAPIRequest(request).toBlockingObservable().subscribe(successObserver);
            return successObserver.wasSuccess();

        } catch (JsonProcessingException e) {
            Log.e(SoundCloudApplication.TAG, "Error processing bulk follow json", e);
        }

        return false;

    }

    private static class BulkFollowingsJsonCreator {
        private BulkFollowingsJsonCreator(Collection<String> tokens) {
            this.tokens = tokens;
        }
        @JsonProperty
        Collection<String> tokens;
    }

    protected static class BulkFollowObserver extends ScSuccessObserver<Void> {

        private UserAssociationStorage mUserAssociationStorage;
        private Collection<UserAssociation> mUserAssociations;

        public BulkFollowObserver(Collection<UserAssociation> userAssociations, UserAssociationStorage userAssociationStorage) {
            mUserAssociations = userAssociations;
            mUserAssociationStorage = userAssociationStorage;

        }

        @Override
        public void onCompleted() {
            for(UserAssociation userAssociation : mUserAssociations){
                mUserAssociationStorage.setFollowingAsSynced(userAssociation);
            }
            super.onCompleted();
        }
    }
}
