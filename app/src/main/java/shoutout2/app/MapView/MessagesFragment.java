package shoutout2.app.MapView;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

import shoutout2.app.MessageArrayAdapter;
import shoutout2.app.R;

public class MessagesFragment extends Fragment {
    public static final String TAG = "messages_fragment";
    private MapActivity mapActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mapActivity = (MapActivity) getActivity();
        View view = inflater.inflate(R.layout.messages_view, container, false);
        final ListView messagesListView = (ListView) view.findViewById(R.id.messages_list);
        final ParseQuery<ParseObject> messageQuery = new ParseQuery<ParseObject>("Messages");
        messageQuery.whereEqualTo("to", ParseUser.getCurrentUser());
        messageQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messageList, ParseException e) {
                if (e == null && messageList != null) {
                    ArrayAdapter<ParseObject> adapter = new MessageArrayAdapter<>(mapActivity, R.layout.single_message_view, R.id.label, messageList, mapActivity.people);
                    messagesListView.setAdapter(adapter);
                }
            }
        });
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ParseObject item = (ParseObject) messagesListView.getAdapter().getItem(i);
                mapActivity.onBackPressed();
                try {
                    mapActivity.startMessageTo(item.fetchIfNeeded().getParseUser("from").fetchIfNeeded().getUsername());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                mapActivity.updateMessageButton();
            }
        });
        return view;
    }
}
