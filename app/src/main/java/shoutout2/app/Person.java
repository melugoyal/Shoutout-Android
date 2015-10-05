package shoutout2.app;

import android.graphics.Bitmap;
import com.parse.ParseUser;

public class Person {
    public final ParseUser parseUser;
    public Bitmap icon;
    public Bitmap activeIcon;
    public Person(ParseUser user) {
        parseUser = user;
    }
}
