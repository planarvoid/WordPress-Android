package com.soundcloud.android.likes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@VisibleForTesting
public class ApiPlaylistLike extends ApiLike {
    public ApiPlaylistLike(@JsonProperty("playlist_urn") Urn urn, @JsonProperty("created_at") Date createdAt) {
        super(urn, createdAt);
    }
}
