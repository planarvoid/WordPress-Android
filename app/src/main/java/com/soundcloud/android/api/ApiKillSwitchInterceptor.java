package com.soundcloud.android.api;

import com.soundcloud.android.configuration.ForceUpdateEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.EventBus;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Response;

import javax.inject.Inject;
import java.io.IOException;

class ApiKillSwitchInterceptor implements Interceptor {

    static final String KILL_SWITCH_HEADER = "SC-Mob-SelfDestruct";
    private final EventBus eventBus;
    private final BuildHelper buildHelper;
    private final DeviceHelper deviceHelper;

    @Inject
    ApiKillSwitchInterceptor(EventBus eventBus, BuildHelper buildHelper, DeviceHelper deviceHelper) {
        this.eventBus = eventBus;
        this.buildHelper = buildHelper;
        this.deviceHelper = deviceHelper;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Response response = chain.proceed(chain.request());
        if (Boolean.valueOf(response.header(KILL_SWITCH_HEADER))) {
            final ForceUpdateEvent event = new ForceUpdateEvent(
                    buildHelper.getAndroidReleaseVersion(),
                    deviceHelper.getAppVersionName(),
                    deviceHelper.getAppVersionCode());
            eventBus.publish(EventQueue.FORCE_UPDATE, event);
        }
        return response;
    }
}
