package com.soundcloud.android.task;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.Intent;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class AddCommentTask extends AsyncTask<Comment, String, Comment> {
    private SoundCloudApplication app;
    private IOException exception;

    public AddCommentTask(SoundCloudApplication app) {
        this.app = app;
    }

    @Override
    protected Comment doInBackground(Comment... params) {
        final Comment comment = params[0];

        if (comment.track_id <= 0 && comment.track != null) {
            comment.track_id = comment.track.id;
        }

        if (comment.track_id > 0) {
            Request request = Request.to(Endpoints.TRACK_COMMENTS, comment.track_id)
                    .add(Params.Comment.BODY, comment.body);

            if (comment.timestamp > -1) request.add(Params.Comment.TIMESTAMP, comment.timestamp);
            if (comment.reply_to_id > 0) request.add(Params.Comment.REPLY_TO, comment.reply_to_id);

            try {
                if (app.post(request).getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                    return comment;
                } //  fall-through
            } catch (IOException e) {
                exception = e;
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Comment comment) {
        if (comment != null) {
            if (SoundCloudApplication.TRACK_CACHE.containsKey(comment.track_id)) {
                final Track track = SoundCloudApplication.TRACK_CACHE.get(comment.track_id);
                if (track.comments == null) track.comments = new ArrayList<Comment>();
                track.comments.add(comment);
            }
            app.sendBroadcast(new Intent(Actions.COMMENT_ADDED)
                    .putExtra("id", comment.track_id)
                    .putExtra("comment", comment));
        } else if (exception instanceof UnknownHostException || exception instanceof SocketException) {
            app.sendBroadcast(new Intent(Actions.CONNECTION_ERROR));
        }
    }
}
