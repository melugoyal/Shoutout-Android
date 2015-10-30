package shoutout2.app;

import android.graphics.Bitmap;
import android.view.View;

import com.parse.ParseUser;

import java.util.Date;

import shoutout2.app.Utils.Utils;

public class Person {
    public String userId;
    public String status;
    public String username;
    public String updatedAt;
    public Bitmap emptyStatusIcon;
    public Bitmap inactiveMarker;
    public Bitmap scaledInactiveMarker;
    public Bitmap icon;
    public View markerView;

    public Person(ParseUser user) {
        this.userId = user.getObjectId();
        this.username = user.getUsername();
        setFields(user.getString("status"), user.getUpdatedAt());
    }

    public void setFields(String status, Date date) {
        this.status = status;
        updatedAt = Utils.dateToString(date);
    }
}
