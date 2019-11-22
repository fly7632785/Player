package com.pockettv.dropscreen.entity;

import android.text.TextUtils;

import org.fourthline.cling.model.meta.Device;

/**
 * 说明：
 */
public class ClingDevice implements IDevice<Device> {

    private Device mDevice;
    /**
     * 是否已选中
     */
    private boolean isSelected;

    public ClingDevice(Device device) {
        this.mDevice = device;
    }

    @Override
    public Device getDevice() {
        return mDevice;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClingDevice)) {
            return false;
        }
        ClingDevice other = (ClingDevice) obj;
        if (mDevice != null && mDevice.getDetails() != null && !TextUtils.isEmpty(mDevice.getDetails().getFriendlyName())
                && other.getDevice() != null && other.getDevice().getDetails() != null && !TextUtils.isEmpty(other.getDevice().getDetails().getFriendlyName())
                && mDevice.getDetails().getFriendlyName().equals(other.getDevice().getDetails().getFriendlyName())
        ) {
            return true;
        }
        return false;
    }
}