package com.soundcloud.android.activities;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.api.legacy.model.activities.AffiliationActivity;
import com.soundcloud.android.api.legacy.model.activities.CommentActivity;
import com.soundcloud.android.api.legacy.model.activities.PlaylistActivity;
import com.soundcloud.android.api.legacy.model.activities.PlaylistLikeActivity;
import com.soundcloud.android.api.legacy.model.activities.PlaylistRepostActivity;
import com.soundcloud.android.api.legacy.model.activities.PlaylistSharingActivity;
import com.soundcloud.android.api.legacy.model.activities.TrackActivity;
import com.soundcloud.android.api.legacy.model.activities.TrackLikeActivity;
import com.soundcloud.android.api.legacy.model.activities.TrackRepostActivity;
import com.soundcloud.android.api.legacy.model.activities.TrackSharingActivity;
import com.soundcloud.android.api.legacy.model.activities.UserMentionActivity;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class ActivitiesAdapterTest {
    @Mock private ImageOperations imageOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private ActivityItemPresenter activityItemPresenter;
    @Mock private EventBus eventBus;

    @InjectMocks
    private ActivitiesAdapter adapter;

    private ViewGroup parent;

    @Before
    public void setUp() throws Exception {
        parent = new FrameLayout(Robolectric.application);
    }

    @Test
    public void shouldReportSpecificTypeForTrackActivity() {
        adapter.addItems(Arrays.<Activity>asList(new TrackActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.TRACK.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForTrackSharingActivity() {
        adapter.addItems(Arrays.<Activity>asList(new TrackSharingActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.TRACK_SHARING.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForTrackRepostActivity() {
        adapter.addItems(Arrays.<Activity>asList(new TrackRepostActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.TRACK_REPOST.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForPlaylistRepostActivity() {
        adapter.addItems(Arrays.<Activity>asList(new PlaylistRepostActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.PLAYLIST_REPOST.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForPlaylistActivity() throws Exception {
        adapter.addItems(Arrays.<Activity>asList(new PlaylistActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.PLAYLIST.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForPlaylistSharingActivity() throws Exception {
        adapter.addItems(Arrays.<Activity>asList(new PlaylistSharingActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.PLAYLIST_SHARING.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForCommentActivity() {
        adapter.addItems(Arrays.<Activity>asList(new CommentActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.COMMENT.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForUserMentionActivity() {
        adapter.addItems(Arrays.<Activity>asList(new UserMentionActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.USER_MENTION.ordinal());
    }

    @Test
    public void shouldReportSpecificTypeForTrackLikeActivity() {
        adapter.addItems(Arrays.<Activity>asList(new TrackLikeActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.TRACK_LIKE.ordinal());
    }

    @Test
    public void shouldReportPlaylistTypeForTrackLikeActivity() {
        adapter.addItems(Arrays.<Activity>asList(new PlaylistLikeActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.PLAYLIST_LIKE.ordinal());
    }

    @Test
    public void shouldReportPlaylistTypeForAffiliationActivity() {
        adapter.addItems(Arrays.<Activity>asList(new AffiliationActivity()));
        expect(adapter.getItemViewType(0)).toEqual(Activity.Type.AFFILIATION.ordinal());
    }

    // -- Here we test all that all the activities possible relies on the activities presenter
    // which is not so useful.
    //
    // I keep this tests because we may need to introduce new presenters and we
    // would have to test this logic.
    //
    // Keep this test until the design is done.

    @Test
    public void shouldCallPresenterToCreateItemViewForTrackActivity() throws Exception {
        // We can't use ModelCitizen unless we add a setter to TrackActivity.
        adapter.addItems(Arrays.<Activity>asList(createTrackActivity(TrackActivity.class)));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    @Test
    public void shouldCallPresenterToCreateItemViewForTrackSharingActivity() throws Exception {
        adapter.addItems(Arrays.<Activity>asList(createTrackActivity(TrackSharingActivity.class)));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    @Test
    public void shouldCallPresenterToCreateItemViewForTrackRepostActivity() throws Exception {
        adapter.addItems(Arrays.<Activity>asList(createTrackActivity(TrackRepostActivity.class)));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    @Test
    public void shouldCallPresenterToCreateItemViewForPlaylistRepostActivity() throws Exception {
        adapter.addItems(Arrays.<Activity>asList(createPlaylistActivity(PlaylistRepostActivity.class)));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    @Test
    public void shouldCallPresenterToCreateItemViewForCommentActivity() throws Exception {
        final CommentActivity commentActivity = new CommentActivity();
        commentActivity.setCreatedAt(new Date());
        commentActivity.comment = ModelFixtures.create(PublicApiComment.class);
        adapter.addItems(Arrays.<Activity>asList(commentActivity));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    @Test
    public void shouldCallPresenterToCreateItemViewForTrackLikeActivity() throws Exception {
        adapter.addItems(Arrays.<Activity>asList(createTrackActivity(TrackLikeActivity.class)));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    @Test
    public void shouldCallPresenterToCreateItemViewForPlaylistLikeActivity() throws Exception {
        adapter.addItems(Arrays.<Activity>asList(createPlaylistActivity(PlaylistLikeActivity.class)));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    @Test
    public void shouldCallPresenterToCreateItemViewForAffiliationActivity() throws Exception {
        final AffiliationActivity activity = new AffiliationActivity();
        activity.setCreatedAt(new Date());
        activity.setUser(ModelFixtures.create(PublicApiUser.class));
        adapter.addItems(Arrays.<Activity>asList(activity));
        adapter.getView(0, null, parent);

        verify(activityItemPresenter).createItemView(anyInt(), eq(parent));
    }

    // We can't use ModelCitizen unless we add a setter to Track.
    private <T extends TrackActivity> T createTrackActivity(Class<T> klazz) throws IllegalAccessException, InstantiationException, CreateModelException {
        T instance = klazz.newInstance();
        instance.track = createModel(PublicApiTrack.class);
        instance.setCreatedAt(new Date());
        if (instance instanceof TrackRepostActivity) {
            ((TrackRepostActivity) instance).user = createModel(PublicApiUser.class);
        }
        if (instance instanceof TrackLikeActivity) {
            ((TrackLikeActivity) instance).user = createModel(PublicApiUser.class);
        }
        return instance;
    }

    // We can't use ModelCitizen unless we add a setter to Playlist.
    private <T extends PlaylistActivity> T createPlaylistActivity(Class<T> klazz) throws IllegalAccessException, InstantiationException, CreateModelException {
        T instance = klazz.newInstance();
        instance.playlist = createModel(PublicApiPlaylist.class);
        instance.setCreatedAt(new Date());
        if (instance instanceof PlaylistRepostActivity) {
            ((PlaylistRepostActivity) instance).user = createModel(PublicApiUser.class);
        }
        if (instance instanceof PlaylistLikeActivity) {
            ((PlaylistLikeActivity) instance).user = createModel(PublicApiUser.class);
        }
        return instance;
    }

    private <T> T createModel(Class<T> clazz) throws CreateModelException {
        return ModelFixtures.create(clazz);
    }
}