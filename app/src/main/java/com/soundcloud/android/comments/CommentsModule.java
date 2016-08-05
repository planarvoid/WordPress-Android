package com.soundcloud.android.comments;

import com.soundcloud.android.presentation.PagingListItemAdapter;
import dagger.Module;
import dagger.Provides;

@Module
public class CommentsModule {

    @Provides
    PagingListItemAdapter<Comment> provideCommentsAdapter(CommentRenderer renderer) {
        return new PagingListItemAdapter<>(renderer);
    }

}
