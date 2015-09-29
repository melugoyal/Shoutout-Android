package shoutout2.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.androidmapsextensions.ClusteringSettings;
import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.androidmapsextensions.SupportMapFragment;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.parse.CountCallback;
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


public class MyMapActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.InfoWindowAdapter {

    private GoogleMap map;
    private Map<String, Marker> markers;
    private Map<String, Person> people;
    private GoogleApiClient mLocationClient;
    private LocationRequest mLocationRequest;
    private static final int UPDATE_INTERVAL = 5;
    private static final int MIN_ZOOM = 3;
    private final Firebase ref = new Firebase("https://shoutout.firebaseIO.com/");
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
    private static final int SHOUTOUT_SLIDE_OFFSET = 500;
    private static ToggleButton mSwitch;
    private static Button updateStatus;
    private static EditText mEdit;
    private static ImageButton updateShoutButton;
    private static ImageButton mButton;
    private ListView messagesListView;
    private ImageButton messageButton;
    private MessageNumCircle messageNumCircle;

    public class Person {
        public final ParseUser parseUser;
        public Bitmap icon;
        public Bitmap activeIcon;
        public Person(ParseUser user) {
            parseUser = user;
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
        markers = new HashMap<>();
        people = new HashMap<>();
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getExtendedMap();
        map.setOnMarkerClickListener(this);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMyLocationEnabled(false);
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
                ParseGeoPoint currLoc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currLoc.getLatitude(), currLoc.getLongitude()), zoomlevel));
                makeMarkerActive(ParseUser.getCurrentUser().getObjectId());
            }
        });
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
                } catch (Exception e) {
                    Log.d("markerNotFoundError", "couldn't show info for center marker");
                }
            }
        });
        map.setClustering(new ClusteringSettings().clusterOptionsProvider(new MapClusteringOptions(getResources())).clusterSize(96.));

        final ParseGeoPoint currloc = ParseUser.getCurrentUser().getParseGeoPoint("geo");
        if (currloc != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currloc.getLatitude(), currloc.getLongitude()), 4));
        }

        messagesListView = (ListView)findViewById(R.id.messages_list);
        initMessageButton();

        // wait for map to show everyone's markers
        try {
            final Handler handler = new Handler();
            Thread th = new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("PARSEOBJECTID", ParseUser.getCurrentUser().getObjectId());
                        while (!markers.containsKey(ParseUser.getCurrentUser().getObjectId()));
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
        mButton = (ImageButton) findViewById(R.id.imagebutton);
        updateShoutButton = (ImageButton) findViewById(R.id.update_shout_button);
        mEdit = (EditText) findViewById(R.id.changeStatus);
        updateStatus = (Button) findViewById(R.id.updateStatusButton);
        mSwitch = (ToggleButton) findViewById(R.id.switch1);

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
                updateStatus();
            }
        });

        // live-update data location and status information from firebase

        refStatus.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {

            }

            @Override
            public void onChildChanged(final DataSnapshot snapshot, String previousChildName) {
                final String status = snapshot.child("status").getValue().toString();
                final String userId = snapshot.getName();
                Log.d("FirebaseChildChanged", "found marker");
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", userId);
                query.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        ParseGeoPoint geoPoint = parseUser.getParseGeoPoint("geo");
                        people.get(userId).activeIcon = null;
                        getActiveMarker(geoPoint, parseUser, status);
                        // wait for the active marker to be updated
                        try {
                            final Handler handler = new Handler();
                            Thread th = new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        while (people.get(userId).activeIcon == null);
                                        handler.post(new Runnable() {
                                            public void run() {
                                                markers.get(userId).setIcon(BitmapDescriptorFactory.fromBitmap(people.get(userId).activeIcon));
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
                final Marker marker = markers.get(snapshot.getName());
                final double newlat = Double.parseDouble(snapshot.child("lat").getValue().toString());
                final double newlong = Double.parseDouble(snapshot.child("long").getValue().toString());
                if (marker != null) {
                    marker.animatePosition(new LatLng(newlat, newlong));
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

    protected void updateStatus(String... statusParam) {
        mSwitch.setChecked(ParseUser.getCurrentUser().getBoolean("visible"));
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!slideUpVisible) {
            mButton.setBackgroundColor(0xaa0088ff);
            if (!firstTime) {
                mButton.setPadding(0, SHOUTOUT_SLIDE_OFFSET, 0, 0);
                firstTime = true;
            } else {
                mButton.setY(mButton.getY() + SHOUTOUT_SLIDE_OFFSET);
            }
            mEdit.setText("");
            mEdit.append(statusParam.length > 0 ? statusParam[0] : ParseUser.getCurrentUser().getString("status"));
            mEdit.setVisibility(View.VISIBLE);
            mEdit.requestFocus();
            mgr.showSoftInput(mEdit, InputMethodManager.SHOW_IMPLICIT);
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
            mButton.setY(mButton.getY() - SHOUTOUT_SLIDE_OFFSET);
            mEdit.setVisibility(View.INVISIBLE);
            mSwitch.setVisibility(View.INVISIBLE);
            updateStatus.setVisibility(View.INVISIBLE);
            mgr.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
        }
        slideUpVisible = !slideUpVisible;
    }

    private void initMessageButton() {
        messageButton = (ImageButton) findViewById(R.id.message_button);
        messageNumCircle = new MessageNumCircle(getResources(), (ImageView) findViewById(R.id.red_circle));
        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (messagesListView.getVisibility() == View.VISIBLE) {
                    messagesListView.setVisibility(View.INVISIBLE);
                } else {
                    initMessagesView();
                }
            }
        });
        updateMessageButton();
    }

    private void updateMessageButton() {
        final ParseQuery<ParseObject> messageQuery = new ParseQuery<ParseObject>("Messages");
        messageQuery.whereEqualTo("to", ParseUser.getCurrentUser());
        messageQuery.whereEqualTo("read", false);
        messageQuery.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                messageNumCircle.setCircle(count);
            }
        });
    }

    private void initMessagesView() {
        ParseQuery<ParseObject> messageQuery = new ParseQuery<ParseObject>("Messages");
        messageQuery.whereEqualTo("to", ParseUser.getCurrentUser());
        messageQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messageList, ParseException e) {
                messageList.add(0, new ParseObject("Messages")); // dummy parse object for header
                ArrayAdapter<ParseObject> adapter = new MessageArrayAdapter<ParseObject>(MyMapActivity.this, R.layout.messages_view, R.id.label, messageList, people, messageButton);
                messagesListView.setAdapter(adapter);
                messagesListView.setVisibility(View.VISIBLE);
            }
        });
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != 0) {
                    ParseObject item = (ParseObject) messagesListView.getAdapter().getItem(i);
                    try {
                        updateStatus("@" + item.getParseUser("from").fetchIfNeeded().getUsername() + " ");
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void hideMessageListView() {
        messagesListView.setVisibility(View.INVISIBLE);
        updateMessageButton();
    }

    private static void checkStatusForMessage(final String status) {
        for (String word : status.split("[^a-zA-Z\\d@]")) { // split on all characters except letters, numbers and @
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
                        message.put("read", false);
                        message.put("message", status);
                        message.saveInBackground();
                    }
                });
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker != null) {
            makeMarkerActive(marker.getTitle());
        }
        return true;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return MyMapActivity.this.getLayoutInflater().inflate(R.layout.no_info_window, null);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    private void makeMarkerActive(String userId) {
        for (Marker marker : markers.values()) {
            if (marker.getTitle().equals(userId)) {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(people.get(userId).activeIcon));
                marker.showInfoWindow();
            }
            else {
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(people.get(marker.getTitle()).icon));
            }
        }
    }

    private void hideUserMarkers(String userId) {
        markers.get(userId).setVisible(false);
    }

    @Override
    public void onLocationChanged(Location mCurrentLocation) {
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
            hideMessageListView();
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
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL);
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);
        map.clear();
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
                    for (int i = objectList.size() - 1; i >= 0; i--) {
                        ParseGeoPoint geoloc = objectList.get(i).getParseGeoPoint("geo");
                        people.put(objectList.get(i).getObjectId(), new Person(objectList.get(i)));
                        getActiveMarker(geoloc, objectList.get(i));
                        getInactiveMarker(geoloc, objectList.get(i));
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("google maps", "GoogleApiClient connection has been suspend");
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

    private String findClosestMarker(double latitude, double longitude) { // returns the userId for the closest marker
        float minDistance = Float.MAX_VALUE;
        String key = "";
        for (Marker marker : markers.values()) {
            double markerLat = marker.getPosition().latitude;
            double markerLong = marker.getPosition().longitude;
            float[] results = new float[3];
            Location.distanceBetween(latitude, longitude, markerLat, markerLong, results);
            if (results[0] < minDistance) {
                minDistance = results[0];
                key = marker.getTitle();
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
        final Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.shout_bubble_active);
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
                        final Bitmap activeBackground = background.copy(Bitmap.Config.ARGB_8888, true);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                placePicInBitmap(mIcon1, activeBackground);
                                int width = (int) getResources().getDimension(R.dimen.active_bubble_width);
                                int height = (int) getResources().getDimension(R.dimen.active_bubble_height);
                                Bitmap markerIcon = Bitmap.createScaledBitmap(activeBackground, width, height, false);
                                writeTextInBubble(markerIcon, status, displayName);
//                                Marker newmark = map.addMarker(new MarkerOptions().position(new LatLng(geoloc.getLatitude(), geoloc.getLongitude()))
//                                        .visible(false)
//                                        .anchor(0.0f, 1.0f)
//                                        .icon(BitmapDescriptorFactory.fromBitmap(markerIcon)));
                                people.get(user.getObjectId()).activeIcon = markerIcon;
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
        paint.setTextSize(getResources().getDimension(R.dimen.status_text_size));
        int y = (int)(getResources().getDimension(R.dimen.status_text_top_padding));
        float x = getResources().getDimension(R.dimen.status_text_left_padding);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(displayName, x, y, paint);
        y += paint.descent() - paint.ascent();

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        int lineNum = 1;
        for (String line : insertStatusNewlines(status).split("\n")) {
            if (lineNum++ > 3) { // only print the first three lines of the status
                break;
            }
            canvas.drawText(line, x, y, paint);
            y += paint.descent() - paint.ascent();
        }
    }

    private void getInactiveMarker(final ParseGeoPoint geoloc, final ParseUser user) {
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
                        final Bitmap iconBackground = background.copy(Bitmap.Config.ARGB_8888, true);
                        handler.post(new Runnable() {
                            public void run() {
                                placePicInBitmap(mIcon1, iconBackground);
                                int width = (int) getResources().getDimension(R.dimen.inactive_bubble_width);
                                int height = (int) getResources().getDimension(R.dimen.inactive_bubble_height);
                                Bitmap markerIcon = Bitmap.createScaledBitmap(iconBackground, width, height, false);
                                Marker newmark = map.addMarker(new MarkerOptions().position(new LatLng(geoloc.getLatitude(), geoloc.getLongitude()))
                                        .anchor(0.0f,1.0f)
                                        .title(user.getObjectId())
                                        .draggable(true)
                                        .icon(BitmapDescriptorFactory.fromBitmap(markerIcon)));
                                if (!user.getBoolean("visible")) {
                                    newmark.setVisible(false);
                                }
                                people.get(user.getObjectId()).icon = markerIcon;

                                if (!user.getBoolean("visible")) {
                                    newmark.setVisible(false);
                                }
                                markers.put(user.getObjectId(), newmark);
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

    private void placePicInBitmap(Bitmap mIcon1, Bitmap iconBackground) {
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
