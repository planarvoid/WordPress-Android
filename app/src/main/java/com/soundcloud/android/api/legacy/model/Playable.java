package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToUser;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public abstract class Playable extends PublicApiResource
        implements PlayableHolder, RelatesToUser, Refreshable, Parcelable, ImageResource {
    public static final int DB_TYPE_TRACK = 0; // TODO should not be exposed
    public static final int DB_TYPE_PLAYLIST = 1;

    private static final Pattern TAG_PATTERN = Pattern.compile("(\"([^\"]+)\")");

    @JsonView(Views.Mini.class) public String title;
    @JsonView(Views.Mini.class) @Nullable public PublicApiUser user;
    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) public long user_id;
    @JsonView(Views.Mini.class) public String artwork_url;
    @JsonView(Views.Mini.class) public String permalink;
    @JsonView(Views.Mini.class) public String permalink_url;

    @JsonView(Views.Full.class) @JsonProperty("user_favorite") public boolean user_like;
    @JsonView(Views.Full.class) public boolean user_repost;

    @JsonView(Views.Full.class) public long duration = ScModel.NOT_SET;
    @JsonView(Views.Full.class) @Nullable public Date created_at;

    @JsonView(Views.Full.class) public boolean streamable;
    @JsonView(Views.Full.class) public boolean downloadable;
    @JsonView(Views.Full.class) public String license;
    @JsonView(Views.Full.class) public int label_id = ScModel.NOT_SET;
    @JsonView(Views.Full.class) public String purchase_url;
    @JsonView(Views.Full.class) public String label_name;
    @JsonView(Views.Full.class) public String ean;
    @JsonView(Views.Full.class) public String release;
    @JsonView(Views.Full.class) public String description;

    @JsonView(Views.Full.class) public String genre;
    @JsonView(Views.Full.class) public String type;

    @JsonView(Views.Full.class) public int release_day = ScModel.NOT_SET;
    @JsonView(Views.Full.class) public int release_year = ScModel.NOT_SET;
    @JsonView(Views.Full.class) public int release_month = ScModel.NOT_SET;

    @JsonView(Views.Full.class) public String purchase_title;
    @JsonView(Views.Full.class) public String embeddable_by;

    @JsonView(Views.Full.class) public int likes_count = ScModel.NOT_SET;
    @JsonView(Views.Full.class) public int reposts_count = ScModel.NOT_SET;
    @JsonView(Views.Full.class) @JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
    public int shared_to_count = ScModel.NOT_SET;
    @JsonView(Views.Full.class) public String tag_list;
    @JsonView(Views.Full.class) @NotNull public Sharing sharing = Sharing.UNDEFINED;  //  public | private

    // app fields
    @JsonIgnore protected CharSequence elapsedTime;
    @JsonIgnore protected String artworkUri;

    public Playable() {
    }

    public Playable(long id) {
        super(id);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPermalinkUrl(String permalinkUrl) {
        this.permalink_url = permalinkUrl;
    }

    public void setCreatedAt(@Nullable Date createdAt) {
        this.created_at = createdAt;
    }

    public Date getCreatedAt() {
        return created_at;
    }

    @Override
    @Nullable
    @JsonIgnore
    public PublicApiUser getUser() {
        return user;
    }

    public void setUser(@Nullable PublicApiUser user) {
        this.user = user;
    }

    @Override
    @JsonIgnore
    public Playable getPlayable() {
        return this;
    }

    public Bundle getBundle() {
        Bundle b = new Bundle();
        b.putLong("id", getId());
        b.putString("title", title);
        b.putParcelable("user", user);
        b.putString("uri", uri);
        b.putLong("user_id", user_id);
        b.putString("artwork_url", artwork_url);
        b.putString("permalink", permalink);
        b.putString("permalink_url", permalink_url);
        b.putBoolean("user_like", user_like);
        b.putBoolean("user_repost", user_repost);
        b.putLong("duration", duration);
        b.putLong("created_at", created_at != null ? created_at.getTime() : -1L);
        b.putBoolean("streamable", streamable);
        b.putBoolean("downloadable", downloadable);
        b.putString("license", license);
        b.putInt("label_id", label_id);
        b.putString("purchase_url", purchase_url);
        b.putString("label_name", label_name);
        b.putString("ean", ean);
        b.putString("release", release);
        b.putString("description", description);
        b.putString("genre", genre);
        b.putString("type", type);
        b.putInt("release_day", release_day);
        b.putInt("release_year", release_year);
        b.putInt("release_month", release_month);
        b.putString("purchase_title", purchase_title);
        b.putString("embeddable_by", embeddable_by);
        b.putInt("likes_count", likes_count);
        b.putInt("reposts_count", reposts_count);
        b.putString("tag_list", tag_list);
        b.putString("sharing", sharing.value());
        b.putCharSequence("elapsedTime", elapsedTime);
        b.putString("list_artwork_uri", artworkUri);
        return b;
    }

    public void readFromBundle(Bundle b) {
        setId(b.getLong("id"));
        title = b.getString("title");
        user = b.getParcelable("user");
        uri = b.getString("uri");
        user_id = b.getLong("user_id");
        artwork_url = b.getString("artwork_url");
        permalink = b.getString("permalink");
        permalink_url = b.getString("permalink_url");
        user_like = b.getBoolean("user_like");
        user_repost = b.getBoolean("user_repost");
        duration = b.getLong("duration");
        created_at = b.getLong("created_at") == -1L ? null : new Date(b.getLong("created_at"));
        streamable = b.getBoolean("streamable");
        downloadable = b.getBoolean("downloadable");
        license = b.getString("license");
        label_id = b.getInt("label_id");
        purchase_url = b.getString("purchase_url");
        label_name = b.getString("label_name");
        ean = b.getString("ean");
        release = b.getString("release");
        description = b.getString("description");
        genre = b.getString("genre");
        type = b.getString("type");
        release_day = b.getInt("release_day");
        release_year = b.getInt("release_year");
        release_month = b.getInt("release_month");
        purchase_title = b.getString("purchase_title");
        embeddable_by = b.getString("embeddable_by");
        likes_count = b.getInt("likes_count");
        reposts_count = b.getInt("reposts_count");
        tag_list = b.getString("tag_list");
        sharing = Sharing.from(b.getString("sharing"));
        elapsedTime = b.getCharSequence("elapsedTime");
        artworkUri = b.getString("list_artwork_uri");
    }

    public Urn getUserUrn() {
        return user != null ? user.getUrn() : Urn.forUser(user_id);
    }

    public long getUserId() {
        return user != null ? user.getId() : user_id;
    }

    public boolean isPublic() {
        return sharing.isPublic();
    }

    public boolean isPrivate() {
        return sharing.isPrivate();
    }

    public String getUsername() {
        return user != null ? user.username : "";
    }

    @NotNull
    public Sharing getSharing() {
        return sharing;
    }

    public List<String> humanTags() {
        List<String> tags = new ArrayList<>();
        if (tag_list == null) {
            return tags;
        }
        Matcher m = TAG_PATTERN.matcher(tag_list);
        while (m.find()) {
            tags.add(tag_list.substring(m.start(2), m.end(2)).trim());
        }
        String singlewords = m.replaceAll("");
        for (String t : singlewords.split("\\s")) {
            if (!TextUtils.isEmpty(t) && t.indexOf(':') == -1 && t.indexOf('=') == -1) {
                tags.add(t);
            }
        }
        return tags;
    }
}
