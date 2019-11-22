package com.pockettv.dropscreen.service.manager;

import android.content.Context;

import com.pockettv.dropscreen.entity.IDevice;


/**
 * 说明：
 */
public interface IDeviceManager {

    /**
     * 获取选中设备
     */
    IDevice getSelectedDevice();

    /**
     * 设置选中设备
     */
    void setSelectedDevice(IDevice selectedDevice);

    /**
     * 取消选中设备
     */
    void cleanSelectedDevice();

    /**
     * 监听投屏端 AVTransport 回调
     * @param context   用于接收到消息发广播
     */
    void registerAVTransport(Context context);

    /**
     * 监听投屏端 RenderingControl 回调
     * @param context   用于接收到消息发广播
     */
    void registerRenderingControl(Context context);

    /**
     * 销毁
     */
    void destroy();
}
