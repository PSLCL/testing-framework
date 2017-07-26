package com.pslcl.dtf.runner.template;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class ConnectInfo {

    // static behavior

    static private boolean allConnectedSuccess = false;

    static public void markAllConnectedSuccess(boolean allConnectedSuccess) {
        ConnectInfo.allConnectedSuccess = allConnectedSuccess;
    }

    static public boolean getAllConnectedSuccess() {
        return allConnectedSuccess;
    }


    // class instance behavior

    ResourceInstance machineInstance;
    ResourceInstance networkInstance;

    public ConnectInfo(ResourceInstance machineInstance, ResourceInstance networkInstance) {
        this.machineInstance = machineInstance;
        this.networkInstance = networkInstance;
    }

    ResourceInstance getMachineInstance() {
        return this.machineInstance;
    }

    ResourceInstance getNetworkInstance() {
        return this.networkInstance;
    }

}
