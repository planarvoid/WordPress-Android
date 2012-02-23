
package com.soundcloud.android.task;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.Intent;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;

public class AddCommentTask extends AsyncTask<Comment, String, Boolean> {
    SoundCloudApplication mApplication;
    com.soundcloud.android.model.Comment mAddComment;

    Exception mException;
    Request mRequest;

    public AddCommentTask(SoundCloudApplication app) {
        mApplication = app;
    }

    @Override
    protected Boolean doInBackground(Comment... params) {
        mAddComment = params[0];
        mRequest = Request.to(Endpoints.TRACK_COMMENTS, mAddComment.track_id)
                          .add(Params.Comment.BODY, mAddComment.body);

        if (mAddComment.timestamp > -1) mRequest.add(Params.Comment.TIMESTAMP, mAddComment.timestamp);
        if (mAddComment.reply_to_id > 0) mRequest.add(Params.Comment.REPLY_TO, mAddComment.reply_to_id);

        try {
            return mApplication.post(mRequest).getStatusLine().getStatusCode() == HttpStatus.SC_CREATED;
        } catch (IOException e) {
           mException = e;
           return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success){
            if (SoundCloudApplication.TRACK_CACHE.containsKey(mAddComment.track_id)) {
                final Track track = SoundCloudApplication.TRACK_CACHE.get(mAddComment.track_id);
                if (track.comments == null) track.comments = new ArrayList<Comment>();
                track.comments.add(mAddComment);
            }

            Intent i = new Intent(Actions.COMMENT_ADDED);
            i.putExtra("id",mAddComment.track_id);
            i.putExtra("comment", mAddComment);
            mApplication.sendBroadcast(i);

        } else if (CloudUtils.isConnectionException(mException)) {
            mApplication.sendBroadcast(new Intent(Actions.CONNECTION_ERROR));
        }


    }
}
