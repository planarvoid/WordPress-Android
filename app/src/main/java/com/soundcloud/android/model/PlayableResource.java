package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.*;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Date;

abstract class PlayableResource extends ScResource implements Playable, Refreshable, Parcelable {
    public abstract Date getCreatedAt();
    public abstract String getArtwork();
    public abstract boolean isStale();

    @JsonView(Views.Mini.class) public String title;
    @JsonView(Views.Mini.class) @Nullable public User user;
    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) public long user_id;
    @JsonView(Views.Mini.class) public String artwork_url;
    @JsonView(Views.Mini.class) public String permalink;
    @JsonView(Views.Mini.class) public String permalink_url;

    @JsonView(Views.Full.class) @JsonProperty("user_favorite") public boolean user_like;
    @JsonView(Views.Full.class) public boolean user_repost;

    @JsonView(Views.Full.class) public int duration = NOT_SET;
    @JsonView(Views.Full.class) @Nullable public Date created_at;

    @JsonView(Views.Full.class) public boolean streamable;
    @JsonView(Views.Full.class) public boolean downloadable;
    @JsonView(Views.Full.class) public String license;
    @JsonView(Views.Full.class) public int label_id = NOT_SET;
    @JsonView(Views.Full.class) public String purchase_url;
    @JsonView(Views.Full.class) public String label_name;
    @JsonView(Views.Full.class) public String ean;
    @JsonView(Views.Full.class) public String release;
    @JsonView(Views.Full.class) public String description;

    @JsonView(Views.Full.class) public String genre;
    @JsonView(Views.Full.class) public String type;

    @JsonView(Views.Full.class) public int release_day = NOT_SET;
    @JsonView(Views.Full.class) public int release_year = NOT_SET;
    @JsonView(Views.Full.class) public int release_month = NOT_SET;

    @JsonView(Views.Full.class) public String purchase_title;
    @JsonView(Views.Full.class) public String embeddable_by;

    @JsonView(Views.Full.class) public int likes_count = NOT_SET;
    @JsonView(Views.Full.class) public int reposts_count = NOT_SET;
    @JsonView(Views.Full.class) public String tag_list;
    @JsonView(Views.Full.class) public Sharing sharing;  //  public | private

    // app fields
    @JsonIgnore protected CharSequence mElapsedTime;
    @JsonIgnore protected String mArtworkUri;

    public CharSequence getTimeSinceCreated(Context context) {
        if (mElapsedTime == null) refreshTimeSinceCreated(context);
        return mElapsedTime;
    }

    public void refreshTimeSinceCreated(Context context) {
        final Date createdAt = getCreatedAt();
        if (createdAt != null) {
            mElapsedTime = ScTextUtils.getTimeElapsed(context.getResources(), createdAt.getTime());
        }
    }

    public void refreshListArtworkUri(Context context) {
        final String iconUrl = getArtwork();
        mArtworkUri = TextUtils.isEmpty(iconUrl) ? null : Consts.GraphicSize.formatUriForList(context, iconUrl);
    }

    public String getListArtworkUrl(Context context) {
        if (TextUtils.isEmpty(mArtworkUri)) refreshListArtworkUri(context);
        return mArtworkUri;
    }

    public String getPlayerArtworkUri(Context context) {
        final String iconUrl = getArtwork();
        return TextUtils.isEmpty(iconUrl) ? null : Consts.GraphicSize.formatUriForPlayer(context, iconUrl);
    }

    public Bundle getBundle() {
        Bundle b = new Bundle();
        b.putLong("id", id);
        b.putString("title", title);
        b.putParcelable("user", user);
        b.putString("uri", uri);
        b.putLong("user_id", user_id);
        b.putString("artwork_url", artwork_url);
        b.putString("permalink", permalink);
        b.putString("permalink_url", permalink_url);
        b.putBoolean("user_like", user_like);
        b.putBoolean("user_repost", user_repost);
        b.putInt("duration", duration);
        b.putLong("created_at",  created_at != null ? created_at.getTime() : -1l);
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
        b.putCharSequence("elapsedTime", mElapsedTime);
        b.putString("list_artwork_uri", mArtworkUri);
        return b;
    }

    public void readFromBundle(Bundle b) {
        id = b.getLong("id");
        title = b.getString("title");
        user = b.getParcelable("user");
        uri = b.getString("uri");
        user_id = b.getLong("user_id");
        artwork_url = b.getString("artwork_url");
        permalink = b.getString("permalink");
        permalink_url = b.getString("permalink_url");
        user_like = b.getBoolean("user_like");
        user_repost = b.getBoolean("user_repost");
        duration = b.getInt("duration");
        created_at = b.getLong("created_at") == -1l ? null : new Date(b.getLong("created_at"));
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
        sharing = Sharing.fromString(b.getString("sharing"));
        mElapsedTime = b.getCharSequence("elapsedTime");
        mArtworkUri = b.getString("list_artwork_uri");
    }

}
