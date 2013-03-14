package com.soundcloud.android.dao;

import android.content.ContentValues;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.act.AffiliationActivity;
import com.soundcloud.android.model.act.CommentActivity;
import com.soundcloud.android.model.act.TrackActivity;
import com.soundcloud.android.model.act.TrackLikeActivity;
import com.soundcloud.android.model.act.TrackSharingActivity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncServiceTest;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

import static com.soundcloud.android.Expect.expect;

public class ActivitiesDAOTest extends BaseDAOTest<ActivityDAO> {

    public ActivitiesDAOTest() {
        super(new ActivityDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void shouldPersistActivitiesInDb() throws Exception {
        Activities stream = TestHelper.readJson(Activities.class, SyncAdapterServiceTest.class, "e1_stream_1.json");
        for (Activity a : stream) {
            getDAO().create(a);
        }
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);
    }

    @Test
    public void shouldPersistAllActivityTypes() throws Exception {
        // need to create track owner for joins to work
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, USER_ID);
        cv.put(DBHelper.Users.USERNAME, "Foo Bar");
        expect(resolver.insert(Content.USERS.uri, cv)).not.toBeNull();

        Activities one_of_each = TestHelper.readJson(Activities.class, ApiSyncServiceTest.class,
                "e1_one_of_each_activity.json");

        expect(ActivityDAO.insert(Content.ME_SOUND_STREAM, resolver, one_of_each)).toBe(7);

        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(7);

        Activities activities = getDAO().queryAll();
        expect(activities.size()).toEqual(7);

        TrackActivity trackActivity = (TrackActivity) activities.get(0);
        expect(trackActivity.getDateString()).toEqual("2012/09/25 19:09:40 +0000");
        expect(trackActivity.uuid).toEqual("8e3bf200-0744-11e2-9817-590114067ab0");
        expect(trackActivity.tags).toEqual("affiliated");
        expect(trackActivity.getType()).toEqual(Activity.Type.TRACK);
        expect(trackActivity.getUser().id).toEqual(1948213l);
        expect(trackActivity.getUser().username).toEqual("Playback Media");
        expect(trackActivity.getPlayable().id).toEqual(61145768l);
        expect(trackActivity.getPlayable().title).toEqual("Total Waxer");
        expect(trackActivity.getPlayable().genre).toEqual("Podcast");
        expect(trackActivity.sharing_note.text).toEqual("this is a sharing note");
        expect(trackActivity.sharing_note.getDateString()).toEqual("2012/09/25 19:09:40 +0000");

        TrackSharingActivity trackSharingActivity = (TrackSharingActivity) activities.get(1);
        expect(trackSharingActivity.getDateString()).toEqual("2012/09/25 17:40:17 +0000");
        expect(trackSharingActivity.uuid).toEqual("11a31680-0738-11e2-8cce-6fced32aa777");
        expect(trackSharingActivity.tags).toEqual("affiliated");
        expect(trackSharingActivity.getType()).toEqual(Activity.Type.TRACK_SHARING);
        expect(trackSharingActivity.getUser().id).toEqual(5833426l);
        expect(trackSharingActivity.getUser().username).toEqual("Stop Out Records");
        expect(trackSharingActivity.getPlayable().id).toEqual(61132541l);
        expect(trackSharingActivity.getPlayable().title).toEqual("Wendyhouse - Hold Me Down (Feat. FRANKi)");
        expect(trackSharingActivity.getPlayable().artwork_url).toEqual("https://i1.sndcdn.com/artworks-000030981203-eerjjh-large.jpg?04ad178");
        expect(trackSharingActivity.sharing_note.text).toEqual("this is a sharing note");

        AffiliationActivity affiliationActivity = (AffiliationActivity) activities.get(2);
        expect(affiliationActivity.getDateString()).toEqual("2012/09/24 22:43:20 +0000");
        expect(affiliationActivity.uuid).toEqual("3d22f400-0699-11e2-919a-b494be7979e7");
        expect(affiliationActivity.tags).toEqual("own");
        expect(affiliationActivity.getType()).toEqual(Activity.Type.AFFILIATION);
        expect(affiliationActivity.getUser().id).toEqual(2746040l);
        expect(affiliationActivity.getUser().username).toEqual("Vicious Lobo");

        TrackLikeActivity trackLikeActivity = (TrackLikeActivity) activities.get(3);
        expect(trackLikeActivity.getDateString()).toEqual("2012/07/10 17:34:07 +0000");
        expect(trackLikeActivity.uuid).toEqual("734ad180-cab5-11e1-9570-52fa262dac01");
        expect(trackLikeActivity.tags).toEqual("own");
        expect(trackLikeActivity.getType()).toEqual(Activity.Type.TRACK_LIKE);
        expect(trackLikeActivity.getUser().permalink).toEqual("designatedave");
        expect(trackLikeActivity.getUser().username).toEqual("D∃SIGNATED∀VΞ");
        expect(trackLikeActivity.getPlayable().tag_list).toEqual("foursquare:venue=4d8990f6eb6d60fc6c8818ca geo:lat=52.50126117 geo:lon=13.34753747 soundcloud:source=android-record");
        expect(trackLikeActivity.getPlayable().label_name).toBeNull();
        expect(trackLikeActivity.getPlayable().license).toEqual("all-rights-reserved");
        expect(trackLikeActivity.getPlayable().permalink).toEqual("android-to-the-big-screen");
        expect(trackLikeActivity.getPlayable().getUser().id).toEqual(5687414l);
        expect(trackLikeActivity.getPlayable().getUser().permalink).toEqual("soundcloud-android");

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

    // TODO test doesn't belong here
    @Test
    public void shouldRemoveTrackActivitiesOnTrackRemove() throws Exception {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, USER_ID);
        cv.put(DBHelper.Users.USERNAME, "Foo Bar");
        expect(resolver.insert(Content.USERS.uri, cv)).not.toBeNull();

        Activities one_of_each = TestHelper.readJson(Activities.class, ApiSyncServiceTest.class,
                "e1_one_of_each_activity.json");

        expect(ActivityDAO.insert(Content.ME_SOUND_STREAM, resolver, one_of_each)).toBe(7);

        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(7);

        ScContentProvider.TrackUnavailableListener.removeTrack(Robolectric.application).execute( 39859648l);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(6);

        ScContentProvider.TrackUnavailableListener.removeTrack(Robolectric.application).execute( 61132541l);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(5);

        ScContentProvider.TrackUnavailableListener.removeTrack(Robolectric.application).execute( 39722328l);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(4);
    }
}
