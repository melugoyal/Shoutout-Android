package shoutout2.app;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

import shoutout2.app.Utils.FirebaseUtils;

public class BackgroundLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final int UPDATE_INTERVAL = 1000 * 60 * 30;
    protected GoogleApiClient mLocationClient;
    public static LocationRequest mLocationRequest;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL);
        mLocationClient.connect();
        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDestroy() {
        if (mLocationClient != null) {
            mLocationClient.disconnect();
            LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, this);
            mLocationClient = null;
        }
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (ParseUser.getCurrentUser() != null) {
            ParseUser.getCurrentUser().put("geo", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));
            ParseUser.getCurrentUser().saveInBackground();
            FirebaseUtils.updateLocation(location);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        if (connectionResult.hasResolution()) {
//            try {
//                // Start an Activity that tries to resolve the error
//                connectionResult.startResolutionForResult(
//                        getApplication(),
//                        1);
//                /*
//                 * Thrown if Google Play services canceled the original
//                 * PendingIntent
//                 */
//            } catch (IntentSender.SendIntentException e) {
//                // Log the error
//                e.printStackTrace();
//            }
//        } else {
//            /*
//             * If no resolution is available, display a dialog to the
//             * user with the error.
//             */
//        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
