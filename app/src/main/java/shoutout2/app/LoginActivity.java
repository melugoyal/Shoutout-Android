package shoutout2.app;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends Activity {

    private EditText usernameField;
    private EditText passwordField;
    private EditText emailField;
    private Button signinButton;
    private TextView welcomeText;
    private Button signupButton;
    private ImageView logo;
    private Button changeIconButton;

    private ParseObject userImageObj;
    private int SELECT_FILE = 1;
    private int REQUEST_CAMERA = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkLocationAndWifi()) {
            return;
        }

        new Permissions(this);

        Parse.initialize(this, Keys.PARSE_APP_ID, Keys.PARSE_CLIENT_KEY);
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            startMapActivity();
            return;
        }

        setContentView(R.layout.activity_login);
        usernameField = (EditText) findViewById(R.id.username);
        passwordField = (EditText) findViewById(R.id.password);
        emailField = (EditText) findViewById(R.id.email);
        signinButton = (Button) findViewById(R.id.signin_button);
        welcomeText = (TextView) findViewById(R.id.text_field);
        signupButton = (Button) findViewById(R.id.signup_button);
        logo = (ImageView) findViewById(R.id.logo);
        changeIconButton = (Button) findViewById(R.id.changeIconButton);

        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = usernameField.getText().toString();
                final String password = passwordField.getText().toString();
                if (usernameTaken(username)) {
                    login(username, password);
                } else {
                    Toast.makeText(LoginActivity.this, "Username doesn't exist. Please sign up.",Toast.LENGTH_LONG);
                }
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signupButton.setVisibility(View.GONE);
                logo.setBackgroundColor(Color.TRANSPARENT);
                logo.setImageBitmap(getRandomPic());
                changeIconButton.setVisibility(View.VISIBLE);
                emailField.setVisibility(View.VISIBLE);
                signinButton.setText("Sign up");
                signinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String username = usernameField.getText().toString();
                        final String password = passwordField.getText().toString();
                        final String email = emailField.getText().toString();
                        signup(username, email, password);
                    }
                });
            }
        });

        changeIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
    }

    private boolean usernameTaken(String username) {
        ParseQuery<ParseUser> newUserQuery = ParseUser.getQuery();
        newUserQuery.whereEqualTo("username", username);
        try {
            return newUserQuery.count() > 0;
        } catch (Exception e) {
            return false;
        }
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
                if (parseUser != null) {
                    startMapActivity();
                } else {
                    Log.e("Login Error", e.getLocalizedMessage());
                    Toast.makeText(LoginActivity.this, "Incorrect password.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void signup(final String username, final String email, final String password) {
        if (usernameTaken(username)) {
            Toast.makeText(LoginActivity.this, "Username taken. Please enter a different username.",Toast.LENGTH_LONG);
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
                // TODO: ONBOARD
                startMapActivity();
            }
        });
    }

    private void FBLogin() {
        List<String> permissions = new ArrayList<>(2);
        permissions.add("public_profile");
        permissions.add("user_friends");
        ParseFacebookUtils.logInWithReadPermissionsInBackground(this, permissions, new LogInCallback() {
            @Override
            public void done(final ParseUser user, ParseException err) {
                if (user == null) {
                    Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                    Log.d("parserr", err.toString());
                } else {
                    Log.d("User", user.toString());
                    Log.d("Username", user.getUsername());
                    if (user.isNew()) {
                        user.put("status", "Just a man and his thoughts");
                        user.put("visible", true);
                        Log.d("userIsNew", "user is new");
                    }
                    if (user.getString("displayName") == null) {
                        new GraphRequest(
                                AccessToken.getCurrentAccessToken(), "/me", null, HttpMethod.GET,
                                new GraphRequest.Callback() {
                                    @Override
                                    public void onCompleted(GraphResponse graphResponse) {
                                        try {
                                            Log.d("graphResponse", graphResponse.toString());
                                            JSONObject obj = graphResponse.getJSONObject();
                                            Log.d("graphResponseId", obj.getString("id"));
                                            Log.d("graphResponseFirstName", obj.getString("first_name"));
                                            String facebookId = obj.getString("id");
                                            user.put("username", facebookId);
                                            user.put("displayName", obj.getString("first_name"));
                                            String picurl = "https://graph.facebook.com/";
                                            picurl += (facebookId + "/picture?width=200&height=200");
                                            user.put("picURL", picurl);
                                        } catch (Exception e) {
                                        }
                                    }
                                }

                        ).executeAsync();
                    }
                    user.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            //link parse user to fb user
                            final ParseUser user = ParseUser.getCurrentUser();
                            if (!ParseFacebookUtils.isLinked(user)) {
                                ParseFacebookUtils.linkWithReadPermissionsInBackground(user, LoginActivity.this, null, new SaveCallback() {
                                    @Override
                                    public void done(ParseException ex) {
                                        if (ParseFacebookUtils.isLinked(user)) {
                                            Log.d("shoutout", "linked user");
                                        }
                                    }
                                });
                            }
                            startMapActivity();
                        }
                    });
                }
            }
        });
    }

    private void startMapActivity() {
        Intent intent = new Intent(this, MyMapActivity.class);
        startActivity(intent);
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
        int size = (int) getResources().getDimension(R.dimen.login_screen_pic_size);
        Bitmap thumbnailCropped = Bitmap.createScaledBitmap(thumbnail, size, size, false);
        thumbnail.recycle();
        logo.setImageBitmap(thumbnailCropped);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnailCropped.compress(Bitmap.CompressFormat.PNG, 100, stream);
        ParseFile imageFile = new ParseFile("userpic.png", stream.toByteArray());
        userImageObj = new ParseObject("Images");
        userImageObj.put("image", imageFile);
        userImageObj.saveInBackground();
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
