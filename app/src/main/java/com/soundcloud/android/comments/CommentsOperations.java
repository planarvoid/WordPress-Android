package com.soundcloud.android.comments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
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
                .addQueryParameters("linked_partitioning", "1")
                .forResource(TypeToken.of(CommentsCollection.class))
                .build();
        return httpClient.<CommentsCollection>fetchModels(request).map(new Func1<CommentsCollection, List<Comment>>() {
            @Override
            public List<Comment> call(CommentsCollection collection) {
                List<Comment> comments = new ArrayList<>(collection.size());
                for (PublicApiComment apiComment : collection) {
                    comments.add(new Comment(apiComment));
                }
                return comments;
            }
        });
    }

    @VisibleForTesting
    static class CommentsCollection extends CollectionHolder<PublicApiComment> {
        @SuppressWarnings("unused") // Jackson calls this
        CommentsCollection() {
        }

        CommentsCollection(List<PublicApiComment> comments) {
            super(comments);
        }
    }
}
