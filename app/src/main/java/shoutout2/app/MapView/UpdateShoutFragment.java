package shoutout2.app.MapView;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import shoutout2.app.Utils.FirebaseUtils;
import shoutout2.app.R;

public class UpdateShoutFragment extends Fragment {

    public static final String TAG = "update_shout_fragment";
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
        final InputMethodManager mgr = (InputMethodManager) mapActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

        String[] statusParam = getArguments().getStringArray("statusParam");
        mEdit.setText("");
        mEdit.append(statusParam.length > 0 ? statusParam[0] : ParseUser.getCurrentUser().getString("status"));
        mEdit.requestFocus();
        mgr.showSoftInput(mEdit, InputMethodManager.SHOW_IMPLICIT);

        changeStatusPin.setImageBitmap(mapActivity.people.get(ParseUser.getCurrentUser().getObjectId()).emptyStatusIcon);

        cancelShoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mgr.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
            }
        });

        updateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status = mEdit.getText().toString();
                ParseUser.getCurrentUser().put("status", status);
                FirebaseUtils.updateStatus(status);
                ParseUser.getCurrentUser().saveInBackground();
                checkStatusForMessage(status);
                cancelShoutButton.callOnClick();
            }
        });
        return view;
    }

    protected static void checkStatusForMessage(final String status) {
        for (String word : status.split("[^a-zA-Z\\d@]")) { // split on all characters except letters, numbers and @
            if (word.startsWith("@")) {
                String username = word.substring(1);
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
                    }
                });
            }
        }
    }

}
