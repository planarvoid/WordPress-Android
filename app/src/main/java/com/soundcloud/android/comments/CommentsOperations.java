package com.soundcloud.android.comments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.android.LegacyPager;

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

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final FeatureFlags featureFlags;
    private final CommentsPager pager = new CommentsPager();

    @Inject
    public CommentsOperations(ApiClientRx apiClientRx,
                              @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                              FeatureFlags featureFlags) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.featureFlags = featureFlags;
    }

    CommentsPager pager() {
        return pager;
    }

    Observable<Comment> addComment(Urn trackUrn, String commentText, long timestamp) {
        return featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE) ?
               addApiMobileComment(trackUrn, commentText, timestamp).map(Comment::new) :
               addPublicApiComment(trackUrn, commentText, timestamp).map(Comment::new);
    }

    Observable<ModelCollection<Comment>> comments(Urn trackUrn) {
        return featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE) ?
               apiMobileComments(trackUrn).map(comments -> comments.transform(Comment::new)) :
               publicApiComments(trackUrn).map(comments -> comments.toModelCollection().transform(Comment::new));
    }

    private Observable<ModelCollection<Comment>> comments(String nextPageUrl) {
        return featureFlags.isEnabled(Flag.COMMENTS_ON_API_MOBILE) ?
               apiMobileNextPage(nextPageUrl).map(comments -> comments.transform(Comment::new)) :
               publicApiNextPage(nextPageUrl).map(comments -> comments.toModelCollection().transform(Comment::new));
    }

    private Observable<ApiComment> addApiMobileComment(Urn trackUrn, String body, long trackTime) {
        final Map<String, Object> comment = new HashMap<>(2);
        comment.put("body", body);
        comment.put("track_time", trackTime);

        final ApiRequest request = ApiRequest.post(ApiEndpoints.TRACK_COMMENTS.path(trackUrn))
                                             .forPrivateApi()
                                             .withContent(comment)
                                             .build();

        return apiClientRx.mappedResponse(request, ApiComment.class).subscribeOn(scheduler);
    }

    private Observable<ModelCollection<ApiComment>> apiMobileComments(Urn trackUrn) {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.TRACK_COMMENTS.path(trackUrn))
                                             .forPrivateApi()
                                             .addQueryParam("threaded", 0)
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, COMMENTS_PAGE_SIZE)
                                             .build();

        return apiClientRx.mappedResponse(request, TYPE_TOKEN).subscribeOn(scheduler);
    }

    private Observable<ModelCollection<ApiComment>> apiMobileNextPage(String nextPageUrl) {
        final ApiRequest request = ApiRequest.get(nextPageUrl).forPrivateApi().build();
        return apiClientRx.mappedResponse(request, TYPE_TOKEN).subscribeOn(scheduler);
    }

    private Observable<PublicApiComment> addPublicApiComment(Urn trackUrn, String commentText, long timestamp) {

        final ApiRequest request = ApiRequest.post(ApiEndpoints.TRACK_COMMENTS.path(trackUrn.getNumericId()))
                                             .forPublicApi()
                                             .withContent(new CommentHolder(commentText, timestamp))
                                             .build();

        return apiClientRx.mappedResponse(request, PublicApiComment.class).subscribeOn(scheduler);
    }

    private Observable<CommentsCollection> publicApiComments(Urn trackUrn) {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.TRACK_COMMENTS.path(trackUrn.getNumericId()))
                                             .forPublicApi()
                                             .addQueryParam("linked_partitioning", "1")
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, COMMENTS_PAGE_SIZE)
                                             .build();
        return apiClientRx.mappedResponse(request, CommentsCollection.class).subscribeOn(scheduler);
    }

    private Observable<CommentsCollection> publicApiNextPage(String nextPageUrl) {
        final ApiRequest request = ApiRequest.get(nextPageUrl).forPublicApi().build();
        return apiClientRx.mappedResponse(request, CommentsCollection.class).subscribeOn(scheduler);
    }

    @VisibleForTesting
    @Deprecated
    static class CommentsCollection extends CollectionHolder<PublicApiComment> {
        @SuppressWarnings("unused")
        CommentsCollection() {
            // Jackson calls this
        }

        CommentsCollection(List<PublicApiComment> comments, String nextHref) {
            super(comments, nextHref);
        }
    }

    final class CommentsPager extends LegacyPager<ModelCollection<Comment>> {

        @Override
        public Observable<ModelCollection<Comment>> call(ModelCollection<Comment> apiComments) {
            return apiComments.getNextLink().transform(next -> comments(next.getHref())).or(LegacyPager.finish());
        }
    }

    @Deprecated
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
