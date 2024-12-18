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

package com.alibaba.nacossync.template.processor;

import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.ConfigTaskAccessService;
import com.alibaba.nacossync.exception.SkyWalkerException;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.pojo.request.ConfigTaskUpdateRequest;
import com.alibaba.nacossync.pojo.result.BaseResult;
import com.alibaba.nacossync.template.Processor;
import com.alibaba.nacossync.util.SkyWalkerUtil;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author NacosSync
 * @version $Id: TaskUpdateProcessor.java, v 0.1 2018-10-17 PM11:11 NacosSync Exp $$
 */
@Slf4j
@Service
public class ConfigTaskUpdateProcessor implements Processor<ConfigTaskUpdateRequest, BaseResult> {

    private final ConfigTaskAccessService configTaskAccessService;

    public ConfigTaskUpdateProcessor(ConfigTaskAccessService configTaskAccessService) {
        this.configTaskAccessService = configTaskAccessService;
    }
    
    @Override
    public void process(ConfigTaskUpdateRequest configTaskUpdateRequest, BaseResult baseResult, Object... others) throws Exception {
        
        ConfigTaskDO taskDO = configTaskAccessService.findByTaskId(configTaskUpdateRequest.getTaskId());
        
        if (!TaskStatusEnum.contains(configTaskUpdateRequest.getTaskStatus())) {
            throw new SkyWalkerException(
                    "configTaskUpdateRequest.getTaskStatus() is not exist , value is :" + configTaskUpdateRequest.getTaskStatus());
        }
        
        if (null == taskDO) {
            throw new SkyWalkerException("configTaskDo is null ,taskId is :" + configTaskUpdateRequest.getTaskId());
        }
        
        taskDO.setTaskStatus(configTaskUpdateRequest.getTaskStatus());
        
        taskDO.setOperationId(SkyWalkerUtil.generateOperationId());

        configTaskAccessService.addConfigTask(taskDO);
    }
    
    
}
