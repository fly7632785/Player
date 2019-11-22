package com.pockettv.dropscreen.entity;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;


public class ClingVolumeResponse extends BaseClingResponse<Integer> {


    public ClingVolumeResponse(ActionInvocation actionInvocation, UpnpResponse operation, String defaultMsg) {
        super(actionInvocation, operation, defaultMsg);
    }

    public ClingVolumeResponse(ActionInvocation actionInvocation, Integer info) {
        super(actionInvocation, info);
    }
}
