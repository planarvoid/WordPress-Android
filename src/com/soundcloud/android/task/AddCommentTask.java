
package com.soundcloud.android.task;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Comment;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class AddCommentTask extends AsyncTask<Comment, String, Boolean> {

    WeakReference<ScActivity> mActivity;
    AddCommentListener mAddCommentListener;
    Comment mAddComment;

    Exception mException;
    List<NameValuePair> mApiParams;

    public AddCommentTask(ScActivity scActivity, final long track_id, long timestamp, final String commentBody, long replyTo, AddCommentListener addCommentListener) {
        this.mActivity = new WeakReference<ScActivity>(scActivity);

        mAddComment = new Comment();
        mAddComment.track_id = track_id;
        mAddComment.created_at = new Date(System.currentTimeMillis());
        mAddComment.user_id = CloudUtils.getCurrentUserId(mActivity.get());

        mAddComment.user = SoundCloudDB.getInstance().resolveUserById(mActivity.get().getContentResolver(), mAddComment.user_id);
        mAddComment.timestamp = timestamp;
        mAddComment.body = commentBody;

        mApiParams = new ArrayList<NameValuePair>();
        mApiParams.add(new BasicNameValuePair("comment[body]", mAddComment.body));
        if (mAddComment.timestamp > -1) mApiParams.add(new BasicNameValuePair("comment[timestamp]", Long.toString(mAddComment.timestamp)));
        if (replyTo > 0) mApiParams.add(new BasicNameValuePair("comment[reply_to]", Long.toString(replyTo)));

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
