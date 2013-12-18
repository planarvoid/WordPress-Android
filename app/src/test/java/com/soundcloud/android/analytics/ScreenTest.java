package com.soundcloud.android.analytics;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.Actions;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class ScreenTest {

    @Test
    public void shouldGetTrackingTag() {
        expect(Screen.EXPLORE_GENRES.get()).toEqual("explore:genres");
    }

    @Test
    public void shouldGetTrackingTagWithAppendedPath() {
        expect(Screen.EXPLORE_GENRES.get("path")).toEqual("explore:genres:path");
    }

    @Test
    public void gettingTagWithAppendedPathShouldNormalizePath() {
        expect(Screen.EXPLORE_GENRES.get("Hello & World")).toEqual("explore:genres:hello_&_world");
    }

    @Test
    public void setsAndGetsScreenFromIntent(){
        final Intent intent = new Intent();
        Screen.ACTIVITIES.addToIntent(intent);
        expect(Screen.fromIntent(intent)).toEqual(Screen.ACTIVITIES);
    }

    @Test
    public void setsAndGetsScreenFromBundle(){
        final Bundle bundle = new Bundle();
        Screen.ACTIVITIES.addToBundle(bundle);
        expect(Screen.fromBundle(bundle)).toEqual(Screen.ACTIVITIES);
    }

    @Test
    public void shouldReturnExploreIntent() throws Exception {
        Intent upIntent = Screen.getUpDestinationFromScreenTag("explore:music:electronic");
        expect(upIntent.getAction()).toEqual(Actions.EXPLORE);
    }

    @Test
    public void shouldReturnStreamIntent() throws Exception {
        Intent upIntent = Screen.getUpDestinationFromScreenTag("stream:main");
        expect(upIntent.getAction()).toEqual(Actions.STREAM);
    }

    @Test
    public void shouldReturnYourSoundsIntent() throws Exception {
        Intent upIntent = Screen.getUpDestinationFromScreenTag("you:posts");
        expect(upIntent.getAction()).toEqual(Actions.YOUR_SOUNDS);
    }

    @Test
    public void shouldReturnYourLikesIntent() throws Exception {
        Intent upIntent = Screen.getUpDestinationFromScreenTag("you:likes");
        expect(upIntent.getAction()).toEqual(Actions.YOUR_LIKES);
    }

    @Test
    public void shouldReturnScreenFromScreenTag() {
        expect(Screen.fromScreenTag("you:playlists")).toEqual(Screen.YOUR_PLAYLISTS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfScreenTagDoesNotExist() {
        Screen.fromScreenTag("you:something");
    }

    @Test(expected = Screen.NoUpDestinationException.class)
    public void shouldThrowIfScreenDoesNotHaveAssociatedUpAction() throws Exception {
        Screen.getUpDestinationFromScreenTag("tour:main");
    }

}
