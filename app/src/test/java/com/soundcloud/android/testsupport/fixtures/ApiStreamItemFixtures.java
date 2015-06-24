package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.api.model.stream.ApiStreamPlaylistPost;
import com.soundcloud.android.api.model.stream.ApiStreamPlaylistRepost;
import com.soundcloud.android.api.model.stream.ApiStreamTrackPost;
import com.soundcloud.android.api.model.stream.ApiStreamTrackRepost;

public class ApiStreamItemFixtures {

    public static ApiStreamItem promotedStreamItemWithoutPromoter() {
        return new ApiStreamItem(PromotedFixtures.promotedStreamItemWithoutPromoter());
    }

    public static ApiStreamItem trackPost() {
        return new ApiStreamItem(ModelFixtures.create(ApiStreamTrackPost.class));
    }

    public static ApiStreamItem trackRepost() {
        return new ApiStreamItem(ModelFixtures.create(ApiStreamTrackRepost.class));
    }

    public static ApiStreamItem playlistPost() {
        return new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistPost.class));
    }

    public static ApiStreamItem playlistRepost() {
        return new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistRepost.class));
    }

}
