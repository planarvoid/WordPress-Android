package com.soundcloud.android.comments

import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import com.soundcloud.android.R
import com.soundcloud.android.SoundCloudApplication
import com.soundcloud.android.accounts.MeOperations
import com.soundcloud.android.feedback.Feedback
import com.soundcloud.android.rx.observers.DefaultCompletableObserver
import com.soundcloud.android.utils.LeakCanaryWrapper
import com.soundcloud.android.view.snackbar.FeedbackController
import javax.inject.Inject

class ConfirmPrimaryEmailDialogFragment : DialogFragment() {

    @Inject internal lateinit var meOperations: MeOperations
    @Inject internal lateinit var feedbackController: FeedbackController
    @Inject internal lateinit var leakCanaryWrapper: LeakCanaryWrapper

    init {
        SoundCloudApplication.getObjectGraph().inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = View.inflate(activity, R.layout.confirm_primary_email_dialog, null)

        return AlertDialog.Builder(activity)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_resend_email_confirmation, { _, _ ->
                    onResendEmailButtonClicked()
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create()
    }

    private fun onResendEmailButtonClicked() {
        meOperations.resendEmailConfirmation()
                .subscribeWith(object : DefaultCompletableObserver() {
                    override fun onComplete() {
                        super.onComplete()
                        feedbackController.showFeedback(Feedback.create(R.string.confirm_primary_email_sent))
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        feedbackController.showFeedback(Feedback.create(R.string.confirm_primary_email_error))
                    }
                })
    }

    override fun onDestroy() {
        super.onDestroy()
        leakCanaryWrapper.watch(this)
    }

    companion object {
        @JvmStatic
        fun create() = ConfirmPrimaryEmailDialogFragment()
    }
}
