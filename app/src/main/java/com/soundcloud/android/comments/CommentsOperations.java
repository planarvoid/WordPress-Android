package com.soundcloud.android.comments;

import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Observable;
import rx.android.Pager;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

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

    private final RxHttpClient httpClient;
    private final CommentsPager pager = new CommentsPager();

    @Inject
    public CommentsOperations(RxHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    CommentsPager pager() {
        return pager;
    }

    Observable<CommentsCollection> comments(TrackUrn trackUrn) {
        final APIRequest request = apiRequest(APIEndpoints.TRACK_COMMENTS.path(trackUrn.numericId))
                .addQueryParameters("linked_partitioning", "1")
                .addQueryParameters("limit", COMMENTS_PAGE_SIZE)
                .build();
        return httpClient.fetchModels(request);
    }

    private RequestBuilder apiRequest(String url) {
        return RequestBuilder.<CommentsCollection>get(url)
                .forPublicAPI()
                .forResource(TypeToken.of(CommentsCollection.class));
    }

    @VisibleForTesting
    static class CommentsCollection extends CollectionHolder<PublicApiComment> {
        @SuppressWarnings("unused") // Jackson calls this
        CommentsCollection() {
        }

        CommentsCollection(List<PublicApiComment> comments, String nextHref) {
            super(comments, nextHref);
        }
    }

    final class CommentsPager extends Pager<CommentsCollection> {

        @Override
        public Observable<CommentsCollection> call(CommentsCollection apiComments) {
            if (apiComments.getNextHref() != null) {
                return httpClient.fetchModels(apiRequest(apiComments.getNextHref()).build());
            } else {
                return Pager.finish();
            }
        }
    }
}
