package com.soundcloud.android.api;


import static com.soundcloud.android.api.WebServices.APIRequestException;
import static com.soundcloud.android.api.WebServices.APIRequestException.APIErrorReason;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.CategoryGroup;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;

import java.util.Collection;


public class SuggestedUsersOperations {

    private WebServices webServices;

    public SuggestedUsersOperations(Context context){
        this(new WebServices(context));
    }

    @VisibleForTesting
    protected SuggestedUsersOperations(WebServices webServices) {
        this.webServices = webServices;
    }

    public Observable<CategoryGroup> getCategories(){
        return Observable.create(new Func1<Observer<CategoryGroup>, Subscription>() {
            @Override
            public Subscription call(Observer<CategoryGroup> categoriesObserver) {
                APIResponse response = webServices.get(WebServiceEndPoint.SUGGESTED_CATEGORIES);
                Collection<CategoryGroup> categories = response.getCollection();

                if(categories.isEmpty()){
                    categoriesObserver.onError(new APIRequestException(APIErrorReason.BAD_RESPONSE));
                } else {
                    for(CategoryGroup categoryGroup : categories){
                        categoriesObserver.onNext(categoryGroup);
                    }
                    categoriesObserver.onCompleted();
                }
                return Subscriptions.empty();
            }
        });
    }

}
