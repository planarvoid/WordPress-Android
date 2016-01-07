package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.users.UserRecord;

public interface UserProfileRecord {
    UserRecord getUser();

    ModelCollection<? extends ApiEntityHolderSource> getSpotlight();

    ModelCollection<? extends TrackRecordHolder> getTracks();

    ModelCollection<? extends PlaylistRecordHolder> getReleases();

    ModelCollection<? extends PlaylistRecordHolder> getPlaylists();

    ModelCollection<? extends ApiEntityHolderSource> getReposts();

    ModelCollection<? extends ApiEntityHolderSource> getLikes();
}
