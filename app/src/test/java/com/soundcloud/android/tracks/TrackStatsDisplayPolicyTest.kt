package com.soundcloud.android.tracks

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.accounts.AccountOperations
import com.soundcloud.android.events.LikesStatusEvent
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.PlaylistFixtures
import com.soundcloud.android.testsupport.TrackFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TrackStatsDisplayPolicyTest {

    @Mock lateinit var accountOperations: AccountOperations
    private lateinit var trackStatsDisplayPolicy: TrackStatsDisplayPolicy
    
    private val loggedInUserUrn = Urn.forUser(1)
    private val anotherUserUrn = Urn.forUser(2)

    @Before
    fun setUp() {
        whenever(accountOperations.loggedInUserUrn).thenReturn(loggedInUserUrn)
        trackStatsDisplayPolicy = TrackStatsDisplayPolicy(accountOperations)
    }

    @Test
    fun displayPlayCountReturnsTrueWhenPlayCountIsGreaterThanZero() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .playCount(1)
                                                        .build())

        val displayPlayCount = trackStatsDisplayPolicy.displayPlaysCount(trackItem)

        assertThat(displayPlayCount).isTrue()
    }

    @Test
    fun displayPlayCountReturnsFalseWhenPlayCountIsZero() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .playCount(0)
                                                        .build())

        val displayPlayCount = trackStatsDisplayPolicy.displayPlaysCount(trackItem)

        assertThat(displayPlayCount).isFalse()
    }

    @Test
    fun displayPlayCountReturnsFalseWhenDisplayStatsIsDisabled() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .playCount(1)
                                                        .creatorUrn(anotherUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayPlayCount = trackStatsDisplayPolicy.displayPlaysCount(trackItem)

        assertThat(displayPlayCount).isFalse()
    }

    @Test
    fun displayPlayCountReturnsTrueWhenDisplayStatsIsDisabledButTrackIsOwnedByCurrentLoggedUser() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .playCount(1)
                                                        .creatorUrn(loggedInUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayPlayCount = trackStatsDisplayPolicy.displayPlaysCount(trackItem)

        assertThat(displayPlayCount).isTrue()
    }

    @Test
    fun displayCommentsCountReturnsTrueWhenDisplayStatsIsDisabledButTrackIsOwnedByCurrentLoggedUser() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .commentsCount(1)
                                                        .commentable(true)
                                                        .creatorUrn(loggedInUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayCommentsCount = trackStatsDisplayPolicy.displayCommentsCount(trackItem)

        assertThat(displayCommentsCount).isTrue()
    }

    @Test
    fun displayCommentsCountReturnsFalseWhenDisplayStatsIsDisabled() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .commentsCount(1)
                                                        .commentable(true)
                                                        .creatorUrn(anotherUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayCommentsCount = trackStatsDisplayPolicy.displayCommentsCount(trackItem)

        assertThat(displayCommentsCount).isFalse()
    }

    @Test
    fun displayCommentsCountReturnsFalseWhenTrackIsNotCommentable() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .commentsCount(1)
                                                        .commentable(false)
                                                        .creatorUrn(anotherUserUrn)
                                                        .displayStatsEnabled(true)
                                                        .build())

        val displayCommentsCount = trackStatsDisplayPolicy.displayCommentsCount(trackItem)

        assertThat(displayCommentsCount).isFalse()
    }

    @Test
    fun displayLikesCountReturnsTrueWhenPlayableItemIsNotATrack() {
        val likeStatus = LikesStatusEvent.LikeStatus.create(Urn.NOT_SET, false, 10)
        val playableItem = PlaylistFixtures.playlistItem().updatedWithLike(likeStatus)

        val displayLikesCount = trackStatsDisplayPolicy.displayLikesCount(playableItem)

        assertThat(displayLikesCount).isTrue()
    }

    @Test
    fun displayLikesCountReturnsFalseWhenDisplayStatusIsDisabled() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .likesCount(10)
                                                        .creatorUrn(anotherUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayLikesCount = trackStatsDisplayPolicy.displayLikesCount(trackItem)

        assertThat(displayLikesCount).isFalse()
    }

    @Test
    fun displayLikesCountReturnsTrueWhenDisplayStatusIsDisabledButTrackIsOwnedByCurrentLoggedUser() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .likesCount(10)
                                                        .creatorUrn(loggedInUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayLikesCount = trackStatsDisplayPolicy.displayLikesCount(trackItem)

        assertThat(displayLikesCount).isTrue()
    }

    @Test
    fun displayRepostsCountReturnsFalseWhenDisplayStatusIsDisabled() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .repostsCount(10)
                                                        .creatorUrn(anotherUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayRepostsCount = trackStatsDisplayPolicy.displayRepostsCount(trackItem)

        assertThat(displayRepostsCount).isFalse()
    }

    @Test
    fun displayRepostsCountReturnsTrueWhenDisplayStatusIsDisabledButTrackIsOwnedByCurrentLoggedUser() {
        val trackItem = TrackFixtures.trackItem(TrackFixtures.trackBuilder()
                                                        .repostsCount(10)
                                                        .creatorUrn(loggedInUserUrn)
                                                        .displayStatsEnabled(false)
                                                        .build())

        val displayRepostsCount = trackStatsDisplayPolicy.displayRepostsCount(trackItem)

        assertThat(displayRepostsCount).isTrue()
    }

    @Test
    fun displayRepostsCountReturnsTrueWhenPlayableItemIsNotATrack() {
        val likeStatus = LikesStatusEvent.LikeStatus.create(Urn.NOT_SET, false, 10)
        val playableItem = PlaylistFixtures.playlistItem().updatedWithLike(likeStatus)

        val displayRepostsCount = trackStatsDisplayPolicy.displayRepostsCount(playableItem)

        assertThat(displayRepostsCount).isTrue()
    }

}
