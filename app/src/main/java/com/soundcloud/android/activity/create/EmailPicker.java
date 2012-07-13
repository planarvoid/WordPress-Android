package com.soundcloud.android.activity.create;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.create.EmailPickerItem;
import org.jetbrains.annotations.Nullable;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class EmailPicker extends ListActivity {
    public static final String BUNDLE_KEY = "emails";
    public static final String SELECTED = "selected";

    private CursorAdapter mEmailAdapter;
    private EditText mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout layout = new FrameLayout(this);
        View.inflate(this, R.layout.email_picker, layout);

        setContentView(layout);

        mEmailAdapter = new EmailCursorAdapter(this, null);
        setListAdapter(mEmailAdapter);
        mEmailAdapter.getFilter().filter("");
        mEmailAdapter.notifyDataSetChanged();

        mEmail = (EditText) findViewById(R.id.email);
        mEmail.setMaxLines(5);
        mEmail.addTextChangedListener(mTextWatcher);

        if (getIntent().hasExtra(BUNDLE_KEY)) {
            String[] emails = getIntent().getExtras().getStringArray(BUNDLE_KEY);
            if (emails != null && emails.length > 0) {
                mEmail.setText(TextUtils.join(", ", emails));
                mEmail.append(", ");

                // put cursor right after selected item
                if (getIntent().hasExtra(SELECTED)) {
                    String selected = getIntent().getStringExtra(SELECTED);

                    if (selected != null) {
                        mEmail.setSelection(Math.min(
                            mEmail.getEditableText().toString().indexOf(selected) + selected.length(),
                            mEmail.length()));
                    } else {
                        mEmail.setSelection(mEmail.length());
                    }
                }
            }
        }

        findViewById(R.id.pick_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent();
                Uri uri = new Uri.Builder().scheme("content")
                        .authority(EmailPicker.this.getClass().getName()).path(BUNDLE_KEY).build();

                List<String> emails = new ArrayList<String>();
                for (String e : mEmail.getText().toString().split("\\s*,\\s*")) {
                    e = e.trim();
                    if (ScTextUtils.isEmail(e)) {
                        emails.add(e);
                    }
                }
                result.putExtra(BUNDLE_KEY, emails.toArray(new String[emails.size()]));
                setResult(RESULT_OK, result.setData(uri));
                finish();
            }
        });

        findViewById(R.id.pick_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEmail.getText().clear();
            }
        });

        mEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    findViewById(R.id.pick_done).performClick();
                }
                return true;
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Editable ed = mEmail.getText();
        ed.replace(ed.toString().lastIndexOf(",") + 1, ed.length(),
                ((TextView) v.findViewById(R.id.email)).getText() + ", ");
    }

    private static class EmailCursorAdapter extends CursorAdapter {
        private final ContentResolver mContentResolver;
        private final LayoutInflater mInflater;

        public EmailCursorAdapter(Context context, Cursor c) {
            super(context, c);
            mContentResolver = context.getContentResolver();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return createViewFromResource(cursor, null, parent);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            createViewFromResource(cursor, view, null);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            Log.v(TAG, "runQueryOnBackgroundThread(" + constraint + ")");
            if (constraint != null) {
                return mContentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Email._ID,
                                ContactsContract.CommonDataKinds.Email.DATA
                        }, "UPPER(" + ContactsContract.Contacts.DISPLAY_NAME + ") GLOB ?",
                        new String[]{
                                constraint.toString().toUpperCase() + "*"
                        }, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
            } else {
                return super.runQueryOnBackgroundThread(constraint);
            }

        }

        private EmailPickerItem createViewFromResource(Cursor cursor,
                                            @Nullable View convertView,
                                            @Nullable ViewGroup parent) {
            EmailPickerItem view;
            if (convertView instanceof EmailPickerItem) {
                view = (EmailPickerItem) convertView;
            } else {
                view = (EmailPickerItem) mInflater.inflate(R.layout.email_picker_item, parent, false);
            }
            view.initializeFromCursor(cursor);
            return view;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEmail.removeTextChangedListener(mTextWatcher);
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) { }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence seq, int start, int before, int count) {
            final String s = seq.toString();
            mEmailAdapter.getFilter().filter(
                    s.contains(",") ?
                        s.subSequence(s.lastIndexOf(",") + 1, s.length()).toString().trim() :
                        s.trim());
        }
    };
}
