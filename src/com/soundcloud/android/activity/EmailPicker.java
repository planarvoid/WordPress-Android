
package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EmailPicker extends ListActivity {
    public static final int PICK_EMAILS = 9002;
    public static final String BUNDLE_KEY = "emails";
    public static final String SELECTED = "selected";

    private TextWatcher mTextWatcher;

    protected ImageLoader mImageLoader;

    private EmailCursorAdapter mEmailAdp;

    static final Pattern EMAIL_REGEXP = Pattern
            .compile("[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");

    private EditText mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout layout = new FrameLayout(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.email_picker, layout);

        setContentView(layout);

        mImageLoader = ImageLoader.get(this);

        mEmailAdp = new EmailCursorAdapter(this, null);

        setListAdapter(mEmailAdp);
        mEmailAdp.getFilter().filter("");
        mEmailAdp.notifyDataSetChanged();

        mEmail = (EditText) findViewById(R.id.email);
        mEmail.setMaxLines(5);

        final StringBuilder sb = new StringBuilder();
        mTextWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sb.setLength(0);
                sb.append(s);
                mEmailAdp.getFilter().filter(
                        sb.indexOf(",") > -1 ? sb.subSequence(sb.lastIndexOf(",") + 1, sb.length())
                                .toString().trim() : sb.toString().trim());
            }

        };
        mEmail.addTextChangedListener(mTextWatcher);

        if (getIntent().hasExtra(BUNDLE_KEY)) {
            String[] emails = getIntent().getExtras().getStringArray(BUNDLE_KEY);
            if (emails != null && emails.length > 0) {
                mEmail.setText(TextUtils.join(", ", emails));
                mEmail.append(", ");

                // put cursor right after selected item
                if (getIntent().hasExtra(SELECTED)) {
                    String selected = getIntent().getStringExtra(SELECTED);

                    if (selected != null && !selected.equals(emails[emails.length - 1])) {
                        int cursorPos = mEmail.getEditableText().toString().indexOf(selected)
                                + selected.length();
                        if (cursorPos <= mEmail.length())
                            mEmail.setSelection(cursorPos);
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
                    if (!TextUtils.isEmpty(e) && EMAIL_REGEXP.matcher(e).matches())
                        emails.add(e);
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
        super.onListItemClick(l, v, position, id);

        Editable ed = mEmail.getText();
        ed.replace(ed.toString().lastIndexOf(",") + 1, ed.length(),
                ((TextView) v.findViewById(R.id.email)).getText() + ", ");
    }

    static class EmailCursorAdapter extends CursorAdapter {
        private ContentResolver mContentResolver;

        private LayoutInflater mInflater;

        private final int resource = R.layout.contacts_list_item;

        private Context mContext;

        public EmailCursorAdapter(Context context, Cursor c) {
            super(context, c);
            mContext = context;
            mContentResolver = context.getContentResolver();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return createViewFromResource(cursor.getInt(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)),
                    cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME)),
                    cursor.getString(cursor
                            .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)), null,
                    parent, resource);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            createViewFromResource(cursor.getInt(cursor
                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)),
                    cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME)),
                    cursor.getString(cursor
                            .getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)), view,
                    null, resource);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            Log.v(TAG, "runQueryOnBackgrounThread(" + constraint + ")");

            if (constraint != null) {
                return mContentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        new String[] {
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Email._ID,
                                ContactsContract.CommonDataKinds.Email.DATA
                        }, "UPPER(" + ContactsContract.Contacts.DISPLAY_NAME + ") GLOB ?",
                        new String[] {
                            constraint.toString().toUpperCase() + "*"
                        }, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
            } else {
                return super.runQueryOnBackgroundThread(constraint);
            }

        }
        
        private View createViewFromResource(int contactId, CharSequence displayName,
                CharSequence email, View convertView, ViewGroup parent, int resource) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(resource, parent, false);
            } else {
                view = convertView;
            }

            ((TextView) view.findViewById(R.id.name)).setText(displayName);
            ((TextView) view.findViewById(R.id.email)).setText(email);

            Bitmap bitmap = CloudUtils.loadContactPhoto(mContentResolver, contactId);
            ((ImageView) view.findViewById(R.id.icon))
                    .setImageBitmap(bitmap == null ? BitmapFactory.decodeResource(
                            mContext.getResources(), R.drawable.ic_contact_list_picture) : bitmap);
            return view;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEmail.removeTextChangedListener(mTextWatcher);
    }
}
