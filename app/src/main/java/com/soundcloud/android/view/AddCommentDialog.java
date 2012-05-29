
package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.task.AddCommentTask;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.MotionEventUtils;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

public class AddCommentDialog extends Dialog {
    private ScListActivity mActivity;
    private EditText mInput;

    public AddCommentDialog(ScListActivity context) {
        super(context, R.style.Theme_AddCommentDialog);
        mActivity = context;

        setContentView(R.layout.add_comment_dialog);

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        getWindow().setAttributes(params);

        setCancelable(true);
        setCanceledOnTouchOutside(true);

        final Comment comment = mActivity.getApp().pendingComment;

        if (comment == null) {
            dismiss();
            return;
        }

        mInput = (EditText) findViewById(R.id.comment_input);
        if (comment.reply_to_id > 0) {
            mInput.setHint(getContext().getString(R.string.comment_hint_reply,
                    comment.reply_to_username,
                    CloudUtils.formatTimestamp(comment.timestamp)));
        } else {
            mInput.setHint(comment.timestamp == -1 ?
                    getContext().getString(R.string.comment_hint_untimed) :
                    getContext().getString(R.string.comment_hint_timed, CloudUtils.formatTimestamp(comment.timestamp)));
        }

        findViewById(R.id.doneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done(comment);
            }
        });

        mInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                            InputMethodManager.HIDE_IMPLICIT_ONLY);

                }
            }
        });
        mInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return actionId == EditorInfo.IME_ACTION_DONE && done(comment);
            }
        });
        mInput.requestFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Comment comment = mActivity.getApp().pendingComment;
        if (comment != null) {
            mActivity.track(Page.Sounds_add_comment, comment.getTrack());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && MotionEventUtils.isOutOfBounds(event, this)) {
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(mInput.getApplicationWindowToken(), 0);
            cancel();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mActivity.removeDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private boolean done(final Comment comment) {
        final String text = mInput.getText().toString();
        if (!TextUtils.isEmpty(text))  {
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(mInput.getApplicationWindowToken(), 0);
            comment.body = text;

            new AddCommentTask(mActivity.getApp()).execute(comment);

            // cannot simply dismiss, or state will be saved
            mActivity.removeDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);

            SoundCloudApplication.TRACK_CACHE.addCommentToTrack(comment);

            if (mActivity instanceof ScPlayer) {
                ((ScPlayer)mActivity).onNewComment(comment);
            }
            return true;
        } else return false;
    }
}
