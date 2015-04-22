package com.soundcloud.android.comments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;
import rx.android.Pager;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CommentsOperations {

    @VisibleForTesting
    static final int COMMENTS_PAGE_SIZE = 50;

    static final Func1<CommentsCollection, List<Comment>> TO_COMMENT_VIEW_MODEL = new Func1<CommentsCollection, List<Comment>>() {
        @Override
        public List<Comment> call(CommentsCollection apiComments) {
            List<Comment> comments = new ArrayList<>(CommentsOperations.COMMENTS_PAGE_SIZE);
            for (PublicApiComment apiComment : apiComments) {
                comments.add(new Comment(apiComment));
            }
            return comments;
        }
    };

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

        final ApiRequest request = ApiRequest.Builder.post(ApiEndpoints.TRACK_COMMENTS.path(trackUrn.getNumericId()))
                .forPublicApi()
                .withContent(new CommentHolder(commentText, timestamp))
                .build();

        return apiClientRx.mappedResponse(request, PublicApiComment.class).subscribeOn(scheduler);
    }

    Observable<CommentsCollection> comments(Urn trackUrn) {
        final ApiRequest request = apiRequest(ApiEndpoints.TRACK_COMMENTS.path(trackUrn.getNumericId()))
                .addQueryParam("linked_partitioning", "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, COMMENTS_PAGE_SIZE)
                .build();
        return apiClientRx.mappedResponse(request, CommentsCollection.class).subscribeOn(scheduler);
    }

    private Observable<CommentsCollection> comments(String nextPageUrl) {
        final ApiRequest request = apiRequest(nextPageUrl).build();
        return apiClientRx.mappedResponse(request, CommentsCollection.class).subscribeOn(scheduler);
    }

    private ApiRequest.Builder apiRequest(String url) {
        return ApiRequest.Builder.<CommentsCollection>get(url).forPublicApi();
    }

    @VisibleForTesting
    static class CommentsCollection extends CollectionHolder<PublicApiComment> {
        @SuppressWarnings("unused")
        CommentsCollection() {
            // Jackson calls this
        }

        CommentsCollection(List<PublicApiComment> comments, String nextHref) {
            super(comments, nextHref);
        }
    }

    final class CommentsPager extends Pager<CommentsCollection> {

        @Override
        public Observable<CommentsCollection> call(CommentsCollection apiComments) {
            if (apiComments.getNextHref() != null) {
                return comments(apiComments.getNextHref());
            } else {
                return Pager.finish();
            }
        }
    }

    public static class CommentHolder {
        @JsonProperty
        final Map<String, String> comment;

        public CommentHolder(String body, long timestamp) {
            comment = Maps.newHashMapWithExpectedSize(2);
            comment.put("body", body);
            comment.put("timestamp", String.valueOf(timestamp));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof CommentHolder && Objects.equal(comment, ((CommentHolder) o).comment);
        }

        @Override
        public int hashCode() {
            return comment.hashCode();
        }
    }

}
