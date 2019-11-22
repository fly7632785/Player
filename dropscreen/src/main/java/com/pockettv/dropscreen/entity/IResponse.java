package com.pockettv.dropscreen.entity;

/**
 * 说明：设备控制 返回结果
 */
public interface IResponse<T> {

    T getResponse();

    void setResponse(T response);
}
