package com.soundcloud.android.stream;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import dagger.Module;
import dagger.Provides;

import android.view.View;

@Module(addsTo = ApplicationModule.class, injects = {SoundStreamFragment.class})
public class SoundStreamModule {

    @Provides
    PagingItemAdapter<PropertySet, View> provideItemAdapter(StreamItemPresenter presenter) {
        return new PagingItemAdapter<PropertySet, View>(presenter, Consts.LIST_PAGE_SIZE);
    }

}
