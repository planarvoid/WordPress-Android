
package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Comment;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AddCommentTask extends AsyncTask<Comment, String, Boolean> {

    WeakReference<ScActivity> mActivity;
    AddCommentListener mAddCommentListener;
    Comment mAddComment;

    Exception mException;
    List<NameValuePair> mApiParams;

    public AddCommentTask(ScActivity scActivity, Comment comment, AddCommentListener addCommentListener) {
        this.mActivity = new WeakReference<ScActivity>(scActivity);

        mAddComment = comment;

        mApiParams = new ArrayList<NameValuePair>();
        mApiParams.add(new BasicNameValuePair("comment[body]", mAddComment.body));
        if (mAddComment.timestamp > -1) mApiParams.add(new BasicNameValuePair("comment[timestamp]", Long.toString(mAddComment.timestamp)));
        if (mAddComment.reply_to_id > 0) mApiParams.add(new BasicNameValuePair("comment[reply_to]", Long.toString(mAddComment.reply_to_id)));

        mAddCommentListener = addCommentListener;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(String... updates) {
    }

    @Override
    protected Boolean doInBackground(Comment... params) {
        if (mActivity.get() != null)
        try {
            if (201 == mActivity.get().getSoundCloudApplication().postContent(
                    CloudAPI.Enddpoints.TRACK_COMMENTS.replace("{track_id}", Long.toString(mAddComment.track_id)), mApiParams).getStatusLine().getStatusCode())
                return true;

        } catch (IOException e) {
           mException = e;
        }
        return false;
    }


    @Override
    protected void onPostExecute(Boolean success) {
        Log.i("asdf","ON POST EXECUTE " + success);
        if (success && mActivity.get() != null)
                mActivity.get().getSoundCloudApplication().uncacheComments(mAddComment.track_id);

        if (mAddCommentListener != null)
            mAddCommentListener.onCommentAdd(success, mAddComment);

        if (mException != null && mAddCommentListener != null)
            mAddCommentListener.onException(mAddComment,mException);
    }

    // Define our custom Listener interface
    public interface AddCommentListener {
        public abstract void onCommentAdd(boolean success, Comment c);
        public abstract void onException(Comment c, Exception e);
    }

}
