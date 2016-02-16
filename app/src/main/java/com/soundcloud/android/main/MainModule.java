package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.view.FullImageDialog;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                LauncherActivity.class,
                MainActivity.class,
                WebViewActivity.class,
                MetadataFragment.class,
                DevDrawerFragment.class,
                FullImageDialog.class
        }, includes = {AssociationsModule.class, AnalyticsModule.class})
public class MainModule {

}
