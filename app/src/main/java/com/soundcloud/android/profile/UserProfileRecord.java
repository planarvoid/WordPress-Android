package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.users.UserRecord;

public interface UserProfileRecord {
    UserRecord getUser();

    ModelCollection<? extends ApiEntityHolderSource> getSpotlight();

    ModelCollection<? extends RecordHolder> getTracks();

    ModelCollection<? extends RecordHolder> getReleases();

    ModelCollection<? extends RecordHolder> getPlaylists();

    ModelCollection<? extends ApiEntityHolderSource> getReposts();

    ModelCollection<? extends ApiEntityHolderSource> getLikes();
}
