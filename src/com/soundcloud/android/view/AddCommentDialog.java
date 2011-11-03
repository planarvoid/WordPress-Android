
package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.task.AddCommentTask;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class AddCommentDialog extends Dialog {

    private ScActivity mActivity;

    private EditText mInput;

    public AddCommentDialog(ScActivity context) {
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
            mInput.setHint(String.format(mActivity.getString(R.string.comment_hint_reply),
                    comment.reply_to_username,CloudUtils.formatTimestamp(comment.timestamp)));
        } else {

            mInput.setHint((comment.timestamp == -1 ? mActivity.getString(R.string.comment_hint_untimed) :
                    String.format(mActivity.getString(R.string.comment_hint_timed), CloudUtils.formatTimestamp(comment.timestamp))));
        }
        findViewById(R.id.positiveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mInput.getText())) return;

                ((InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(mInput.getApplicationWindowToken(), 0);
                comment.body = mInput.getText().toString();
                new AddCommentTask(mActivity.getApp()).execute(comment);

                // cannot simply dismiss, or state will be saved
                mActivity.removeDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);

                if (mActivity instanceof ScPlayer){
                    ((ScPlayer)mActivity).onNewComment(comment);
                }
            }
        });

        // imm.showSoftInput(input,InputMethodManager.SHOW_FORCED);
        mInput.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    InputMethodManager imm = (InputMethodManager) mActivity
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                            InputMethodManager.HIDE_IMPLICIT_ONLY);

                }
            }
        });
        mInput.requestFocus();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && isOutOfBounds(event)) {
            ((InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(mInput.getApplicationWindowToken(), 0);
            cancel();
            return true;
        }
        return false;
    }

    private boolean isOutOfBounds(MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int slop = ViewConfiguration.get(mActivity).getScaledWindowTouchSlop();
        final View decorView = getWindow().getDecorView();
        return (x < -slop) || (y < -slop) || (x > (decorView.getWidth() + slop))
                || (y > (decorView.getHeight() + slop));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mActivity.removeDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
