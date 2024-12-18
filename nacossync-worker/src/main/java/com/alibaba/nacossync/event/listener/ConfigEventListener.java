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

package com.alibaba.nacossync.event.listener;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.event.ConfigSyncTaskEvent;
import com.alibaba.nacossync.event.DeleteConfigTaskEvent;
import com.alibaba.nacossync.event.DeleteTaskEvent;
import com.alibaba.nacossync.event.SyncTaskEvent;
import com.alibaba.nacossync.extension.ConfigSyncManagerService;
import com.alibaba.nacossync.extension.SyncManagerService;
import com.alibaba.nacossync.monitor.MetricsManager;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

/**
 * @author NacosSync
 * @version $Id: EventListener.java, v 0.1 2018-09-27 AM1:21 NacosSync Exp $$
 */
@Slf4j
@Service
public class ConfigEventListener {

    private final MetricsManager metricsManager;

    private final ConfigSyncManagerService configSyncManagerService;

    private final EventBus eventBus;

    private final SkyWalkerCacheServices skyWalkerCacheServices;

    public ConfigEventListener(MetricsManager metricsManager, ConfigSyncManagerService configSyncManagerService, EventBus eventBus,
                               SkyWalkerCacheServices skyWalkerCacheServices) {
        this.metricsManager = metricsManager;
        this.configSyncManagerService = configSyncManagerService;
        this.eventBus = eventBus;
        this.skyWalkerCacheServices = skyWalkerCacheServices;
    }
    
    @PostConstruct
    public void register() {
        eventBus.register(this);
    }
    
    @Subscribe
    public void sync(ConfigSyncTaskEvent configSyncTaskEvent) {
        
        try {
            
            Stopwatch stopwatch = Stopwatch.createStarted();
            if (configSyncManagerService.sync(configSyncTaskEvent.getConfigTaskDO(), null)) {
                skyWalkerCacheServices.addFinishedConfigTask(configSyncTaskEvent.getConfigTaskDO());
                metricsManager.record(MetricsStatisticsType.SYNC_CONFIG_TASK_RT, stopwatch.elapsed().toMillis());
            } else {
                log.warn("syncConfigTaskEvent process error");
            }
        } catch (Exception e) {
            log.warn("syncConfigTaskEvent process error", e);
        }
        
    }
    
    @Subscribe
    public void delete(DeleteConfigTaskEvent deleteTaskEvent) {
        
        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            if (configSyncManagerService.delete(deleteTaskEvent.getConfigTaskDO())) {
                skyWalkerCacheServices.removeFinishedConfigTask(deleteTaskEvent.getConfigTaskDO().getOperationId());
                metricsManager.record(MetricsStatisticsType.DELETE_TASK_RT, stopwatch.elapsed().toMillis());
            } else {
                log.warn("deleteConfigTaskEvent delete failure");
            }
        } catch (Exception e) {
            log.warn("deleteConfigTaskEvent delete failure.", e);
        }
    }
}
