package com.soundcloud.android.accounts

import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.api.ApiClientRxV2
import com.soundcloud.android.api.ApiEndpoints
import com.soundcloud.android.api.ApiRequest
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.Completable
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class MeOperations
@Inject
constructor(
        private val apiClientRx: ApiClientRxV2,
        @param:Named(ApplicationModule.RX_HIGH_PRIORITY) private val scheduler: Scheduler) {

    fun resendEmailConfirmation(): Completable {
        val request = ApiRequest.post(ApiEndpoints.RESEND_EMAIL_CONFIRMATION.path())
                .forPrivateApi()
                .build()
        return apiClientRx.ignoreResultRequest(request).subscribeOn(scheduler)
    }
}
