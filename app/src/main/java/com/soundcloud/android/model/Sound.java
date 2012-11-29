package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.soundcloud.android.Consts;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Date;

public abstract class Sound extends ScResource implements Playable, Refreshable, Parcelable {
    public static final int DB_TYPE_TRACK    = 0; // TODO should not be exposed
    public static final int DB_TYPE_PLAYLIST = 1;

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

    public Sound() { }

    public Sound(Cursor cursor) {

            final int trackIdIdx = cursor.getColumnIndex(DBHelper.ActivityView.SOUND_ID);
            if (trackIdIdx == -1) {
                id = cursor.getLong(cursor.getColumnIndex(DBHelper.SoundView._ID));
            } else {
                id = cursor.getLong(cursor.getColumnIndex(DBHelper.ActivityView.SOUND_ID));
            }
            permalink = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.PERMALINK));
            duration = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.DURATION));

            created_at = new Date(cursor.getLong(cursor.getColumnIndex(DBHelper.SoundView.CREATED_AT)));
            tag_list = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.TAG_LIST));
            title = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.TITLE));
            permalink_url = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.PERMALINK_URL));
            artwork_url = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.ARTWORK_URL));
            downloadable = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.DOWNLOADABLE)) == 1;
            streamable = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.STREAMABLE)) == 1;
            sharing = Sharing.fromString(cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.SHARING)));
            likes_count = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.LIKES_COUNT));
            reposts_count = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.REPOSTS_COUNT));
            user_id = cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView.USER_ID));

            final long lastUpdated = cursor.getLong(cursor.getColumnIndex(DBHelper.SoundView.LAST_UPDATED));
            if (lastUpdated > 0) {
                last_updated = lastUpdated;
            }

            // gets joined in
            final int favIdx = cursor.getColumnIndex(DBHelper.SoundView.USER_LIKE);
            if (favIdx != -1) {
                user_like = cursor.getInt(favIdx) == 1;
            }
            final int repostIdx = cursor.getColumnIndex(DBHelper.SoundView.USER_REPOST);
            if (repostIdx != -1) {
                user_repost = cursor.getInt(repostIdx) == 1;
            }

        }


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
        b.putString("sharing", sharing != null ? sharing.value() : null);
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

    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();

        cv.put(DBHelper.Sounds.PERMALINK, permalink);
        cv.put(DBHelper.Sounds._TYPE, getTypeId());

        // account for partial objects, don't overwrite local full objects
        if (title != null) cv.put(DBHelper.Sounds.TITLE, title);
        if (duration != 0) cv.put(DBHelper.Sounds.DURATION, duration);
        if (user_id != 0) {
            cv.put(DBHelper.Sounds.USER_ID, user_id);
        } else if (user != null && user.isSaved()) {
            cv.put(DBHelper.Sounds.USER_ID, user.id);
        }
        if (created_at != null) cv.put(DBHelper.Sounds.CREATED_AT, created_at.getTime());
        if (tag_list != null) cv.put(DBHelper.Sounds.TAG_LIST, tag_list);
        if (permalink_url != null) cv.put(DBHelper.Sounds.PERMALINK_URL, permalink_url);
        if (artwork_url != null) cv.put(DBHelper.Sounds.ARTWORK_URL, artwork_url);
        if (downloadable) cv.put(DBHelper.Sounds.DOWNLOADABLE, downloadable);
        if (streamable) cv.put(DBHelper.Sounds.STREAMABLE, streamable);
        if (sharing != null) cv.put(DBHelper.Sounds.SHARING, sharing.value);
        if (likes_count != -1) cv.put(DBHelper.Sounds.LIKES_COUNT, likes_count);
        if (reposts_count != -1) cv.put(DBHelper.Sounds.REPOSTS_COUNT, reposts_count);
        return cv;
    }

    public Sound updateFrom(Sound updatedItem, CacheUpdateMode cacheUpdateMode) {
        id = updatedItem.id;
        title = updatedItem.title;
        permalink = updatedItem.permalink;

        user_id = updatedItem.user_id;
        uri = updatedItem.uri;

        if (cacheUpdateMode == CacheUpdateMode.FULL) {
            duration = updatedItem.duration;
            sharing = updatedItem.sharing;
            streamable = updatedItem.streamable;
            artwork_url = updatedItem.artwork_url;
            user = updatedItem.user;
            likes_count = updatedItem.likes_count;
            reposts_count = updatedItem.reposts_count;
            user_like = updatedItem.user_like;
            created_at = updatedItem.created_at;
            //user_repost = updatedItem.user_repost; THIS DOES NOT COME FROM THE API, ONLY UPDATE FROM DB

            // these will get refreshed
            mElapsedTime = null;
            mArtworkUri = null;
        }

        return this;
    }

    public abstract int getTypeId();
}
