package com.pockettv.player;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.pockettv.player.MediaCodecType.HARD;
import static com.pockettv.player.MediaCodecType.SMART;
import static com.pockettv.player.MediaCodecType.SOFT;


/**
 * 视频解码模式
 */
@IntDef({SMART, HARD, SOFT})
@Retention(RetentionPolicy.SOURCE)
public @interface MediaCodecType {

    int SMART = 0;

    int HARD = 1;

    int SOFT = 2;

}
