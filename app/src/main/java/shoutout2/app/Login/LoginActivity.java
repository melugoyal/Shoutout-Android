package shoutout2.app.Login;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseUser;

import shoutout2.app.Keys;
import shoutout2.app.Permissions;
import shoutout2.app.R;
import shoutout2.app.Utils.Utils;


public class LoginActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkLocationAndWifi()) {
            return;
        }

        new Permissions(this);

        try {
            Parse.initialize(this, Keys.PARSE_APP_ID, Keys.PARSE_CLIENT_KEY);
        } catch (Exception e) {
            Log.d("Parse already init", "assuming user logged out");
        }
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            Utils.startMapActivity(this);
            return;
        }

        setContentView(R.layout.login_activity);

        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(LandingFragment.TAG);

        if (fragment == null) {
            fragment = new LandingFragment();
        }

        Utils.addFragment(fragmentManager, R.id.login_activity_container, LandingFragment.TAG, fragment);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.login, menu);
//        return true;
//    }
//
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
    private boolean checkLocationAndWifi() {
//        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//        if (!mWifi.isConnected()) {
//            return false;
//        }

        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }
        if (!(gps_enabled && network_enabled)) {
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
