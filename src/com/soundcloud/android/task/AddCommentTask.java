
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;

import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;

import java.io.IOException;

public class AddCommentTask extends AsyncTask<com.soundcloud.android.objects.Comment, String, Boolean> {

    SoundCloudApplication mApplication;
    AddCommentListener mAddCommentListener;
    com.soundcloud.android.objects.Comment mAddComment;

    Exception mException;
    Params mApiParams;

    public AddCommentTask(SoundCloudApplication app, AddCommentListener addCommentListener) {
        mApplication = app;
        mAddCommentListener = addCommentListener;
    }

    @Override
    protected Boolean doInBackground(com.soundcloud.android.objects.Comment... params) {
        mAddComment = params[0];
        mApiParams = new Params();
        mApiParams.add(Params.Comment.BODY, mAddComment.body);
        if (mAddComment.timestamp > -1) mApiParams.add(Params.Comment.TIMESTAMP, Long.toString(mAddComment.timestamp));
        if (mAddComment.reply_to_id > 0) mApiParams.add(Params.Comment.REPLY_TO, Long.toString(mAddComment.reply_to_id));

        try {
            return mApplication.postContent(
                    String.format(Endpoints.TRACK_COMMENTS, mAddComment.track_id),
                    mApiParams).getStatusLine().getStatusCode() == HttpStatus.SC_CREATED;
        } catch (IOException e) {
           mException = e;
           return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (mAddCommentListener != null) {
            mAddCommentListener.onCommentAdd(success, mAddComment);
            if (mException != null) mAddCommentListener.onException(mAddComment,mException);
        }
    }

    // Define our custom Listener interface
    public interface AddCommentListener {
        void onCommentAdd(boolean success, com.soundcloud.android.objects.Comment c);
        void onException(com.soundcloud.android.objects.Comment c, Exception e);
    }
}
