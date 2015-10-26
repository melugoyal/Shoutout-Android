package shoutout2.app.Login;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import shoutout2.app.R;
import shoutout2.app.Utils.Utils;

public class LandingFragment extends Fragment {

    public static final String TAG = "landing_fragment_tag";
    private ViewPager pager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.first_screen, container, false);

        Button loginButton = (Button) view.findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                Fragment fragment = fragmentManager.findFragmentByTag(LoginFragment.TAG);

                if (fragment == null) {
                    fragment = new LoginFragment();
                }

                Utils.replaceFragment(fragmentManager, R.id.login_activity_container, LoginFragment.TAG, fragment);
            }
        });

        Button signupButton = (Button) view.findViewById(R.id.signup_button);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                Fragment fragment = fragmentManager.findFragmentByTag(SignupFragment.TAG);

                if (fragment == null) {
                    fragment = new SignupFragment();
                }

                Utils.replaceFragment(fragmentManager, R.id.login_activity_container, SignupFragment.TAG, fragment);
            }
        });

        pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(new ScreenSlidePagerAdapter(((FragmentActivity) getActivity()).getSupportFragmentManager()));
        pager.setOffscreenPageLimit(4);
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (pager.getCurrentItem() == 0) {
                        // If the user is currently looking at the first step, allow the system to handle the
                        // Back button. This calls finish() on this activity and pops the back stack.
                        getActivity().onBackPressed();
                    } else {
                        // Otherwise, select the previous step.
                        pager.setCurrentItem(pager.getCurrentItem() - 1);
                    }
                    return true;
                }
                return false;
            }
        });
        return view;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            android.support.v4.app.Fragment f = new ScreenSlideFragment();
            Bundle args = new Bundle();
            args.putInt("imageId", getResources().getIdentifier("slider_image_" + (position + 1), "drawable", getActivity().getPackageName()));
            f.setArguments(args);
            return f;
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}

