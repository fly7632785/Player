package com.pockettv.player.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.pockettv.player.R;
import com.pockettv.player.utils.AnimHelper;
import com.pockettv.player.utils.MotionEventUtils;
import com.pockettv.player.utils.NetWorkUtils;
import com.pockettv.player.utils.StringUtils;
import com.pockettv.player.utils.WindowUtils;
import com.pockettv.player.widgets.MarqueeTextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static android.view.GestureDetector.OnGestureListener;
import static android.view.GestureDetector.SimpleOnGestureListener;
import static com.pockettv.player.utils.StringUtils.generateTime;
import static tv.danmaku.ijk.media.player.IMediaPlayer.OnInfoListener;

public class IjkPlayerView extends FrameLayout implements View.OnClickListener {
    private static final String TAG = IjkPlayerView.class.getSimpleName();

    // 默认隐藏控制栏时间
    private static final int DEFAULT_HIDE_TIMEOUT = 5000;
    // 更新进度消息
    private static final int MSG_UPDATE_SEEK = 10086;
    // 使能翻转消息
    private static final int MSG_ENABLE_ORIENTATION = 10087;
    // 尝试重连消息
    private static final int MSG_TRY_RELOAD = 10088;
    // 无效变量
    private static final int INVALID_VALUE = -1;
    // 达到文件时长的允许误差值，用来判断是否播放完成
    private static final int INTERVAL_TIME = 1000;
    // default音量
    private static final float DEFAULT_VOLUME = 0.5f;

    // 原生的IjkPlayer
    private IjkVideoView mVideoView;
    // 加载
    private View mLoadingView;
    private TextView mLoadingText;
    // 音量
    private TextView mTvVolume;
    // 亮度
    private TextView mTvBrightness;
    // 快进
    private TextView mTvFastForward;
    // 触摸信息布局
    private FrameLayout mFlTouchLayout;
    // 全屏下的后退键
    private ImageView mIvBack;
    // 全屏下的标题
    private MarqueeTextView mTvTitle;
    // 全屏下的TopBar
    private LinearLayout mFullscreenTopBar;
    // 窗口模式的后退键
    private ImageView mIvBackWindow;
    // 窗口模式的TopBar
    private FrameLayout mWindowTopBar;
    // 播放键
    private ImageView mIvPlay;
    // 全屏切换按钮
    private ImageView mIvFullscreen;
    // BottomBar
    private LinearLayout mLlBottomBar;
    // 整个视频框架布局
    private FrameLayout mFlVideoBox;
    // 锁屏键
    private ImageView mIvPlayerLock;
    // 还原屏幕
    private TextView mTvRecoverScreen;
    // 宽高比选项
    private TextView mTvSettings;
    private RadioGroup mAspectRatioOptions;
    // 关联的Activity
    private AppCompatActivity mAttachActivity;
    // 重试
    // 重试模式的后退键
    private ImageView mIvBackReload;
    private TextView mTvReload;
    private View mFlReload;

    //投屏相关
    private View mViewDropcreen;
    private TextView mTvDropScreenName;
    private TextView mTvDropScreenStatus;
    private TextView mTvDropScreenRestart;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_ENABLE_ORIENTATION) {
                if (mOrientationListener != null) {
                    mOrientationListener.enable();
                }
            } else if (msg.what == MSG_TRY_RELOAD) {
                if (mIsNetConnected) {
                    reload();
                }
                msg = new Message();
                msg.what = MSG_TRY_RELOAD;
                sendMessageDelayed(msg, 3000);
            }
        }
    };
    // 音量控制
    private AudioManager mAudioManager;
    // 手势控制
    private GestureDetector mGestureDetector;
    // 最大音量
    private int mMaxVolume;
    // 锁屏
    private boolean mIsForbidTouch = false;
    // 是否显示控制栏
    private boolean mIsShowBar = false;
    // 是否全屏
    private boolean mIsFullscreen;
    // 是否播放结束
    private boolean mIsPlayComplete = false;
    // 是否正在拖拽进度条
    private boolean mIsSeeking;
    // 目标进度
    private long mTargetPosition = INVALID_VALUE;
    // 当前进度
    private int mCurPosition = INVALID_VALUE;
    // 当前音量
    private int mCurVolume = INVALID_VALUE;
    // 当前亮度
    private float mCurBrightness = INVALID_VALUE;
    // 初始高度
    private int mInitHeight;
    // 屏幕宽/高度
    private int mWidthPixels;
    // 屏幕UI可见性
    private int mScreenUiVisibility;
    // 屏幕旋转角度监听
    private OrientationEventListener mOrientationListener;
    // 外部监听器
    private OnInfoListener mOutsideInfoListener;
    private IMediaPlayer.OnCompletionListener mCompletionListener;
    // 禁止翻转，默认为禁止
    private boolean mIsForbidOrientation = true;
    // 是否固定全屏状态
    private boolean mIsAlwaysFullScreen = false;
    // 记录按退出全屏时间
    private long mExitTime = 0;
    // 视频Matrix
    private Matrix mVideoMatrix = new Matrix();
    private Matrix mSaveMatrix = new Matrix();
    // 是否需要显示恢复屏幕按钮
    private boolean mIsNeedRecoverScreen = false;
    // 选项列表高度
    private int mAspectOptionsHeight;
    // 异常中断时的播放进度
    private int mInterruptPosition;
    // 是否是准备好状态 可以播放
    private boolean mIsReady = false;
    /**
     * 默认设置为直播状态（目前我们只有直播没有点播）
     */
    private boolean mIsLive = true;
    //是否开启进度条快进快退模式
    private boolean enableProgressSlide = false;
    //手动暂停，再次resume的时候 不再自动播放
    private boolean mIsManualPause;
    //是否允许三指缩放
    private boolean enableTreePointRotate;
    //是否是homeFragment下使用
    private boolean mIsInHome;
    /**
     * 版权检查和错误处理的handler
     */
    private VideoHandler mVideoHandler;
    //流量提醒layout
    private View mLlMobileNotify;
    //流量提醒继续播放
    private View mTvMobileGoOn;
    //是否允许数据流量播放
    private boolean mIsAllowMobileDataPlay = true;
    //是否开启投屏模式
    private boolean mEnableDropScreen = false;
    //是否正在录屏
    private boolean mIsRecording;

    public IjkPlayerView(Context context) {
        this(context, null);
    }

    public IjkPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        _initView(context);
    }

    private void _initView(Context context) {
        if (context instanceof AppCompatActivity) {
            mAttachActivity = (AppCompatActivity) context;
        } else {
            throw new IllegalArgumentException("Context must be AppCompatActivity");
        }
        View.inflate(context, R.layout.layout_player_view, this);
        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        mLoadingView = findViewById(R.id.layout_loading);
        mLoadingText = findViewById(R.id.loading_text);
        mTvVolume = (TextView) findViewById(R.id.tv_volume);
        mTvBrightness = (TextView) findViewById(R.id.tv_brightness);
        mTvFastForward = (TextView) findViewById(R.id.tv_fast_forward);
        mFlTouchLayout = (FrameLayout) findViewById(R.id.fl_touch_layout);
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mTvTitle = (MarqueeTextView) findViewById(R.id.tv_title);
        mFullscreenTopBar = (LinearLayout) findViewById(R.id.fullscreen_top_bar);
        mIvBackWindow = (ImageView) findViewById(R.id.iv_back_window);
        mWindowTopBar = (FrameLayout) findViewById(R.id.window_top_bar);
        mIvPlay = (ImageView) findViewById(R.id.iv_play);
        mIvFullscreen = (ImageView) findViewById(R.id.iv_fullscreen);
        mLlBottomBar = (LinearLayout) findViewById(R.id.ll_bottom_bar);
        mFlVideoBox = (FrameLayout) findViewById(R.id.fl_video_box);
        mIvPlayerLock = (ImageView) findViewById(R.id.iv_player_lock);
        mTvRecoverScreen = (TextView) findViewById(R.id.tv_recover_screen);
        mIvBackReload = findViewById(R.id.iv_reload_back);
        mTvReload = (TextView) findViewById(R.id.tv_reload);
        mFlReload = findViewById(R.id.fl_reload_layout);
        //视频宽高比设置
        mTvSettings = (TextView) findViewById(R.id.tv_settings);
        //流量播放提醒
        mLlMobileNotify = findViewById(R.id.view_video_hint);
        mTvMobileGoOn = findViewById(R.id.view_video_play);

        mAspectRatioOptions = (RadioGroup) findViewById(R.id.aspect_ratio_group);
        mAspectOptionsHeight = getResources().getDimensionPixelSize(R.dimen.aspect_btn_size) * 4;
        mAspectRatioOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.aspect_fit_parent) {
                    mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
                } else if (checkedId == R.id.aspect_fit_screen) {
                    mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT);
                } else if (checkedId == R.id.aspect_16_and_9) {
                    mVideoView.setAspectRatio(IRenderView.AR_16_9_FIT_PARENT);
                } else if (checkedId == R.id.aspect_4_and_3) {
                    mVideoView.setAspectRatio(IRenderView.AR_4_3_FIT_PARENT);
                }
                AnimHelper.doClipViewHeight(mAspectRatioOptions, mAspectOptionsHeight, 0, 150);
            }
        });

        //投屏相关
        mViewDropcreen = findViewById(R.id.view_drop_screen);
        mTvDropScreenName = findViewById(R.id.drop_screen_name);
        mTvDropScreenStatus = findViewById(R.id.drop_screen_status);
        mTvDropScreenRestart = findViewById(R.id.drop_screen_restart);

        _initVideoSkip();
        _initReceiver();

        //设置不可以左右滑动 快进快退
        mIvPlay.setOnClickListener(this);
        mIvBack.setOnClickListener(this);
        mIvFullscreen.setOnClickListener(this);
        mIvBackWindow.setOnClickListener(this);
        mIvPlayerLock.setOnClickListener(this);
        mTvRecoverScreen.setOnClickListener(this);
        mTvSettings.setOnClickListener(this);
        mTvReload.setOnClickListener(this);
        mIvBackReload.setOnClickListener(this);
        mTvMobileGoOn.setOnClickListener(this);

        mTvDropScreenRestart.setOnClickListener(this);
        findViewById(R.id.drop_screen_quit).setOnClickListener(this);
        findViewById(R.id.drop_screen_devices).setOnClickListener(this);
        findViewById(R.id.drop_screen_add_volume).setOnClickListener(this);
        findViewById(R.id.drop_screen_reduce_volume).setOnClickListener(this);

        findViewById(R.id.tv_screen_shot).setOnClickListener(this);
        findViewById(R.id.tv_start_record).setOnClickListener(this);
        findViewById(R.id.tv_stop_record).setOnClickListener(this);

        mWidthPixels = getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 初始化
     */
    private void _initMediaPlayer() {
        // 加载 IjkMediaPlayer 库
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        // 声音
        mAudioManager = (AudioManager) mAttachActivity.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 亮度
        try {
            int e = Settings.System.getInt(mAttachActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            float progress = 1.0F * (float) e / 255.0F;
            WindowManager.LayoutParams layout = mAttachActivity.getWindow().getAttributes();
            if (progress < DEFAULT_VOLUME) {
                progress = DEFAULT_VOLUME;
            }
            mAttachActivity.getWindow().setAttributes(layout);
            layout.screenBrightness = progress;
        } catch (Settings.SettingNotFoundException var7) {
            var7.printStackTrace();
            //如果设置失败 直接设置50%默认亮度
            WindowManager.LayoutParams layout = mAttachActivity.getWindow().getAttributes();
            layout.screenBrightness = DEFAULT_VOLUME;
            mAttachActivity.getWindow().setAttributes(layout);
        }
        // 视频监听
        mVideoView.setOnInfoListener(mInfoListener);
        // 触摸控制
        mGestureDetector = new GestureDetector(mAttachActivity, mPlayerGestureListener);
        mFlVideoBox.setClickable(true);
        mFlVideoBox.setOnTouchListener(mPlayerTouchListener);
        // 屏幕翻转控制
        mOrientationListener = new OrientationEventListener(mAttachActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                _handleOrientation(orientation);
            }
        };
        if (mIsForbidOrientation) {
            // 禁止翻转
            mOrientationListener.disable();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mInitHeight == 0) {
            mInitHeight = getHeight();
        }
    }

    /**============================ 外部调用接口 ============================*/

    /**
     * Activity.onResume() 里调用
     */
    public void onResume() {
        /**
         * 手动暂停 不再次重新resume视频播放
         *
         * 投屏模式下 不处理视频播放的生命周期
         */
        if (mIsManualPause || mEnableDropScreen) {
            return;
        }
        // 如果出现锁屏则需要重新渲染器Render，不然会出现只有声音没有动画
        // 目前只在锁屏时会出现图像不动的情况，如果有遇到类似情况可以尝试按这个方法解决
        mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);

        mVideoView.resume();
        if (!mIsForbidTouch && !mIsForbidOrientation) {
            mOrientationListener.enable();
        }
        if (mCurPosition != INVALID_VALUE) {
            // 重进后 seekTo 到指定位置播放时，通常会回退到前几秒，关键帧??
            seekTo(mCurPosition);
            mCurPosition = INVALID_VALUE;
        }
    }

    /**
     * Activity.onPause() 里调用
     */
    public void onPause() {
        /**
         * 投屏模式下 不处理视频播放的生命周期
         */
        if (mEnableDropScreen) {
            return;
        }
        mCurPosition = mVideoView.getCurrentPosition();
        mVideoView.pause();
        mIvPlay.setSelected(false);
        mOrientationListener.disable();
    }

    /**
     * Activity.onDestroy() 里调用
     *
     * @return 返回播放进度
     */
    public int onDestroy() {
        // 记录播放进度
        int curPosition = mVideoView.getCurrentPosition();
        mVideoView.destroy();
        IjkMediaPlayer.native_profileEnd();
        mHandler.removeMessages(MSG_TRY_RELOAD);
        mHandler.removeMessages(MSG_UPDATE_SEEK);
        // 注销广播
        mAttachActivity.unregisterReceiver(mBatteryReceiver);
        mAttachActivity.unregisterReceiver(mNetReceiver);
        unRegisterNetChangedReceiver();

        // 关闭屏幕常亮
        mAttachActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return curPosition;
    }


    /**
     * 设置是否为直播模式
     * 默认是直播模式
     */
    public void enableLive(boolean mIsLive) {
        this.mIsLive = mIsLive;
    }

    /**
     * 设置是否允许快进快退
     */
    public void enableProgressSlide(boolean enableProgressSlide) {
        this.enableProgressSlide = enableProgressSlide;
    }

    /**
     * 设置是否允许三指控制旋转缩放视频
     */
    public void enableThreePointerRotate(boolean enable) {
        this.enableTreePointRotate = enable;
    }

    /**
     * 处理音量键，避免外部按音量键后导航栏和状态栏显示出来退不回去的状态
     *
     * @param keyCode
     * @return
     */
    public boolean handleVolumeKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            _setVolume(true);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            _setVolume(false);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 设置画面比例
     *
     * @param aspectRatio
     */
    public void setAspectRatio(int aspectRatio) {
        mVideoView.setAspectRatio(aspectRatio);
    }

    /**
     * 回退，全屏时退回竖屏
     *
     * @return
     */
    public boolean onBackPressed() {
        if (mIsForbidTouch) {
            Toast.makeText(getContext(), "屏幕已锁定，请先解锁", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (mIsAlwaysFullScreen) {
            _exit();
            return true;
        } else if (mIsFullscreen) {
            mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (mIsForbidTouch) {
                // 锁住状态则解锁
                mIsForbidTouch = false;
                mIvPlayerLock.setSelected(false);
                _setControlBarVisible(mIsShowBar);
            }
            return true;
        }
        return false;
    }

    /**
     * 初始化，必须要先调用
     *
     * @return
     */
    public IjkPlayerView init() {
        _initMediaPlayer();
        if (!isWifi(getContext()) && new com.pockettv.player.Settings(getContext()).isMobileNetworkHint()) {
            mIsAllowMobileDataPlay = false;
            mLlMobileNotify.setVisibility(View.VISIBLE);
        }
        mVideoHandler = new VideoHandler(this);
        registerNetChangedReceiver();
        return this;
    }

    public static boolean isWifi(Context context) {
        try {

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 切换视频
     *
     * @param url
     * @return
     */
    public IjkPlayerView switchVideoPath(String url) {
        return switchVideoPath(Uri.parse(url));
    }

    /**
     * 切换视频
     *
     * @param uri
     * @return
     */
    public IjkPlayerView switchVideoPath(Uri uri) {
        reset();
        return setVideoPath(uri);
    }

    /**
     * 设置播放资源
     *
     * @param url
     * @return
     */
    public IjkPlayerView setVideoPath(String url) {
        return setVideoPath(Uri.parse(url));
    }

    /**
     * 设置播放资源
     */
    public IjkPlayerView setVideoPath(Uri uri, Map<String, String> headers) {
        mVideoView.setVideoURI(uri, headers);
        if (mCurPosition != INVALID_VALUE) {
            seekTo(mCurPosition);
            mCurPosition = INVALID_VALUE;
        } else {
            seekTo(0);
        }
        return this;
    }

    /**
     * 设置播放资源
     */
    public IjkPlayerView setVideoPath(Uri uri) {
        mVideoView.setVideoURI(uri);
        if (mCurPosition != INVALID_VALUE) {
            seekTo(mCurPosition);
            mCurPosition = INVALID_VALUE;
        } else {
            seekTo(0);
        }
        return this;
    }

    /**
     * 设置标题，全屏的时候可见
     *
     * @param title
     */
    public IjkPlayerView setTitle(String title) {
        mTvTitle.setText(title);
        return this;
    }

    /**
     * 设置只显示全屏状态
     */
    public IjkPlayerView alwaysFullScreen() {
        mIsAlwaysFullScreen = true;
        _setFullScreen(true);
        mIvFullscreen.setVisibility(GONE);
        mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        _setUiLayoutFullscreen();
        return this;
    }

    /**
     * 设置一开始进入是横屏
     */
    public IjkPlayerView firstInFullScreen() {
        _setFullScreen(true);
        mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        _setUiLayoutFullscreen();
        return this;
    }

    /**
     * 开始播放
     *
     * @return
     */
    public void start() {
        if (!mIsAllowMobileDataPlay) {
            return;
        }
        if (mIsManualPause) {
            return;
        }
        if (mIsPlayComplete) {
            mIsPlayComplete = false;
        }
        if (!mVideoView.isPlaying()) {
            mIsManualPause = false;
            mIvPlay.setSelected(true);
            showLoading();
            mVideoView.start();
            // 更新进度
            mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
        }
        // 视频播放时开启屏幕常亮
        mAttachActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 重新开始
     */
    public void reload() {
        mFlReload.setVisibility(GONE);
        if (!mVideoView.isPlaying()) {
            showLoading();
        }
        if (mIsReady) {
            // 确保网络正常时
            if (NetWorkUtils.isNetworkAvailable(mAttachActivity)) {
                mVideoView.reload();
                mVideoView.start();
                if (!mIsLive && mInterruptPosition > 0) {
                    seekTo(mInterruptPosition);
                    mInterruptPosition = 0;
                }
            }
        } else {
            mVideoView.release(false);
            mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);
            start();
        }
        // 更新进度
        mHandler.removeMessages(MSG_UPDATE_SEEK);
        mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
    }

    /**
     * 是否正在播放
     *
     * @return
     */
    public boolean isPlaying() {
        return mVideoView.isPlaying();
    }

    /**
     * 暂停
     */
    public void pause() {
        mIvPlay.setSelected(false);
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
        // 视频暂停时关闭屏幕常亮
        mAttachActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 是否是手动暂停的
     */
    public boolean isManualPause() {
        return mIsManualPause;
    }

    /**
     * 跳转
     *
     * @param position 位置
     */
    public void seekTo(int position) {
        mVideoView.seekTo(position);
    }

    /**
     * 停止
     */
    public void stop() {
        pause();
        mVideoHandler.removeCallbacksAndMessages(null);
        mVideoView.stopPlayback();
        //停止就重新设置 防止重新开始播放 画面不动
        mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);
    }

    /**
     * 重置状态
     */
    public void reset() {
        mIsManualPause = false;
        hideLoading();
        mCurPosition = 0;
        stop();
    }

    /**============================ 控制栏处理 ============================*/


    /**
     * 隐藏视图Runnable
     * <p>
     * 投屏的时候不隐藏
     */
    private Runnable mHideBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mEnableDropScreen) {
                _hideAllView(false);
            }
        }
    };

    /**
     * 隐藏除视频外所有视图
     */
    private void _hideAllView(boolean isTouchLock) {
        mFlTouchLayout.setVisibility(View.GONE);
        mFullscreenTopBar.setVisibility(View.GONE);
        mWindowTopBar.setVisibility(View.GONE);
        mLlBottomBar.setVisibility(View.GONE);
        _showAspectRatioOptions(false);
        if (!isTouchLock) {
            mIvPlayerLock.setVisibility(View.GONE);
            mIsShowBar = false;
        }
        if (mIsNeedRecoverScreen) {
            mTvRecoverScreen.setVisibility(GONE);
        }
    }

    /**
     * 设置控制栏显示或隐藏
     *
     * @param isShowBar
     */
    private void _setControlBarVisible(boolean isShowBar) {
        if (mIsForbidTouch) {
            mIvPlayerLock.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
        } else {
            mLlBottomBar.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
            if (!isShowBar) {
                _showAspectRatioOptions(false);
            }
            // 全屏切换显示的控制栏不一样
            if (mIsFullscreen) {
                // 只在显示控制栏的时候才设置时间，因为控制栏通常不显示且单位为分钟，所以不做实时更新
                mTvSystemTime.setText(StringUtils.getCurFormatTime());
                mFullscreenTopBar.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
                mWindowTopBar.setVisibility(View.GONE);
                mIvPlayerLock.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
                if (mIsNeedRecoverScreen) {
                    mTvRecoverScreen.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
                }
            } else {
                mWindowTopBar.setVisibility(isShowBar && !mIsInHome ? View.VISIBLE : View.GONE);
                mFullscreenTopBar.setVisibility(View.GONE);
                mIvPlayerLock.setVisibility(View.GONE);
                if (mIsNeedRecoverScreen) {
                    mTvRecoverScreen.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * 开关控制栏，单击界面的时候
     */
    private void _toggleControlBar() {
        mIsShowBar = !mIsShowBar;
        _setControlBarVisible(mIsShowBar);
        //投屏的时候不隐藏
        if (mIsShowBar) {
            // 发送延迟隐藏控制栏的操作
            mHandler.postDelayed(mHideBarRunnable, DEFAULT_HIDE_TIMEOUT);
            // 发送更新 Seek 消息
            mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
        }
    }

    /**
     * 显示控制栏
     *
     * @param timeout 延迟隐藏时间
     */
    private void _showControlBar(int timeout) {
        if (!mIsShowBar) {
            mIsShowBar = true;
        }
        _setControlBarVisible(true);
        mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
        // 先移除隐藏控制栏 Runnable，如果 timeout=0 则不做延迟隐藏操作
        mHandler.removeCallbacks(mHideBarRunnable);
        if (timeout != 0) {
            mHandler.postDelayed(mHideBarRunnable, timeout);
        }
    }

    /**
     * 视频切换播放状态，点击播放按钮时
     */
    private void _togglePlayStatus() {
        if (mVideoView.isPlaying()) {
            mIsManualPause = true;
//            pause();
            /**
             * 现在直播 只有播放和停止 没有播放和暂停
             */
            if (startAndStopClickListener != null) {
                startAndStopClickListener.onStop();
            }
        } else {
            mIsManualPause = false;
            mIvPlay.setSelected(true);
            if (startAndStopClickListener != null) {
                startAndStopClickListener.onStart();
            }
//            start();
        }
    }

    /**
     * 投屏的暂停和播放
     * 使用的控件和视频的一样
     */
    private void _toggleDropScreenPlayStatus() {
        if (mIsManualPause) {
            mIsManualPause = false;
            //start
            if (mDropScreenListener != null) {
                mIvPlay.setSelected(true);
                mDropScreenListener.onStart();
            }
        } else {
            mIsManualPause = true;
            //pause
            if (mDropScreenListener != null) {
                mIvPlay.setSelected(false);
                mDropScreenListener.onPause();
            }
        }
    }

    /**
     * 刷新隐藏控制栏的操作
     */
    private void _refreshHideRunnable() {
        mHandler.removeCallbacks(mHideBarRunnable);
        mHandler.postDelayed(mHideBarRunnable, DEFAULT_HIDE_TIMEOUT);
    }

    /**
     * 切换控制锁
     */
    private void _togglePlayerLock() {
        mIsForbidTouch = !mIsForbidTouch;
        mIvPlayerLock.setSelected(mIsForbidTouch);
        if (mIsForbidTouch) {
            mOrientationListener.disable();
            _hideAllView(true);
        } else {
            if (!mIsForbidOrientation) {
                mOrientationListener.enable();
            }
            mFullscreenTopBar.setVisibility(View.VISIBLE);
            mLlBottomBar.setVisibility(View.VISIBLE);
            if (mIsNeedRecoverScreen) {
                mTvRecoverScreen.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * 显示宽高比设置
     *
     * @param isShow
     */
    private void _showAspectRatioOptions(boolean isShow) {
        if (isShow) {
            AnimHelper.doClipViewHeight(mAspectRatioOptions, 0, mAspectOptionsHeight, 150);
        } else {
            ViewGroup.LayoutParams layoutParams = mAspectRatioOptions.getLayoutParams();
            layoutParams.height = 0;
        }
    }

    @Override
    public void onClick(View v) {
        _refreshHideRunnable();
        int id = v.getId();
        if (id == R.id.iv_back) {
            if (mIsAlwaysFullScreen) {
                _exit();
                return;
            }
            mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (id == R.id.iv_back_window) {
            mAttachActivity.finish();
        } else if (id == R.id.iv_play) {
            /**
             * 投屏模式下 暂停和播放由投屏事件触发处理
             */
            if (mEnableDropScreen) {
                _toggleDropScreenPlayStatus();
            } else {
                _togglePlayStatus();
            }
        } else if (id == R.id.iv_fullscreen) {
            if (mFullClickListener != null) {
                mFullClickListener.onClick();
            } else {
                _toggleFullScreen();
            }
        } else if (id == R.id.iv_player_lock) {
            _togglePlayerLock();
        } else if (id == R.id.iv_cancel_skip) {
            mHandler.removeCallbacks(mHideSkipTipRunnable);
            _hideSkipTip();
        } else if (id == R.id.tv_do_skip) {
            showLoading();
            // 视频跳转
            seekTo(mSkipPosition);
            mHandler.removeCallbacks(mHideSkipTipRunnable);
            _hideSkipTip();
        } else if (id == R.id.tv_recover_screen) {
            mVideoView.resetVideoView(true);
            mIsNeedRecoverScreen = false;
            mTvRecoverScreen.setVisibility(GONE);
        } else if (id == R.id.tv_settings) {
            _showAspectRatioOptions(true);
        } else if (id == R.id.iv_reload_back) {
            mAttachActivity.finish();
        } else if (id == R.id.tv_reload) {
            reload();
        } else if (id == R.id.view_video_play) {
            mIsAllowMobileDataPlay = true;
            mLlMobileNotify.setVisibility(GONE);
            start();
        } else if (id == R.id.drop_screen_restart) {
            if (mDropScreenListener != null) {
                mDropScreenListener.onDropScreen();
            }
        } else if (id == R.id.drop_screen_quit) {
            if (mDropScreenListener != null) {
                mDropScreenListener.onQuitDropScreen();
            }
        } else if (id == R.id.drop_screen_add_volume) {
            if (mDropScreenListener != null) {
                mDropScreenListener.onVolumeAdd();
            }
        } else if (id == R.id.drop_screen_reduce_volume) {
            if (mDropScreenListener != null) {
                mDropScreenListener.onVolumeReduce();
            }
        } else if (id == R.id.drop_screen_devices) {
            if (mDropScreenListener != null) {
                mDropScreenListener.onDeviceSearch();
            }
        } else if (id == R.id.tv_screen_shot) {
            screenShot(new File(""));
        } else if (id == R.id.tv_start_record) {
            startRecord("todo url");
        } else if (id == R.id.tv_stop_record) {
            stopRecord();
        }
    }

    /**==================== 屏幕翻转/切换处理 ====================*/

    /**
     * 使能视频翻转
     */
    public IjkPlayerView enableOrientation() {
        mIsForbidOrientation = false;
        mOrientationListener.enable();
        return this;
    }

    /**
     * 全屏切换，点击全屏按钮
     */
    public void _toggleFullScreen() {
        if (WindowUtils.getScreenOrientation(mAttachActivity) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    /**
     * 设置全屏或窗口模式
     *
     * @param isFullscreen
     */
    private void _setFullScreen(boolean isFullscreen) {
        mIsFullscreen = isFullscreen;
        _handleActionBar(isFullscreen);
        _changeHeight(isFullscreen);
        mIvFullscreen.setSelected(isFullscreen);
        mHandler.post(mHideBarRunnable);
        mLlBottomBar.setBackgroundResource(isFullscreen ? R.color.bg_video_view : android.R.color.transparent);
        // 处理三指旋转缩放，如果之前进行了相关操作则全屏时还原之前旋转缩放的状态，窗口模式则将整个屏幕还原为未操作状态
        if (mIsNeedRecoverScreen) {
            if (isFullscreen) {
                mVideoView.adjustVideoView(1.0f);
                mTvRecoverScreen.setVisibility(mIsShowBar ? View.VISIBLE : View.GONE);
            } else {
                mVideoView.resetVideoView(false);
                mTvRecoverScreen.setVisibility(GONE);
            }
        }
        // 非全屏隐藏宽高比设置
        if (!isFullscreen) {
            _showAspectRatioOptions(false);
        }
    }

    /**
     * 处理屏幕翻转
     *
     * @param orientation
     */
    private void _handleOrientation(int orientation) {
        if (mIsForbidTouch || !mIsFullscreen) {
            return;
        }
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return; // 手机平放时，检测不到有效的角度
        }
        // 只检测是否有四个角度的改变
        if (orientation > 350 || orientation < 10) {
            // 0度：手机默认竖屏状态（home键在正下方）
//            if (!mIsAlwaysFullScreen) {
//                mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            }
            Log.d(TAG, "下");
        } else if (orientation > 80 && orientation < 100) {
            // 90度：手机顺时针旋转90度横屏（home建在左侧）
            mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            Log.d(TAG, "左");
        } else if (orientation > 170 && orientation < 190) {
            // 180度：手机顺时针旋转180度竖屏（home键在上方）
            Log.d(TAG, "上");
        } else if (orientation > 260 && orientation < 280) {
            // 270度：手机顺时针旋转270度横屏，（home键在右侧）
            mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            Log.d(TAG, "右");
        }
//
//
//        if (mIsFullscreen && !mIsAlwaysFullScreen) {
//            // 根据角度进行竖屏切换，如果为固定全屏则只能横屏切换
//            if (orientation >= 0 && orientation <= 30 || orientation >= 330) {
//                // 请求屏幕翻转
//                mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//            }
//        } else {
//            // 根据角度进行横屏切换
//            if (orientation >= 60 && orientation <= 120) {
//                mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
//            } else if (orientation >= 240 && orientation <= 300) {
//                mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            }
//        }
    }

    /**
     * 当屏幕执行翻转操作后调用禁止翻转功能，延迟3000ms再使能翻转，避免不必要的翻转
     */
    private void _refreshOrientationEnable() {
        if (!mIsForbidOrientation) {
            mOrientationListener.disable();
            mHandler.removeMessages(MSG_ENABLE_ORIENTATION);
            mHandler.sendEmptyMessageDelayed(MSG_ENABLE_ORIENTATION, 3000);
        }
    }

    /**
     * 隐藏/显示 ActionBar
     *
     * @param isFullscreen
     */
    private void _handleActionBar(boolean isFullscreen) {
        ActionBar supportActionBar = mAttachActivity.getSupportActionBar();
        if (supportActionBar != null) {
            if (isFullscreen) {
                supportActionBar.hide();
            } else {
                supportActionBar.show();
            }
        }
    }

    /**
     * 改变视频布局高度
     *
     * @param isFullscreen
     */
    private void _changeHeight(boolean isFullscreen) {
        if (mIsAlwaysFullScreen) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (mInitHeight == 0) {
            mInitHeight = layoutParams.height;
        }
        if (isFullscreen) {
            // 高度扩展为横向全屏
            layoutParams.height = mWidthPixels;
        } else {
            // 还原高度
            layoutParams.height = mInitHeight;
        }
        setLayoutParams(layoutParams);
    }

    /**
     * 设置UI沉浸式显示
     */
    private void _setUiLayoutFullscreen() {
        if (Build.VERSION.SDK_INT >= 14) {
            // 获取关联 Activity 的 DecorView
            View decorView = mAttachActivity.getWindow().getDecorView();
            // 沉浸式使用这些Flag
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            mAttachActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    /**
     * 屏幕翻转后的处理，在 Activity.configurationChanged() 调用
     * SYSTEM_UI_FLAG_LAYOUT_STABLE：维持一个稳定的布局
     * SYSTEM_UI_FLAG_FULLSCREEN：Activity全屏显示，且状态栏被隐藏覆盖掉
     * SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN：Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住
     * SYSTEM_UI_FLAG_HIDE_NAVIGATION：隐藏虚拟按键(导航栏)
     * SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION：效果同View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
     * SYSTEM_UI_FLAG_IMMERSIVE：沉浸式，从顶部下滑出现状态栏和导航栏会固定住
     * SYSTEM_UI_FLAG_IMMERSIVE_STICKY：黏性沉浸式，从顶部下滑出现状态栏和导航栏过几秒后会缩回去
     *
     * @param newConfig
     */
    public void configurationChanged(Configuration newConfig) {
        _refreshOrientationEnable();
        // 沉浸式只能在SDK19以上实现
        if (Build.VERSION.SDK_INT >= 14) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // 获取关联 Activity 的 DecorView
                View decorView = mAttachActivity.getWindow().getDecorView();
                // 保存旧的配置
                mScreenUiVisibility = decorView.getSystemUiVisibility();
                // 沉浸式使用这些Flag
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                _setFullScreen(true);
                mAttachActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                View decorView = mAttachActivity.getWindow().getDecorView();
                // 还原
                decorView.setSystemUiVisibility(mScreenUiVisibility);
                _setFullScreen(false);
                mAttachActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    /**
     * 从总显示全屏状态退出处理{@link #alwaysFullScreen()}
     */
    private void _exit() {
        if (System.currentTimeMillis() - mExitTime > 2000) {
            Toast.makeText(mAttachActivity, "再按一次退出", Toast.LENGTH_SHORT).show();
            mExitTime = System.currentTimeMillis();
        } else {
            mAttachActivity.finish();
        }
    }

    /**============================ 触屏操作处理 ============================*/

    /**
     * 手势监听
     */
    private OnGestureListener mPlayerGestureListener = new SimpleOnGestureListener() {
        // 是否是按下的标识，默认为其他动作，true为按下标识，false为其他动作
        private boolean isDownTouch;
        // 是否声音控制,默认为亮度控制，true为声音控制，false为亮度控制
        private boolean isVolume;
        // 是否横向滑动，默认为纵向滑动，true为横向滑动，false为纵向滑动
        private boolean isLandscape;

        @Override
        public boolean onDown(MotionEvent e) {
            isDownTouch = true;
            return super.onDown(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!mIsForbidTouch) {
                float mOldX = e1.getX(), mOldY = e1.getY();
                float deltaY = mOldY - e2.getY();
                float deltaX = mOldX - e2.getX();
                if (isDownTouch) {
                    // 判断左右或上下滑动
                    isLandscape = Math.abs(distanceX) >= Math.abs(distanceY);
                    // 判断是声音或亮度控制
                    isVolume = mOldX > getResources().getDisplayMetrics().widthPixels * 0.5f;
                    isDownTouch = false;
                }

                if (isLandscape && enableProgressSlide) {
                    _onProgressSlide(-deltaX / mVideoView.getWidth());
                } else {
                    float percent = deltaY / mVideoView.getHeight();
                    if (isVolume) {
                        _onVolumeSlide(percent);
                    } else {
                        _onBrightnessSlide(percent);
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!mEnableDropScreen) {
                _toggleControlBar();
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!mIsForbidTouch) {
                _refreshHideRunnable();
                _togglePlayStatus();
            }
            return true;
        }
    };

    /**
     * 隐藏视图Runnable
     */
    private Runnable mHideTouchViewRunnable = new Runnable() {
        @Override
        public void run() {
            _hideTouchView();
        }
    };

    /**
     * 触摸监听
     */
    private OnTouchListener mPlayerTouchListener = new OnTouchListener() {
        // 触摸模式：正常、无效、缩放旋转
        private static final int NORMAL = 1;
        private static final int INVALID_POINTER = 2;
        private static final int ZOOM_AND_ROTATE = 3;
        // 触摸模式
        private int mode = NORMAL;
        // 缩放的中点
        private PointF midPoint = new PointF(0, 0);
        // 旋转角度
        private float degree = 0;
        // 用来标识哪两个手指靠得最近，我的做法是取最近的两指中点和余下一指来控制旋转缩放
        private int fingerFlag = INVALID_VALUE;
        // 初始间距
        private float oldDist;
        // 缩放比例
        private float scale;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (MotionEventCompat.getActionMasked(event)) {
                case MotionEvent.ACTION_DOWN:
                    mode = NORMAL;
                    mHandler.removeCallbacks(mHideBarRunnable);
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == 3 && mIsFullscreen && enableTreePointRotate) {
                        _hideTouchView();
                        // 进入三指旋转缩放模式，进行相关初始化
                        mode = ZOOM_AND_ROTATE;
                        MotionEventUtils.midPoint(midPoint, event);
                        fingerFlag = MotionEventUtils.calcFingerFlag(event);
                        degree = MotionEventUtils.rotation(event, fingerFlag);
                        oldDist = MotionEventUtils.calcSpacing(event, fingerFlag);
                        // 获取视频的 Matrix
                        mSaveMatrix = mVideoView.getVideoTransform();
                    } else {
                        mode = INVALID_POINTER;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == ZOOM_AND_ROTATE) {
                        // 处理旋转
                        float newRotate = MotionEventUtils.rotation(event, fingerFlag);
                        mVideoView.setVideoRotation((int) (newRotate - degree));
                        // 处理缩放
                        mVideoMatrix.set(mSaveMatrix);
                        float newDist = MotionEventUtils.calcSpacing(event, fingerFlag);
                        scale = newDist / oldDist;
                        mVideoMatrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        mVideoView.setVideoTransform(mVideoMatrix);
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    if (mode == ZOOM_AND_ROTATE) {
                        // 调整视频界面，让界面居中显示在屏幕
                        mIsNeedRecoverScreen = mVideoView.adjustVideoView(scale);
                        if (mIsNeedRecoverScreen && mIsShowBar) {
                            mTvRecoverScreen.setVisibility(VISIBLE);
                        }
                    }
                    mode = INVALID_POINTER;
                    break;
            }
            // 触屏手势处理
            if (mode == NORMAL) {
                if (mGestureDetector.onTouchEvent(event)) {
                    return true;
                }
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP) {
                    _endGesture();
                }
            }
            return false;
        }
    };

    /**
     * 设置快进
     *
     * @param time
     */
    private void _setFastForward(String time) {
        if (mFlTouchLayout.getVisibility() == View.GONE) {
            mFlTouchLayout.setVisibility(View.VISIBLE);
        }
        if (mTvFastForward.getVisibility() == View.GONE) {
            mTvFastForward.setVisibility(View.VISIBLE);
        }
        mTvFastForward.setText(time);
    }

    /**
     * 隐藏触摸视图
     */
    private void _hideTouchView() {
        if (mFlTouchLayout.getVisibility() == View.VISIBLE) {
            mTvFastForward.setVisibility(View.GONE);
            mTvVolume.setVisibility(View.GONE);
            mTvBrightness.setVisibility(View.GONE);
            mFlTouchLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 快进或者快退滑动改变进度，这里处理触摸滑动不是拉动 SeekBar
     *
     * @param percent 拖拽百分比
     */
    private void _onProgressSlide(float percent) {
        int position = mVideoView.getCurrentPosition();
        long duration = mVideoView.getDuration();
        // 单次拖拽最大时间差为100秒或播放时长的1/2
        long deltaMax = Math.min(100 * 1000, duration / 2);
        // 计算滑动时间
        long delta = (long) (deltaMax * percent);
        // 目标位置
        mTargetPosition = delta + position;
        if (mTargetPosition > duration) {
            mTargetPosition = duration;
        } else if (mTargetPosition <= 0) {
            mTargetPosition = 0;
        }
        int deltaTime = (int) ((mTargetPosition - position) / 1000);
        String desc;
        // 对比当前位置来显示快进或后退
        if (mTargetPosition > position) {
            desc = generateTime(mTargetPosition) + "/" + generateTime(duration) + "\n" + "+" + deltaTime + "秒";
        } else {
            desc = generateTime(mTargetPosition) + "/" + generateTime(duration) + "\n" + deltaTime + "秒";
        }
        _setFastForward(desc);
    }

    /**
     * 设置声音控制显示
     *
     * @param volume
     */
    private void _setVolumeInfo(int volume) {
        if (mFlTouchLayout.getVisibility() == View.GONE) {
            mFlTouchLayout.setVisibility(View.VISIBLE);
        }
        if (mTvVolume.getVisibility() == View.GONE) {
            mTvVolume.setVisibility(View.VISIBLE);
        }
        mTvVolume.setText((volume * 100 / mMaxVolume) + "%");
    }

    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void _onVolumeSlide(float percent) {
        /**
         * 投屏模式下 不处理滑动来改变声音
         */
        if (mEnableDropScreen) {
            return;
        }

        if (mCurVolume == INVALID_VALUE) {
            mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (mCurVolume < 0) {
                mCurVolume = 0;
            }
        }
        int index = (int) (percent * mMaxVolume) + mCurVolume;
        if (index > mMaxVolume) {
            index = mMaxVolume;
        } else if (index < 0) {
            index = 0;
        }
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        // 变更进度条
        _setVolumeInfo(index);
    }


    /**
     * 递增或递减音量，量度按最大音量的 1/15
     *
     * @param isIncrease 递增或递减
     */
    private void _setVolume(boolean isIncrease) {
        /**
         * 投屏模式下 直接触发投屏模式音量事件
         */
        if (mEnableDropScreen) {
            if (mDropScreenListener != null) {
                if (isIncrease) {
                    mDropScreenListener.onVolumeAdd();
                } else {
                    mDropScreenListener.onVolumeReduce();
                }
            }
            return;
        }
        int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (isIncrease) {
            curVolume += mMaxVolume / 15;
        } else {
            curVolume -= mMaxVolume / 15;
        }
        if (curVolume > mMaxVolume) {
            curVolume = mMaxVolume;
        } else if (curVolume < 0) {
            curVolume = 0;
        }
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, 0);
        // 变更进度条
        _setVolumeInfo(curVolume);
        mHandler.removeCallbacks(mHideTouchViewRunnable);
        mHandler.postDelayed(mHideTouchViewRunnable, 1000);
    }

    /**
     * 设置亮度控制显示
     *
     * @param brightness
     */
    private void _setBrightnessInfo(float brightness) {
        if (mFlTouchLayout.getVisibility() == View.GONE) {
            mFlTouchLayout.setVisibility(View.VISIBLE);
        }
        if (mTvBrightness.getVisibility() == View.GONE) {
            mTvBrightness.setVisibility(View.VISIBLE);
        }
        mTvBrightness.setText(Math.ceil(brightness * 100) + "%");
    }

    /**
     * 滑动改变亮度大小
     *
     * @param percent
     */
    private void _onBrightnessSlide(float percent) {
        /**
         * 投屏模式下 不处理滑动来改变亮度
         */
        if (mEnableDropScreen) {
            return;
        }


        if (mCurBrightness < 0) {
            mCurBrightness = mAttachActivity.getWindow().getAttributes().screenBrightness;
            if (mCurBrightness < 0.0f) {
                mCurBrightness = DEFAULT_VOLUME;
            } else if (mCurBrightness < 0.01f) {
                mCurBrightness = 0.01f;
            }
        }
        WindowManager.LayoutParams attributes = mAttachActivity.getWindow().getAttributes();
        attributes.screenBrightness = mCurBrightness + percent;
        if (attributes.screenBrightness > 1.0f) {
            attributes.screenBrightness = 1.0f;
        } else if (attributes.screenBrightness < 0.01f) {
            attributes.screenBrightness = 0.01f;
        }
        _setBrightnessInfo(attributes.screenBrightness);
        mAttachActivity.getWindow().setAttributes(attributes);
    }

    /**
     * 手势结束调用
     */
    private void _endGesture() {
        if (mTargetPosition >= 0 && mTargetPosition != mVideoView.getCurrentPosition()) {
            // 更新视频播放进度
            seekTo((int) mTargetPosition);
            mTargetPosition = INVALID_VALUE;
        }
        // 隐藏触摸操作显示图像
        _hideTouchView();
        _refreshHideRunnable();
        mCurVolume = INVALID_VALUE;
        mCurBrightness = INVALID_VALUE;
    }

    /**
     * ============================ 播放状态控制 ============================
     */


    // 视频播放状态监听
    private OnInfoListener mInfoListener = new OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int status, int extra) {
            _switchStatus(status);
            if (mOutsideInfoListener != null) {
                mOutsideInfoListener.onInfo(iMediaPlayer, status, extra);
            }
            return true;
        }
    };

    /**
     * 视频播放状态处理
     *
     * @param status
     */
    private void _switchStatus(int status) {
        Log.i("IjkPlayerView", "status " + status);
        switch (status) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                showLoading();
                mHandler.removeMessages(MSG_TRY_RELOAD);
            case MediaPlayerParams.STATE_PREPARING:
                break;
            case MediaPlayerParams.STATE_PREPARED:
                mIsReady = true;
                break;
            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                mVideoHandler.sendEmptyMessageDelayed(VideoHandler.WHAT_BUFFER_TIMEOUT, new com.pockettv.player.Settings(getContext()).getDefaultTimeout());
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                mVideoHandler.removeMessages(VideoHandler.WHAT_BUFFER_TIMEOUT);
                hideLoading();
                // 更新进度
                mHandler.removeMessages(MSG_UPDATE_SEEK);
                mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
                if (mSkipPosition != INVALID_VALUE) {
                    _showSkipTip(); // 显示跳转提示
                }
                if (mVideoView.isPlaying() && mIsNetConnected) {
                    mInterruptPosition = 0;
                    if (!mIvPlay.isSelected()) {
                        // 这里处理断网重连后不会播放情况
                        mVideoView.start();
                        mIvPlay.setSelected(true);
                    }
                }
                break;
            case MediaPlayerParams.STATE_PLAYING:
                hideLoading();
                mHandler.removeMessages(MSG_TRY_RELOAD);
                break;

            case MediaPlayerParams.STATE_ERROR:
                if (errorListener != null) {
                    errorListener.onNeedSwitchSource();
                }
                mInterruptPosition = Math.max(mVideoView.getInterruptPosition(), mInterruptPosition);
                stop();
                break;
            case MediaPlayerParams.STATE_NO_SIGNAL_ERROR:
                mInterruptPosition = Math.max(mVideoView.getInterruptPosition(), mInterruptPosition);
                stop();
                //无信号 可以停止 可以重试
                mFlReload.setBackgroundResource(R.mipmap.bg_no_signal);
                hideLoading();
                mFlReload.setVisibility(VISIBLE);
                break;
            case MediaPlayerParams.STATE_NO_COPYRIGHT_ERROR:
                //无版权 直接 停止播放
                stop();
                mFlReload.setBackgroundResource(R.mipmap.bg_no_signal);
                hideLoading();
                mFlReload.setVisibility(VISIBLE);
                if (!mIsInHome) {
                    mIvBackReload.setVisibility(VISIBLE);
                    mTvReload.setVisibility(GONE);
                } else {
                    mIvBackReload.setVisibility(GONE);
                }
                break;
            case MediaPlayerParams.STATE_COMPLETED:
                pause();
                hideLoading();
                mIsPlayComplete = true;
                if (mCompletionListener != null) {
                    mCompletionListener.onCompletion(mVideoView.getMediaPlayer());
                }
                break;
        }
    }

    /**============================ Listener ============================*/

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
        mVideoView.setOnPreparedListener(l);
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public IjkPlayerView setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
        mCompletionListener = l;
//        mVideoView.setOnCompletionListener(l);
        return this;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public IjkPlayerView setOnErrorListener(IMediaPlayer.OnErrorListener l) {
        mVideoView.setOnErrorListener(l);
        return this;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public IjkPlayerView setOnInfoListener(OnInfoListener l) {
        mOutsideInfoListener = l;
        return this;
    }

    /**
     * ============================ 跳转提示 ============================
     */

    // 取消跳转
    private ImageView mIvCancelSkip;
    // 跳转时间
    private TextView mTvSkipTime;
    // 执行跳转
    private TextView mTvDoSkip;
    // 跳转布局
    private View mLlSkipLayout;
    // 跳转目标时间
    private int mSkipPosition = INVALID_VALUE;

    /**
     * 跳转提示初始化
     */
    private void _initVideoSkip() {
        mLlSkipLayout = findViewById(R.id.ll_skip_layout);
        mIvCancelSkip = (ImageView) findViewById(R.id.iv_cancel_skip);
        mTvSkipTime = (TextView) findViewById(R.id.tv_skip_time);
        mTvDoSkip = (TextView) findViewById(R.id.tv_do_skip);
        mIvCancelSkip.setOnClickListener(this);
        mTvDoSkip.setOnClickListener(this);
    }

    /**
     * 返回当前进度
     *
     * @return
     */
    public int getCurPosition() {
        return mVideoView.getCurrentPosition();
    }

    /**
     * 设置跳转提示
     *
     * @param targetPosition 目标进度,单位:ms
     */
    public IjkPlayerView setSkipTip(int targetPosition) {
        mSkipPosition = targetPosition;
        return this;
    }

    /**
     * 显示跳转提示
     */
    private void _showSkipTip() {
        if (mSkipPosition != INVALID_VALUE && mLlSkipLayout.getVisibility() == GONE) {
            mLlSkipLayout.setVisibility(VISIBLE);
            mTvSkipTime.setText(generateTime(mSkipPosition));
            AnimHelper.doSlideRightIn(mLlSkipLayout, mWidthPixels, 0, 800);
            mHandler.postDelayed(mHideSkipTipRunnable, DEFAULT_HIDE_TIMEOUT * 3);
        }
    }

    /**
     * 隐藏跳转提示
     */
    private void _hideSkipTip() {
        if (mLlSkipLayout.getVisibility() == GONE) {
            return;
        }
        ViewCompat.animate(mLlSkipLayout).translationX(-mLlSkipLayout.getWidth()).alpha(0).setDuration(500)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(View view) {
                        mLlSkipLayout.setVisibility(GONE);
                    }
                }).start();
        mSkipPosition = INVALID_VALUE;
    }

    /**
     * 隐藏跳转提示线程
     */
    private Runnable mHideSkipTipRunnable = new Runnable() {
        @Override
        public void run() {
            _hideSkipTip();
        }
    };


    /**
     * ============================ 电量、时间、锁屏、截屏 ============================
     */

    // 电量显示
    private ProgressBar mPbBatteryLevel;
    // 系统时间显示
    private TextView mTvSystemTime;
    // 电量变化广播接收器
    private BatteryBroadcastReceiver mBatteryReceiver;
    // 网络变化广播
    private NetBroadcastReceiver mNetReceiver;

    /**
     * 初始化电量、锁屏、时间处理
     */
    private void _initReceiver() {
        mPbBatteryLevel = (ProgressBar) findViewById(R.id.pb_battery);
        mTvSystemTime = (TextView) findViewById(R.id.tv_system_time);
        mTvSystemTime.setText(StringUtils.getCurFormatTime());
        mBatteryReceiver = new BatteryBroadcastReceiver();
        mNetReceiver = new NetBroadcastReceiver();
        //注册接受广播
        mAttachActivity.registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mAttachActivity.registerReceiver(mNetReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void showLoading() {
        mLoadingView.setVisibility(VISIBLE);
    }

    public void showLoading(String text) {
        mLoadingView.setVisibility(VISIBLE);
        mLoadingText.setText(text);
    }

    public void hideLoading() {
        mLoadingView.setVisibility(GONE);
        mLoadingText.setText("");
    }


    /**
     * 接受电量改变广播
     */
    class BatteryBroadcastReceiver extends BroadcastReceiver {

        // 低电量临界值
        private static final int BATTERY_LOW_LEVEL = 15;

        @Override
        public void onReceive(Context context, Intent intent) {
            // 接收电量变化信息
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                // 电量百分比
                int curPower = level * 100 / scale;
                int status = intent.getIntExtra("status", BatteryManager.BATTERY_HEALTH_UNKNOWN);
                // SecondaryProgress 用来展示低电量，Progress 用来展示正常电量
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    mPbBatteryLevel.setSecondaryProgress(0);
                    mPbBatteryLevel.setProgress(curPower);
                    mPbBatteryLevel.setBackgroundResource(R.mipmap.ic_battery_charging);
                } else if (curPower < BATTERY_LOW_LEVEL) {
                    mPbBatteryLevel.setProgress(0);
                    mPbBatteryLevel.setSecondaryProgress(curPower);
                    mPbBatteryLevel.setBackgroundResource(R.mipmap.ic_battery_red);
                } else {
                    mPbBatteryLevel.setSecondaryProgress(0);
                    mPbBatteryLevel.setProgress(curPower);
                    mPbBatteryLevel.setBackgroundResource(R.mipmap.ic_battery);
                }
            }
        }
    }

    private boolean mIsNetConnected;

    public class NetBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果相等的话就说明网络状态发生了变化
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                mIsNetConnected = NetWorkUtils.isNetworkAvailable(mAttachActivity);
            }
        }
    }


    /**
     * ============================ 自定义listner 和版权屏蔽 请求超时还原等 ============================
     */

    static class VideoHandler extends Handler {

        public static final int WHAT_BUFFER_TIMEOUT = 0x1;

        public static final int WHAT_CHECK_BLOCK_PLAY = 0x2;

        private WeakReference<IjkPlayerView> videoViewWeakReference;

        public VideoHandler(IjkPlayerView videoView) {
            videoViewWeakReference = new WeakReference<>(videoView);
        }

        @Override
        public void handleMessage(Message msg) {

            IjkPlayerView playerView = videoViewWeakReference.get();
            if (playerView == null) {
                return;
            }

            switch (msg.what) {
                //换源
                case WHAT_BUFFER_TIMEOUT:
                    //onError处理
                    if (playerView.getErrorListener() != null) {
                        playerView.getErrorListener().onNeedSwitchSource();
                    }
                    break;
                //检查版权 一分钟一次
                case WHAT_CHECK_BLOCK_PLAY:
                    try {
                        if (playerView.getCopyrightChecker() != null
                                && playerView.isPlaying()
                                && playerView.getCopyrightChecker().isBlock()) {
                            //屏蔽版权
                            playerView.noCopyRightDeal();
                            break;
                        }
                        removeMessages(WHAT_CHECK_BLOCK_PLAY);
                        sendEmptyMessageDelayed(WHAT_CHECK_BLOCK_PLAY, TimeUnit.MINUTES.toMillis(1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    public void noSignalDeal() {
        _switchStatus(MediaPlayerParams.STATE_NO_SIGNAL_ERROR);
    }

    public void noCopyRightDeal() {
        _switchStatus(MediaPlayerParams.STATE_NO_COPYRIGHT_ERROR);
    }

    private OnFullClickListener mFullClickListener;

    public IjkPlayerView setFullClickListener(OnFullClickListener mFullClickListener) {
        this.mFullClickListener = mFullClickListener;
        return this;
    }

    public interface OnFullClickListener {
        void onClick();
    }

    public interface StartAndStopClickListener {
        void onStart();

        void onStop();
    }

    private StartAndStopClickListener startAndStopClickListener;

    /**
     * 现在直播 只有播放和停止 没有播放和暂停
     */
    public IjkPlayerView setStartAndStopClickListener(StartAndStopClickListener startAndStopClickListener) {
        this.startAndStopClickListener = startAndStopClickListener;
        return this;
    }

    private CopyrightChecker copyrightChecker;

    public interface CopyrightChecker {
        boolean isBlock();
    }

    public CopyrightChecker getCopyrightChecker() {
        return copyrightChecker;
    }

    /**
     * 口袋电视暂时不屏蔽
     */
    public IjkPlayerView enableCopyrightChecker(CopyrightChecker copyrightChecker) {
        this.copyrightChecker = copyrightChecker;
        mVideoHandler.sendEmptyMessage(VideoHandler.WHAT_CHECK_BLOCK_PLAY);
        return this;
    }

    public interface ErrorListener {
        void onNeedSwitchSource();
    }

    private ErrorListener errorListener;

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    public IjkPlayerView setSwitchSourceWhenErrorListener(ErrorListener listener) {
        this.errorListener = listener;
        return this;
    }

    public IjkPlayerView setIsInHome(boolean isInHome) {
        this.mIsInHome = isInHome;
        return this;
    }

    NetWorkChangReceiver mNetChangedReceiver;

    public void registerNetChangedReceiver() {
        if (mNetChangedReceiver == null) {
            mNetChangedReceiver = new NetWorkChangReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            mAttachActivity.registerReceiver(mNetChangedReceiver, filter);
        }
    }

    public void unRegisterNetChangedReceiver() {
        if (mNetChangedReceiver != null) {
            mAttachActivity.unregisterReceiver(mNetChangedReceiver);
        }
    }

    public class NetWorkChangReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                //获取联网状态的NetworkInfo对象
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    //如果当前的网络连接成功并且网络连接可用
                    if (new com.pockettv.player.Settings(getContext()).isMobileNetworkHint() && NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                        if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                            if (isPlaying()) {
                                pause();
                                mLlMobileNotify.setVisibility(VISIBLE);
                                mIsAllowMobileDataPlay = false;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ============================ 投屏相关 ============================
     */

    public boolean isEnableDropScreen() {
        return mEnableDropScreen;
    }

    /**
     * 是否开启投屏模式
     *
     * @param enable 是否开启
     */
    public void enableDropScreen(boolean enable) {
        mEnableDropScreen = enable;
        if (enable) {
            //开启控制栏
            _showControlBar(DEFAULT_HIDE_TIMEOUT);
            mViewDropcreen.setVisibility(VISIBLE);
            reset();
        } else {
            mViewDropcreen.setVisibility(GONE);
        }
    }

    /**
     * 设置投屏播放与暂停按钮的状态
     *
     * @param start 是否正在播放
     */
    public void setDropScrennPlayStatus(boolean start) {
        if (!mEnableDropScreen) return;
        if (start) {
            mIvPlay.setSelected(true);
            mIsManualPause = false;
        } else {
            mIvPlay.setSelected(false);
            mIsManualPause = true;
        }
    }

    /**
     * 设置投屏的 "重新投屏" 的按钮状态
     * 错误的时候展示，成功的时候隐藏
     *
     * @param enable
     */
    public void setDropScrennPlayRestartStatus(boolean enable) {
        if (!mEnableDropScreen) return;
        mTvDropScreenRestart.setVisibility(enable ? VISIBLE : GONE);
    }

    public interface DropScreenListener {
        /**
         * 投屏"重选设备"的事件触发
         */
        void onDeviceSearch();

        /**
         * 投屏"重选设备"的事件触发
         */
        void onDropScreen();

        /**
         * 投屏"音量增加"的事件触发
         */
        void onVolumeAdd();

        /**
         * 投屏"音量减少"的事件触发
         */
        void onVolumeReduce();

        /**
         * 投屏"暂停"的事件触发
         */
        void onPause();

        /**
         * 投屏"播放"的事件触发
         */
        void onStart();

        /**
         * 投屏"退出投屏"的事件触发
         */
        void onQuitDropScreen();
    }

    private DropScreenListener mDropScreenListener;

    /**
     * 设置投屏的事件监听
     *
     * @param mDropScreenListener
     * @return
     */
    public IjkPlayerView setDropScreenListener(DropScreenListener mDropScreenListener) {
        this.mDropScreenListener = mDropScreenListener;
        return this;
    }

    /**
     * 设置投屏的标题
     *
     * @param friendlyName 盒子的名字
     */
    public void setDropScreenName(String friendlyName) {
        if (!mEnableDropScreen) return;
        mTvDropScreenName.setText(friendlyName);
    }

    /**
     * 设置投屏的状态
     *
     * @param status 投屏状态
     */
    public void setDropScreenStatus(String status) {
        if (!mEnableDropScreen) return;
        mTvDropScreenStatus.setText(status);
    }

    /**
     * 重置投屏的状态
     */
    public void resetDropScreen() {
        setDropScreenName("");
        setDropScreenStatus("");
        setDropScrennPlayStatus(true);
        setDropScrennPlayRestartStatus(false);
    }


    /**
     * 录屏相关===========================================================
     */

    public void startRecord(String path) {
        mIsRecording = true;
        mVideoView.startRecord(path);
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void stopRecord() {
        mIsRecording = false;
        mVideoView.stopRecord();
    }

    public void screenShot(File file) {
        mVideoView.snapshotPicture(file);
    }
}
