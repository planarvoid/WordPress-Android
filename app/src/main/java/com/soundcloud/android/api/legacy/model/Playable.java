package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public abstract class Playable extends PublicApiResource implements PlayableHolder, RelatesToUser, Refreshable, Parcelable, PropertySetSource {
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

    protected static int getIntOrNotSet(Cursor c, String column) {
        final int index = c.getColumnIndex(column);
        return c.isNull(index) ? ScModel.NOT_SET : c.getInt(index);
    }

    public Playable() {
    }

    public Playable(long id) {
        super(id);
    }

    public Playable(Cursor cursor) {

        final int trackIdIdx = cursor.getColumnIndex(TableColumns.ActivityView.SOUND_ID);
        if (trackIdIdx == -1) {
            setId(cursor.getLong(cursor.getColumnIndex(TableColumns.SoundView._ID)));
        } else {
            setId(cursor.getLong(cursor.getColumnIndex(TableColumns.ActivityView.SOUND_ID)));
        }
        permalink = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.PERMALINK));
        duration = cursor.getLong(cursor.getColumnIndex(TableColumns.SoundView.DURATION));

        created_at = new Date(cursor.getLong(cursor.getColumnIndex(TableColumns.SoundView.CREATED_AT)));
        tag_list = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.TAG_LIST));
        title = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.TITLE));
        permalink_url = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.PERMALINK_URL));
        artwork_url = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.ARTWORK_URL));
        downloadable = cursor.getInt(cursor.getColumnIndex(TableColumns.SoundView.DOWNLOADABLE)) == 1;
        streamable = cursor.getInt(cursor.getColumnIndex(TableColumns.SoundView.STREAMABLE)) == 1;
        sharing = Sharing.from(cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.SHARING)));
        license = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.LICENSE));
        genre = cursor.getString(cursor.getColumnIndex(TableColumns.SoundView.GENRE));
        likes_count = getIntOrNotSet(cursor, TableColumns.SoundView.LIKES_COUNT);
        reposts_count = getIntOrNotSet(cursor, TableColumns.SoundView.REPOSTS_COUNT);
        user_id = cursor.getInt(cursor.getColumnIndex(TableColumns.SoundView.USER_ID));

        final long lastUpdated = cursor.getLong(cursor.getColumnIndex(TableColumns.SoundView.LAST_UPDATED));
        if (lastUpdated > 0) {
            last_updated = lastUpdated;
        }

        // gets joined in
        final int favIdx = cursor.getColumnIndex(TableColumns.SoundView.USER_LIKE);
        if (favIdx != -1) {
            user_like = cursor.getInt(favIdx) == 1;
        }
        final int repostIdx = cursor.getColumnIndex(TableColumns.SoundView.USER_REPOST);
        if (repostIdx != -1) {
            user_repost = cursor.getInt(repostIdx) == 1;
        }

        user = SoundCloudApplication.sModelManager.getCachedUserFromSoundViewCursor(cursor);

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
    public boolean isStale() {
        return false;
    }

    @Override
    public boolean isIncomplete() {
        return user == null || user.isIncomplete();
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

    public String getArtwork() {
        if (shouldLoadArtwork()) {
            return artwork_url;
        } else if (user != null && user.shouldLoadIcon()) {
            return user.avatar_url;
        } else {
            return null;
        }
    }

    public boolean shouldLoadArtwork() {
        return ImageUtils.checkIconShouldLoad(artwork_url);
    }

    public CharSequence getTimeSinceCreated(Context context) {
        if (elapsedTime == null) {
            refreshTimeSinceCreated(context);
        }
        return elapsedTime;
    }

    public void refreshTimeSinceCreated(Context context) {
        if (created_at != null) {
            elapsedTime = ScTextUtils.formatTimeElapsed(context.getResources(), created_at.getTime());
        }
    }

    @Override
    public void putDependencyValues(BulkInsertMap destination) {
        if (user != null) {
            user.putFullContentValues(destination);
        }
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
        b.putLong("created_at", created_at != null ? created_at.getTime() : -1l);
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
        sharing = Sharing.from(b.getString("sharing"));
        elapsedTime = b.getCharSequence("elapsedTime");
        artworkUri = b.getString("list_artwork_uri");
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();

        cv.put(TableColumns.Sounds.PERMALINK, permalink);
        cv.put(TableColumns.Sounds._TYPE, getTypeId());

        // account for partial objects, don't overwrite local full objects
        if (title != null) {
            cv.put(TableColumns.Sounds.TITLE, title);
        } else {
            // adding this in for the time being to better understand these crashes:
            // https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/53ebe1b3e3de5099baa83a9a
            ErrorUtils.handleSilentException(new IllegalStateException("Attempting to insert a playable with a null title; id=" + getId()));
        }
        if (duration > 0) {
            cv.put(TableColumns.Sounds.DURATION, duration);
        }
        if (user_id != 0) {
            cv.put(TableColumns.Sounds.USER_ID, user_id);
        } else if (user != null && user.isSaved()) {
            cv.put(TableColumns.Sounds.USER_ID, user.getId());
        }
        if (created_at != null) {
            cv.put(TableColumns.Sounds.CREATED_AT, created_at.getTime());
        }
        if (tag_list != null) {
            cv.put(TableColumns.Sounds.TAG_LIST, tag_list);
        }
        if (permalink_url != null) {
            cv.put(TableColumns.Sounds.PERMALINK_URL, permalink_url);
        }
        if (artwork_url != null) {
            cv.put(TableColumns.Sounds.ARTWORK_URL, artwork_url);
        }
        if (downloadable) {
            cv.put(TableColumns.Sounds.DOWNLOADABLE, downloadable);
        }
        if (streamable) {
            cv.put(TableColumns.Sounds.STREAMABLE, streamable);
        }
        if (sharing != Sharing.UNDEFINED) {
            cv.put(TableColumns.Sounds.SHARING, sharing.value);
        }
        if (license != null) {
            cv.put(TableColumns.Sounds.LICENSE, license);
        }
        if (genre != null) {
            cv.put(TableColumns.Sounds.GENRE, genre);
        }
        if (likes_count != -1) {
            cv.put(TableColumns.Sounds.LIKES_COUNT, likes_count);
        }
        if (reposts_count != -1) {
            cv.put(TableColumns.Sounds.REPOSTS_COUNT, reposts_count);
        }
        return cv;
    }

    public Playable updateFrom(Playable updatedItem, CacheUpdateMode cacheUpdateMode) {
        setId(updatedItem.getId());
        title = updatedItem.title;
        permalink = updatedItem.permalink;

        user_id = updatedItem.user_id;
        uri = updatedItem.uri;

        if (cacheUpdateMode == CacheUpdateMode.FULL) {
            duration = updatedItem.duration;
            sharing = updatedItem.sharing;
            streamable = updatedItem.streamable;
            downloadable = updatedItem.downloadable;
            artwork_url = updatedItem.artwork_url;
            permalink_url = updatedItem.permalink_url;
            user = updatedItem.user;
            likes_count = updatedItem.likes_count;
            reposts_count = updatedItem.reposts_count;
            created_at = updatedItem.created_at;
            description = updatedItem.description;
            tag_list = updatedItem.tag_list;
            license = updatedItem.license;
            label_id = updatedItem.label_id;
            label_name = updatedItem.label_name;
            ean = updatedItem.ean;
            genre = updatedItem.genre;
            type = updatedItem.type;

            purchase_url = updatedItem.purchase_url;
            purchase_title = updatedItem.purchase_title;

            release = updatedItem.release;
            release_day = updatedItem.release_day;
            release_year = updatedItem.release_year;
            release_month = updatedItem.release_month;

            embeddable_by = updatedItem.embeddable_by;

            // these will get refreshed
            elapsedTime = null;
            artworkUri = null;

            last_updated = updatedItem.last_updated;
        }

        return this;
    }

    public Urn getUserUrn() {
        return user != null ? user.getUrn() : Urn.forUser(user_id);
    }

    public long getUserId() {
        return user != null ? user.getId() : user_id;
    }

    public abstract int getTypeId();

    public boolean isPublic() {
        return sharing.isPublic();
    }

    public boolean isPrivate() {
        return sharing.isPrivate();
    }

    public boolean hasAvatar() {
        return user != null && user.hasAvatarUrl();
    }

    public String getUsername() {
        return user != null ? user.username : "";
    }

    @NotNull
    public Sharing getSharing() {
        return sharing;
    }

    protected static boolean isTrackCursor(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(TableColumns.Sounds._TYPE)) == DB_TYPE_TRACK;
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

    @Override
    public PropertySet toPropertySet() {
        if (title == null) {
            ErrorUtils.handleSilentException(new IllegalStateException("Attempting to create PropertySet with a null title; urn=" + getUrn()));
        }
        return PropertySet.from(
                PlayableProperty.DURATION.bind(duration),
                // titles are sometimes null from public api (unfortunately) playlist endpoint.
                PlayableProperty.TITLE.bind(title == null ? ScTextUtils.EMPTY_STRING : title),
                PlayableProperty.URN.bind(getUrn()),
                PlayableProperty.CREATOR_URN.bind(getUserUrn()),
                PlayableProperty.IS_PRIVATE.bind(sharing.isPrivate()),
                PlayableProperty.REPOSTS_COUNT.bind(reposts_count),
                PlayableProperty.LIKES_COUNT.bind(likes_count),
                PlayableProperty.IS_REPOSTED.bind(user_repost),
                PlayableProperty.IS_LIKED.bind(user_like),
                PlayableProperty.CREATED_AT.bind(created_at == null ? new Date() : created_at),
                // we may have null usernames if it is my like/sound that hasn't been lazily updated
                PlayableProperty.CREATOR_NAME.bind(user != null && user.getUsername() != null ? user.getUsername()
                        : ScTextUtils.EMPTY_STRING)
        );
    }
}
