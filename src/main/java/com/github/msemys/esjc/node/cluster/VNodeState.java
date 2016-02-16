package com.github.msemys.esjc.node.cluster;

public enum VNodeState {
    Initializing,
    Unknown,
    PreReplica,
    CatchingUp,
    Clone,
    Slave,
    PreMaster,
    Master,
    Manager,
    ShuttingDown,
    Shutdown
}
