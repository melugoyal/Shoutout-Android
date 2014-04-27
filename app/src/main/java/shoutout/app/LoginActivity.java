package shoutout.app;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Parse.initialize(this, "S5HVjNqmiwUgiGjMDiJLYh361p5P7Ob3fCOabrJ9", "3GWNcqZ7LJhBtGbbmQfs0ROHKFM5sX6GDT9IWhCk");
        ParseFacebookUtils.initialize("213646248845245");
        super.onCreate(savedInstanceState);
        if (ParseUser.getCurrentUser() != null)
            startActivity(new Intent(this, MyMapActivity.class));
        setContentView(R.layout.activity_login);
        ParseFacebookUtils.logIn(this, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user == null) {
                    Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                //} else if (user.isNew()) {
                  //  Log.d("MyApp", "User signed up and logged in through Facebook!");
                } else {
                    link(user);
                    //Log.d("MyApp", "User logged in through Facebook!");

                }
            }
        });
    }

    protected void link(final ParseUser user){
        if (!ParseFacebookUtils.isLinked(user)) {
            ParseFacebookUtils.link(user, this, new SaveCallback() {
                @Override
                public void done(ParseException ex) {
                    if (ParseFacebookUtils.isLinked(user)) {
                        //Log.d("MyApp", "Woohoo, user logged in with Facebook!");
                        Intent intent = new Intent(LoginActivity.this, MyMapActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                    }
                }
            });
        }
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
        ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
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

}
