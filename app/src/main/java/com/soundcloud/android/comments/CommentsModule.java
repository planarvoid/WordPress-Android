package com.soundcloud.android.comments;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {
        TrackCommentsActivity.class,
        AddCommentDialogFragment.class,
        CommentsFragment.class
})
public class CommentsModule {

    @Provides
    PagingItemAdapter<Comment> provideCommentsAdapter(CommentRenderer renderer) {
        return new PagingItemAdapter<>(renderer);
    }

}
