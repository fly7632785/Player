package com.pockettv.dropscreen.control.callback;


import com.pockettv.dropscreen.entity.IResponse;

/**
 * 说明：设备控制操作 回调
 */
public interface ControlCallback<T> {

    void success(IResponse<T> response);

    void fail(IResponse<T> response);
}
