package com.pockettv.dropscreen;

/**
 * 说明：
 */
public class Config {

    // mp4 格式
    //http://mp4.res.hunantv.com/video/1155/79c71f27a58042b23776691d206d23bf.mp4
    // ts 格式
    public static String TEST_URL = "http://buka.maoxiongtv.com/buka/zhejiang.m3u8?auth_key=1715629686-0-0-8dc525d64171a7d0d7d1d12002507979";
    // m3u8 格式
//    public static String TEST_URL = "http://127.0.0.1:8602/10051/hls-seg-1159605.ts";

    /*** 因为后台给的地址是固定的，如果不测试投屏，请设置为 false*/
    public static final boolean DLAN_DEBUG = true;
    /*** 轮询获取播放位置时间间隔(单位毫秒)*/
    public static final long REQUEST_GET_INFO_INTERVAL = 2000;
    /** 投屏设备支持进度回传 */
    private boolean hasRelTimePosCallback;
    private static Config mInstance;

    public static Config getInstance() {
        if (null == mInstance) {
            mInstance = new Config();
        }
        return mInstance;
    }

    public boolean getHasRelTimePosCallback() {
        return hasRelTimePosCallback;
    }

    public void setHasRelTimePosCallback(boolean hasRelTimePosCallback) {
        this.hasRelTimePosCallback = hasRelTimePosCallback;
    }

}
