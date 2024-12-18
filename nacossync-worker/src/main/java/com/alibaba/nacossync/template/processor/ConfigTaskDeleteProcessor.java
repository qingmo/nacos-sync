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

import com.google.common.eventbus.EventBus;

import com.alibaba.nacossync.constant.SkyWalkerConstants;
import com.alibaba.nacossync.dao.ConfigTaskAccessService;
import com.alibaba.nacossync.event.DeleteAllSubConfigTaskEvent;
import com.alibaba.nacossync.event.DeleteConfigTaskEvent;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.pojo.request.ConfigTaskDeleteRequest;
import com.alibaba.nacossync.pojo.result.BaseResult;
import com.alibaba.nacossync.template.Processor;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author NacosSync
 * @version $Id: TaskDeleteProcessor.java, v 0.1 2018-09-30 PM12:52 NacosSync Exp $$
 */
@Slf4j
@Service
public class ConfigTaskDeleteProcessor implements Processor<ConfigTaskDeleteRequest, BaseResult> {

    private final ConfigTaskAccessService configTaskAccessService;

    private final EventBus eventBus;

    public ConfigTaskDeleteProcessor(ConfigTaskAccessService configTaskAccessService, EventBus eventBus) {
        this.configTaskAccessService = configTaskAccessService;
        this.eventBus = eventBus;
    }
    
    @Override
    public void process(ConfigTaskDeleteRequest configTaskDeleteRequest, BaseResult baseResult, Object... others) {
        ConfigTaskDO taskDO = configTaskAccessService.findByTaskId(configTaskDeleteRequest.getTaskId());
        // delete all sub task when ServiceName is all
        if (SkyWalkerConstants.NACOS_ALL_SERVICE_NAME.equalsIgnoreCase(taskDO.getDataId())) {
            eventBus.post(new DeleteAllSubConfigTaskEvent(taskDO));
        } else {
            eventBus.post(new DeleteConfigTaskEvent(taskDO));
        }
        log.info("删除同步任务数据之前，发出一个同步事件:{}", taskDO);
        configTaskAccessService.deleteConfigTaskById(configTaskDeleteRequest.getTaskId());
    }
    
    
}
