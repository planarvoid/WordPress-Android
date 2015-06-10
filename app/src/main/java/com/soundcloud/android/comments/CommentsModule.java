package com.soundcloud.android.comments;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.presentation.PagingListItemAdapter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class, injects = {
        TrackCommentsActivity.class,
        AddCommentDialogFragment.class,
        CommentsFragment.class
})
public class CommentsModule {

    @Provides
    PagingListItemAdapter<Comment> provideCommentsAdapter(CommentRenderer renderer) {
        return new PagingListItemAdapter<>(renderer);
    }

}
