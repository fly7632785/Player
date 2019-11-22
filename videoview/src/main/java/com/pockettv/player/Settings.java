/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pockettv.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.pockettv.player.media.IRenderView;

import java.util.concurrent.TimeUnit;


public class Settings {
    private Context mAppContext;
    private SharedPreferences mSharedPreferences;

    public static final int PV_PLAYER__Auto = 0;
    public static final int PV_PLAYER__AndroidMediaPlayer = 1;
    public static final int PV_PLAYER__IjkMediaPlayer = 2;
    public static final int PV_PLAYER__IjkExoMediaPlayer = 3;
    public static final int PV_PLAYER__IjkKooMediaPlayer = 4;

    public static final String PREF_MOBILE_NETWORK_HINT = "pref_mobile_network_hint";

    public Settings(Context context) {
        mAppContext = context.getApplicationContext();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
    }

    public boolean getEnableBackgroundPlay() {
        String key = mAppContext.getString(R.string.pref_key_enable_background_play);
        return mSharedPreferences.getBoolean(key, false);
    }

    public int getPlayer() {
        String key = mAppContext.getString(R.string.pref_key_player);
        String value = mSharedPreferences.getString(key, "");
        try {
            return Integer.valueOf(value).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setPlayer(int playerType) {
        String key = mAppContext.getString(R.string.pref_key_player);
        mSharedPreferences.edit().putString(key, String.valueOf(playerType)).apply();
    }

    public int getUsingMediaCodec() {
        String key = mAppContext.getString(R.string.pref_key_using_media_codec);
        return mSharedPreferences.getInt(key, 2);
    }

    public void setUsingMediaCodec(int codec) {
        String key = mAppContext.getString(R.string.pref_key_using_media_codec);
        mSharedPreferences.edit().putInt(key, codec).apply();
    }

    public boolean getUsingMediaCodecAutoRotate() {
        String key = mAppContext.getString(R.string.pref_key_using_media_codec_auto_rotate);
        return mSharedPreferences.getBoolean(key, false);
    }

    public int getAspectRatio() {
        String key = mAppContext.getString(R.string.pref_key_aspect_ratio);
        return mSharedPreferences.getInt(key, IRenderView.AR_16_9_FIT_PARENT);
    }

    public void setAspectRatio(int index) {
        String key = mAppContext.getString(R.string.pref_key_aspect_ratio);
        mSharedPreferences.edit().putInt(key, index).apply();
    }

    public boolean getMediaCodecHandleResolutionChange() {
        String key = mAppContext.getString(R.string.pref_key_media_codec_handle_resolution_change);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getUsingOpenSLES() {
        String key = mAppContext.getString(R.string.pref_key_using_opensl_es);
        return mSharedPreferences.getBoolean(key, false);
    }

    public String getPixelFormat() {
        String key = mAppContext.getString(R.string.pref_key_pixel_format);
        return mSharedPreferences.getString(key, "");
    }

    public boolean getEnableNoView() {
        String key = mAppContext.getString(R.string.pref_key_enable_no_view);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getEnableSurfaceView() {
        String key = mAppContext.getString(R.string.pref_key_enable_surface_view);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getEnableTextureView() {
        String key = mAppContext.getString(R.string.pref_key_enable_texture_view);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getEnableDetachedSurfaceTextureView() {
        String key = mAppContext.getString(R.string.pref_key_enable_detached_surface_texture);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getUsingMediaDataSource() {
        String key = mAppContext.getString(R.string.pref_key_using_mediadatasource);
        return mSharedPreferences.getBoolean(key, false);
    }

    public String getLastDirectory() {
        String key = mAppContext.getString(R.string.pref_key_last_directory);
        return mSharedPreferences.getString(key, "/");
    }

    public void setLastDirectory(String path) {
        String key = mAppContext.getString(R.string.pref_key_last_directory);
        mSharedPreferences.edit().putString(key, path).apply();
    }

    public int getDefaultTimeout() {
        String key = mAppContext.getString(R.string.pref_key_timeout);
        return mSharedPreferences.getInt(key, (int) TimeUnit.SECONDS.toMillis(8));
    }

    public void setDefaultTimeout(int time) {
        String key = mAppContext.getString(R.string.pref_key_timeout);
        mSharedPreferences.edit().putInt(key, time).apply();
    }

    /**
     * 使用流量提醒
     *
     * @return
     */
    public boolean isMobileNetworkHint() {
        return mSharedPreferences.getBoolean(PREF_MOBILE_NETWORK_HINT, true);
    }

    public void setMobileNetworkHint(boolean isHint) {
        mSharedPreferences.edit().putBoolean(PREF_MOBILE_NETWORK_HINT, isHint).apply();
    }

}
