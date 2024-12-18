package com.alibaba.nacossync.event;

import com.alibaba.nacossync.pojo.model.ConfigTaskDO;

import lombok.Data;

@Data
public class DeleteAllSubConfigTaskEvent {
    public DeleteAllSubConfigTaskEvent(ConfigTaskDO configTaskDO) {
        this.configTaskDO = configTaskDO;
    }
    
    private final ConfigTaskDO configTaskDO;
}
