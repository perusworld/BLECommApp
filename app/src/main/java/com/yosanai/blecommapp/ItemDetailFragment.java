package com.yosanai.blecommapp;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.yosanai.blecommapp.dummy.DummyContent;
import com.yosanai.blecommj.BLEComm;
import com.yosanai.blecommj.BLECommCallback;
import com.yosanai.blecommj.BLECommConfig;
import com.yosanai.blecommj.BLECommConfigBuilder;
import com.yosanai.blecommj.BLEObject;
import com.yosanai.blecommj.DefaultBLEComm;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 */
public class ItemDetailFragment extends Fragment {
    public static final String ARG_ITEM_NAME = "item_name";
    public static final String ARG_ITEM_ADDRESS = "item_address";
    public static final String SUUID = "fff0";
    public static final String TUUID = "fff1";
    public static final String RUUID = "fff2";
    public static final String FUUID = "fff3";

    private String name;
    private String address;

    private BLEComm bleComm;
    private boolean connected;

    private ArrayAdapter<String> msgs;

    private int packetSize = 20;
    private int notificationDelay = 50;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_NAME)) {
            name = getArguments().getString(ARG_ITEM_NAME);
            address = getArguments().getString(ARG_ITEM_ADDRESS);

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(name);
            }
        }

        msgs = new ArrayAdapter<String>(this.getContext(), R.layout.item_detail);
        msgs.setNotifyOnChange(true);

        final DefaultBLEComm defaultBleComm = new DefaultBLEComm();
        BLECommConfigBuilder bleCommConfigBuilder = new BLECommConfigBuilder();
        bleCommConfigBuilder.setCallback(new BLECommCallback() {
            @Override
            public void onConnect() {
                connected = true;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        msgs.insert("Connected", 0);
                    }
                });
            }

            @Override
            public void onDisconnect() {
                connected = false;
                Activity activity = getActivity();
                if (null != activity) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            msgs.insert("Disconnected", 0);
                        }
                    });
                }
            }

            @Override
            public void onData(final String data) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        msgs.insert(data, 0);
                    }
                });
            }
        });
        bleCommConfigBuilder.setsUUID(SUUID);
        bleCommConfigBuilder.setTxUUID(TUUID);
        bleCommConfigBuilder.setRxUUID(RUUID);
        bleCommConfigBuilder.setfUUID(FUUID);
        bleCommConfigBuilder.setPacketSize(packetSize);
        bleCommConfigBuilder.setNotificationDelay(notificationDelay);
        BLECommConfig cfg = bleCommConfigBuilder.build();
        defaultBleComm.init(this.getActivity(), cfg);
        bleComm = defaultBleComm;
        bleComm.connect(getActivity(), address);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.msg_view, container, false);

        ListView msgsView = (ListView) rootView.findViewById(R.id.msgs);
        msgsView.setAdapter(msgs);

        final EditText msg = (EditText)rootView.findViewById(R.id.msg);

        ((Button) rootView.findViewById(R.id.btnSend)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleComm.send(msg.getText().toString());
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != bleComm) {
            bleComm.disconnect();
        }
    }
}
