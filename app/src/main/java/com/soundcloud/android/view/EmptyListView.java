package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class EmptyListView extends RelativeLayout {
    protected ProgressBar mProgressBar;

    @Nullable protected ViewGroup mEmptyLayout;

    private RelativeLayout mEmptyViewHolder;
    private TextView mTxtMessage;
    private TextView mTxtLink;
    @Nullable private ImageView mImage;
    @Nullable private View mErrorView;
    protected Button mBtnAction;

    private int     mMessageResource, mLinkResource, mImageResource, mActionTextResource;
    private String  mMessage;

    private ActionListener mButtonActionListener;
    private ActionListener mImageActionListener;
    protected int mMode;

    public interface Status extends HttpStatus {
        int WAITING = -1;
        int ERROR = -2; //generic error
        int CONNECTION_ERROR = -3;
        int OK = SC_OK; //generic OK
    }

    public EmptyListView(final Context context, final Intent... intents) {
        super(context);
        setActionListener(context, intents);
        init();
    }

    public EmptyListView(final Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setActionListener(context);
        init();
    }

    public EmptyListView setActionListener(final Context context, final Intent... intents) {
        if (intents.length > 0) {
            setButtonActionListener(new ActionListener() {
                @Override
                public void onAction() {
                    context.startActivity(intents[0]);
                }
                @Override
                public void onSecondaryAction() {
                    if (intents.length > 1) {
                        context.startActivity(intents[1]);
                    }
                }
            });
        }
        return this;
    }

    private void init(){
        ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.empty_list, this);

        mEmptyViewHolder = ((RelativeLayout) findViewById(R.id.empty_view_holder));
        mProgressBar = (ProgressBar) findViewById(R.id.list_loading);
    }

    /**
     * Configure display based on response code
     * @param code
     * @return whether the code was handled here
     */
    public boolean setStatus(int code) {
        if (mMode != code) {
            mMode = code;

            if (code == Status.WAITING) {
                // don't show empty screen, show progress
                mProgressBar.setVisibility(View.VISIBLE);
                if (mEmptyLayout != null) mEmptyLayout.setVisibility(View.GONE);
                if (mErrorView != null) mErrorView.setVisibility(View.GONE);
                return true;

            } else if (Wrapper.isStatusCodeOk(code))  {
                // at rest, no error
                mProgressBar.setVisibility(View.GONE);
                showEmptyLayout();
                return true;

            } else {
                // error,
                mProgressBar.setVisibility(View.GONE);
                showError(code);
                return true;

            }

        }
        return true;
    }

    public int getStatus() {
        return mMode;
    }

    private void showError(int responseCode){
        if (mErrorView == null) {
            mErrorView = View.inflate(getContext(), R.layout.empty_list_error, null);
            mEmptyViewHolder.addView(mErrorView);
        } else {
            mErrorView.setVisibility(View.VISIBLE);
        }
        if (mEmptyLayout != null) mEmptyLayout.setVisibility(View.GONE);


        final TextView errorTextView = (TextView) mErrorView.findViewById(R.id.txt_message);
        if (responseCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
            errorTextView.setText(R.string.error_soundcloud_is_down);

        } else if (Wrapper.isStatusCodeError(responseCode)) {
            errorTextView.setText(R.string.error_soundcloud_server_problems);

        } else if (responseCode == Status.CONNECTION_ERROR) {
            errorTextView.setText(R.string.no_internet_connection);

        } else if (responseCode == Status.ERROR) {
            errorTextView.setText("");
        } else {
            errorTextView.setText("");
            Log.w(SoundCloudApplication.TAG,"Unhandled response code: " + responseCode);
        }
    }

    protected void showEmptyLayout() {
        if (mEmptyLayout == null){
            mEmptyLayout = (ViewGroup) View.inflate(getContext(), getEmptyViewLayoutId(), null);

            final RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            mEmptyViewHolder.addView(mEmptyLayout, params);

            mTxtMessage = (TextView) findViewById(R.id.txt_message);
            mTxtLink = (TextView) findViewById(R.id.txt_link);
            mBtnAction = (Button) findViewById(R.id.btn_action);
            mImage = (ImageView) findViewById(R.id.img_1);
            if (mImage != null) {
                mImage.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mImageActionListener != null) {
                            mImageActionListener.onAction();
                        }
                    }
                });
                setImage(mImageResource);
            }

            mBtnAction.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mButtonActionListener != null) {
                        mButtonActionListener.onAction();
                    }
                }
            });

            // set values
            if (TextUtils.isEmpty(mMessage)) {
                setMessageText(mMessageResource);
            } else {
                setMessageText(mMessage);
            }
            setSecondaryText(mLinkResource);
            setActionText(mActionTextResource);


        } else {
            mEmptyLayout.setVisibility(View.VISIBLE);
        }


        if (mErrorView != null) mErrorView.setVisibility(View.GONE);
    }

    protected int getEmptyViewLayoutId() {
        return R.layout.empty_collection_view;
    }

    public EmptyListView setImage(int imageId){
        mImageResource = imageId;
        if (mImage != null){
            if (imageId > 0){
                mImage.setVisibility(View.VISIBLE);
                mImage.setImageResource(imageId);
            } else {
                mImage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setMessageText(int messageId){
        mMessageResource = messageId;
        mMessage = null;
        if (mTxtMessage != null) {
            if (messageId > 0) {
                mTxtMessage.setText(messageId);
                mTxtMessage.setVisibility(View.VISIBLE);
            } else {
                mTxtMessage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setMessageText(String s) {
        mMessage = s;
        mMessageResource = -1;
        if (mTxtMessage != null) {
            if (!TextUtils.isEmpty(s)) {
                mTxtMessage.setText(s);
                mTxtMessage.setVisibility(View.VISIBLE);
            } else {
                mTxtMessage.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setSecondaryText(int secondaryTextId) {
        mLinkResource = secondaryTextId;
        if (mTxtLink != null) {
            if (secondaryTextId > 0) {
                mTxtLink.setText(secondaryTextId);
                mTxtLink.setVisibility(View.VISIBLE);
                ScTextUtils.clickify(mTxtLink, mTxtLink.getText().toString(), new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        if (mButtonActionListener != null) {
                            mButtonActionListener.onSecondaryAction();
                        }
                    }
                }, true, false);
            } else {
                mTxtLink.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public EmptyListView setActionText(int textId){
        mActionTextResource = textId;
        if (mBtnAction != null){
            if (textId > 0){
                mBtnAction.setVisibility(View.VISIBLE);
                mBtnAction.setText(textId);
            } else {
                mBtnAction.setVisibility(View.INVISIBLE);
            }
        }
        return this;
    }

    public EmptyListView setButtonActionListener(ActionListener listener){
        mButtonActionListener = listener;
        return this;
    }

    public EmptyListView setImageActionListener(ActionListener listener){
        mImageActionListener = listener;
        return this;
    }

    public interface ActionListener {
        void onAction();
        void onSecondaryAction();
    }

    @Deprecated
    public static EmptyListView fromContent(final Context context, final Content content) {
        switch (content) {
            case ME_SOUND_STREAM:
                return new EmptyListView(context).setMessageText(R.string.list_empty_stream_message)
                                        .setImage(R.drawable.empty_follow)
                                        .setActionText(R.string.list_empty_stream_action)
                                        .setSecondaryText(R.string.list_empty_stream_secondary)
                                        .setButtonActionListener(new EmptyListView.ActionListener() {
                                            @Override
                                            public void onAction() {
                                                context.startActivity(new Intent(Actions.WHO_TO_FOLLOW));
                                            }

                                            @Override
                                            public void onSecondaryAction() {
                                                context.startActivity(new Intent(Actions.FRIEND_FINDER));
                                            }
                                        });
            case ME_ACTIVITIES:
                User loggedInUser = SoundCloudApplication.fromContext(context).getLoggedInUser();
                if (loggedInUser == null || loggedInUser.track_count > 0) {
                                   return new EmptyListView(context).setMessageText(R.string.list_empty_activity_message)
                                           .setImage(R.drawable.empty_share)
                                           .setActionText(R.string.list_empty_activity_action)
                                           .setSecondaryText(R.string.list_empty_activity_secondary)
                                           .setButtonActionListener(new EmptyListView.ActionListener() {
                                               @Override
                                               public void onAction() {
                                                   context.startActivity(new Intent(Actions.YOUR_SOUNDS));
                                               }

                                               @Override
                                               public void onSecondaryAction() {
                                                   goTo101s(context);
                                               }
                                           });
                               } else {
                                   final EmptyListView.ActionListener record = new EmptyListView.ActionListener() {
                                       @Override
                                       public void onAction() {
                                           context.startActivity(new Intent(Actions.RECORD));
                                       }

                                       @Override
                                       public void onSecondaryAction() {
                                           goTo101s(context);
                                       }
                                   };

                                   return new EmptyListView(context).setMessageText(R.string.list_empty_activity_nosounds_message)
                                           .setImage(R.drawable.empty_rec)
                                           .setActionText(R.string.list_empty_activity_nosounds_action)
                                           .setSecondaryText(R.string.list_empty_activity_nosounds_secondary)
                                           .setButtonActionListener(record)
                                           .setImageActionListener(record);
                               }

            case ME_FRIENDS:
                return new FriendFinderEmptyCollection(context);

            default:
                return new EmptyListView(context);
        }
    }

    private static void goTo101s(Context context){
        context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101")));
    }

}
