package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiPlaylistPost;
import com.soundcloud.android.api.model.stream.ApiPlaylistRepost;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.api.model.stream.ApiTrackPost;
import com.soundcloud.android.api.model.stream.ApiTrackRepost;

public class ApiStreamItemFixtures {

    public static ApiStreamItem promotedStreamItemWithoutPromoter(){
        return new ApiStreamItem(PromotedFixtures.promotedStreamItemWithoutPromoter());
    }

    public static ApiStreamItem promotedStreamItemWithPromoter(){
        return new ApiStreamItem(PromotedFixtures.promotedStreamItemWithPromoter(ModelFixtures.create((ApiUser.class))));
    }

    public static ApiStreamItem trackPost(){
        return new ApiStreamItem(ModelFixtures.create(ApiTrackPost.class));
    }

    public static ApiStreamItem trackRepost() {
        return new ApiStreamItem(ModelFixtures.create(ApiTrackRepost.class));
    }

    public static ApiStreamItem playlistPost() {
        return new ApiStreamItem(ModelFixtures.create(ApiPlaylistPost.class));
    }

    public static ApiStreamItem playlistRepost() {
        return new ApiStreamItem(ModelFixtures.create(ApiPlaylistRepost.class));
    }

}
