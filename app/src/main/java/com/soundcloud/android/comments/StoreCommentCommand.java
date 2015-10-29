package com.soundcloud.android.comments;

import static java.util.Collections.singleton;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Tables.Comments;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class StoreCommentCommand extends DefaultWriteStorageCommand<CommentRecord, TxnResult> {

    private long lastRowId = -1;

    @Inject
    public StoreCommentCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final CommentRecord comment) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                final ContentValues commentValues = buildContentValues(comment);
                step(new StoreUsersCommand(propeller).call(singleton(comment.getUser())));
                lastRowId = step(propeller.insert(Comments.TABLE, commentValues)).getRowId();
            }
        });
    }

    public long lastRowId() {
        return lastRowId;
    }

    private static ContentValues buildContentValues(CommentRecord comment) {
        final ContentValuesBuilder contentValues = ContentValuesBuilder.values();
        contentValues.put(Comments.URN, comment.getUrn().toString());
        contentValues.put(Comments.TRACK_ID, comment.getTrackUrn().getNumericId());
        contentValues.put(Comments.USER_ID, comment.getUser().getUrn().getNumericId());
        contentValues.put(Comments.BODY, comment.getBody());
        contentValues.put(Comments.CREATED_AT, comment.getCreatedAt().getTime());
        contentValues.put(Comments.TIMESTAMP, comment.getTrackTime());
        return contentValues.get();
    }
}
