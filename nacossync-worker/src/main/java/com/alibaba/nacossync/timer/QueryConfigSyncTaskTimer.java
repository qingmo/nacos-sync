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
package com.alibaba.nacossync.timer;

import com.google.common.eventbus.EventBus;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.constant.SkyWalkerConstants;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.ConfigTaskAccessService;
import com.alibaba.nacossync.dao.SystemConfigAccessService;
import com.alibaba.nacossync.event.ConfigSyncTaskEvent;
import com.alibaba.nacossync.event.DeleteConfigTaskEvent;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.pojo.model.SystemConfigDO;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * @author NacosSync
 * @version $Id: SkyWalkerServices.java, v 0.1 2018-09-26 AM1:39 NacosSync Exp $$
 */
@Slf4j
@Service
public class QueryConfigSyncTaskTimer implements CommandLineRunner {

    private static final int INITIAL_DELAY = 0;

    private static final int DELAY = 3000;

    @Resource
    private MetricsManager metricsManager;

    @Resource
    private SkyWalkerCacheServices skyWalkerCacheServices;

    @Resource
    private ConfigTaskAccessService configTaskAccessService;

    @Resource
    private EventBus eventBus;

    @Resource
    private ScheduledExecutorService scheduledExecutorService;

    @Resource
    private SystemConfigAccessService systemConfigAccessService;


    @Override
    public void run(String... args) {
        SystemConfigDO configSyncSwitch = systemConfigAccessService.findByConfigKey(SkyWalkerConstants.CONFIG_SYNC_ENABLE);
        if (configSyncSwitch == null || !"1".equals(configSyncSwitch.getConfigValue())) {
            log.warn("config sync is not switch on");
            return;
        }
        /** Fetch the task list from the database every 3 seconds */
        scheduledExecutorService.scheduleWithFixedDelay(new ConfigCheckRunningStatusThread(), INITIAL_DELAY, DELAY,
                TimeUnit.MILLISECONDS);
        
        log.info("QueryConfigSyncTaskTimer has started successfully");
    }

    private class ConfigCheckRunningStatusThread implements Runnable {

        @Override
        public void run() {

            long start = System.currentTimeMillis();
            try {

                List<ConfigTaskDO> taskDOS = configTaskAccessService.findAllByServiceNameNotEqualAll();

                taskDOS.forEach(taskDO -> {

                    if ((null != skyWalkerCacheServices.getFinishedConfigTask(taskDO))) {

                        return;
                    }

                    if (TaskStatusEnum.SYNC.getCode().equals(taskDO.getTaskStatus())) {

                        eventBus.post(new ConfigSyncTaskEvent(taskDO));
                        log.info("从数据库中查询到一个同步配置任务，发出一个同步事件:{}", taskDO);
                    }

                    if (TaskStatusEnum.DELETE.getCode().equals(taskDO.getTaskStatus())) {

                        eventBus.post(new DeleteConfigTaskEvent(taskDO));
                        log.info("从数据库中查询到一个删除配置任务，发出一个同步事件:{}", taskDO);
                    }
                });

            } catch (Exception e) {
                log.warn("ConfigCheckRunningStatusThread Exception", e);
            }

            metricsManager.record(MetricsStatisticsType.DISPATCHER_TASK, System.currentTimeMillis() - start);
        }
    }
}
