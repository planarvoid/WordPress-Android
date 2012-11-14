package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.Expect;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ShortcutTest {
    private Shortcut following, like, group;

    @Before
    public void before() throws IOException {
        Shortcut[] shortcuts = new ObjectMapper().readValue(getClass().getResourceAsStream("shortcuts.json"), Shortcut[].class);
        expect(shortcuts.length).toEqual(3);
        following = shortcuts[0];
        like  = shortcuts[1];
        group = shortcuts[2];
    }

    @Test
    public void testFollowing() throws Exception {
        expect(following.getDataUri()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/2");
        expect(following.getText()).toEqual("Eric");
        expect(following.id).toEqual(2l);
        expect(following.kind).toEqual("following");
        expect(following.permalink_url).toEqual("http://soundcloud.com/eric");
        expect(following.avatar_url).toEqual("https://i1.sndcdn.com/avatars-000006111783-xqaxy3-tiny.jpg?2479809");
        expect(following.artwork_url).toBeNull();
    }

    @Test
    public void testLike() throws Exception {
        expect(like.getDataUri()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/tracks/64629168");
        expect(like.getText()).toEqual("Halls - Roses For The Dead (Max Cooper remix)");
        expect(like.id).toEqual(64629168l);
        expect(like.kind).toEqual("like");
        expect(like.permalink_url).toEqual("http://soundcloud.com/no-pain-in-pop/halls-roses-for-the-dead-max");
        expect(like.artwork_url).toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
        expect(like.avatar_url).toBeNull();
    }


    @Test
    public void testGroup() throws Exception {
        expect(group.getDataUri()).toBeNull();
        expect(group.getText()).toEqual("Field Recordings");
        expect(group.id).toEqual(8l);
        expect(group.kind).toEqual("group");
        expect(group.permalink_url).toEqual("http://soundcloud.com/groups/field-recordings");
        expect(group.artwork_url).toEqual("https://i1.sndcdn.com/artworks-000000481489-cw4cwt-tiny.jpg?2479809");
        expect(group.avatar_url).toBeNull();
    }

    @Test
    public void shouldBuildContentValuesFollowing() throws Exception {
        ContentValues cv = following.buildContentValues();

        expect(cv.getAsString(DBHelper.Suggestions.INTENT_DATA)).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/2");
        expect(cv.getAsString(DBHelper.Suggestions.COLUMN_TEXT1)).toEqual("Eric");
        expect(cv.getAsString(DBHelper.Suggestions.ICON_URL)).toEqual("https://i1.sndcdn.com/avatars-000006111783-xqaxy3-tiny.jpg?2479809");
        expect(cv.getAsString(DBHelper.Suggestions.PERMALINK_URL)).toEqual("http://soundcloud.com/eric");
        expect(cv.getAsString(DBHelper.Suggestions.TEXT)).toEqual("Eric");
        expect(cv.getAsString(DBHelper.Suggestions.KIND)).toEqual("following");
        expect(cv.getAsLong(DBHelper.Suggestions.ID)).toEqual(2l);
    }

    @Test
    public void shouldBuildContentValuesLike() throws Exception {
        ContentValues cv = like.buildContentValues();

        expect(cv.getAsString(DBHelper.Suggestions.INTENT_DATA)).toEqual("content://com.soundcloud.android.provider.ScContentProvider/tracks/64629168");
        expect(cv.getAsString(DBHelper.Suggestions.COLUMN_TEXT1)).toEqual("Halls - Roses For The Dead (Max Cooper remix)");
        expect(cv.getAsString(DBHelper.Suggestions.ICON_URL)).toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
        expect(cv.getAsString(DBHelper.Suggestions.TEXT)).toEqual("Halls - Roses For The Dead (Max Cooper remix)");
        expect(cv.getAsString(DBHelper.Suggestions.KIND)).toEqual("like");
        expect(cv.getAsLong(DBHelper.Suggestions.ID)).toEqual(64629168l);
    }
}
