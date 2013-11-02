package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;
import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PlaySourceInfoTest {

    @Test
    public void shouldBeEqual() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        final PlaySourceInfo expected = new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldNotBeEqual() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(actual).not.toEqual(new PlaySourceInfo.Builder().originUrl("origin-url2").exploreTag("explore-tag").recommenderVersion("version-1").build());
        expect(actual).not.toEqual(new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag2").recommenderVersion("version-1").build());
        expect(actual).not.toEqual(new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-2").build());
    }

    @Test
    public void shouldBeParcelable() throws Exception {
        final PlaySourceInfo original = new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        expect(original).toEqual(new PlaySourceInfo(parcel));
    }

    @Test
    public void shouldCreateParams() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(toQueryParams(playInfo)).toEqual("playSource-recommenderVersion=version-1&playSource-exploreTag=explore-tag&playSource-originUrl=origin-url");
    }

    @Test
    public void shouldBeAbleToRecreateFromUriParams() throws Exception {
        Uri uri = Uri.parse("http://something.com");
        final PlaySourceInfo expected = new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        expect(PlaySourceInfo.fromUriParams(expected.appendAsQueryParams(uri.buildUpon()).build())).toEqual(expected);
    }

    @Test
    public void shouldSetRecommenderVersion() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().originUrl("origin-url").exploreTag("explore-tag").recommenderVersion("version-1").build();
        playInfo.setRecommenderVersion("version-2");
        expect(toQueryParams(playInfo)).toEqual("playSource-recommenderVersion=version-2&playSource-exploreTag=explore-tag&playSource-originUrl=origin-url");
    }

    @Test
    public void shouldCreateAutoTrackSourceInfo() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().build();
        expect(playInfo.getAutoTrackSource()).toEqual(TrackSourceInfo.auto());
    }

    @Test
    public void shouldCreateRecommenderTrackSourceInfo() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().recommenderVersion("version_a").build();
        expect(playInfo.getAutoTrackSource()).toEqual(TrackSourceInfo.fromRecommender("version_a"));
    }

    private String toQueryParams(PlaySourceInfo playSourceInfo) {
        return playSourceInfo.appendAsQueryParams(new Uri.Builder()).build().getQuery().toString();
    }
}
