package shoutout2.app.MapView;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import shoutout2.app.Person;
import shoutout2.app.Utils.FirebaseUtils;
import shoutout2.app.R;

public class UpdateShoutFragment extends Fragment {

    public static final String TAG = "update_shout_fragment";
    private static final int MAX_STATUS_LENGTH = 120;
    private MapActivity mapActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mapActivity = (MapActivity) getActivity();
        final View view = inflater.inflate(R.layout.update_shout, container, false);
        final EditText mEdit = (EditText) view.findViewById(R.id.newStatus);
        final Button updateStatus = (Button) view.findViewById(R.id.updateStatusButton);
        final ImageButton cancelShoutButton = (ImageButton) view.findViewById(R.id.cancel_shout);
        final ImageView changeStatusPin = (ImageView) view.findViewById(R.id.changeStatusPin);
        final TextView charCounter = (TextView) view.findViewById(R.id.char_counter);
        final RelativeLayout updateShoutView = (RelativeLayout) view.findViewById(R.id.update_shout_view);
        final InputMethodManager mgr = (InputMethodManager) mapActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

        mEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                charCounter.setText(Integer.toString(s.length()) + "/" + Integer.toString(MAX_STATUS_LENGTH));
                if (s.length() > MAX_STATUS_LENGTH) {
                    charCounter.setTextColor(Color.RED);
                } else {
                    charCounter.setTextColor(Color.BLUE);
                }
            }
        });

        final ParseUser currentUser;
        try {
            currentUser = ParseUser.getCurrentUser().fetchIfNeeded();
        } catch (Exception e) {
            Log.e("fetch error", e.getLocalizedMessage() + " ");
            return view;
        }

        String[] statusParam = getArguments().getStringArray("statusParam");
        mEdit.setText("");
        mEdit.append(statusParam.length > 0 ? statusParam[0] : currentUser.getString("status"));
        mEdit.requestFocus();
        mgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        cancelShoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mgr.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
                mapActivity.onBackPressed();
            }
        });

        updateShoutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelShoutButton.callOnClick();
            }
        });

        updateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status = mEdit.getText().toString();
                if (status.length() <= MAX_STATUS_LENGTH) {
                    currentUser.put("status", status);
                    FirebaseUtils.updateStatus(status);
                    currentUser.saveInBackground();
                    checkStatusForMessage(status);
                    cancelShoutButton.callOnClick();
                }
            }
        });
        Person person = mapActivity.people.get(currentUser.getObjectId());
        if (person == null) {
            return view;
        }
        changeStatusPin.setImageBitmap(person.emptyStatusIcon);

        return view;
    }

    protected static void checkStatusForMessage(final String status) {
        for (String word : status.split("[^a-zA-Z\\d@]")) { // split on all characters except letters, numbers and @
            if (word.startsWith("@")) {
                String username = word.substring(1).toLowerCase();
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("username", username);
                query.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        ParseObject message = new ParseObject("Messages");
                        message.put("from", ParseUser.getCurrentUser());
                        message.put("to", parseUser);
                        message.put("read", false);
                        message.put("message", status);
                        message.saveInBackground();

                        ParseQuery<ParseInstallation> pushQuery = ParseInstallation.getQuery();
                        pushQuery.whereEqualTo("user", parseUser);

                        ParsePush push = new ParsePush();
                        push.setQuery(pushQuery);
                        push.setMessage(ParseUser.getCurrentUser().getUsername() + ": " + status);
                        push.sendInBackground();
                    }
                });
            }
        }
    }

}
