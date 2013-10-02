package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

import java.io.Serializable;

@RunWith(SoundCloudTestRunner.class)
public class PlaySourceInfoTest {

    @Test
    public void shouldBeEqual() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo.Builder(1234L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        final PlaySourceInfo expected = new PlaySourceInfo.Builder(1234L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldNotBeEqual() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo.Builder(123L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(actual).not.toEqual(new PlaySourceInfo.Builder(1234L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build());
        expect(actual).not.toEqual(new PlaySourceInfo.Builder(123L).originUrl("origin-url2").exploreTag("explore-tag").recommenderVersion("version-1").build());
        expect(actual).not.toEqual(new PlaySourceInfo.Builder(123L).originUrl("origin-url").exploreTag("explore-tag2").recommenderVersion("version-1").build());
        expect(actual).not.toEqual(new PlaySourceInfo.Builder(123L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-2").build());
    }

    @Test
    public void shouldBeSerializeable() throws Exception {
        final Serializable original = new PlaySourceInfo.Builder(123L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        Serializable copy = (Serializable) SerializationUtils.clone(original);
        expect(original).toEqual(copy);
    }

    @Test
    public void shouldCreateParams() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder(123L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(playInfo.toQueryParams()).toEqual("playSource-originUrl=origin-url&playSource-exploreTag=explore-tag&playSource-recommenderVersion=version-1&playSource-initialTrackId=123");
    }

    @Test
    public void shouldPersistFromParams() throws Exception {
        Uri uri = Uri.parse("http://something.com");
        final PlaySourceInfo expected = new PlaySourceInfo.Builder(123L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(PlaySourceInfo.fromUriParams(expected.appendAsQueryParams(uri.buildUpon()).build())).toEqual(expected);
    }

    @Test
    public void shouldSetRecommenderVersion() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder(123L).originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        playInfo.setRecommenderVersion("version-2");
        expect(playInfo.toQueryParams()).toEqual("playSource-originUrl=origin-url&playSource-exploreTag=explore-tag&playSource-recommenderVersion=version-2&playSource-initialTrackId=123");

    }
}
