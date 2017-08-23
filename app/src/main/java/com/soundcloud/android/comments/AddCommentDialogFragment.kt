package com.soundcloud.android.comments


import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import com.soundcloud.android.R
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.main.PlayerActivity
import com.soundcloud.android.model.Urn
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.android.tracks.TrackRepository
import com.soundcloud.android.utils.LeakCanaryWrapper
import com.soundcloud.android.utils.extensions.formatTimestamp
import com.soundcloud.android.utils.extensions.getUrn
import com.soundcloud.android.utils.extensions.putUrn
import kotlinx.android.synthetic.main.comment_input.view.*
import javax.inject.Inject

@Suppress("LongParameterList")
class AddCommentDialogFragment : DialogFragment() {

    @Inject internal lateinit var trackRepository: TrackRepository
    @Inject internal lateinit var leakCanaryWrapper: LeakCanaryWrapper

    init {
        SoundCloudApplication.getObjectGraph().inject(this)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val trackUrn = arguments.getUrn(EXTRA_TRACK_URN)
        val trackTitle = arguments.getString(EXTRA_TRACK_TITLE)
        val creatorUrn = arguments.getUrn(EXTRA_CREATOR_URN)
        val creatorName = arguments.getString(EXTRA_CREATOR_NAME)
        val position = arguments.getLong(EXTRA_POSITION)
        val timeFormatted = position.formatTimestamp()

        val dialogView = View.inflate(activity, R.layout.comment_input, null)
        with(dialogView) {
            custom_dialog_title.text = getString(R.string.comment_on_tracktitle, trackTitle)
            comment_input.hint = getString(R.string.comment_at_time, timeFormatted)
        }

        return AlertDialog.Builder(activity)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_post) { _, _ ->
                    dialogView.comment_input.text.toString().let {
                        if (it.isNotEmpty()) {
                            addComment(it, trackUrn, trackTitle, creatorUrn, creatorName, position)
                            dismiss()
                        }
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
    }

    private fun addComment(commentText: String, trackUrn: Urn?, trackTitle: String?, creatorUrn: Urn?, creatorName: String?, position: Long) {
        val originScreen = arguments.getString(EXTRA_ORIGIN_SCREEN)
        val activity = activity as PlayerActivity
        activity.addComment(AddCommentArguments.create(trackTitle, trackUrn, creatorName, creatorUrn, position, commentText, originScreen))
    }

    override fun onDestroy() {
        super.onDestroy()
        leakCanaryWrapper.watch(this)
    }

    companion object {
        private const val EXTRA_TRACK_URN = "track_urn"
        private const val EXTRA_TRACK_TITLE = "track_title"
        private const val EXTRA_CREATOR_URN = "creator_urn"
        private const val EXTRA_CREATOR_NAME = "creator_name"
        private const val EXTRA_POSITION = "position"
        private const val EXTRA_ORIGIN_SCREEN = "origin"

        @JvmStatic
        fun create(track: TrackItem, position: Long, originScreen: String): AddCommentDialogFragment {
            val fragment = AddCommentDialogFragment()
            fragment.arguments = Bundle().apply {
                putUrn(EXTRA_TRACK_URN, track.urn)
                putString(EXTRA_TRACK_TITLE, track.title())
                putUrn(EXTRA_CREATOR_URN, track.creatorUrn())
                putString(EXTRA_CREATOR_NAME, track.creatorName())
                putLong(EXTRA_POSITION, position)
                putString(EXTRA_ORIGIN_SCREEN, originScreen)
            }
            return fragment
        }
    }
}
