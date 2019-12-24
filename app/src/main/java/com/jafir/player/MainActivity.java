package com.jafir.player;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jafir.player.dao.AppDatabase;
import com.jafir.player.dao.RecordingVideoDao;
import com.pockettv.dropscreen.Intents;
import com.pockettv.dropscreen.control.ClingPlayControl;
import com.pockettv.dropscreen.control.callback.ControlCallback;
import com.pockettv.dropscreen.entity.ClingDevice;
import com.pockettv.dropscreen.entity.ClingDeviceList;
import com.pockettv.dropscreen.entity.DLANPlayState;
import com.pockettv.dropscreen.entity.IResponse;
import com.pockettv.dropscreen.service.ClingUpnpService;
import com.pockettv.dropscreen.service.manager.ClingManager;
import com.pockettv.dropscreen.service.manager.DeviceManager;
import com.pockettv.player.MediaCodecType;
import com.pockettv.player.Settings;
import com.pockettv.player.media.IjkPlayerView;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    //请求投屏
    private static final int REQUEST_DROP_SCREEN_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    //是否第一次进入就全屏（可缩小）
    private static final String INTENT_KEY_FIRST_FULL = "intent_key_first_full";


    private String MOCK_PLAY_URL = "http://cctvalih5ca.v.myalicdn.com/live/cctv4_2/index.m3u8";
    //    private String MOCK_PLAY_URL = "rtsp://61.156.103.73:554/PLTV/88888888/224/3221226043/10000100000000060000000000286778_0.smil";
//        private String MOCK_PLAY_URL = "rtsp://117.159.12.233:554/pag://10.10.2.17:7302:35010000001310011390:0:MAIN:TCP";
    //需要m3u8地址
    private String MOCK_DROP_URL = "http://cctvalih5ca.v.myalicdn.com/live/cctv4_2/index.m3u8";
    public static final String MOCK_TITLE = "播放标题";

    //投屏是否成功
    private boolean mIsDropScreenSuccess;
    private String recordingFilePath;


    public static Intent createIntent(Context context, boolean isFirstFull) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(INTENT_KEY_FIRST_FULL, isFirstFull);
        return intent;
    }

    /**
     * 连接设备状态: 播放状态
     */
    public static final int PLAY_ACTION = 0xa1;
    /**
     * 连接设备状态: 暂停状态
     */
    public static final int PAUSE_ACTION = 0xa2;
    /**
     * 连接设备状态: 停止状态
     */
    public static final int STOP_ACTION = 0xa3;
    /**
     * 连接设备状态: 加载状态
     */
    public static final int TRANSITIONING_ACTION = 0xa4;
    /**
     * 投放失败
     */
    public static final int ERROR_ACTION = 0xa5;
    //现在的音量
    private int mCurrentVolume = 50;
    //每次增加或者减少的音量
    private int VOLUME_STEP = 10;

    /**
     * 投屏控制器
     */
    private ClingPlayControl mClingPlayControl = new ClingPlayControl();

    /**
     * 投屏handler
     */
    private Handler mHandler = new InnerHandler();

    //需要开启投屏控制的本地服务器，判断是否运行
    private boolean mIsServiceRunning;
    //是否已经绑定了service，投屏随着此界面结束而结束
    private boolean mIsBindService;
    //投屏的全局控制监听广播
    private TransportStateBroadcastReceiver mTransportStateBroadcastReceiver;
    //投屏的service 连接
    private ServiceConnection mUpnpServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "mUpnpServiceConnection onServiceConnected");
            ClingUpnpService.LocalBinder binder = (ClingUpnpService.LocalBinder) service;
            ClingUpnpService beyondUpnpService = binder.getService();
            ClingManager clingUpnpServiceManager = ClingManager.getInstance();
            clingUpnpServiceManager.setUpnpService(beyondUpnpService);
            clingUpnpServiceManager.setDeviceManager(new DeviceManager());
            mIsServiceRunning = true;
            //第一次连接好之后直接跳转到搜索界面
            startActivityForResult(DropScreenActivity.createIntent(MainActivity.this), REQUEST_DROP_SCREEN_CODE);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mIsServiceRunning = false;
            Log.e(TAG, "mUpnpServiceConnection onServiceDisconnected");
            ClingManager.getInstance().setUpnpService(null);
        }
    };


    private Disposable mRecordObserver;

    @BindView(R.id.view_video)
    IjkPlayerView mPlayerView;
    @BindView(R.id.record)
    View mRecord;
    @BindView(R.id.screenshot)
    View mScreenshot;
    @BindView(R.id.to_record_list)
    View mToRecordList;
    @BindView(R.id.layout_record)
    View mRecordLayout;
    @BindView(R.id.txt_record)
    TextView mRecordTxt;
    @BindView(R.id.img_record)
    ImageView mRecordImg;
    @BindView(R.id.record_time)
    TextView mRecordTime;
    @BindView(R.id.mode)
    Switch mSwitch;
    RecordingVideoDao mRecordingVideoDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(android.R.color.black));
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();
        AppDatabase appDatabase = AppDatabase.create(getApplication());
        mRecordingVideoDao = appDatabase.recordingVideoDao();
        new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)
                .subscribe(new DisposableObserver<Boolean>() {
                    @Override
                    public void onNext(Boolean bool) {
                        if (!bool) {
                            finish();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        String now = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Log.d(TAG, "now:" + now);
//        MOCK_DROP_URL = String.format(MOCK_DROP_URL, now, now);
//        MOCK_PLAY_URL = String.format(MOCK_DROP_URL, now, now);
    }

    private void initView() {
        initVideo();
        registerReceivers();
        initSettingView();
    }


    /**
     * 注册投屏状态和控制的全局广播监听器
     */
    private void registerReceivers() {
        //Register play status broadcast
        mTransportStateBroadcastReceiver = new TransportStateBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_PLAYING);
        filter.addAction(Intents.ACTION_PAUSED_PLAYBACK);
        filter.addAction(Intents.ACTION_STOPPED);
        filter.addAction(Intents.ACTION_TRANSITIONING);
        filter.addAction(Intents.ACTION_VOLUME_CALLBACK);
        registerReceiver(mTransportStateBroadcastReceiver, filter);
    }

    private void initVideo() {
        //设置软解
        mPlayerView.init()
                .enableOrientation()
                .setTitle("播放标题")
                .setStartAndStopClickListener(new IjkPlayerView.StartAndStopClickListener() {
                    @Override
                    public void onStart() {
                        playUrl();
                    }

                    @Override
                    public void onStop() {
                        //停止播放 就需要 停止录制
                        if (mPlayerView.isRecording()) {
                            stopRecord();
                        }

                        mPlayerView.stop();
                    }
                })
                .setOnCompletionListener(iMediaPlayer -> {
                    //如果点击的是 现在正在直播的  则意为 退出回放 传值0
                    mPlayerView.reset();
                    //todo do on complete
                    playUrl();
                })
                .setOnErrorListener((iMediaPlayer, i, i1) -> {
                    if (!mPlayerView.isEnableDropScreen()) {
                        //todo  doOnError
                        playUrl();
                    }
                    return true;
                })
                .setDropScreenListener(new IjkPlayerView.DropScreenListener() {
                    @Override
                    public void onDeviceSearch() {
                        resetDropScreen();
                        //重新到列表界面选择
                        searchDropScreen();
                    }

                    @Override
                    public void onDropScreen() {
                        //开启投屏
                        startDropScreen();
                    }

                    @Override
                    public void onVolumeAdd() {
                        mCurrentVolume += VOLUME_STEP;
                        if (mCurrentVolume > 100) {
                            mCurrentVolume = 100;
                        }
                        setVolume(mCurrentVolume);
                    }

                    @Override
                    public void onVolumeReduce() {
                        mCurrentVolume -= VOLUME_STEP;
                        if (mCurrentVolume < 0) {
                            mCurrentVolume = 0;
                        }
                        setVolume(mCurrentVolume);
                    }

                    @Override
                    public void onPause() {
                        pauseDropScreen();
                    }

                    @Override
                    public void onStart() {
                        playDropScreen();
                    }

                    @Override
                    public void onQuitDropScreen() {
                        quitDropScreen();
                    }
                });
        if (getIntent().getBooleanExtra(INTENT_KEY_FIRST_FULL, false)) {
            mPlayerView.firstInFullScreen();
        }
    }


    private void initSettingView() {
        mRecord.setOnClickListener(v -> {
            //录制
            mRecordLayout.setVisibility(View.VISIBLE);
        });

        mScreenshot.setOnClickListener(v -> {
            if (new Settings(this).getUsingMediaCodec() == MediaCodecType.SOFT) {
                if (!mPlayerView.isPlaying()) {
                    Toast.makeText(MainActivity.this, "视频尚未成功播放", Toast.LENGTH_SHORT).show();
                    return;
                }
                String shotPath = FileManager.snapshotFileDir + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg";
                File file = new File(shotPath);
                file.getParentFile().mkdirs();
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                        mPlayerView.screenShot(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(MainActivity.this, "截图保存：" + shotPath, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "截图不支持硬解", Toast.LENGTH_SHORT).show();
            }
        });
        mToRecordList.setOnClickListener(v -> {
            startActivity(RecordVideoListActivity.createIntent(this));
        });
        mRecordLayout.setOnClickListener(v -> {
            if (mPlayerView.isRecording()) {
                stopRecord();
            } else {
//                todo 硬解也可以播放 但是有时候会莫名其妙出错 还没有还清楚为什么
//                if (new Settings(this).getUsingMediaCodec() != MediaCodecType.SOFT) {
//                    Toast.makeText(MainActivity.this, "只有软件解码方式才能录制视频，请设置", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                if (!mPlayerView.isPlaying()) {
                    Toast.makeText(MainActivity.this, "视频没有播放，无法录制", Toast.LENGTH_SHORT).show();
                    return;
                }
                //开始
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("温馨提示")
                        .setMessage("录制直播过程中，请勿切换频道、后台运行")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("了解", (dialog, which) -> {
                            startRecord();
                        }).show();
            }
        });
        mSwitch.setChecked(new Settings(MainActivity.this).getUsingMediaCodec() != MediaCodecType.HARD);
        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                new Settings(MainActivity.this).setUsingMediaCodec(MediaCodecType.SOFT);
            } else {
                new Settings(MainActivity.this).setUsingMediaCodec(MediaCodecType.HARD);
            }
            mPlayerView.reload();
        });
    }


    @OnClick(R.id.airdrop)
    public void onAirDrop() {
        // 投屏
        searchDropScreen();
    }

    public void quitDropScreen() {
        mPlayerView.enableDropScreen(false);
        resetDropScreen();
        //重新请求加载数据
        mPlayerView.reset();
        playUrl();
    }

    private void playUrl() {
        //todo 模拟请求数据
        showVideoLoading();
        new Handler().postDelayed(() -> {
            if (mPlayerView != null && !mPlayerView.isEnableDropScreen()) {
                mPlayerView.setVideoPath(Uri.parse(MOCK_PLAY_URL));
                mPlayerView.start();
            }
            //playerView 会在加载成功调用 hideVideoLoading();
        }, 1000);
    }

    public void showMediaPlayFail() {
        mPlayerView.noSignalDeal();
    }

    public void showNoCopyright() {
        mPlayerView.noCopyRightDeal();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mPlayerView.onResume();
//      有些源的地址不是一直有效 需要每次重加加载一遍
        if (!mPlayerView.isEnableDropScreen() && !mPlayerView.isManualPause()) {
            Log.d(TAG, "onResume player");
            playUrl();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayerView.isRecording()) {
            stopRecord();
        }
        if (!mPlayerView.isEnableDropScreen()) {
            Log.d(TAG, "onPause reset");
            mPlayerView.reset();
        }
    }

    @Override
    protected void onDestroy() {
        resetDropScreen();
        //父类的 已经unbind了 所以在之前调用
        mPlayerView.onDestroy();
        mClingPlayControl = null;
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        if (mIsBindService) {
            unbindService(mUpnpServiceConnection);
        }
        unregisterReceiver(mTransportStateBroadcastReceiver);
        ClingManager.getInstance().destroy();
        ClingDeviceList.getInstance().destroy();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPlayerView.configurationChanged(newConfig);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPlayerView.handleVolumeKey(keyCode)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mPlayerView.onBackPressed()) {
            return;
        }
        if (mPlayerView.isRecording()) {
            new AlertDialog.Builder(this)
                    .setTitle("确定要退出吗？")
                    .setMessage("正在录屏中，退出会停止录屏")
                    .setPositiveButton("确定", (dialog, which) -> finish())
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DROP_SCREEN_CODE && resultCode == RESULT_OK) {
            //开启投屏
            Log.d(TAG, "onActivityResult startDropScreen");
            mPlayerView.enableDropScreen(true);
            mHandler.sendEmptyMessage(TRANSITIONING_ACTION);
            playDropScreen();
        }
    }

    private void startRecord() {
        mRecordImg.setImageResource(R.drawable.stop_record);
        mRecordTxt.setText("停止缓存");
        mRecordObserver = Observable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(i -> {
                    mRecordTime.setText("已录制 " + TimeCompat.secToTime(i.intValue()));
                });
        recordingFilePath = FileManager.videoFileDir +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ".mov";
        File file = new File(recordingFilePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            file.createNewFile();
            boolean rel = mPlayerView.startRecord(file.getAbsolutePath());
            if (rel) {
                Toast.makeText(MainActivity.this, "开始录制", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "录制失败", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecord() {
        //停止
        if (!mRecordObserver.isDisposed()) {
            mRecordObserver.dispose();
        }
        mRecordTime.setText("");
        mRecordTxt.setText("开始缓存");
        mRecordImg.setImageResource(R.drawable.start_record);

        // 创建数据库数据
        mPlayerView.stopRecord();
        File file = new File(recordingFilePath);
        if (file.exists() && file.length() > 0) {
            Pair<String, String> pair = getAvatarAndDuration(file);
            RecordingModel model = new RecordingModel(MOCK_TITLE,
                    recordingFilePath,
                    pair.first,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                    pair.second,
                    file.length()
            );
            addRecordingVideo(model);
            Toast.makeText(MainActivity.this, "视频已保存：" + recordingFilePath, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "录制失败", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 增加录制视频model
     */
    public void addRecordingVideo(RecordingModel... list) {
        Completable.create(emitter -> mRecordingVideoDao.insertOrUpdateRecordVideo(list))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "addRecordingVideo");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "addRecordingVideo " + e);
                    }
                });
    }

    private Pair<String, String> getAvatarAndDuration(File file) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getAbsolutePath());
        Bitmap previewBitmap = mmr.getFrameAtTime();
        final File saveFile = new File(
                FileManager.snapshotFileDir,
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg");
        if (!saveFile.getParentFile().exists()) {
            saveFile.getParentFile().mkdirs();
        }
        //开启保存图片线程
        new Thread(() -> saveBitmap(saveFile, previewBitmap)).start();

        // 获取时长
        String strDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mmr.release();

        int duration = Integer.parseInt(strDuration) / 1000;
        String formattedDuration = TimeCompat.secToTime(duration);
        Pair<String, String> pair = new Pair<>(saveFile.getAbsolutePath(), formattedDuration);
        return pair;
    }

    private void saveBitmap(File file, Bitmap srcBitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            srcBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 跳转到频道搜索界面
     * 需要先开启service
     */
    private void searchDropScreen() {
        // Bind UPnP service
        if (!mIsServiceRunning) {
            Intent upnpServiceIntent = new Intent(this, ClingUpnpService.class);
            bindService(upnpServiceIntent, mUpnpServiceConnection, Context.BIND_AUTO_CREATE);
            mIsBindService = true;
        } else {
            //跳转到搜索界面
            startActivityForResult(DropScreenActivity.createIntent(MainActivity.this), REQUEST_DROP_SCREEN_CODE);
        }
    }

    /**
     * 重置投屏状态和视频里投屏控件状态
     */
    private void resetDropScreen() {
        mPlayerView.resetDropScreen();
        //退出关闭投屏
        stop();
        //清除已选中的投屏设备
        ClingManager.getInstance().cleanSelectedDevice();
    }

    /**
     * 开启投屏
     */
    public void startDropScreen() {
        Log.d(TAG, "startDropScreen");
        mPlayerView.enableDropScreen(true);
        playDropScreen();
    }

    public void showVideoLoading() {
        //解析数据也需要花很多时间 需要转圈提示
        mPlayerView.showLoading();
    }

    public void showVideoLoading(String s) {
        mPlayerView.showLoading(s);
    }

    public void hideVideoLoading() {
        mPlayerView.hideLoading();
    }

    /**
     * 停止
     */
    private void stop() {
        mClingPlayControl.stop(new ControlCallback() {
            @Override
            public void success(IResponse response) {
                Log.d(TAG, "dropscreen control stop success");
            }

            @Override
            public void fail(IResponse response) {
                Log.e(TAG, "dropscreen control stop  fail");
            }
        });
    }

    /**
     * 暂停
     */
    private void pauseDropScreen() {
        mClingPlayControl.pause(new ControlCallback() {
            @Override
            public void success(IResponse response) {
                Log.d(TAG, "dropscreen control pauseDropScreen success");
            }

            @Override
            public void fail(IResponse response) {
                Log.e(TAG, "dropscreen control pauseDropScreen fail");
            }
        });
    }

    /**
     * 播放视频
     */
    private void playDropScreen() {
        @DLANPlayState.DLANPlayStates int currentState = mClingPlayControl.getCurrentState();

        /**
         * 通过判断状态 来决定 是继续播放 还是重新播放
         */
        if (currentState == DLANPlayState.STOP) {
//        if (true) {
            String url = String.format(MOCK_DROP_URL, new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()));
            if (TextUtils.isEmpty(url)) {
                mHandler.removeMessages(ERROR_ACTION);
                mHandler.sendEmptyMessageDelayed(ERROR_ACTION, 1000);
                return;
            }

            Log.d(TAG, "drop url ：" + url);
            mHandler.sendEmptyMessage(TRANSITIONING_ACTION);
            mClingPlayControl.playNew(url, MOCK_TITLE, new ControlCallback() {

                @Override
                public void success(IResponse response) {
                    Log.d(TAG, "dropscreen control playDropScreen success");
                    ClingManager.getInstance().registerAVTransport(MainActivity.this.getApplicationContext());
                    ClingManager.getInstance().registerRenderingControl(MainActivity.this.getApplicationContext());
                    mHandler.sendEmptyMessage(PLAY_ACTION);
                }

                @Override
                public void fail(IResponse response) {
                    Log.e(TAG, "dropscreen control playDropScreen fail");
                    mHandler.removeMessages(ERROR_ACTION);
                    mHandler.sendEmptyMessageDelayed(ERROR_ACTION, 1000);
                }
            });
        } else {
            mHandler.sendEmptyMessage(TRANSITIONING_ACTION);
            mClingPlayControl.play(new ControlCallback() {
                @Override
                public void success(IResponse response) {
                    Log.d(TAG, "dropscreen control playDropScreen success");
                }

                @Override
                public void fail(IResponse response) {
                    Log.e(TAG, "dropscreen control playDropScreen fail");
                    mHandler.removeMessages(ERROR_ACTION);
                    mHandler.sendEmptyMessageDelayed(ERROR_ACTION, 1000);
                }
            });
        }
    }

    /**
     * 设置音量
     *
     * @param currentVolume 0-100
     */
    public void setVolume(int currentVolume) {
        mClingPlayControl.setVolume(currentVolume, new ControlCallback() {
            @Override
            public void success(IResponse response) {
                Log.d(TAG, "dropscreen control volume success");
            }

            @Override
            public void fail(IResponse response) {
                Log.e(TAG, "dropscreen control volume fail");
            }
        });
    }

    /******************* end progress changed listener *************************/

    private final class InnerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case PLAY_ACTION:
                    Log.i(TAG, "Execute PLAY_ACTION");
                    mIsDropScreenSuccess = true;
                    mClingPlayControl.setCurrentState(DLANPlayState.PLAY);
                    mPlayerView.setDropScreenName(((ClingDevice) ClingManager.getInstance().getSelectedDevice()).getDevice().getDetails().getFriendlyName());
                    mPlayerView.setDropScreenStatus("正在投放");
                    mPlayerView.setDropScrennPlayStatus(true);
                    mPlayerView.setDropScrennPlayRestartStatus(false);
                    break;
                case PAUSE_ACTION:
                    Log.i(TAG, "Execute PAUSE_ACTION");
                    mClingPlayControl.setCurrentState(DLANPlayState.PAUSE);
                    mPlayerView.setDropScreenStatus("暂停投放");
                    mPlayerView.setDropScrennPlayStatus(false);
                    mPlayerView.setDropScrennPlayRestartStatus(false);
                    break;
                case STOP_ACTION:
                    Log.i(TAG, "Execute STOP_ACTION");
                    mClingPlayControl.setCurrentState(DLANPlayState.STOP);
                    mPlayerView.setDropScreenStatus("停止投放");
                    mPlayerView.setDropScrennPlayStatus(false);
                    mPlayerView.setDropScrennPlayRestartStatus(true);
                    //如果没有成功过 就停止了 也说明是播放不了 换源播放
                    if (!mIsDropScreenSuccess) {
                        //todo 切换url重试
                        playDropScreen();
                    }
                    break;
                case TRANSITIONING_ACTION:
                    Log.i(TAG, "Execute TRANSITIONING_ACTION");
                    mPlayerView.setDropScreenStatus("正在连接");
                    break;
                case ERROR_ACTION:
                    Log.e(TAG, "Execute ERROR_ACTION");
                    mIsDropScreenSuccess = false;
                    mPlayerView.setDropScreenStatus("投放失败");
                    mPlayerView.setDropScrennPlayStatus(false);
                    mPlayerView.setDropScrennPlayRestartStatus(true);
                    //todo 切换url重试
                    playDropScreen();
                    break;
            }
        }
    }

    /**
     * 接收状态改变信息
     */
    private class TransportStateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Receive playback intent:" + action);
            if (Intents.ACTION_PLAYING.equals(action)) {
                mHandler.sendEmptyMessage(PLAY_ACTION);
            } else if (Intents.ACTION_PAUSED_PLAYBACK.equals(action)) {
                mHandler.sendEmptyMessage(PAUSE_ACTION);
            } else if (Intents.ACTION_STOPPED.equals(action)) {
                mHandler.sendEmptyMessage(STOP_ACTION);
            } else if (Intents.ACTION_TRANSITIONING.equals(action)) {
                mHandler.sendEmptyMessage(TRANSITIONING_ACTION);
            } else if (Intents.ACTION_VOLUME_CALLBACK.equals(action)) {
                mCurrentVolume = intent.getIntExtra(Intents.EXTRA_VOLUME, mCurrentVolume);
            }
        }
    }
}
