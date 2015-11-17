package shoutout2.app.MapView;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import shoutout2.app.R;
import shoutout2.app.Utils.FirebaseUtils;

public class DisabledAppFragment extends Fragment {
    public static final String TAG = "disabled_app_fragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.disabled_app_screen, container, false);
        final Button button = (Button) view.findViewById(R.id.enable_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUtils.setPrivacy(true);
                ParseUser.getCurrentUser().put("visible", true);
                ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        DisabledAppFragment.this.getActivity().onBackPressed();
                    }
                });
            }
        });
        return view;
    }
}
