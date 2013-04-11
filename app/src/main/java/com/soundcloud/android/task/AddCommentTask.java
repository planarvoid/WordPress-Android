package com.soundcloud.android.task;

import android.content.Intent;
import android.os.AsyncTask;
import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;

public class AddCommentTask extends AsyncTask<Comment, Comment, Comment> {
    private SoundCloudApplication app;
    private IOException exception;

    public AddCommentTask(SoundCloudApplication app) {
        this.app = app;
    }

    @Override
    protected Comment doInBackground(Comment... comments) {
        final Comment comment = comments[0];

        if (comment.track_id <= 0 && comment.track != null) {
            comment.track_id = comment.track.id;
        }

        if (comment.track_id > 0) {
            Request request = Request.to(Endpoints.TRACK_COMMENTS, comment.track_id)
                    .add(Params.Comment.BODY, comment.body);

            if (comment.timestamp > -1) request.add(Params.Comment.TIMESTAMP, comment.timestamp);
            if (comment.reply_to_id > 0) request.add(Params.Comment.REPLY_TO, comment.reply_to_id);

            try {
                Comment created = app.create(request);
                publishProgress(comment, created);
                return created;
            } catch (IOException e) {
                exception = e;
            }
        }
        publishProgress(comment, null);
        return null;
    }

    @Override
    protected void onProgressUpdate(Comment... comments) {
        final Comment comment          = comments[0];
        final @Nullable Comment added  = comments[1];

        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(comment.track_id);
        // udpate the cached track comments list
        if (t != null) {
            if (t.comments != null) {
                // remove the dummy comment, as we now have the real one
                Comment toRemove = null;
                for (Comment c : t.comments) {
                    if (comment == c) { //instance check
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
                if (exception != null) {
                    app.sendBroadcast(new Intent(Actions.CONNECTION_ERROR));
                }
            }
            app.sendBroadcast(new Intent(Playable.COMMENTS_UPDATED).putExtra("id", t.id));
        }
    }
}
