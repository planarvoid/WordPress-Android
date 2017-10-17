package com.soundcloud.android.comments;

import com.soundcloud.android.presentation.PagingListItemAdapter;
import dagger.Module;
import dagger.Provides;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class CommentsModule {

    @Provides
    static PagingListItemAdapter<Comment> provideCommentsAdapter(CommentRenderer renderer) {
        return new PagingListItemAdapter<>(renderer);
    }

}
