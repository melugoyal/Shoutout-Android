package shoutout.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
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
import com.parse.ParseFacebookUtils;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioApiCallback;
import com.rdio.android.api.RdioListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MyMapActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        RdioListener {
    private GoogleMap map;
    private Map<String, Marker> dict; // Parse user id -> marker
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private static final int UPDATE_INTERVAL = 5;
    private Location mCurrentLocation;
    private final Firebase refStatus = new Firebase("https://shoutout.firebaseIO.com/status");
    private final Firebase refLoc = new Firebase("https://shoutout.firebaseIO.com/loc");
    private Marker lastOpen;
    private MediaPlayer player;
    private String rdioAppKey = "thrhvh2bkpy5devcntw4qat6";
    private String rdioAppSecret = "Nrzm8K5G4m";
    private static Rdio rdio;
    private static boolean slideUpVisible = false;
    private static boolean firstTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_map);

        //link parse user to fb user
        final ParseUser user = ParseUser.getCurrentUser();
        if (!ParseFacebookUtils.isLinked(user)) {
            ParseFacebookUtils.link(user, this, new SaveCallback() {
                @Override
                public void done(ParseException ex) {
                    if (ParseFacebookUtils.isLinked(user)) {
                        Log.d("shoutout", "linked user");
                    }
                }
            });
        }
        rdio = new Rdio(rdioAppKey, rdioAppSecret, null, null, this, this);
        rdio.prepareForPlayback();
        dict = new HashMap<String, Marker>();
        mLocationClient = new LocationClient(this, this, this);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL);

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();
        map.setOnInfoWindowClickListener(this);
        map.setOnMarkerClickListener(this);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);

        // wait for map to show our own marker, then display our status
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        while (!dict.containsKey(ParseUser.getCurrentUser().getObjectId()));
                        handler.post(new Runnable() {
                            public void run() {
                                if (ParseUser.getCurrentUser().getBoolean("visible"))
                                    dict.get(ParseUser.getCurrentUser().getObjectId()).showInfoWindow();
                                findViewById(R.id.loading).setVisibility(View.INVISIBLE);
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
        final ImageButton mButton = (ImageButton)findViewById(R.id.imagebutton);
        final EditText mEdit = (EditText)findViewById(R.id.changeStatus);
        final Switch mSwitch = (Switch)findViewById(R.id.switch1);
        final Firebase ref = new Firebase("https://shoutout.firebaseIO.com/");
        final int offset = 350;
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!slideUpVisible) {
                    mButton.setBackgroundColor(0xaa0088ff);
                    if (!firstTime) {
                        mButton.setPadding(0,offset,0,0);
                        firstTime = true;
                    }
                    else
                        mButton.setY(mButton.getY() + offset);
                    mEdit.requestFocus();
                    mEdit.setText(ParseUser.getCurrentUser().getString("status"));
                    mEdit.setVisibility(View.VISIBLE);
                    mSwitch.setVisibility(View.VISIBLE);
                }
                else {
                    boolean privacy = mSwitch.isChecked();
                    ParseUser.getCurrentUser().put("status", mEdit.getText().toString());
                    ParseUser.getCurrentUser().saveInBackground();
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
                    mButton.setBackgroundColor(0x55006666);
                    mButton.setY(mButton.getY()-offset);
                    mEdit.setVisibility(View.INVISIBLE);
                    mSwitch.setVisibility(View.INVISIBLE);
                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
                }
                slideUpVisible = !slideUpVisible;
            }
        });

        // live-update data location and status information from firebase

        refStatus.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                Marker marker = dict.get(snapshot.getName());
                if (marker == null)
                    return;
                marker.hideInfoWindow();
                marker.setSnippet(snapshot.child("status").getValue().toString());
                marker.showInfoWindow();
                if (snapshot.child("privacy").getValue().toString().equals("NO"))
                    marker.setVisible(false);
                else if (snapshot.child("privacy").getValue().toString().equals("YES")) {
                    marker.setVisible(true);
                    marker.showInfoWindow();
                }
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


        refLoc.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                Marker marker = dict.get(snapshot.getName());
                double newlat = Double.parseDouble(snapshot.child("lat").getValue().toString());
                double newlong = Double.parseDouble(snapshot.child("long").getValue().toString());
                if (marker != null) {
                    marker.hideInfoWindow();
                    marker.setPosition(new LatLng(newlat, newlong));
                    marker.showInfoWindow();
                }
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

    @Override
    public void onLocationChanged(Location mCurrentLocation) {
        this.mCurrentLocation = mCurrentLocation;
        ParseUser.getCurrentUser().put("geo", new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
        ParseUser.getCurrentUser().saveInBackground();
        refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("lat").setValue(mCurrentLocation.getLatitude());
        refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("long").setValue(mCurrentLocation.getLongitude());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        rdio.cleanup();
        if (player != null) {
            player.reset();
            player.release();
            player = null;
        }
        super.onDestroy();
    }
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // update the user's location on firebase and parse
        mCurrentLocation = mLocationClient.getLastLocation();
        if (mCurrentLocation != null) {
            ParseUser.getCurrentUser().put("geo", new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
            refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("lat").setValue(mCurrentLocation.getLatitude());
            refLoc.child(ParseUser.getCurrentUser().getObjectId()).child("long").setValue(mCurrentLocation.getLongitude());
        }
        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                ParseGeoPoint currloc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
                if (currloc != null)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 4));
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereExists("geo");
                query.findInBackground(new FindCallback<ParseUser>() {
                    public void done(List<ParseUser> objectList, ParseException e) {
                        if (e == null) {
                            for (int i = 0; i < objectList.size(); i++) {
                                ParseGeoPoint geoloc = objectList.get(i).getParseGeoPoint("geo");
                                getBMP(geoloc, objectList.get(i));
                            }
                        }
                    }
                });
            }
        });
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }
    /*
    * Called by Location Services if the attempt to
    * Location Services fails.
    */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        1);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            AlertDialog alertDialog = new AlertDialog.Builder(MyMapActivity.this).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage(Integer.toString(connectionResult.getErrorCode()));
            alertDialog.show();
        }
    }

    @Override
    public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
        return;
    }

    @Override
    public void onRdioReadyForPlayback() {
        return;
    }

    @Override
    public void onRdioUserPlayingElsewhere() {
        return;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (marker.getSnippet().toLowerCase().contains("listening to ")) {
            // play rdio snippet
            List<NameValuePair> getArgs = new LinkedList<NameValuePair>();
            getArgs.add(new BasicNameValuePair("query", marker.getSnippet().substring(13)));
            rdio.apiCall("searchSuggestions", getArgs, new RdioApiCallback() {
                @Override
                public void onApiFailure(String s, Exception e) {
                    // do nothing
                }
                @Override
                public void onApiSuccess(JSONObject jsonObject) {
                    try {
                        final JSONArray result = jsonObject.getJSONArray("result");
                        try {
                            final Handler handler = new Handler();
                            Thread th = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        for (int i = 0; i < result.length(); i++) {
                                            player = rdio.getPlayerForTrack(result.getJSONObject(i).getString("key"), null, true);
                                            if (result.getJSONObject(i).getString("type").equals("t"))
                                                break;
                                        }
                                        player.prepare();

                                        handler.post(new Runnable() {
                                            public void run() {
                                                player.start();
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
                    catch (Exception e) {
                        // do nothing
                    }
                }
            });
        }
        // open sms intent
        String userid = "";
        for (Map.Entry entry : dict.entrySet()) {
            if (entry.getValue().equals(marker)) {
                userid = entry.getKey().toString();
                break;
            }
        }
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("objectId", userid);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> objectList, ParseException e) {
                if (e == null) {
                    String phone = objectList.get(0).getString("phone");
                    if (phone == null || phone.isEmpty())
                        return;
                    phone = "smsto:" + phone;
                    Intent sendIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(phone));
                    sendIntent.putExtra("exit_on_sent", true);
                    startActivity(sendIntent);
                }
            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (lastOpen != null) {
            lastOpen.hideInfoWindow();
            if (lastOpen.equals(marker)) {
                lastOpen = null;
                return true;
            }
        }
        marker.showInfoWindow();
        lastOpen = marker;
        return true;
    }

    private void getBMP(final ParseGeoPoint geoloc, final ParseUser user) {
        try {
            Log.d("username", user.getUsername());
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    URL url = null;
                    InputStream content = null;
                    try {
                        String userpic = user.getString("picURL");
                        //userpic = userpic.substring(0, userpic.indexOf('?'));
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
                                if (!user.getBoolean("visible")) {
                                    newmark.setVisible(false);
                                }
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
