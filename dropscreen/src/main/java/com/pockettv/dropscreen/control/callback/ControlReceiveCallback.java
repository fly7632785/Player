package com.pockettv.dropscreen.control.callback;


import com.pockettv.dropscreen.entity.IResponse;

/**
 * 说明：手机端接收投屏端信息回调
 */
public interface ControlReceiveCallback extends ControlCallback{

    void receive(IResponse response);
}
