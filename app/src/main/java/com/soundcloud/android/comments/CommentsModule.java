package com.soundcloud.android.comments;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {
        TrackCommentsActivity.class,
        CommentsFragment.class
})
public class CommentsModule {

    @Provides
    EndlessAdapter<Comment> provideCommentsAdapter() {
        return new EndlessAdapter<>(new CommentPresenter());
    }

}
