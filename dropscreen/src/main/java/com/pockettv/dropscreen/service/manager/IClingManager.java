package com.pockettv.dropscreen.service.manager;



import com.pockettv.dropscreen.service.ClingUpnpService;

import org.fourthline.cling.registry.Registry;

/**
 * 说明：
 */
public interface IClingManager extends IDLNAManager {

    void setUpnpService(ClingUpnpService upnpService);

    void setDeviceManager(IDeviceManager deviceManager);

    Registry getRegistry();
}
