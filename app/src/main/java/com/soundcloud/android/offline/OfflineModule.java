package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.net.ssl.HostnameVerifier;

@Module(addsTo = ApplicationModule.class,
        injects = {
                OfflineContentService.class
        })
public class OfflineModule {

    @Provides
    @Named("DownloadHttpClient")
    public OkHttpClient provideOkHttpClient() {
        return new OkHttpClient();
    }

    @Provides
    public HostnameVerifier provideHostnameVerifier() {
        return new DownloadHostnameVerifier();
    }

}
