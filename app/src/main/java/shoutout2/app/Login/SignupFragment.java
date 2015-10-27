package shoutout2.app.Login;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;

import shoutout2.app.Permissions;
import shoutout2.app.R;
import shoutout2.app.Utils.Utils;

public class SignupFragment extends Fragment {
    public static final String TAG = "signup_fragment_tag";
    private ImageView userPic;
    private ParseObject userImageObj;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.create_profile, container, false);

        userPic = (ImageView) v.findViewById(R.id.userPic);
        userPic.setBackgroundColor(Color.TRANSPARENT);
        userPic.setImageBitmap(scaleImage(getRandomPic()));
        Button backButton = (Button) v.findViewById(R.id.create_profile_back_button);
        Button nextButton = (Button) v.findViewById(R.id.create_profile_next_button);
        final EditText usernameField = (EditText) v.findViewById(R.id.username);
        final EditText passwordField = (EditText) v.findViewById(R.id.password);
        final EditText emailField = (EditText) v.findViewById(R.id.email);
        ImageView createProfileBubble = (ImageView) v.findViewById(R.id.create_profile_bubble);
        createProfileBubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.selectImage(SignupFragment.this);
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = usernameField.getText().toString();
                final String password = passwordField.getText().toString();
                final String email = emailField.getText().toString();
                signup(username, email, password);
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        return v;
    }

    private Bitmap getRandomPic() {
        ParseQuery query = new ParseQuery("DefaultImage");
        try {
            ParseObject defaultImageObj = query.getFirst();
            JSONArray images = defaultImageObj.getJSONArray("images");
            int picIndex = (int) (Math.random() * images.length());
            String picId = images.getJSONObject(picIndex).getString("objectId");
            ParseQuery imageQuery = new ParseQuery("Images");
            imageQuery.whereEqualTo("objectId", picId);
            userImageObj = imageQuery.getFirst();
            byte[] imageData = userImageObj.fetchIfNeeded().getParseFile("image").getData();
            return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        } catch (Exception e) {
            Log.e("ERR ON GET RANDOM PIC", e.getLocalizedMessage());
        }
        return null;
    }

    private void signup(final String username, final String email, final String password) {
        if (Utils.usernameTaken(username)) {
            Toast.makeText(getActivity(), "Username taken. Please enter a different username.", Toast.LENGTH_LONG).show();
            return;
        }
        if (username.contains(" ")) {
            Toast.makeText(getActivity(), "Username must be one word.", Toast.LENGTH_LONG).show();
            return;
        }
        ParseUser currentUser = new ParseUser();
        currentUser.setUsername(username.toLowerCase());
        currentUser.setPassword(password);
        currentUser.setEmail(email);
        currentUser.put("displayName", username);
        currentUser.put("status", "");
        currentUser.put("visible", true);
        currentUser.put("online", false);
        currentUser.put("profileImage", userImageObj);
        currentUser.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Utils.startMapActivity(getActivity());
                } else {
                    Log.e("signup error", "error: " + e.getLocalizedMessage());
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private Bitmap scaleImage(Bitmap img) {
        int size = (int) getResources().getDimension(R.dimen.login_screen_pic_size);
        return Bitmap.createScaledBitmap(img, size, size, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != getActivity().RESULT_OK) {
            return;
        }
        Bitmap thumbnail = Utils.photoActivityResultHelper(SignupFragment.this, requestCode, data);
        Bitmap thumbnailCropped = Utils.getCroppedBitmap(scaleImage(thumbnail));
        thumbnail.recycle();
        userPic.setImageBitmap(thumbnailCropped);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnailCropped.compress(Bitmap.CompressFormat.PNG, 100, stream);
        ParseFile imageFile = new ParseFile("userpic.png", stream.toByteArray());
        userImageObj = new ParseObject("Images");
        userImageObj.put("image", imageFile);
        userImageObj.saveInBackground();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Permissions.permissionInts.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Utils.startChoosePhotoIntent(SignupFragment.this);
            } else {
                Toast.makeText(getActivity(), "Please allow Shoutout to access your device storage.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
