package com.pockettv.player.media;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.View;
import android.widget.TableLayout;

import com.pockettv.player.R;

import java.lang.ref.WeakReference;
import java.util.Locale;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MediaPlayerProxy;

public class InfoHudViewHolder {
    private TableLayoutBinder mTableLayoutBinder;
    private SparseArray<View> mRowMap = new SparseArray<View>();
    private IMediaPlayer mMediaPlayer;
    private long mLoadCost = 0;
    private long mSeekCost = 0;

    private Handler mHandler;

    public InfoHudViewHolder(Context context, TableLayout tableLayout) {
        mTableLayoutBinder = new TableLayoutBinder(context, tableLayout);
        mHandler = new UpdateHuDHandler(this);
    }

    private void appendSection(int nameId) {
        mTableLayoutBinder.appendSection(nameId);
    }

    private void appendRow(int nameId) {
        View rowView = mTableLayoutBinder.appendRow2(nameId, null);
        mRowMap.put(nameId, rowView);
    }

    private void setRowValue(int id, String value) {
        View rowView = mRowMap.get(id);
        if (rowView == null) {
            rowView = mTableLayoutBinder.appendRow2(id, value);
            mRowMap.put(id, rowView);
        } else {
            mTableLayoutBinder.setValueText(rowView, value);
        }
    }

    public void setMediaPlayer(IMediaPlayer mp) {
        mMediaPlayer = mp;
        if (mMediaPlayer != null) {
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
        } else {
            mHandler.removeMessages(MSG_UPDATE_HUD);
        }
    }

    private static String formatedDurationMilli(long duration) {
        if (duration >=  1000) {
            return String.format(Locale.US, "%.2f sec", ((float)duration) / 1000);
        } else {
            return String.format(Locale.US, "%d msec", duration);
        }
    }

    private static String formatedSpeed(long bytes,long elapsed_milli) {
        if (elapsed_milli <= 0) {
            return "0 B/s";
        }

        if (bytes <= 0) {
            return "0 B/s";
        }

        float bytes_per_sec = ((float)bytes) * 1000.f /  elapsed_milli;
        if (bytes_per_sec >= 1000 * 1000) {
            return String.format(Locale.US, "%.2f MB/s", ((float)bytes_per_sec) / 1000 / 1000);
        } else if (bytes_per_sec >= 1000) {
            return String.format(Locale.US, "%.1f KB/s", ((float)bytes_per_sec) / 1000);
        } else {
            return String.format(Locale.US, "%d B/s", (long)bytes_per_sec);
        }
    }

    public void updateLoadCost(long time)  {
        mLoadCost = time;
    }

    public void updateSeekCost(long time)  {
        mSeekCost = time;
    }

    private static String formatedSize(long bytes) {
        if (bytes >= 100 * 1000) {
            return String.format(Locale.US, "%.2f MB", ((float)bytes) / 1000 / 1000);
        } else if (bytes >= 100) {
            return String.format(Locale.US, "%.1f KB", ((float)bytes) / 1000);
        } else {
            return String.format(Locale.US, "%d B", bytes);
        }
    }

    private static final int MSG_UPDATE_HUD = 1;

    private static class UpdateHuDHandler extends Handler {

        private WeakReference<InfoHudViewHolder> holderWeakReference;

        public UpdateHuDHandler(InfoHudViewHolder infoHudViewHolder) {
            this.holderWeakReference = new WeakReference<>(infoHudViewHolder);
        }

        @Override
        public void handleMessage(Message msg) {
            InfoHudViewHolder infoHudViewHolder = holderWeakReference.get();
            if (infoHudViewHolder == null) return;

            switch (msg.what) {
                case MSG_UPDATE_HUD: {
                    IjkMediaPlayer mp = null;
                    if (infoHudViewHolder.mMediaPlayer == null)
                        break;
                    if (infoHudViewHolder.mMediaPlayer instanceof IjkMediaPlayer) {
                        mp = (IjkMediaPlayer) infoHudViewHolder.mMediaPlayer;
                    } else if (infoHudViewHolder.mMediaPlayer instanceof MediaPlayerProxy) {
                        MediaPlayerProxy proxy = (MediaPlayerProxy) infoHudViewHolder.mMediaPlayer;
                        IMediaPlayer internal = proxy.getInternalMediaPlayer();
                        if (internal != null && internal instanceof IjkMediaPlayer)
                            mp = (IjkMediaPlayer) internal;
                    }
                    if (mp == null)
                        break;

                    int vdec = mp.getVideoDecoder();
                    switch (vdec) {
                        case IjkMediaPlayer.FFP_PROPV_DECODER_AVCODEC:
                            infoHudViewHolder.setRowValue(R.string.vdec, "avcodec");
                            break;
                        case IjkMediaPlayer.FFP_PROPV_DECODER_MEDIACODEC:
                            infoHudViewHolder.setRowValue(R.string.vdec, "MediaCodec");
                            break;
                        default:
                            infoHudViewHolder.setRowValue(R.string.vdec, "");
                            break;
                    }

                    float fpsOutput = mp.getVideoOutputFramesPerSecond();
                    float fpsDecode = mp.getVideoDecodeFramesPerSecond();
                    infoHudViewHolder.setRowValue(R.string.fps, String.format(Locale.US, "%.2f / %.2f", fpsDecode, fpsOutput));

                    long videoCachedDuration = mp.getVideoCachedDuration();
                    long audioCachedDuration = mp.getAudioCachedDuration();
                    long videoCachedBytes    = mp.getVideoCachedBytes();
                    long audioCachedBytes    = mp.getAudioCachedBytes();
                    long tcpSpeed            = mp.getTcpSpeed();
                    long bitRate             = mp.getBitRate();
                    long seekLoadDuration    = mp.getSeekLoadDuration();

                    infoHudViewHolder.setRowValue(R.string.v_cache, String.format(Locale.US, "%s, %s", formatedDurationMilli(videoCachedDuration), formatedSize(videoCachedBytes)));
                    infoHudViewHolder.setRowValue(R.string.a_cache, String.format(Locale.US, "%s, %s", formatedDurationMilli(audioCachedDuration), formatedSize(audioCachedBytes)));
                    infoHudViewHolder.setRowValue(R.string.load_cost, String.format(Locale.US, "%d ms", infoHudViewHolder.mLoadCost));
                    infoHudViewHolder.setRowValue(R.string.seek_cost, String.format(Locale.US, "%d ms", infoHudViewHolder.mSeekCost));
                    infoHudViewHolder.setRowValue(R.string.seek_load_cost, String.format(Locale.US, "%d ms", seekLoadDuration));
                    infoHudViewHolder.setRowValue(R.string.tcp_speed, String.format(Locale.US, "%s", formatedSpeed(tcpSpeed, 1000)));
                    infoHudViewHolder.setRowValue(R.string.bit_rate, String.format(Locale.US, "%.2f kbs", bitRate/1000f));

                    removeMessages(MSG_UPDATE_HUD);
                    sendEmptyMessageDelayed(MSG_UPDATE_HUD, 500);
                }
            }
        }
    }
}
