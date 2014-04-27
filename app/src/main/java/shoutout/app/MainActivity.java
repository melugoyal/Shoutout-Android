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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Firebase ref = new Firebase("https://shoutout.firebaseIO.com/");
        mButton = (Button)findViewById(R.id.button1);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", ParseUser.getCurrentUser().getObjectId());
                // Retrieve the object by id
                query.findInBackground(new FindCallback<ParseUser>() {
                    public void done(List<ParseUser> objectList, ParseException e) {
                        if (e == null) {
                            EditText mEdit = (EditText)findViewById(R.id.editText1);
                            //ParseObject statusObject = new ParseObject("StatusObject");
                            objectList.get(0).put("status", mEdit.getText().toString());
                            //             Location mCurrentLocation = LocationClient.getLastLocation();
                            objectList.get(0).put("geo", new ParseGeoPoint(40.1106,-88.24));
                            objectList.get(0).saveInBackground();
                            ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("status").setValue(mEdit.getText().toString());
                            ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue("YES");
                            ref.child("loc").child(ParseUser.getCurrentUser().getObjectId()).child("lat").setValue("40.1106");
                            ref.child("loc").child(ParseUser.getCurrentUser().getObjectId()).child("long").setValue("-88.24");
                        }
                    }
                });


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
