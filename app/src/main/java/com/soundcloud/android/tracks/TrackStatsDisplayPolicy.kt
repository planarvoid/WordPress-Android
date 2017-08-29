package com.soundcloud.android.tracks

import com.soundcloud.android.accounts.AccountOperations
import com.soundcloud.android.presentation.PlayableItem
import com.soundcloud.android.utils.OpenForTesting

import javax.inject.Inject

@OpenForTesting
class TrackStatsDisplayPolicy
@Inject
constructor(private val accountOperations: AccountOperations) {

    fun displayPlaysCount(trackItem: TrackItem) =
            trackItem.hasPlayCount() && trackItem.displayStatsForCurrentUser()

    fun displayCommentsCount(trackItem: TrackItem) =
            trackItem.isCommentable
                    && trackItem.commentsCount() > 0
                    && trackItem.displayStatsForCurrentUser()

    fun displayLikesCount(playableItem: PlayableItem) =
            when (playableItem) {
                is TrackItem -> playableItem.likesCount() > 0 && playableItem.displayStatsForCurrentUser()
                else -> playableItem.likesCount() > 0
            }

    fun displayRepostsCount(playableItem: PlayableItem) =
            when (playableItem) {
                is TrackItem -> playableItem.repostsCount() > 0 && playableItem.displayStatsForCurrentUser()
                else -> playableItem.repostsCount() > 0
            }

    private fun TrackItem.displayStatsForCurrentUser() =
            displayStatsEnabled() || accountOperations.loggedInUserUrn == creatorUrn()
}
