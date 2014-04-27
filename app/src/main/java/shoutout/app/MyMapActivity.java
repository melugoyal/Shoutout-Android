package shoutout.app;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyMapActivity extends Activity {
    private GoogleMap map;
    private Context context;
    private Map<String, Marker> dict; // Parse user id -> marker

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_map);
        dict = new HashMap<String, Marker>();
        context = this.getApplicationContext();
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();

        // show the existing Parse data on the map
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereExists("geo");
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> objectList, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < objectList.size(); i++) {
                        ParseGeoPoint geoloc = objectList.get(i).getParseGeoPoint("geo");
                        getBMP(i, geoloc, objectList.get(i));
                    }
                }
            }
        });
        ParseGeoPoint currloc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 4));
        // update the user's location on firebase and parse

        // live-update data location and status information from firebase
        final Firebase refStatus = new Firebase("https://shoutout.firebaseIO.com/status");
        refStatus.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                Marker marker = dict.get(snapshot.getName());
                marker.hideInfoWindow();
                marker.setSnippet(snapshot.child("status").getValue().toString());
                marker.showInfoWindow();
                if (snapshot.child("privacy").getValue().toString().equals("NO"))
                    marker.hideInfoWindow();
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(FirebaseError err) {

            }
        });

        final Firebase refLoc = new Firebase("https://shoutout.firebaseIO.com/loc");
        refLoc.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                Marker marker = dict.get(snapshot.getName());
                double newlat = Double.parseDouble(snapshot.child("lat").getValue().toString());
                double newlong = Double.parseDouble(snapshot.child("long").getValue().toString());
                marker.hideInfoWindow();
                marker.setPosition(new LatLng(newlat, newlong));
                marker.showInfoWindow();
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onCancelled(FirebaseError err) {

            }
        });
    }

    private void getBMP(final int i, final ParseGeoPoint geoloc, final ParseUser user) {
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    URL url = null;
                    InputStream content = null;
                    try {
                        String userpic = user.getString("picURL");
                        userpic = userpic.substring(0, userpic.indexOf('?'));
                        url = new URL(userpic);
                        content = (InputStream)url.getContent();
                        Drawable d = Drawable.createFromStream(content, "src");
                        final Bitmap mIcon1 = BitmapFactory.decodeStream(url.openConnection().getInputStream());;
                        handler.post(new Runnable() {
                            public void run() {
                                Marker newmark = map.addMarker(new MarkerOptions().position(new LatLng(geoloc.getLatitude(), geoloc.getLongitude()))
                                        .title(user.getUsername())
                                        .snippet(user.getString("status"))
                                        .icon(BitmapDescriptorFactory.fromBitmap(mIcon1)));
                                dict.put(user.getObjectId(), newmark);
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        handler.post(new Runnable() {

                            public void run() {
                                Log.e("error", "error");
                            }
                        });
                    }


                }
            });
            th.start();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_map, menu);
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
