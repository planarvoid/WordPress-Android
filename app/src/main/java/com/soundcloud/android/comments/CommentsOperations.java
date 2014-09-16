package com.soundcloud.android.comments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

class CommentsOperations {

    private final RxHttpClient httpClient;

    @Inject
    public CommentsOperations(RxHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    Observable<List<Comment>> comments(TrackUrn trackUrn) {
        APIRequest<CommentsCollection> request = SoundCloudAPIRequest.RequestBuilder
                .<CommentsCollection>get(APIEndpoints.TRACK_COMMENTS.path(trackUrn.numericId))
                .forPublicAPI()
                .forResource(TypeToken.of(CommentsCollection.class))
                .build();
        return httpClient.fetchModels(request);
    }

    @VisibleForTesting
    static class CommentsCollection extends CollectionHolder<Comment> {
        @SuppressWarnings("unused") // Jackson calls this
        CommentsCollection() {
        }

        CommentsCollection(List<Comment> comments) {
            super(comments);
        }
    }
}
