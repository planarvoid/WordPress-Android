package com.soundcloud.android.task;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Sound;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.Intent;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class AddCommentTask extends AsyncTask<Void, String, Comment> {
    private SoundCloudApplication app;
    private IOException exception;
    private Comment mComment;

    public AddCommentTask(SoundCloudApplication app, Comment comment) {
        this.app = app;
        mComment = comment;
    }

    @Override
    protected Comment doInBackground(Void... params) {


        if (mComment.track_id <= 0 && mComment.track != null) {
            mComment.track_id = mComment.track.id;
        }

        if (mComment.track_id > 0) {
            Request request = Request.to(Endpoints.TRACK_COMMENTS, mComment.track_id)
                    .add(Params.Comment.BODY, mComment.body);

            if (mComment.timestamp > -1) request.add(Params.Comment.TIMESTAMP, mComment.timestamp);
            if (mComment.reply_to_id > 0) request.add(Params.Comment.REPLY_TO, mComment.reply_to_id);

            try {
                final HttpResponse response = app.post(request);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                    return app.getMapper().readValue(response.getEntity().getContent(), Comment.class);
                } //  fall-through
            } catch (IOException e) {
                exception = e;
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Comment added) {
        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(mComment.track_id);
        // udpate the cached track comments list
        if (t != null) {
            if (t.comments != null) {
                // remove the dummy comment, as we now have the real one
                Comment toRemove = null;
                for (Comment c : t.comments) {
                    if (mComment == c) { //instance check
                        toRemove = c;
                    }
                }
                if (toRemove != null) t.comments.remove(toRemove);
            }

            if (added != null) {
                if (t.comments == null) t.comments = new ArrayList<Comment>();
                t.comments.add(added);
            } else {
                t.comment_count--;
                // error adding comment
                if (exception instanceof UnknownHostException || exception instanceof SocketException) {
                    app.sendBroadcast(new Intent(Actions.CONNECTION_ERROR));
                }
            }
            app.sendBroadcast(new Intent(Sound.COMMENTS_UPDATED).putExtra("id", t.id));
        }
    }
}
