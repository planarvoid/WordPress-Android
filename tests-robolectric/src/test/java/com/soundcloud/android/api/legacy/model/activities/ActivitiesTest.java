package com.soundcloud.android.api.legacy.model.activities;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.api.legacy.PublicApi.CloudDateFormat.fromString;
import static com.soundcloud.android.testsupport.TestHelper.getActivities;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.sync.ApiSyncServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@RunWith(DefaultTestRunner.class)
public class ActivitiesTest {

    public static final long USER_ID = 133201L;
    ScModelManager manager;
    ContentResolver resolver;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        // XXX Code in Activities refers to sModelManager, remove circular references
        manager = SoundCloudApplication.sModelManager;
        resolver = Robolectric.application.getContentResolver();
    }

    @Test
    public void testIsEmpty() throws Exception {
        Activities activities = new Activities();
        expect(activities.isEmpty()).toBeTrue();
        expect(activities.size()).toEqual(0);
    }

    @Test
    public void testIsEmptyParsed() throws Exception {
        Activities a = TestHelper.readJson(Activities.class, "/com/soundcloud/android/sync/activities_empty.json");
        expect(a.isEmpty()).toBeTrue();
    }

    @Test
    public void testFull() throws Exception {
        Activities activities = new Activities(new TrackRepostActivity(), new CommentActivity());
        expect(activities.isEmpty()).toEqual(false);
        expect(activities.size()).toEqual(2);
    }

    @Test
    public void testGetUniqueUsers() throws Exception {
        Activities activities = new Activities();
        expect(activities.getUniqueUsers().size()).toEqual(0);
        Activity e1 = new TrackActivity() { public PublicApiUser getUser() { return new PublicApiUser() { { setId(1); } }; } };
        Activity e2 = new TrackActivity() { public PublicApiUser getUser() { return new PublicApiUser() { { setId(1); } }; } };
        Activity e3 = new TrackActivity() { public PublicApiUser getUser() { return new PublicApiUser() { { setId(3); } }; } };
        activities = new Activities(e1, e2, e3);
        expect(activities.getUniqueUsers().size()).toEqual(2);
    }

    @Test
    public void testGetUniqueTracks() throws Exception {
        Activities activities = new Activities();
        expect(activities.getUniquePlayables().size()).toEqual(0);
        Activity e1 = new TrackActivity() { public PublicApiTrack getPlayable() { return new PublicApiTrack() { { setId(1); } }; } };
        Activity e2 = new TrackActivity() { public PublicApiTrack getPlayable() { return new PublicApiTrack() { { setId(1); } }; } };
        Activity e3 = new TrackActivity() { public PublicApiTrack getPlayable() { return new PublicApiTrack() { { setId(3); } }; } };
        activities = new Activities(e1, e2, e3);
        expect(activities.getUniquePlayables().size()).toEqual(2);
    }

    @Test
    public void testAllActivityTypesHaveUsers() throws Exception {
        Activities activities = TestHelper.readJson(Activities.class, ApiSyncServiceTest.class, "e1_one_of_each_activity.json");
        for (Activity a : activities){
            expect(a.getUser()).not.toBeNull();
        }
    }

    @Test
    public void testFromJSON() throws Exception {
        Activities a = getDefaultActivities();
        expect(a.size()).toEqual(17);
        expect(a.getUniquePlayables().size()).toEqual(5);
        expect(a.getUniqueUsers().size()).toEqual(12);
    }

    @Test
    public void testFavoritings() throws Exception {
        Activities trackLikes = getDefaultActivities().trackLikes();
        expect(trackLikes.size()).toEqual(4);
    }

    @Test
    public void testTracks() throws Exception {
        Activities tracks = getDefaultActivities().tracks();
        expect(tracks.size()).toEqual(4); // includes the 4 track likes
    }

    @Test
    public void testSharings() throws Exception {
        Activities sharings = getDefaultActivities().sharings();
        expect(sharings.size()).toEqual(0);
    }

    @Test
    public void testComments() throws Exception {
        Activities comments = getDefaultActivities().comments();
        expect(comments.size()).toEqual(5);
    }

    @Test
    public void testSelectReposts() throws Exception {
        Activities reposts = getActivitiesWithRepost().trackReposts();
        expect(reposts.size()).toEqual(1);
    }

    @Test
    public void testSelectNewFollowers() throws Exception {
        Activities followers = getActivitiesWithNewFollowers().followers();
        expect(followers.size()).toEqual(1);
    }

    @Test
    public void testGroupedByTrack() throws Exception {
        Map<Playable,Activities> grouped = getDefaultActivities().groupedByPlayable();
        expect(grouped.size()).toEqual(6);
        for (Map.Entry<Playable,Activities> entry : grouped.entrySet()) {
            expect(entry.getValue().isEmpty()).toEqual(false);
        }
    }

    @Test
    public void testGetCursor() throws Exception {
        Activities activities = new Activities();
        expect(activities.getCursor()).toBeNull();
        activities.next_href = "http://foo.com?cursor=dada";
        expect(activities.getCursor()).toEqual("dada");
    }

    private static Activities getDefaultActivities() throws IOException {
        return getActivities("/com/soundcloud/android/sync/e1_activities.json");
    }

    private static Activities getActivitiesWithRepost() throws IOException {
        return getActivities("/com/soundcloud/android/sync/e1_activities_with_repost.json");
    }

    private static Activities getActivitiesWithNewFollowers() throws IOException {
        return getActivities("/com/soundcloud/android/sync/e1_activities_with_affiliation.json");
    }

    @Test
    public void testMerge() throws Exception {
        Activities a1 = getActivities("/com/soundcloud/android/sync/e1_activities_1.json");
        Activities a2 = getActivities("/com/soundcloud/android/sync/e1_activities_2.json");
        Activities all = getActivities("/com/soundcloud/android/sync/e1_activities.json");

        Activities merged = a2.merge(a1);
        expect(merged.size()).toEqual(all.size());

        expect(merged.future_href).toEqual("https://api.soundcloud.com/e1/me/activities?uuid%5Bto%5D=3d22f400-0699-11e2-919a-b494be7979e7");
        expect(merged.next_href).toEqual("https://api.soundcloud.com/e1/me/activities?cursor=79fd0100-07e7-11e2-8aa5-5d4327b064fb");
        expect(merged.get(0).getCreatedAt().after(merged.get(merged.size() - 1).getCreatedAt())).toBeTrue();
    }

    @Test
    public void testMergeWithEmpty() throws Exception {
        Activities a1 = getActivities("/com/soundcloud/android/sync/e1_activities_1.json");
        expect(a1.merge(Activities.EMPTY)).toBe(a1);
    }

    @Test
    public void testFilter() throws Exception {
        Activities a2 = getActivities("/com/soundcloud/android/sync/e1_activities_2.json");
        Date start = fromString("2012/05/10 12:39:28 +0000");

        Activities filtered = a2.filter(start);
        expect(filtered.size()).toEqual(8);
        expect(filtered.get(0).getCreatedAt().after(start)).toBeTrue();
    }

    @Test
    public void testGetNextRequest() throws Exception {
        Activities a1 = getActivities("/com/soundcloud/android/sync/e1_activities_1.json");
        expect(a1.moreResourcesExist()).toBeTrue();
        expect(a1.getNextRequest().toUrl()).toEqual("/e1/me/activities?cursor=79fd0100-07e7-11e2-8aa5-5d4327b064fb");
    }

    @Test
    public void shouldFetchEmptyFromApi() throws Exception {
        TestHelper.addPendingHttpResponse(ApiSyncServiceTest.class,
                "activities_empty.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application.getCloudAPI(), Content.ME_SOUND_STREAM.request(), 100);
        expect(a.size()).toEqual(0);
        expect(a.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
        expect(a.isEmpty()).toBeTrue();
        expect(a.moreResourcesExist()).toBeFalse();
    }

    @Test
    public void shouldFetchFromApi() throws Exception {
        TestHelper.addPendingHttpResponse(ApiSyncServiceTest.class,
                "e1_stream_1.json",
                "e1_stream_2.json",
                "activities_empty.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application.getCloudAPI(), Content.ME_SOUND_STREAM.request(), 100);
        expect(a.size()).toEqual(50);
        expect(a.future_href).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");
        expect(a.moreResourcesExist()).toBeFalse();
    }

    @Test
    public void shouldStopFetchingAfterMax() throws Exception {
        TestHelper.addPendingHttpResponse(ApiSyncServiceTest.class,
                "e1_stream_1.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application.getCloudAPI(), Content.ME_SOUND_STREAM.request(), 20);
        expect(a.size()).toEqual(22); // 1 page
        expect(a.future_href).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");
        expect(a.moreResourcesExist()).toBeTrue();
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        Activities a = getActivities("/com/soundcloud/android/sync/e1_activities_1.json");
        ContentValues[] cv = a.buildContentValues(-1);
        expect(cv.length).toEqual(a.size());
    }



    @Test
    public void shouldGetArtworkUrls() throws Exception {
        Activities a = getActivities("/com/soundcloud/android/sync/e1_one_of_each_activity.json");

        Set<String> urls = a.artworkUrls();
        expect(urls.size()).toEqual(6);
        expect(urls).toContain(
            "https://i1.sndcdn.com/artworks-000031001595-r74u1y-large.jpg?04ad178",
            "https://i1.sndcdn.com/artworks-000019924877-kskpwr-large.jpg?04ad178",
            "https://i1.sndcdn.com/artworks-000030981203-eerjjh-large.jpg?04ad178",
            "https://i1.sndcdn.com/artworks-000019994056-v9g624-large.jpg?04ad178"
        );
    }

    @Test
    public void shouldReturnCorrectIcon() throws Exception {
        String artwork_1 = "artwork1.jpg";
        String artwork_2 = "artwork2.jpg";
        String avatar_1 = "avatar1.jpg";
        String avatar_2 = "avatar2.jpg";

        Activities a = new Activities();
        a.add(makeActivity(makeTrack(null,null)));
        a.add(makeActivity(makeTrack(null,null)));
        expect(a.getFirstAvailableArtwork()).toBeNull();
        expect(a.getFirstAvailableAvatar()).toBeNull();

        a = new Activities();
        a.add(makeActivity(makeTrack(null,artwork_1)));
        a.add(makeActivity(makeTrack(null,artwork_2)));
        expect(a.getFirstAvailableArtwork()).toEqual(artwork_1);
        expect(a.getFirstAvailableAvatar()).toBeNull();

        a = new Activities();
        a.add(makeActivity(makeTrack(makeUser(avatar_1),null)));
        a.add(makeActivity(makeTrack(null,artwork_2)));
        expect(a.getFirstAvailableArtwork()).toEqual(artwork_2);
        expect(a.getFirstAvailableAvatar()).toEqual(avatar_1);

        a = new Activities();
        a.add(makeActivity(makeTrack(null, null)));
        a.add(makeActivity(makeTrack(makeUser(avatar_2),null)));
        expect(a.getFirstAvailableArtwork()).toEqual(avatar_2);
        expect(a.getFirstAvailableAvatar()).toEqual(avatar_2);
    }

    @Test @Ignore
    public void shouldNotCreateNewUserObjectsIfObjectIdIsTheSame() throws Exception {
        Activities a = getActivities("/com/soundcloud/android/model/act/two_activities_by_same_user.json");
        // two comments by same user
        PublicApiUser u1 = a.get(0).getUser();
        PublicApiUser u2 = a.get(1).getUser();

        expect(u1).toBe(u2);
    }


    private Activity makeActivity(PublicApiTrack t){
        TrackActivity a = new TrackActivity();
        a.track = t;
        return a;
    }

    private PublicApiTrack makeTrack(@Nullable PublicApiUser u, @Nullable String artworkUrl){
        PublicApiTrack t = new PublicApiTrack();
        t.artwork_url = artworkUrl;
        t.user = u;
        return t;
    }

    private PublicApiUser makeUser(String avatarUrl){
        PublicApiUser u = new PublicApiUser();
        u.avatar_url = avatarUrl;
        return u;
    }

}
