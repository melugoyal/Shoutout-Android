package shoutout2.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyMapActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.InfoWindowAdapter {
    private GoogleMap map;
    private Map<String, Marker> activeMarkers; // Parse user id -> marker
    private Map<String, Marker> inactiveMarkers; // Parse user id -> marker
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private static final int UPDATE_INTERVAL = 5;
    private static final int MIN_ZOOM = 3;
    private Location mCurrentLocation;
    private final Firebase refStatus = new Firebase("https://shoutout.firebaseIO.com/status");
    private final Firebase refLoc = new Firebase("https://shoutout.firebaseIO.com/loc");
    private static boolean slideUpVisible = false;
    private static boolean firstTime = false;
    private static float zoomlevel;
    private static double maplat;
    private static double maplong;
    private static boolean firstConnect = true;
    private static int picSize = -1;
    private static int picPadding = -1;
    private static final double BUBBLE_SCALE = 3./4;
    private static String idForActiveMarker = null;
    private ClusterManager<Person> clusterManager;

    public class Person implements ClusterItem {
        private final LatLng position;
        private final ParseUser parseUser;
        private Bitmap icon;
        private Bitmap activeIcon;
        public Person(ParseGeoPoint geoloc, ParseUser user) {
            position = new LatLng(geoloc.getLatitude(),geoloc.getLongitude());
            parseUser = user;
        }
        @Override
        public LatLng getPosition() {
            return position;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_map);

        //link parse user to fb user
        final ParseUser user = ParseUser.getCurrentUser();
        if (!ParseFacebookUtils.isLinked(user)) {
            ParseFacebookUtils.linkWithReadPermissionsInBackground(user, this, null, new SaveCallback() {
                @Override
                public void done(ParseException ex) {
                    if (ParseFacebookUtils.isLinked(user)) {
                        Log.d("shoutout", "linked user");
                    }
                }
            });
        }
        slideUpVisible = false;
        firstTime = false;
        activeMarkers = new HashMap<String, Marker>();
        inactiveMarkers = new HashMap<String, Marker>();
        mLocationClient = new LocationClient(this, this, this);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL);
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();
        final View mapView = findViewById(R.id.map);
        final View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        locationButton.setBackgroundColor(Color.TRANSPARENT);
        locationButton.setAlpha(0.0f); // hide the location button
        map.setOnMarkerClickListener(this);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.setMyLocationEnabled(true);
        map.setInfoWindowAdapter(this);
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                makeMarkerActive(""); // make all markers inactive
            }
        });

        ImageButton myLocationButton = (ImageButton) findViewById(R.id.my_location_button);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationButton.callOnClick();
            }
        });
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                makeMarkerActive(ParseUser.getCurrentUser().getObjectId());
                return false;
            }
        });
        clusterManager = new ClusterManager<Person>(this, map);
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                zoomlevel = cameraPosition.zoom;
                if (zoomlevel < MIN_ZOOM) {
                    map.animateCamera(CameraUpdateFactory.zoomTo(MIN_ZOOM));
                    zoomlevel = MIN_ZOOM;
                }
                maplat = cameraPosition.target.latitude;
                maplong = cameraPosition.target.longitude;
                try {
                    makeMarkerActive(findClosestMarker(maplat, maplong));
//                    clusterManager.onCameraChange(cameraPosition);
                } catch (Exception e) {
                    Log.d("markerNotFoundError", "couldn't show info for center marker");
                }
            }
        });

        final ParseGeoPoint currloc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
        if (currloc != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 4));
        }

        // wait for map to show our own marker, then display our status
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("PARSEOBJECTID", ParseUser.getCurrentUser().getObjectId());
                        while (!activeMarkers.containsKey(ParseUser.getCurrentUser().getObjectId()) || !inactiveMarkers.containsKey(ParseUser.getCurrentUser().getObjectId()));
                        handler.post(new Runnable() {
                            public void run() {
                                if (ParseUser.getCurrentUser().getBoolean("visible") && currloc != null) {
                                    makeMarkerActive(ParseUser.getCurrentUser().getObjectId());
                                }
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
        final ImageButton mButton = (ImageButton) findViewById(R.id.imagebutton);
        final ImageButton updateShoutButton = (ImageButton) findViewById(R.id.update_shout_button);
        final EditText mEdit = (EditText) findViewById(R.id.changeStatus);
        final Button updateStatus = (Button) findViewById(R.id.updateStatusButton);
        final ToggleButton mSwitch = (ToggleButton) findViewById(R.id.switch1);
        mSwitch.setChecked(ParseUser.getCurrentUser().getBoolean("visible"));
        final Firebase ref = new Firebase("https://shoutout.firebaseIO.com/");
        final int offset = 500;

        updateShoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mButton.callOnClick();
            }
        });
        updateStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton.callOnClick();
            }
        });
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!slideUpVisible) {
                    mButton.setBackgroundColor(0xaa0088ff);
                    if (!firstTime) {
                        mButton.setPadding(0, offset, 0, 0);
                        firstTime = true;
                    } else {
                        mButton.setY(mButton.getY() + offset);
                    }
                    mEdit.requestFocus();
                    mEdit.setText(ParseUser.getCurrentUser().getString("status"));
                    mEdit.setVisibility(View.VISIBLE);
                    mSwitch.setVisibility(View.VISIBLE);
                    updateStatus.setVisibility(View.VISIBLE);
                } else {
                    boolean privacy = mSwitch.isChecked();
                    String status = mEdit.getText().toString();
                    ParseUser.getCurrentUser().put("status", status);
                    ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("status").setValue(status);
                    if (privacy) {
                        ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue("YES");
                        ParseUser.getCurrentUser().put("visible", true);
                    } else {
                        ref.child("status").child(ParseUser.getCurrentUser().getObjectId()).child("privacy").setValue("NO");
                        ParseUser.getCurrentUser().put("visible", false);
                    }
                    ParseUser.getCurrentUser().saveInBackground();
                    checkStatusForMessage(status);
                    mButton.setBackgroundColor(0x55006666);
                    mButton.setY(mButton.getY() - offset);
                    mEdit.setVisibility(View.INVISIBLE);
                    mSwitch.setVisibility(View.INVISIBLE);
                    updateStatus.setVisibility(View.INVISIBLE);
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
            public void onChildChanged(final DataSnapshot snapshot, String previousChildName) {
                Log.d("FirebaseChildChanged", "change listener firebase");
                Log.d("FirebaseChildChanged", snapshot.child("status").getValue().toString());
                final String userId = snapshot.getName();
                Marker marker = activeMarkers.get(userId);
                if (marker == null) {
                    return;
                }
                Log.d("FirebaseChildChanged", "found marker");
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", userId);
                query.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        ParseGeoPoint geoPoint = parseUser.getParseGeoPoint("geo");
                        activeMarkers.remove(userId);
                        getActiveMarker(geoPoint, parseUser);
                        // wait for the active marker to be updated
                        try {
                            final Handler handler = new Handler();
                            Thread th = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        while (!activeMarkers.containsKey(userId));
                                        handler.post(new Runnable() {
                                            public void run() {
                                                if (snapshot.child("privacy").getValue().toString().equals("NO")) {
                                                    hideUserMarkers(userId);
                                                }
                                                else if (snapshot.child("privacy").getValue().toString().equals("YES")) {
                                                    makeMarkerActive(userId);
                                                }
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
                });
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
                final Marker marker = activeMarkers.get(snapshot.getName());
                final double newlat = Double.parseDouble(snapshot.child("lat").getValue().toString());
                final double newlong = Double.parseDouble(snapshot.child("long").getValue().toString());
                if (marker != null) {
                    final Handler handler = new Handler();
                    final long start = SystemClock.uptimeMillis();
                    Projection proj = map.getProjection();
                    Point startPoint = proj.toScreenLocation(marker.getPosition());
                    final LatLng startLatLng = proj.fromScreenLocation(startPoint);
                    final long duration = 500;

                    final Interpolator interpolator = new LinearInterpolator();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            long elapsed = SystemClock.uptimeMillis() - start;
                            float t = interpolator.getInterpolation((float) elapsed
                                    / duration);
                            double lng = t * newlong + (1 - t)
                                    * startLatLng.longitude;
                            double lat = t * newlat + (1 - t)
                                    * startLatLng.latitude;
                            marker.setPosition(new LatLng(lat, lng));

                            if (t < 1.0) {
                                // Post again 16ms later.
                                handler.postDelayed(this, 16);
                            } else {
                                marker.setVisible(true);
                            }
                        }
                    });
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

    private void checkStatusForMessage(final String status) {
        for (String word : status.split(" ")) {
            if (word.startsWith("@")) {
                String username = word.substring(1);
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("username", username);
                query.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        ParseObject message = new ParseObject("Messages");
                        message.put("from", ParseUser.getCurrentUser());
                        message.put("to", parseUser);
                        message.put("message", status);
                        message.saveInBackground();
                    }
                });
            }
        }
    }

    private void makeMarkerActive(String userId) {
        for (Map.Entry<String, Marker> entry : inactiveMarkers.entrySet()) {
            if (entry.getKey().equals(userId)) {
                entry.getValue().setVisible(false);
            }
            else {
                entry.getValue().setVisible(true);
            }
        }
        for (Map.Entry<String, Marker> entry : activeMarkers.entrySet()) {
            if (entry.getKey().equals(userId)) {
                entry.getValue().setVisible(true);
                entry.getValue().showInfoWindow();
            }
            else {
                entry.getValue().setVisible(false);
            }
        }
        idForActiveMarker = userId;
    }

    private void hideUserMarkers(String userId) {
        inactiveMarkers.get(userId).setVisible(false);
        activeMarkers.get(userId).setVisible(false);
        if (idForActiveMarker.equals(userId)) {
            idForActiveMarker = null;
        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return this.getLayoutInflater().inflate(R.layout.no_info_window, null);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        mLocationClient.disconnect();
        super.onStop();
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
                if (firstConnect) {
                    if (currloc != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 15));
                    }
                    firstConnect = false;
                } else {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(maplat, maplong), zoomlevel));
                }
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereWithinMiles("geo", currloc, 50);
                query.findInBackground(new FindCallback<ParseUser>() {
                    public void done(List<ParseUser> objectList, ParseException e) {
                        if (e == null) {
                            for (int i = 0; i < objectList.size(); i++) {
                                ParseGeoPoint geoloc = objectList.get(i).getParseGeoPoint("geo");
                                getInactiveMarker(geoloc, objectList.get(i));
                                getActiveMarker(geoloc, objectList.get(i));
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
    public boolean onMarkerClick(Marker marker) {
        for (Map.Entry entry : inactiveMarkers.entrySet()) {
            if (marker.equals(entry.getValue())) {
                makeMarkerActive((String)entry.getKey());
//                clusterManager.onMarkerClick(marker);
                return true;
            }
        }
        return true;
    }

    private String findClosestMarker(double latitude, double longitude) { // returns the userId for the closest marker
        float minDistance = Float.MAX_VALUE;
        String key = "";
        for (Map.Entry entry : inactiveMarkers.entrySet()) {
            Marker marker = (Marker) entry.getValue();
            double markerLat = marker.getPosition().latitude;
            double markerLong = marker.getPosition().longitude;
            float[] results = new float[3];
            Location.distanceBetween(latitude, longitude, markerLat, markerLong, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                key = entry.getKey().toString();
            }
        }
        return key;
    }

    // read the background image file to determine where to position the profile picture and the status text
    private void setPicSizeAndPadding(Bitmap background) {
        int firstColor = background.getPixel(0, 0);
        int yCenter = 0;
        for(int i = 0; i < background.getHeight(); i++) {
            if (background.getPixel(0,i) == firstColor) {
                continue;
            }
            int firstBlue = Color.blue(background.getPixel(0,i));
            for (int j = 0; j < background.getWidth(); j++) {
                if (Color.blue(background.getPixel(j,i)) == firstBlue) {
                    continue;
                }
                yCenter = i;
                picPadding = j;
                break;
            }
            break;
        }
        firstColor = background.getPixel(picPadding, yCenter);
        for (int i = picPadding+1; i < background.getWidth(); i++) {
            if (background.getPixel(i, yCenter) == firstColor) {
                continue;
            }
            picSize = i - picPadding;
            break;
        }
    }

    private Bitmap getCroppedBitmap(Bitmap sbmp) {
        Bitmap output = Bitmap.createBitmap(sbmp.getWidth(),
                sbmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, sbmp.getWidth(), sbmp.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(sbmp.getWidth() / 2f, sbmp.getHeight() / 2f,
                sbmp.getWidth() / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sbmp, rect, rect, paint);
        return output;
    }

    private void getActiveMarker(final ParseGeoPoint geoloc, final ParseUser user, final String... statusParam) {
        final Bitmap activeBackground = BitmapFactory.decodeResource(getResources(), R.drawable.shout_bubble_active);
        final String userpic = user.getString("picURL");
        final String status = statusParam.length > 0 ? statusParam[0] : user.getString("status");
        String display = user.getString("displayName");
        final String displayName = display == null ? user.getUsername() : display;
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    URL url = null;
                    try {
                        url = new URL(userpic);
                        final Bitmap mIcon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        final Bitmap mIcon1 = mIcon.copy(Bitmap.Config.ARGB_8888, true);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap activeIconBackground = placePicInBitmap(mIcon1, activeBackground);
                                writeTextInBubble(activeIconBackground, status, displayName);
                                Marker newmark = map.addMarker(new MarkerOptions().position(new LatLng(geoloc.getLatitude(), geoloc.getLongitude()))
                                        .visible(false)
                                        .anchor(0.0f, 1.0f)
                                        .icon(BitmapDescriptorFactory.fromBitmap(activeIconBackground)));
                                activeMarkers.put(user.getObjectId(), newmark);
                            }
                        });
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        getActiveMarker(geoloc, user);
                    }
                }
            });
            th.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            getActiveMarker(geoloc, user);
        }
    }

    private String insertStatusNewlines(String status) {
        final int status_line_length = 13;
        int len = status.length();
        for (int i = status_line_length; i < len; i += status_line_length) {
            int j;
            for (j = i; status.charAt(j) != ' '; j--) { // iterate backwards to find the beginning of the word, change space to newline
                if (j==0) {
                    return status;
                }
            }
            char[] newStatus = status.toCharArray();
            newStatus[j] = '\n';
            status = String.valueOf(newStatus);
        }
        return status;
    }

    private void writeTextInBubble(Bitmap activeIconBackground, String status, String displayName) {
        Canvas canvas = new Canvas(activeIconBackground);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        int y = picPadding * 3;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(displayName, picSize * 1.2f, y, paint);
        y += paint.descent() - paint.ascent();

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (String line : insertStatusNewlines(status).split("\n")) {
            canvas.drawText(line, picSize * 1.2f, y, paint);
            y += paint.descent() - paint.ascent();
        }
    }

    private void getInactiveMarker(final ParseGeoPoint geoloc, final ParseUser user) {
//        clusterManager.addItem(new Person(geoloc, user));
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    URL url = null;
                    try {
                        String userpic = user.getString("picURL");
                        url = new URL(userpic);
                        final Bitmap mIcon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        final Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.shout_bubble_inactive);
                        final Bitmap mIcon1 = mIcon.copy(Bitmap.Config.ARGB_8888, true);
                        handler.post(new Runnable() {
                            public void run() {
                                Bitmap iconBackground = placePicInBitmap(mIcon1, background);
                                Marker newmark = map.addMarker(new MarkerOptions().position(new LatLng(geoloc.getLatitude(), geoloc.getLongitude()))
                                        .anchor(0.0f,1.0f)
                                        .icon(BitmapDescriptorFactory.fromBitmap(iconBackground)));
                                if (!user.getBoolean("visible")) {
                                    newmark.setVisible(false);
                                }
                                inactiveMarkers.put(user.getObjectId(), newmark);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        getInactiveMarker(geoloc, user);
                    }
                }
            });
            th.start();
        } catch (Exception e1) {
            e1.printStackTrace();
            getInactiveMarker(geoloc, user);
        }
    }

    private Bitmap placePicInBitmap(Bitmap mIcon1, Bitmap background) {
        final Bitmap iconBackground = Bitmap.createScaledBitmap(background, (int)(background.getWidth()*BUBBLE_SCALE), (int)(background.getHeight()*BUBBLE_SCALE), false);
        if (picSize == -1 || picPadding == -1) {
            setPicSizeAndPadding(iconBackground);
        }
        Bitmap icon = Bitmap.createScaledBitmap(mIcon1, picSize, picSize, false);
        icon = getCroppedBitmap(icon);
        for (int i = 0; i < icon.getWidth(); i++) {
            for (int j = 0; j < icon.getHeight(); j++) {
                if (Math.pow(i - icon.getWidth() / 2, 2) + Math.pow(j - icon.getHeight() / 2, 2) < Math.pow(icon.getWidth() / 2, 2)) {
                    iconBackground.setPixel(Math.min(i + picPadding, iconBackground.getWidth() - 1), Math.min(j + picPadding, iconBackground.getHeight() - 1), icon.getPixel(i, j));
                }
            }
        }
        return iconBackground;
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
