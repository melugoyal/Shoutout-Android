package shoutout2.app;

import android.Manifest;
import android.app.AlertDialog;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class LoginActivity extends FragmentActivity {

    private ViewPager pager;
    private ImageView userPic;

    private ParseObject userImageObj;
    private int SELECT_FILE = 1;
    private int REQUEST_CAMERA = 2;

    private int screen;
    private final int FIRST_SCREEN = 1;
    private final int CREATE_PROFILE_SCREEN = 2;
    private final int LOGIN_SCREEN = 3;

    private RelativeLayout rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkLocationAndWifi()) {
            return;
        }

        new Permissions(this);

        try {
            Parse.initialize(this, Keys.PARSE_APP_ID, Keys.PARSE_CLIENT_KEY);
        } catch (Exception e) {
            Log.d("Parse already init", "assuming user logged out");
        }
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            startMapActivity();
            return;
        }

        setContentView(R.layout.first_screen);
        screen = FIRST_SCREEN;
        rootView = (RelativeLayout) findViewById(R.id.first_screen);
        Button loginButton = (Button) findViewById(R.id.login_button);
        Button signupButton = (Button) findViewById(R.id.signup_button);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new ScreenSlidePagerAdapter(getSupportFragmentManager()));
        pager.setOffscreenPageLimit(4);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeViews(R.layout.login_screen);
                screen = LOGIN_SCREEN;
                Button backButton = (Button) findViewById(R.id.login_back_button);
                final Button nextButton = (Button) findViewById(R.id.login_next_button);
                Button forgotPasswordButton = (Button) findViewById(R.id.forgot_password_button);
                final EditText usernameField = (EditText) findViewById(R.id.login_username);
                final EditText passwordField = (EditText) findViewById(R.id.login_password);
                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String username = usernameField.getText().toString();
                        final String password = passwordField.getText().toString();
                        if (Utils.usernameTaken(username)) {
                            login(username, password);
                        } else {
                            Toast.makeText(LoginActivity.this, "Username doesn't exist. Please sign up.",Toast.LENGTH_LONG).show();
                        }
                    }
                });
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LoginActivity.this.onBackPressed();
                    }
                });
                forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        usernameField.setVisibility(View.GONE);
                        passwordField.setHint("Email");
                        nextButton.setText("Send");
                        nextButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ParseUser.requestPasswordResetInBackground(passwordField.getText().toString());
                            }
                        });
                    }
                });
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeViews(R.layout.create_profile);
                screen = CREATE_PROFILE_SCREEN;
//                Button changeIconButton = (Button) findViewById(R.id.changeIconButton);
                userPic = (ImageView) findViewById(R.id.userPic);
                userPic.setBackgroundColor(Color.TRANSPARENT);
                userPic.setImageBitmap(scaleImage(getRandomPic()));
                Button backButton = (Button) findViewById(R.id.create_profile_back_button);
                Button nextButton = (Button) findViewById(R.id.create_profile_next_button);
                final EditText usernameField = (EditText) findViewById(R.id.username);
                final EditText passwordField = (EditText) findViewById(R.id.password);
                final EditText emailField = (EditText) findViewById(R.id.email);
                ImageView createProfileBubble = (ImageView) findViewById(R.id.create_profile_bubble);
                createProfileBubble.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectImage();
                    }
                });
//                changeIconButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        selectImage();
//                    }
//                });
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
                        LoginActivity.this.onBackPressed();
                    }
                });
            }
        });
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

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library", "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals("Choose from Library")) {
                    if (Permissions.requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        startChoosePhotoIntent();
                    }
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void startChoosePhotoIntent() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(intent, "Select File"),
                SELECT_FILE);
    }

    private void login(String username, String password) {
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (parseUser != null && e == null) {
                    startMapActivity();
                } else {
                    Log.e("Login Error", e.getLocalizedMessage());
                    Toast.makeText(LoginActivity.this, "Incorrect password.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void signup(final String username, final String email, final String password) {
        if (Utils.usernameTaken(username)) {
            Toast.makeText(LoginActivity.this, "Username taken. Please enter a different username.",Toast.LENGTH_LONG).show();
            return;
        }
        if (username.contains(" ")) {
            Toast.makeText(LoginActivity.this, "Username must be one word.",Toast.LENGTH_LONG).show();
            return;
        }
        ParseUser currentUser = new ParseUser();
        currentUser.setUsername(username);
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
                    startMapActivity();
                } else {
                    Log.e("signup error", "error: " + e.getLocalizedMessage());
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (screen == FIRST_SCREEN) {
            if (pager.getCurrentItem() == 0) {
                // If the user is currently looking at the first step, allow the system to handle the
                // Back button. This calls finish() on this activity and pops the back stack.
                super.onBackPressed();
            } else {
                // Otherwise, select the previous step.
                pager.setCurrentItem(pager.getCurrentItem() - 1);
            }
        } else if (screen == CREATE_PROFILE_SCREEN){
            rootView.removeView(rootView.findViewById(R.id.create_profile));
        } else if (screen == LOGIN_SCREEN) {
            rootView.removeView(rootView.findViewById(R.id.login_screen));
        }
        screen = FIRST_SCREEN;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment f = new ScreenSlideFragment();
            Bundle args = new Bundle();
            args.putInt("imageId", getResources().getIdentifier("slider_image_"+(position+1), "drawable", getPackageName()));
            f.setArguments(args);
            return f;
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    private void changeViews(int resource) {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(resource, rootView);
        view.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
    }

    private void startMapActivity() {
        Intent intent = new Intent(this, MyMapActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        Bitmap thumbnail;
        if (requestCode == REQUEST_CAMERA) {
            thumbnail = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, bytes);
            File destination = new File(Environment.getExternalStorageDirectory(),
                    System.currentTimeMillis() + ".png");
            FileOutputStream fo;
            try {
                destination.createNewFile();
                fo = new FileOutputStream(destination);
                fo.write(bytes.toByteArray());
                fo.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (requestCode == SELECT_FILE) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            thumbnail = BitmapFactory.decodeFile(picturePath);
        } else {
            return;
        }
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

    private Bitmap scaleImage(Bitmap img) {
        int size = (int) getResources().getDimension(R.dimen.login_screen_pic_size);
        return Bitmap.createScaledBitmap(img, size, size, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkLocationAndWifi() {
//        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//        if (!mWifi.isConnected()) {
//            return false;
//        }

        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}
        if(!(gps_enabled && network_enabled)) {
            // notify user
            Toast.makeText(this, "Please enable network and location services", Toast.LENGTH_LONG).show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 2000);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Permissions.permissionInts.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startChoosePhotoIntent();
            } else {
                Toast.makeText(LoginActivity.this, "Please allow Shoutout to access your device storage.", Toast.LENGTH_LONG).show();
            }
            return;
        }
    }
}
