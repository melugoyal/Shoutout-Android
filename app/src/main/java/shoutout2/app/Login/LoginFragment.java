package shoutout2.app.Login;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

import shoutout2.app.R;
import shoutout2.app.Utils.Utils;

public class LoginFragment extends Fragment {

    public static final String TAG = "login_fragment_tag";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.login_screen, container, false);
        Button backButton = (Button) v.findViewById(R.id.login_back_button);
        final Button nextButton = (Button) v.findViewById(R.id.login_next_button);
        final Button forgotPasswordButton = (Button) v.findViewById(R.id.forgot_password_button);
        final EditText usernameField = (EditText) v.findViewById(R.id.login_username);
        final EditText passwordField = (EditText) v.findViewById(R.id.login_password);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = usernameField.getText().toString();
                final String password = passwordField.getText().toString();
                if (Utils.usernameTaken(username)) {
                    login(username.toLowerCase(), password);
                } else {
                    Toast.makeText(getActivity(), "Username doesn't exist. Please sign up.", Toast.LENGTH_LONG).show();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usernameField.setVisibility(View.GONE);
                forgotPasswordButton.setVisibility(View.GONE);
                passwordField.setText("");
                passwordField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                passwordField.setHint("Email");
                nextButton.setText("Send");
                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ParseUser.requestPasswordResetInBackground(passwordField.getText().toString(), new RequestPasswordResetCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    Toast.makeText(getActivity(), "Please check your email for instructions on resetting your password.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getActivity(), "Error resetting password. Please try a different email.", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                });
            }
        });
        return v;
    }

    private void login(String username, String password) {
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (parseUser != null && e == null) {
                    Utils.startMapActivity(getActivity());
                } else {
                    Log.e("Login Error", e.getLocalizedMessage());
                    Toast.makeText(getActivity(), "Incorrect password.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
