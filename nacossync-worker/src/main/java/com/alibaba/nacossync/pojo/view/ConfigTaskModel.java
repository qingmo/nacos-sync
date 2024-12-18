/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacossync.pojo.view;

import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.pojo.model.TaskDO;

import lombok.Data;

/**
 * @author NacosSync
 * @version $Id: TaskModel.java, v 0.1 2018-09-25 PM11:10 NacosSync Exp $$
 */
@Data
public class ConfigTaskModel {
    
    private String taskId;
    
    private String sourceClusterId;
    
    private String destClusterId;
    
    private String dataId;
    
    private String groupName;
    
    private String taskStatus;
    
    public static ConfigTaskModel from(ConfigTaskDO configTaskDO) {
        ConfigTaskModel taskModel = new ConfigTaskModel();
        taskModel.setDestClusterId(configTaskDO.getDestClusterId());
        taskModel.setGroupName(configTaskDO.getGroupName());
        taskModel.setDataId(configTaskDO.getDataId());
        taskModel.setSourceClusterId(configTaskDO.getSourceClusterId());
        taskModel.setTaskStatus(configTaskDO.getTaskStatus());
        taskModel.setTaskId(configTaskDO.getTaskId());
        return taskModel;
    }
}