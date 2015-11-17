package shoutout2.app.MapView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;

import shoutout2.app.Login.LoginActivity;
import shoutout2.app.Person;
import shoutout2.app.Utils.FirebaseUtils;
import shoutout2.app.Permissions;
import shoutout2.app.R;
import shoutout2.app.Utils.Utils;

public class SettingsFragment extends Fragment{
    public static final String TAG = "settings_fragment";
    private MapActivity mapActivity;
    private ImageView userPic;
    private ToggleButton mSwitch;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mapActivity = (MapActivity) getActivity();
        final View view = inflater.inflate(R.layout.settings, container, false);
        mSwitch = (ToggleButton) view.findViewById(R.id.switch1);
        mSwitch.setChecked(ParseUser.getCurrentUser().getBoolean("visible"));
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean privacy) {
                FirebaseUtils.setPrivacy(privacy);
                if (privacy) {
                    ParseUser.getCurrentUser().put("visible", true);
                } else {
                    ParseUser.getCurrentUser().put("visible", false);
                }
                ParseUser.getCurrentUser().saveInBackground();
            }
        });

        ImageButton changeIconButton = (ImageButton) view.findViewById(R.id.settings_change_icon);
        changeIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.selectImage(SettingsFragment.this);
            }
        });

        final ParseUser currentUser;
        try {
            currentUser = ParseUser.getCurrentUser().fetchIfNeeded();
        } catch (Exception e) {
            Log.e("fetch error", e.getLocalizedMessage() + " ");
            return view;
        }

        Person person = mapActivity.people.get(currentUser.getObjectId());
        if (person != null) {
            userPic = (ImageView) view.findViewById(R.id.settings_user_bubble);
            userPic.setImageDrawable(new BitmapDrawable(getResources(), person.icon));
        }

        ImageButton closeButton = (ImageButton) view.findViewById(R.id.settings_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapActivity.onBackPressed();
            }
        });

        final EditText usernameField = (EditText) view.findViewById(R.id.change_username_field);
        usernameField.setText(currentUser.getUsername());
        Button changeUsernameButton = (Button) view.findViewById(R.id.change_username_button);
        changeUsernameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameField.getText().toString();
                if (Utils.usernameTaken(username)) {
                    Toast.makeText(mapActivity, "Username taken. Please enter a different username.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (username.contains(" ")) {
                    Toast.makeText(mapActivity, "Username must be one word.",Toast.LENGTH_LONG).show();
                    return;
                }
                currentUser.setUsername(username.toLowerCase());
                currentUser.saveInBackground();
                InputMethodManager imm = (InputMethodManager)mapActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                mapActivity.people.get(currentUser.getObjectId()).username = username;
            }
        });

        Button feedbackButton = (Button) view.findViewById(R.id.feedback_button);
        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://getshoutout.co/feedback.html");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button logoutButton = (Button) view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.getCurrentUser().put("online", false);
                ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        ParseUser.logOut();
                        mapActivity.finish();
                        startActivity(new Intent(mapActivity, LoginActivity.class));
                    }
                });
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSwitch != null) {
            mSwitch.setChecked(ParseUser.getCurrentUser().getBoolean("visible"));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != mapActivity.RESULT_OK) {
            return;
        }
        Bitmap thumbnail = Utils.photoActivityResultHelper(SettingsFragment.this, requestCode, data);
        userPic.setImageDrawable(new BitmapDrawable(getResources(), Utils.getCroppedBitmap(thumbnail)));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream);
        ParseFile imageFile = new ParseFile("userpic.png", stream.toByteArray());
        final ParseObject userImageObj = new ParseObject("Images");
        userImageObj.put("image", imageFile);
        userImageObj.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                ParseUser.getCurrentUser().put("profileImage", userImageObj);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Permissions.permissionInts.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Utils.startChoosePhotoIntent(SettingsFragment.this);
            } else {
                Toast.makeText(getActivity(), "Please allow Shoutout to access your device storage.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
