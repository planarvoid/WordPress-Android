package com.soundcloud.android.stream;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {SoundStreamFragment.class})
public class SoundStreamModule {

    @Provides
    PagingItemAdapter<PropertySet> provideItemAdapter(StreamItemPresenter presenter) {
        return new PagingItemAdapter<PropertySet>(R.layout.list_loading_item, presenter);
    }

}
