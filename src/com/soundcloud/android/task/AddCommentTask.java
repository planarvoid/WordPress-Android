
package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Comment;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddCommentTask extends AsyncTask<Comment, String, Boolean> {

    SoundCloudApplication mApplication;
    AddCommentListener mAddCommentListener;
    Comment mAddComment;

    Exception mException;
    List<NameValuePair> mApiParams;

    public AddCommentTask(SoundCloudApplication app, AddCommentListener addCommentListener) {
        mApplication = app;
        mAddCommentListener = addCommentListener;
    }

    @Override
    protected Boolean doInBackground(Comment... params) {
        mAddComment = params[0];
        mApiParams = new ArrayList<NameValuePair>();
        mApiParams.add(new BasicNameValuePair("comment[body]", mAddComment.body));
        if (mAddComment.timestamp > -1) mApiParams.add(new BasicNameValuePair("comment[timestamp]", Long.toString(mAddComment.timestamp)));
        if (mAddComment.reply_to_id > 0) mApiParams.add(new BasicNameValuePair("comment[reply_to]", Long.toString(mAddComment.reply_to_id)));


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
        if (success) mApplication.uncacheComments(mAddComment.track_id);

        if (mAddCommentListener != null) {
            mAddCommentListener.onCommentAdd(success, mAddComment);
            if (mException != null) mAddCommentListener.onException(mAddComment,mException);
        }
    }

    // Define our custom Listener interface
    public interface AddCommentListener {
        public abstract void onCommentAdd(boolean success, Comment c);
        public abstract void onException(Comment c, Exception e);
    }
}
