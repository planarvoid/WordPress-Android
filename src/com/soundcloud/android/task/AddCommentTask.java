
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Http;

import org.apache.http.HttpStatus;

import android.os.AsyncTask;

import java.io.IOException;

public class AddCommentTask extends AsyncTask<Comment, String, Boolean> {

    SoundCloudApplication mApplication;
    AddCommentListener mAddCommentListener;
    Comment mAddComment;

    Exception mException;
    Http.Params mApiParams;

    public AddCommentTask(SoundCloudApplication app, AddCommentListener addCommentListener) {
        mApplication = app;
        mAddCommentListener = addCommentListener;
    }

    @Override
    protected Boolean doInBackground(Comment... params) {
        mAddComment = params[0];
        mApiParams = new Http.Params();
        mApiParams.add(CloudAPI.CommentParams.BODY, mAddComment.body);
        if (mAddComment.timestamp > -1) mApiParams.add(CloudAPI.CommentParams.TIMESTAMP, Long.toString(mAddComment.timestamp));
        if (mAddComment.reply_to_id > 0) mApiParams.add(CloudAPI.CommentParams.REPLY_TO, Long.toString(mAddComment.reply_to_id));

        try {
            return mApplication.postContent(
                    CloudAPI.Enddpoints.TRACK_COMMENTS.replace("{track_id}", Long.toString(mAddComment.track_id)),
                    mApiParams).getStatusLine().getStatusCode() == HttpStatus.SC_CREATED;
        } catch (IOException e) {
           mException = e;
           return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) mApplication.getTrackFromCache(mAddComment.track_id).comments = null;

        if (mAddCommentListener != null) {
            mAddCommentListener.onCommentAdd(success, mAddComment);
            if (mException != null) mAddCommentListener.onException(mAddComment,mException);
        }
    }

    // Define our custom Listener interface
    public interface AddCommentListener {
        void onCommentAdd(boolean success, Comment c);
        void onException(Comment c, Exception e);
    }
}
