package shoutout2.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkLocationServices()) {
            return;
        }
        FacebookSdk.sdkInitialize(getApplicationContext());
        Parse.initialize(this, Keys.PARSE_APP_ID, Keys.PARSE_CLIENT_KEY);
        ParseFacebookUtils.initialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null && ParseFacebookUtils.isLinked(currentUser)) {
            startMapActivity();
        }
        FBLogin();
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
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
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

    private boolean checkLocationServices() {
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
}
