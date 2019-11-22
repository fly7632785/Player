package com.pockettv.player.media;

public class MediaPlayerParams {

    /**================================= 播放状态 =================================*/
    // 空闲
    public static final int STATE_IDLE = 330;
    // 错误
    public static final int STATE_ERROR = 331;
    // 加载中
    public static final int STATE_PREPARING = 332;
    // 加载完成
    public static final int STATE_PREPARED = 333;
    // 播放中
    public static final int STATE_PLAYING = 334;
    // 暂停
    public static final int STATE_PAUSED = 335;
    // 结束
    public static final int STATE_COMPLETED = 336;
    // 无版权错误
    public static final int STATE_NO_COPYRIGHT_ERROR = 337;
    // 无信号错误
    public static final int STATE_NO_SIGNAL_ERROR = 338;
}
