package com.soundcloud.android.likes;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.likes.LoadLikedTracksCommand.Params;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class LoadLikedTracksCommandTest extends StorageIntegrationTest {

    private static final Date LIKED_AT_1 = new Date(1000);
    private static final Date LIKED_AT_2 = new Date(2000);
    private ApiTrack apiTrack1;
    private ApiTrack apiTrack2;

    private LoadLikedTracksCommand command;

    @Before
    public void setUp() throws Exception {
        command = new LoadLikedTracksCommand(propeller());
        apiTrack1 = testFixtures().insertLikedTrack(LIKED_AT_1);
        apiTrack2 = testFixtures().insertLikedTrack(LIKED_AT_2);
    }

    @Test
    public void loadsLikedTracks() {
        List<Like> likes = command.call(Optional.absent());

        assertThat(likes).containsExactly(
                Like.create(apiTrack2.getUrn(), LIKED_AT_2),
                Like.create(apiTrack1.getUrn(), LIKED_AT_1)
        );
    }

    @Test
    public void loadsLikedTracksWithLimit() {
        List<Like> likes = command.call(Optional.of(Params.from(3000, 1)));

        assertThat(likes).containsExactly(
                Like.create(apiTrack2.getUrn(), LIKED_AT_2)
        );
    }

    @Test
    public void loadsLikedTracksWithBeforeDate() {
        List<Like> likes = command.call(Optional.of(Params.from(2000, 1)));

        assertThat(likes).containsExactly(
                Like.create(apiTrack1.getUrn(), LIKED_AT_1)
        );
    }
}
