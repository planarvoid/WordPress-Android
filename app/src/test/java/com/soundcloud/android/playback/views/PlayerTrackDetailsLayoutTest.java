package com.soundcloud.android.playback.views;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class PlayerTrackDetailsLayoutTest {

    PlayerTrackDetailsLayout detailsLayout;

    @Before
    public void setUp() throws Exception {
        detailsLayout = new PlayerTrackDetailsLayout(Robolectric.application.getApplicationContext());
    }

    @Test
    public void showLikesRowWhenTrackHasLikes() throws Exception {
        Track track = new Track(1L);
        track.likes_count = 1;
        detailsLayout.setTrack(track);

        View likersRow = detailsLayout.findViewById(R.id.likers_row);
        expect(likersRow.getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void hideLikesRowWhenTrackHasNoLikes() throws Exception {
        Track track = new Track(1L);
        track.likes_count = 0;
        detailsLayout.setTrack(track);

        View likersRow = detailsLayout.findViewById(R.id.likers_row);
        expect(likersRow.getVisibility()).toBe(View.GONE);
    }

    @Test
    public void showRepostsRowWhenTrackHasReposts() throws Exception {
        Track track = new Track(1L);
        track.reposts_count = 1;
        detailsLayout.setTrack(track);

        View repostersRow = detailsLayout.findViewById(R.id.reposters_row);
        expect(repostersRow.getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void hideRepostsRowWhenTrackHasNoReposts() throws Exception {
        Track track = new Track(1L);
        track.reposts_count = 0;
        detailsLayout.setTrack(track);

        View repostersRow = detailsLayout.findViewById(R.id.reposters_row);
        expect(repostersRow.getVisibility()).toBe(View.GONE);
    }

    @Test
    public void showCommentRowWhenTrackHasComments() throws Exception {
        Track track = new Track(1L);
        track.comments = Lists.newArrayList(new Comment());
        track.comment_count = 1;
        detailsLayout.setTrack(track);

        View commentRow = detailsLayout.findViewById(R.id.comments_row);
        expect(commentRow.getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void hideCommentRowWhenTrackHasNoComments() throws Exception {
        Track track = new Track(1L);
        track.comments = Lists.newArrayList();
        track.comment_count = 0;
        detailsLayout.setTrack(track);

        View commentRow = detailsLayout.findViewById(R.id.comments_row);
        expect(commentRow.getVisibility()).toBe(View.GONE);
    }
}
