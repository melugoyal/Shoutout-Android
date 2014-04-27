package shoutout.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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
        setContentView(R.layout.activity_login);
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null && ParseFacebookUtils.isLinked(currentUser))
            startMapActivity();
        FBLogin();
    }

    private void FBLogin() {
        ParseFacebookUtils.logIn(this, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException err) {
                if (user == null) {
                    Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                } else {
                    if (user.isNew())
                        user.put("status", "Just a man and his thoughts");
                    startMapActivity();
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
