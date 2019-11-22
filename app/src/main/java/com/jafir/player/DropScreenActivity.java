package com.jafir.player;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.jafir.TitleBar;
import com.pockettv.dropscreen.entity.ClingDevice;
import com.pockettv.dropscreen.entity.ClingDeviceList;
import com.pockettv.dropscreen.entity.IDevice;
import com.pockettv.dropscreen.listener.BrowseRegistryListener;
import com.pockettv.dropscreen.listener.DeviceListChangedListener;
import com.pockettv.dropscreen.service.manager.ClingManager;
import com.pockettv.dropscreen.util.Utils;

import org.fourthline.cling.model.meta.Device;

import java.util.ArrayList;


import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by MvpGenerator on 2019/02/25
 */
public class DropScreenActivity extends AppCompatActivity {
    private static final String TAG = DropScreenActivity.class.getSimpleName();


    @BindView(R.id.header_view)
    TitleBar mHeadview;

    @BindView(R.id.recycler)
    RecyclerView mRecycler;
    private DropScreenAdapter mAdapter;

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, DropScreenActivity.class);
        return intent;
    }


    /**
     * 用于监听发现设备
     */
    private BrowseRegistryListener mBrowseRegistryListener = new BrowseRegistryListener();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop_screen);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        //初始化view
        mHeadview.getLeftImg().setOnClickListener(v -> {
            finish();
        });
        mRecycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DropScreenAdapter(R.layout.item_drop_screen);
        mRecycler.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            // 选择连接设备
            ClingDevice item = mAdapter.getItem(position);
            if (Utils.isNull(item)) {
                return;
            }

            ClingManager.getInstance().setSelectedDevice(item);
            mAdapter.notifyDataSetChanged();
            Device device = item.getDevice();
            if (Utils.isNull(device)) {
                return;
            }
            //连接开启投屏
            setResult(RESULT_OK);
            finish();
        });
        mAdapter.setNewData(new ArrayList<>(ClingDeviceList.getInstance().getClingDeviceList()));

        // 设置发现设备监听
        mBrowseRegistryListener.setOnDeviceListChangedListener(new DeviceListChangedListener() {
            @Override
            public void onDeviceAdded(final IDevice device) {
                runOnUiThread(() -> {
                    mAdapter.setNewData(new ArrayList<>(ClingDeviceList.getInstance().getClingDeviceList()));
                });
            }

            @Override
            public void onDeviceRemoved(final IDevice device) {
                runOnUiThread(() -> {
                    mAdapter.setNewData(new ArrayList<>(ClingDeviceList.getInstance().getClingDeviceList()));
                });
            }
        });

        ClingManager clingUpnpServiceManager = ClingManager.getInstance();
        clingUpnpServiceManager.getRegistry().addListener(mBrowseRegistryListener);
        //Search on service created.
        clingUpnpServiceManager.searchDevices();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBrowseRegistryListener.onDestory();
    }
}
