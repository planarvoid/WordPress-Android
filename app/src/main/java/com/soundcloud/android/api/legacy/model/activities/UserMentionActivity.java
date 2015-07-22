package com.soundcloud.android.api.legacy.model.activities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

public class UserMentionActivity extends Activity {
    @JsonProperty public PublicApiComment comment;

    // for deserialization
    public UserMentionActivity() {
        super();
    }

    public UserMentionActivity(Cursor cursor) {
        super(cursor);
        comment = new PublicApiComment(cursor, true);
        comment.track = SoundCloudApplication.sModelManager.getTrack(comment.track_id);
        comment.user = SoundCloudApplication.sModelManager.getUser(comment.user_id);
    }

    @Override
    public Type getType() {
        return Type.USER_MENTION;
    }

    @Override
    public Playable getPlayable() {
        return comment.track;
    }

    @Override
    public PublicApiUser getUser() {
        return comment.user;
    }

    @Override
    public void cacheDependencies() {
        comment.user = SoundCloudApplication.sModelManager.cache(comment.user, PublicApiResource.CacheUpdateMode.MINI);
        comment.track = SoundCloudApplication.sModelManager.cache(comment.track, PublicApiResource.CacheUpdateMode.MINI);
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(TableColumns.Activities.COMMENT_ID, comment.getId());
        return cv;
    }

    @Override
    public List<PublicApiResource> getDependentModels() {
        List<PublicApiResource> models = super.getDependentModels();
        models.add(comment);
        return models;
    }

    @Override
    public Refreshable getRefreshableResource() {
        return comment.user;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public PropertySet toPropertySet() {
        return super.toPropertySet()
                .put(ActivityProperty.TYPE, ActivityProperty.TYPE_USER_MENTION)
                .put(ActivityProperty.SOUND_TITLE, comment.track.getTitle());
    }
}
