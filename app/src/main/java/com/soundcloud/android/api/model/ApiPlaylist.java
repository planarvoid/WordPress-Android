package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.legacy.model.PlaylistStats;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class ApiPlaylist implements ImageResource, ApiEntityHolder, PlaylistRecord, PlaylistRecordHolder, ApiSyncable {

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract Optional<String> getImageUrlTemplate();

    public abstract String getTitle();

    public abstract ApiUser getUser();

    @Nullable
    public abstract String getGenre();

    @Nullable
    public abstract List<String> getTags();

    public abstract int getTrackCount();

    public abstract PlaylistStats getStats();

    public abstract long getDuration();

    public boolean isPublic() {
        return getSharing().isPublic();
    }

    public abstract Sharing getSharing();

    public abstract String getPermalinkUrl();

    public abstract Date getCreatedAt();

    public abstract boolean isAlbum();

    public abstract String getSetType();

    public abstract String getReleaseDate();

    public long getId() {
        return getUrn().getNumericId();
    }

    public String getUsername(){
        return getUser().getUsername();
    }

    @Override
    public int getLikesCount() {
        return getStats().getLikesCount();
    }

    @Override
    public int getRepostsCount() {
        return getStats().getRepostsCount();
    }

    @Override
    public PlaylistRecord getPlaylistRecord() {
        return this;
    }

    @JsonCreator
    public static ApiPlaylist create(@JsonProperty("urn") Urn urn,
                                     @JsonProperty("artwork_url_template") String imageUrlTemplate,
                                     @JsonProperty("title") String title,
                                     @JsonProperty("genre") String genre,
                                     @JsonProperty("user_tags") List<String> tags,
                                     @JsonProperty("trackCount") int trackCount,
                                     @JsonProperty("_embedded") RelatedResources relatedResources,
                                     @JsonProperty("duration") long duration,
                                     @JsonProperty("sharing") Sharing sharing,
                                     @JsonProperty("permalink_url") String permalinkUrl,
                                     @JsonProperty("created_at") Date createdAt,
                                     @JsonProperty("is_album") boolean album,
                                     @JsonProperty("set_type") String setType,
                                     @JsonProperty("release_date") String releaseDate) {
        return builder()
                .urn(urn)
                .imageUrlTemplate(Optional.fromNullable(imageUrlTemplate))
                .title(title)
                .user(relatedResources.user)
                .genre(genre)
                .tags(tags)
                .trackCount(trackCount)
                .stats(relatedResources.stats)
                .duration(duration)
                .sharing(sharing)
                .permalinkUrl(permalinkUrl)
                .createdAt(createdAt)
                .album(album)
                .setType(setType)
                .releaseDate(releaseDate)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ApiPlaylist.Builder();
    }

    private static class RelatedResources {
        private ApiUser user;
        private PlaylistStats stats;

        void setUser(ApiUser user) {
            this.user = user;
        }

        void setStats(PlaylistStats stats) {
            this.stats = stats;
        }
    }

    public abstract ApiPlaylist.Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder urn(Urn newUrn);

        public abstract Builder imageUrlTemplate(Optional<String> imageUrlTemplate);

        public abstract Builder title(String title);

        public abstract Builder user(ApiUser user);

        public abstract Builder genre(String genre);

        public abstract Builder tags(List<String> tags);

        public abstract Builder trackCount(int trackCount);

        public abstract Builder stats(PlaylistStats stats);

        public abstract Builder duration(long duration);

        public abstract Builder sharing(Sharing sharing);

        public abstract Builder permalinkUrl(String permalinkUrl);

        public abstract Builder createdAt(Date createdAt);

        public abstract Builder album(boolean album);

        public abstract Builder setType(String setType);

        public abstract Builder releaseDate(String releaseDate);

        public abstract ApiPlaylist build();
    }
}
