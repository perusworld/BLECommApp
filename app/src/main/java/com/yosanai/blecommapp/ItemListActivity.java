package com.yosanai.blecommapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yosanai.blecommj.BLEObject;
import com.yosanai.blecommj.BLEPermissions;
import com.yosanai.blecommj.BLEScan;
import com.yosanai.blecommj.BLEScanCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ItemListActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD = 5000;
    BLEScan bleScan;
    SimpleItemRecyclerViewAdapter adapter;
    BLEPermissions blePermissions = new BLEPermissions();
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Starting scan again", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                checkAndDoScan();
            }
        });

        View recyclerView = findViewById(R.id.item_list);
        assert recyclerView != null;
        adapter = new SimpleItemRecyclerViewAdapter(new ArrayList<BLEObject>());
        ((RecyclerView) recyclerView).setAdapter(adapter);

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
        checkAndDoScan();
    }

    private void checkAndDoScan() {
        startScanIf(blePermissions.ensurePermissions(this));
    }

    private void startScan() {
        bleScan = new BLEScan();
        if (!bleScan.init(this, BLEScan.getUUID("fff0"))) {
            finish();
        }
        adapter.mValues.clear();
        adapter.notifyDataSetChanged();
        bleScan.scan(SCAN_PERIOD, true, new BLEScanCallback() {
            @Override
            public void onDone(final Collection<BLEObject> devices) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.mValues.clear();
                        for (BLEObject device : devices) {
                            adapter.mValues.add(device);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startScanIf(blePermissions.checkResultActivity(this, requestCode, resultCode));
    }

    private void startScanIf(BLEPermissions.Status status) {
        switch (status) {
            case TRUE:
                startScan();
                break;
            case FALSE:
                //Show error
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startScanIf(blePermissions.checkRequestPermissionsResult(requestCode, permissions, grantResults));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<BLEObject> mValues;

        public SimpleItemRecyclerViewAdapter(List<BLEObject> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            holder.mIdView.setText(mValues.get(position).getName());
            holder.mContentView.setText(mValues.get(position).getSerialNumber());

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTwoPane) {
                        Bundle arguments = new Bundle();
                        arguments.putString(ItemDetailFragment.ARG_ITEM_ADDRESS, holder.mItem.getAddress());
                        arguments.putString(ItemDetailFragment.ARG_ITEM_NAME, holder.mItem.getName());
                        ItemDetailFragment fragment = new ItemDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.item_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, ItemDetailActivity.class);
                        intent.putExtra(ItemDetailFragment.ARG_ITEM_ADDRESS, holder.mItem.getAddress());
                        intent.putExtra(ItemDetailFragment.ARG_ITEM_NAME, holder.mItem.getName());

                        context.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public BLEObject mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}
