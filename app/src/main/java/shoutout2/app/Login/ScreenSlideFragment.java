package shoutout2.app.Login;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import shoutout2.app.R;

public class ScreenSlideFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ImageView rootView = (ImageView) inflater.inflate(
                R.layout.screen_slide, container, false);
        rootView.setBackgroundResource(getArguments().getInt("imageId"));
        return rootView;
    }
}
