package com.soundcloud.android.model;

import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;

public class Shortcut extends ScModel {

    public String kind;         // following | like | group
    public String permalink_url;
    public String username;     // username
    public String title;        // sound
    public String name;         // group
    public String avatar_url;
    public String artwork_url;

    @Override
    public @Nullable ContentValues buildContentValues() {
        if (getDataUri() == null) return null;

        ContentValues cv = new ContentValues();

        cv.put(DBHelper.Suggestions.ID,   id);
        cv.put(DBHelper.Suggestions.KIND, kind);

        if (!TextUtils.isEmpty(avatar_url)) {
            cv.put(DBHelper.Suggestions.ICON_URL, avatar_url);
        } else if (!TextUtils.isEmpty(artwork_url)) {
            cv.put(DBHelper.Suggestions.ICON_URL, artwork_url);
        }

        if (!TextUtils.isEmpty(getText())) {
            cv.put(DBHelper.Suggestions.COLUMN_TEXT1, getText());
            cv.put(DBHelper.Suggestions.TEXT, getText());
        }

        if (getDataUri() != null) {
            cv.put(DBHelper.Suggestions.INTENT_DATA, getDataUri().toString());
        }

        return cv;
    }

    public @Nullable Uri getDataUri() {
        if ("following".equals(kind)) {
            return ClientUri.forUser(id);
        } else if ("like".equals(kind)) {
            return ClientUri.forTrack(id);
        } else {
            return null;
        }
    }

    public @NotNull String getText() {
        if (!TextUtils.isEmpty(username)) {
            return username;
        } else if (!TextUtils.isEmpty(title)) {
            return title;
        } else if (!TextUtils.isEmpty(name)) {
            return name;
        } else {
            return "";
        }
    }
}
