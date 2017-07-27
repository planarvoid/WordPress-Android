package com.soundcloud.android.comments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.android.LegacyPager;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CommentsOperations {

    static final TypeToken<ModelCollection<ApiComment>> TYPE_TOKEN = new TypeToken<ModelCollection<ApiComment>>() {};

    @VisibleForTesting
    static final int COMMENTS_PAGE_SIZE = 50;

    static final Func1<ModelCollection<ApiComment>, List<Comment>> TO_COMMENT_VIEW_MODEL = apiComments -> Lists.transform(apiComments.getCollection(), Comment::new);

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final CommentsPager pager = new CommentsPager();

    @Inject
    public CommentsOperations(ApiClientRx apiClientRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
    }

    CommentsPager pager() {
        return pager;
    }

    Observable<PublicApiComment> addComment(Urn trackUrn, String commentText, long timestamp) {

        final ApiRequest request = ApiRequest.post(ApiEndpoints.TRACK_COMMENTS.path(trackUrn.getNumericId()))
                                             .forPublicApi()
                                             .withContent(new CommentHolder(commentText, timestamp))
                                             .build();

        return apiClientRx.mappedResponse(request, PublicApiComment.class).subscribeOn(scheduler);
    }

    Observable<ModelCollection<ApiComment>> comments(Urn trackUrn) {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.TRACK_COMMENTS.path(trackUrn))
                                             .forPrivateApi()
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, COMMENTS_PAGE_SIZE)
                                             .addQueryParam("threaded", 1)
                                             .build();
        return apiClientRx.mappedResponse(request, TYPE_TOKEN).subscribeOn(scheduler);
    }

    private Observable<ModelCollection<ApiComment>> comments(String nextPageUrl) {
        final ApiRequest request = ApiRequest.get(nextPageUrl).forPrivateApi().build();
        return apiClientRx.mappedResponse(request, TYPE_TOKEN).subscribeOn(scheduler);
    }


    final class CommentsPager extends LegacyPager<ModelCollection<ApiComment>> {

        @Override
        public Observable<ModelCollection<ApiComment>> call(ModelCollection<ApiComment> apiComments) {
            return apiComments.getNextLink().transform(next -> comments(next.getHref())).or(LegacyPager.finish());
        }
    }

    public static class CommentHolder {
        @JsonProperty
        final Map<String, String> comment;

        public CommentHolder(String body, long timestamp) {
            comment = new HashMap<>(2);
            comment.put("body", body);
            comment.put("timestamp", String.valueOf(timestamp));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof CommentHolder && MoreObjects.equal(comment, ((CommentHolder) o).comment);
        }

        @Override
        public int hashCode() {
            return comment.hashCode();
        }
    }

}
