
package com.soundcloud.android.view;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.task.AddCommentTask;
import com.soundcloud.android.task.AddCommentTask.AddCommentListener;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class AddCommentDialog extends Dialog {

    private ScActivity mActivity;

    private EditText mInput;

    public AddCommentDialog(ScActivity context, final Comment comment,
            final AddCommentListener listener) {
        super(context, R.style.Theme_AddCommentDialog);

        mActivity = context;

        setContentView(R.layout.add_comment_dialog);

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        getWindow().setAttributes(params);

        setCancelable(true);
        setCanceledOnTouchOutside(true);

        mInput = (EditText) findViewById(R.id.comment_input);
        if (comment.reply_to_id > 0) {
            mInput.setHint("Reply to " + comment.reply_to_username + " at "
                    + CloudUtils.formatTimestamp(comment.timestamp));
        } else {
            mInput.setHint((comment.timestamp == -1 ? "Add an untimed comment" : "Add comment at "
                    + CloudUtils.formatTimestamp(comment.timestamp)));
        }
        ((Button) findViewById(R.id.positiveButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mInput.getText())) return;

                ((InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(mInput.getApplicationWindowToken(), 0);
                comment.body = mInput.getText().toString();

                new AddCommentTask(mActivity, comment,
                        listener == null ? mActivity.mAddCommentListener : listener).execute();
                dismiss();
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

}
