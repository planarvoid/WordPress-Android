package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlaySourceInfoTest {

//    @Test
//    public void shouldBeEqual() throws Exception {
//        final PlaySourceInfo actual = new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-1").build();
//        final PlaySourceInfo expected = new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-1").build();
//        expect(actual).toEqual(expected);
//    }
//
//    @Test
//    public void shouldNotBeEqual() throws Exception {
//        final PlaySourceInfo actual = new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-1").build();
//        expect(actual).not.toEqual(new PlaySourceInfo.Builder().originUrl("origin-url2").exploreVersion("explore-tag").recommenderVersion("version-1").build());
//        expect(actual).not.toEqual(new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag2").recommenderVersion("version-1").build());
//        expect(actual).not.toEqual(new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-2").build());
//    }
//
//    @Test
//    public void shouldBeParcelable() throws Exception {
//        final PlaySourceInfo original = new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-1").build();
//        Parcel parcel = Parcel.obtain();
//        original.writeToParcel(parcel, 0);
//        expect(original).toEqual(new PlaySourceInfo(parcel));
//    }
//
//    @Test
//    public void shouldCreateParams() throws Exception {
//        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-1").initialTrackId(123L).build();
//        expect(toQueryParams(playInfo)).toEqual("playSource-recommenderVersion=version-1&playSource-exploreVersion=explore-tag&playSource-originUrl=origin-url&playSource-initialTrackId=123");
//    }
//
//    @Test
//    public void shouldBeAbleToRecreateFromUriParams() throws Exception {
//        Uri uri = Uri.parse("http://something.com");
//        final PlaySourceInfo expected = new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-1").build();
//        expect(PlaySourceInfo.fromUriParams(expected.appendAsQueryParams(uri.buildUpon()).build())).toEqual(expected);
//    }
//
//    @Test
//    public void shouldSetRecommenderVersion() throws Exception {
//        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().originUrl("origin-url").exploreVersion("explore-tag").recommenderVersion("version-1").build();
//        playInfo.setRecommenderVersion("version-2");
//        expect(toQueryParams(playInfo)).toEqual("playSource-recommenderVersion=version-2&playSource-exploreVersion=explore-tag&playSource-originUrl=origin-url");
//    }
//
//    @Test
//    public void shouldCreateExploreVersionTrackInfoWhenExploreVersionExistsAndIsInitialTrackId() throws Exception {
//        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().initialTrackId(123L).exploreVersion("asdf").originUrl("originUrl").build();
//        expect(playInfo.getTrackSource(123L)).toEqual(TrackSourceInfo.fromExplore("asdf"));
//    }
//
//    @Test
//    public void shouldCreateRecommenderTrackSourceInfoWhenRecommenderVersionExistsAndNotInitialTrackId() throws Exception {
//        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().initialTrackId(123L).recommenderVersion("asdf").originUrl("originUrl").build();
//        expect(playInfo.getTrackSource(321L)).toEqual(TrackSourceInfo.fromRecommender("asdf", "originUrl"));
//    }
//
//    @Test
//    public void shouldBeBlankTrackSourceInfoWhenFirstTrackAndExploreVersionDoesNotExist() throws Exception {
//        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().initialTrackId(123L).build();
//        expect(playInfo.getTrackSource(123L)).toEqual(new TrackSourceInfo());
//    }
//
//    @Test
//    public void shouldBeBlankTrackSourceInfoWhenNotFirstTrackAndRecommenderVersionDoesNotExist() throws Exception {
//        final PlaySourceInfo playInfo = new PlaySourceInfo.Builder().initialTrackId(123L).build();
//        expect(playInfo.getTrackSource(321L)).toEqual(new TrackSourceInfo());
//    }
//
//    private String toQueryParams(PlaySourceInfo playSourceInfo) {
//        return playSourceInfo.appendAsQueryParams(new Uri.Builder()).build().getQuery().toString();
//    }
}
