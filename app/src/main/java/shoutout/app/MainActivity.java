package shoutout.app;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.firebase.client.Firebase;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class MainActivity extends Activity {
    Button mButton;
    Switch mSwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Firebase ref = new Firebase("https://shoutout.firebaseIO.com/");
        mButton = (Button)findViewById(R.id.button1);
        mSwitch = (Switch)findViewById(R.id.switch1);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                boolean privacy = mSwitch.isChecked();
                EditText mEdit = (EditText)findViewById(R.id.editText1);
                ParseUser.getCurrentUser().put("status", mEdit.getText().toString());
                ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("status").setValue(mEdit.getText().toString());
                if (privacy) {
                    ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue("NO");
                    ParseUser.getCurrentUser().put("visible", false);
                }
                else {
                    ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue("YES");
                    ParseUser.getCurrentUser().put("visible", true);
                }
                ParseUser.getCurrentUser().saveInBackground();
                startActivity(new Intent(MainActivity.this, MyMapActivity.class));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
