package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.model.Urn.forPlaylist;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiPlaylistLike;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackLike;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Likes;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class RemoveLikesCommandTest extends StorageIntegrationTest {

    private RemoveLikesCommand command;

    @Before
    public void setup() {
        command = new RemoveLikesCommand(propeller(), Tables.Sounds.TYPE_TRACK);
    }

    @Test
    public void shouldRemoveLikes() throws PropellerWriteException {
        final ApiLike trackLike = testFixtures().insertTrackLike();
        final ApiLike trackLike2 = testFixtures().insertTrackLike();

        command.call(asList(trackLike, trackLike2));

        assertThat(select(from(Likes.TABLE))).isEmpty();
    }

    @Test
    public void shouldRemoveJustTrackLikeWhenIdsAreTheSame() throws PropellerWriteException {
        ApiTrack apiTrack = TrackFixtures.apiTrack();
        ApiPlaylist apiPlaylist = create(ApiPlaylist.class);
        apiPlaylist.setUrn(forPlaylist(apiTrack.getId()));

        final ApiLike trackLike = apiTrackLike(apiTrack);
        testFixtures().insertLike(trackLike);

        final ApiLike playlistLike = apiPlaylistLike(apiPlaylist);
        testFixtures().insertLike(playlistLike);

        command.call(singletonList(trackLike));

        assertThat(select(from(Likes.TABLE))).counts(1);
    }

    @Test
    public void shouldRemoveLotsOfLikes() throws Exception {

        List<ApiTrack> tracks = TrackFixtures.apiTracks(2000);
        for (ApiTrack track : tracks) {
            final ApiLike trackLike = apiTrackLike(track);
            testFixtures().insertLike(trackLike);
        }

        assertThat(select(from(Likes.TABLE))).counts(2000);

        WriteResult call = command.call(Lists.transform(tracks, input -> ApiLike.create(input.getUrn(), new Date())));

        Assertions.assertThat(call.success()).isTrue();
        assertThat(select(from(Likes.TABLE))).counts(0);

    }
}
