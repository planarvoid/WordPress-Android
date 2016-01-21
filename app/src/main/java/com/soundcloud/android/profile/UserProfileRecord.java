package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;

public interface UserProfileRecord {
    ApiEntityHolder getUser();

    ModelCollection<? extends ApiEntityHolderSource> getSpotlight();

    ModelCollection<? extends ApiEntityHolder> getTracks();

    ModelCollection<? extends ApiEntityHolder> getReleases();

    ModelCollection<? extends ApiEntityHolder> getPlaylists();

    ModelCollection<? extends ApiEntityHolderSource> getReposts();

    ModelCollection<? extends ApiEntityHolderSource> getLikes();
}
