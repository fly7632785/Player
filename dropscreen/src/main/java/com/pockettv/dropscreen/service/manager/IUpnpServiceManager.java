package com.pockettv.dropscreen.service.manager;

import com.pockettv.dropscreen.entity.IControlPoint;
import com.pockettv.dropscreen.entity.IDevice;

import java.util.Collection;

/**
 * 说明：
 */
public interface IUpnpServiceManager {

    /**
     * 搜索所有的设备
     */
    void searchDevices();

    /**
     * 获取支持 Media 类型的设备
     *
     * @return  设备列表
     */
    Collection<? extends IDevice> getDmrDevices();

    /**
     * 获取控制点
     *
     * @return  控制点
     */
    IControlPoint getControlPoint();

    /**
     * 获取选中的设备
     *
     * @return  选中的设备
     */
    IDevice getSelectedDevice();

    /**
     * 设置选中的设备
     * @param device    已选中设备
     */
    void setSelectedDevice(IDevice device);

    /**
     * 销毁
     */
    void destroy();
}
