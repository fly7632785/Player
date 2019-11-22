package com.pockettv.dropscreen.control;

import android.content.Context;
import android.support.annotation.NonNull;

import com.pockettv.dropscreen.entity.IDevice;
import com.pockettv.dropscreen.service.callback.AVTransportSubscriptionCallback;
import com.pockettv.dropscreen.service.callback.RenderingControlSubscriptionCallback;
import com.pockettv.dropscreen.service.manager.ClingManager;
import com.pockettv.dropscreen.util.ClingUtils;
import com.pockettv.dropscreen.util.Utils;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.Device;

/**
 * 说明：
 */
public class SubscriptionControl implements ISubscriptionControl<Device> {

    private AVTransportSubscriptionCallback mAVTransportSubscriptionCallback;
    private RenderingControlSubscriptionCallback mRenderingControlSubscriptionCallback;

    public SubscriptionControl() {
    }

    @Override
    public void registerAVTransport(@NonNull IDevice<Device> device, @NonNull Context context) {
        if (Utils.isNotNull(mAVTransportSubscriptionCallback)) {
            mAVTransportSubscriptionCallback.end();
        }
        final ControlPoint controlPointImpl = ClingUtils.getControlPoint();
        if (Utils.isNull(controlPointImpl)) {
            return;
        }

        mAVTransportSubscriptionCallback = new AVTransportSubscriptionCallback(device.getDevice().findService(ClingManager.AV_TRANSPORT_SERVICE), context);
        controlPointImpl.execute(mAVTransportSubscriptionCallback);
    }

    @Override
    public void registerRenderingControl(@NonNull IDevice<Device> device, @NonNull Context context) {
        if (Utils.isNotNull(mRenderingControlSubscriptionCallback)) {
            mRenderingControlSubscriptionCallback.end();
        }
        final ControlPoint controlPointImpl = ClingUtils.getControlPoint();
        if (Utils.isNull(controlPointImpl)) {
            return;
        }
        mRenderingControlSubscriptionCallback = new RenderingControlSubscriptionCallback(device.getDevice().findService(ClingManager
                .RENDERING_CONTROL_SERVICE), context);
        controlPointImpl.execute(mRenderingControlSubscriptionCallback);
    }

    @Override
    public void destroy() {
        if (Utils.isNotNull(mAVTransportSubscriptionCallback)) {
            mAVTransportSubscriptionCallback.end();
        }
        if (Utils.isNotNull(mRenderingControlSubscriptionCallback)) {
            mRenderingControlSubscriptionCallback.end();
        }
    }
}
