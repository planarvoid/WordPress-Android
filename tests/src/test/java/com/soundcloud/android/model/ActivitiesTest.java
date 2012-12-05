package com.soundcloud.android.model;

import static com.soundcloud.android.AndroidCloudAPI.CloudDateFormat.fromString;
import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.act.AffiliationActivity;
import com.soundcloud.android.model.act.CommentActivity;
import com.soundcloud.android.model.act.TrackActivity;
import com.soundcloud.android.model.act.TrackLikeActivity;
import com.soundcloud.android.model.act.TrackRepostActivity;
import com.soundcloud.android.model.act.TrackSharingActivity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.sync.ApiSyncServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.util.DatabaseConfig;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;

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
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        // XXX Code in Activities refers to MODEL_MANAGER, remove circular references
        manager = SoundCloudApplication.MODEL_MANAGER;
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
        Activities a = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("activities_empty.json"));
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
        Activity e1 = new TrackActivity() { public User getUser() { return new User() { { id = 1; } }; } };
        Activity e2 = new TrackActivity() { public User getUser() { return new User() { { id = 1; } }; } };
        Activity e3 = new TrackActivity() { public User getUser() { return new User() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        expect(activities.getUniqueUsers().size()).toEqual(2);
    }

    @Test
    public void testGetUniqueTracks() throws Exception {
        Activities activities = new Activities();
        expect(activities.getUniqueTracks().size()).toEqual(0);
        Activity e1 = new TrackActivity() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Activity e2 = new TrackActivity() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Activity e3 = new TrackActivity() { public Track getTrack() { return new Track() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        expect(activities.getUniqueTracks().size()).toEqual(2);
    }

    @Test
    public void testFromJSON() throws Exception {
        Activities a = getActivities();
        expect(a.size()).toEqual(17);
        expect(a.getUniqueTracks().size()).toEqual(4);
        expect(a.getUniqueUsers().size()).toEqual(12);
    }

    @Test
    public void testFavoritings() throws Exception {
        Activities trackLikes = getActivities().trackLikes();
        expect(trackLikes.size()).toEqual(4);
    }

    @Test
    public void testTracks() throws Exception {
        Activities tracks = getActivities().tracks();
        expect(tracks.size()).toEqual(0);
    }

    @Test
    public void testSharings() throws Exception {
        Activities sharings = getActivities().sharings();
        expect(sharings.size()).toEqual(0);
    }

    @Test
    public void testComments() throws Exception {
        Activities comments = getActivities().comments();
        expect(comments.size()).toEqual(5);
    }

    @Test
    public void testSelectReposts() throws Exception {
        Activities reposts = getActivitiesWithRepost().trackReposts();
        expect(reposts.size()).toEqual(1);
    }

    @Test
    public void testGroupedByTrack() throws Exception {
        Map<Track,Activities> grouped = getActivities().groupedByTrack();
        expect(grouped.size()).toEqual(5);
        for (Map.Entry<Track,Activities> entry : grouped.entrySet()) {
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

    private Activities getActivities() throws IOException {
        return manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities.json"));
    }

    private Activities getActivitiesWithRepost() throws IOException {
        return manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities_with_repost.json"));
    }

    @Test
    public void testMerge() throws Exception {
        Activities a1 = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities_1.json"));
        Activities a2 = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities_2.json"));
        Activities all = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities.json"));

        Activities merged = a2.merge(a1);
        expect(merged.size()).toEqual(all.size());

        expect(merged.future_href).toEqual("https://api.soundcloud.com/e1/me/activities?uuid%5Bto%5D=3d22f400-0699-11e2-919a-b494be7979e7");
        expect(merged.next_href).toEqual("https://api.soundcloud.com/e1/me/activities?cursor=79fd0100-07e7-11e2-8aa5-5d4327b064fb");
        expect(merged.get(0).created_at.after(merged.get(merged.size() - 1).created_at)).toBeTrue();
    }

    @Test
    public void testMergeWithEmpty() throws Exception {
        Activities a1 = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities_1.json"));
        expect(a1.merge(Activities.EMPTY)).toBe(a1);
    }

    @Test
    public void testFilter() throws Exception {
        Activities a2 = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities_2.json"));
        Date start = fromString("2012/05/10 12:39:28 +0000");

        Activities filtered = a2.filter(start);
        expect(filtered.size()).toEqual(8);
        expect(filtered.get(0).created_at.after(start)).toBeTrue();
    }

    @Test
    public void testGetNextRequest() throws Exception {
        Activities a1 = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities_1.json"));
        expect(a1.hasMore()).toBeTrue();
        expect(a1.getNextRequest().toUrl()).toEqual("/e1/me/activities?cursor=79fd0100-07e7-11e2-8aa5-5d4327b064fb");
    }

    @Test
    public void shouldFetchEmptyFromApi() throws Exception {
        TestHelper.addCannedResponses(ApiSyncServiceTest.class,
                "activities_empty.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application, Content.ME_SOUND_STREAM.request(), 100);
        expect(a.size()).toEqual(0);
        expect(a.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
        expect(a.isEmpty()).toBeTrue();
        expect(a.hasMore()).toBeFalse();
    }

    @Test
    public void shouldFetchFromApi() throws Exception {
        TestHelper.addCannedResponses(ApiSyncServiceTest.class,
                "e1_stream_1.json",
                "e1_stream_2.json",
                "activities_empty.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application, Content.ME_SOUND_STREAM.request(), 100);
        expect(a.size()).toEqual(50);
        expect(a.future_href).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");
        expect(a.hasMore()).toBeFalse();
    }

    @Test
    public void shouldStopFetchingAfterMax() throws Exception {
        TestHelper.addCannedResponses(ApiSyncServiceTest.class,
                "e1_stream_1.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application, Content.ME_SOUND_STREAM.request(), 20);
        expect(a.size()).toEqual(22); // 1 page
        expect(a.future_href).toEqual("https://api.soundcloud.com/e1/me/stream?uuid%5Bto%5D=ee57b180-0959-11e2-8afd-9083bddf9fde");
        expect(a.hasMore()).toBeTrue();
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        Activities a = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_activities_1.json"));
        ContentValues[] cv = a.buildContentValues(-1);
        expect(cv.length).toEqual(a.size());
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfContentPassedToClearIsUnrelated() throws Exception {
        Activities.clear(Content.ME, resolver);
    }

    @Test
    public void shouldPersistAllActivityTypes() throws Exception {
        // need to insert track owner for joins to work
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, USER_ID);
        cv.put(DBHelper.Users.USERNAME, "Foo Bar");
        expect(resolver.insert(Content.USERS.uri, cv)).not.toBeNull();

        Activities a = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_one_of_each_activity.json"));
        expect(a.insert(Content.ME_ACTIVITIES, resolver)).toBe(7);

        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(7);

        Activities activities = Activities.getSince(Content.ME_ALL_ACTIVITIES, resolver, -1);
        expect(activities.size()).toEqual(7);

        TrackActivity trackActivity = (TrackActivity) activities.get(0);
        expect(trackActivity.getDateString()).toEqual("2012/09/25 19:09:40 +0000");
        expect(trackActivity.uuid).toEqual("8e3bf200-0744-11e2-9817-590114067ab0");
        expect(trackActivity.tags).toEqual("affiliated");
        expect(trackActivity.getType()).toEqual(Activity.Type.TRACK);
        expect(trackActivity.getUser().id).toEqual(1948213l);
        expect(trackActivity.getUser().username).toEqual("Playback Media");
        expect(trackActivity.getTrack().id).toEqual(61145768l);
        expect(trackActivity.getTrack().title).toEqual("Total Waxer");
        expect(trackActivity.getTrack().genre).toEqual("Podcast");
        expect(trackActivity.getTrack().waveform_url).toEqual("https://w1.sndcdn.com/DsoyShDam62m_m.png");
        expect(trackActivity.sharing_note.text).toEqual("this is a sharing note");
        expect(trackActivity.sharing_note.getDateString()).toEqual("2012/09/25 19:09:40 +0000");

        TrackSharingActivity trackSharingActivity = (TrackSharingActivity) activities.get(1);
        expect(trackSharingActivity.getDateString()).toEqual("2012/09/25 17:40:17 +0000");
        expect(trackSharingActivity.uuid).toEqual("11a31680-0738-11e2-8cce-6fced32aa777");
        expect(trackSharingActivity.tags).toEqual("affiliated");
        expect(trackSharingActivity.getType()).toEqual(Activity.Type.TRACK_SHARING);
        expect(trackSharingActivity.getUser().id).toEqual(5833426l);
        expect(trackSharingActivity.getUser().username).toEqual("Stop Out Records");
        expect(trackSharingActivity.getTrack().id).toEqual(61132541l);
        expect(trackSharingActivity.getTrack().title).toEqual("Wendyhouse - Hold Me Down (Feat. FRANKi)");
        expect(trackSharingActivity.getTrack().original_format).toEqual("mp3");
        expect(trackSharingActivity.getTrack().artwork_url).toEqual("https://i1.sndcdn.com/artworks-000030981203-eerjjh-large.jpg?04ad178");
        expect(trackSharingActivity.sharing_note.text).toEqual("this is a sharing note");

        AffiliationActivity affiliationActivity = (AffiliationActivity) activities.get(2);
        expect(affiliationActivity.getDateString()).toEqual("2012/09/24 22:43:20 +0000");
        expect(affiliationActivity.uuid).toEqual("3d22f400-0699-11e2-919a-b494be7979e7");
        expect(affiliationActivity.tags).toEqual("own");
        expect(affiliationActivity.getType()).toEqual(Activity.Type.AFFILIATION);
        expect(affiliationActivity.getUser().id).toEqual(2746040l);
        expect(affiliationActivity.getUser().username).toEqual("Vicious Lobo");
        expect(affiliationActivity.getUser().country).toEqual("United States");

        TrackLikeActivity trackLikeActivity = (TrackLikeActivity) activities.get(3);
        expect(trackLikeActivity.getDateString()).toEqual("2012/07/10 17:34:07 +0000");
        expect(trackLikeActivity.uuid).toEqual("734ad180-cab5-11e1-9570-52fa262dac01");
        expect(trackLikeActivity.tags).toEqual("own");
        expect(trackLikeActivity.getType()).toEqual(Activity.Type.TRACK_LIKE);
        expect(trackLikeActivity.getUser().permalink).toEqual("designatedave");
        expect(trackLikeActivity.getUser().username).toEqual("D∃SIGNATED∀VΞ");
        expect(trackLikeActivity.getTrack().tag_list).toEqual("foursquare:venue=4d8990f6eb6d60fc6c8818ca geo:lat=52.50126117 geo:lon=13.34753747 soundcloud:source=android-record");
        expect(trackLikeActivity.getTrack().label_name).toBeNull();
        expect(trackLikeActivity.getTrack().license).toEqual("all-rights-reserved");
        expect(trackLikeActivity.getTrack().permalink).toEqual("android-to-the-big-screen");
        expect(trackLikeActivity.getTrack().getUser().id).toEqual(5687414l);
        expect(trackLikeActivity.getTrack().getUser().permalink).toEqual("soundcloud-android");

        CommentActivity commentActivity = (CommentActivity) activities.get(4);
        expect(commentActivity.getDateString()).toEqual("2012/07/04 11:34:41 +0000");
        expect(commentActivity.uuid).toEqual("b035de80-6dc9-11e1-84dc-e1bbf59e9e64");
        expect(commentActivity.tags).toEqual("own, affiliated");
        expect(commentActivity.getType()).toEqual(Activity.Type.COMMENT);
        expect(commentActivity.comment.body).toEqual("Even more interesting");
        expect(commentActivity.comment.timestamp).toEqual(1136845l);
        expect(commentActivity.comment.user.id).toEqual(5696093l);
        expect(commentActivity.comment.user.username).toEqual("Liraz Axelrad");
        expect(commentActivity.comment.track_id).toEqual(39722328l);
        expect(commentActivity.comment.track.title).toEqual("Transaction and Services: Nfc by Hauke Meyn at droidcon");
    }

    @Test

    public void shouldRemoveTrackActivitiesOnTrackRemove() throws Exception {

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, USER_ID);
        cv.put(DBHelper.Users.USERNAME, "Foo Bar");
        expect(resolver.insert(Content.USERS.uri, cv)).not.toBeNull();

        Activities a = manager.getActivitiesFromJson(ApiSyncServiceTest.class.getResourceAsStream("e1_one_of_each_activity.json"));
        expect(a.insert(Content.ME_ACTIVITIES, resolver)).toBe(7);

        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(7);

        new ScContentProvider.TrackUnavailableListener().onReceive(
                Robolectric.application,new Intent("com.soundcloud.android.trackunavailable")
                .putExtra(CloudPlaybackService.BroadcastExtras.id, 39859648l));
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(6);

        new ScContentProvider.TrackUnavailableListener().onReceive(
                        Robolectric.application,new Intent("com.soundcloud.android.trackunavailable")
                        .putExtra(CloudPlaybackService.BroadcastExtras.id, 61132541l));
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(5);

        new ScContentProvider.TrackUnavailableListener().onReceive(
                                Robolectric.application,new Intent("com.soundcloud.android.trackunavailable")
                                .putExtra(CloudPlaybackService.BroadcastExtras.id, 39722328l));
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(4);

    }

    @Test
    public void shouldGetArtworkUrls() throws Exception {
        Activities a = manager.getActivitiesFromJson(
                ApiSyncServiceTest.class.getResourceAsStream("e1_one_of_each_activity.json"));

        Set<String> urls = a.artworkUrls();
        expect(urls.size()).toEqual(4);
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

    @Test
    @Ignore
    public void shouldNotCreateNewUserObjectsIfObjectIdIsTheSame() throws Exception {

        Activities a = manager.getActivitiesFromJson(
                ApiSyncServiceTest.class.getResourceAsStream("two_activities_by_same_user.json"));

        // fronx favorites + comments
        User u1 = a.get(0).getUser();
        User u2 = a.get(1).getUser();

        expect(u1).toBe(u2);
    }


    private Activity makeActivity(Track t){
        TrackActivity a = new TrackActivity();
        a.track = t;
        return a;
    }

    private Track makeTrack(@Nullable User u, @Nullable String artworkUrl){
        Track t = new Track();
        t.artwork_url = artworkUrl;
        t.user = u;
        return t;
    }

    private User makeUser(String avatarUrl){
        User u = new User();
        u.avatar_url = avatarUrl;
        return u;
    }

}
