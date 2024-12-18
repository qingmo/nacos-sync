/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.alibaba.nacossync.extension.impl;

import com.google.common.base.Stopwatch;

import com.alibaba.nacos.api.config.ConfigChangeEvent;
import com.alibaba.nacos.api.config.ConfigChangeItem;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.listener.impl.AbstractConfigChangeListener;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.extension.ConfigSyncService;
import com.alibaba.nacossync.extension.annotation.NacosConfigSyncService;
import com.alibaba.nacossync.extension.holder.NacosConfigServerHolder;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.util.BatchTaskExecutor;
import com.alibaba.nacossync.util.StringUtils;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import static com.alibaba.nacossync.util.NacosUtils.getGroupNameOrDefault;

/**
 * @author yangyshdan
 * @version $Id: ConfigServerSyncManagerService.java, v 0.1 2018-11-12 下午5:17 NacosSync Exp $$
 */

@Slf4j
@NacosConfigSyncService(sourceCluster = ClusterTypeEnum.NACOS, destinationCluster = ClusterTypeEnum.NACOS)
public class ConfigSyncNacosToNacosServiceImpl implements ConfigSyncService, InitializingBean, DisposableBean {

    private final Map<String, Listener> listenerMap = new ConcurrentHashMap<>();

    private final Map<String, Integer> syncTaskTap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ConfigTaskDO> allSyncTaskMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService;

    private final MetricsManager metricsManager;

    private final NacosConfigServerHolder nacosConfigServerHolder;


    public ConfigSyncNacosToNacosServiceImpl(MetricsManager metricsManager, NacosConfigServerHolder nacosConfigServerHolder) {
        this.metricsManager = metricsManager;
        this.nacosConfigServerHolder = nacosConfigServerHolder;
    }

    /**
     * Due to network issues or other reasons, the Nacos Sync synchronization tasks may fail,
     * resulting in the target cluster's registry missing synchronized instances.
     * To prevent the target cluster's registry from missing synchronized instances for an extended period,
     * a fallback worker thread is started every 5 minutes to execute all synchronization tasks.
     */
    @Override
    public void afterPropertiesSet() {
        initializeExecutorService();
        scheduleSyncTasks();
    }

    @Override
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializeExecutorService() {
        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("com.alibaba.nacossync.basic.config.synctask");
            return t;
        });
    }

    private void scheduleSyncTasks() {
        executorService.scheduleWithFixedDelay(this::executeSyncTasks, 0, 300, TimeUnit.SECONDS);
    }

    private void executeSyncTasks() {
        if (allSyncTaskMap.isEmpty()) {
            return;
        }

        Collection<ConfigTaskDO> taskCollections = allSyncTaskMap.values();
        List<ConfigTaskDO> taskDOList = new ArrayList<>(taskCollections);

        if (CollectionUtils.isNotEmpty(taskDOList)) {
            BatchTaskExecutor.batchOperation(taskDOList, this::executeTask);
        }
    }

    private void executeTask(ConfigTaskDO task) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String taskId = task.getTaskId();
        try {
            ConfigService sourceNamingService = nacosConfigServerHolder.get(task.getSourceClusterId());
            ConfigService destNamingService = nacosConfigServerHolder.get(task.getDestClusterId());
            doSync(taskId, task, sourceNamingService, destNamingService);
        } catch (NacosException e) {
            log.error("sync task from nacos to nacos failed, taskId:{}", taskId, e);
            metricsManager.recordError(MetricsStatisticsType.SYNC_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error during sync task, taskId:{}", taskId, e);
            metricsManager.recordError(MetricsStatisticsType.SYNC_ERROR);
        } finally {
            stopwatch.stop();
            log.debug("Task execution time for taskId {}: {} ms", taskId, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public boolean delete(ConfigTaskDO configTaskDO) {
        try {
            ConfigService sourceConfigService = nacosConfigServerHolder.get(configTaskDO.getSourceClusterId());
            String taskId = configTaskDO.getTaskId();

            //Handle individual service
            if (StringUtils.isEmpty(taskId)) {
                log.warn("taskId is null config data synchronization is not currently performed.{}", taskId);
                return false;
            }
            Listener listener = listenerMap.remove(taskId);
            if (listener != null) {
                sourceConfigService.removeListener(configTaskDO.getDataId(), getGroupNameOrDefault(configTaskDO.getGroupName()), listener);
            }
            // Remove all tasks that need to be synchronized.
            allSyncTaskMap.remove(taskId);

        } catch (Exception e) {
            log.error("delete task from nacos to nacos was failed, operationalId:{}", configTaskDO.getOperationId(), e);
            metricsManager.recordError(MetricsStatisticsType.DELETE_CONFIG_ERROR);
            return false;
        }
        return true;
    }

    @Override
    public boolean sync(ConfigTaskDO configTaskDO, Integer index) {
        log.info("Thread {} started config synchronization at {}", Thread.currentThread().getId(), System.currentTimeMillis());
        String taskId = configTaskDO.getTaskId();
        try {
            ConfigService sourceConfigService = nacosConfigServerHolder.get(configTaskDO.getSourceClusterId());
            ConfigService destConfigService = nacosConfigServerHolder.get(configTaskDO.getDestClusterId());
            allSyncTaskMap.put(taskId, configTaskDO);
            Stopwatch stopwatch = Stopwatch.createStarted();
            doSync(taskId, configTaskDO, sourceConfigService, destConfigService);
            log.debug("Time taken to synchronize config: {} ms",
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
            this.listenerMap.putIfAbsent(taskId, new AbstractConfigChangeListener() {

                @Override
                public void receiveConfigChange(ConfigChangeEvent configChangeEvent) {
                    for (ConfigChangeItem changeItem : configChangeEvent.getChangeItems()) {
                        log.info("Detected changes in config information, taskId: {}, key:{}, oldContent:{}, newContent:{} initiating synchronization",
                                taskId, changeItem.getKey(), changeItem.getOldValue(), changeItem.getNewValue());
                        try {
                            doSync(taskId, configTaskDO, sourceConfigService, destConfigService);
                            log.info("Detected synchronization end for config information {}", taskId);
                        } catch (Exception e) {
                            log.error("config process fail, taskId:{}", taskId, e);
                            metricsManager.recordError(MetricsStatisticsType.SYNC_CONFIG_ERROR);
                        }
                    }
                }
            });
            sourceConfigService.addListener(configTaskDO.getDataId(), getGroupNameOrDefault(configTaskDO.getGroupName()), listenerMap.get(taskId));
        } catch (Exception e) {
            log.error("sync config task from nacos to nacos was failed, taskId:{}", taskId, e);
            metricsManager.recordError(MetricsStatisticsType.SYNC_CONFIG_ERROR);
            return false;
        }
        return true;
    }

    private void doSync(String taskId, ConfigTaskDO configTaskDO, ConfigService sourceConfigService,
                        ConfigService destConfigService) throws NacosException {
        if (syncTaskTap.putIfAbsent(taskId, 1) != null) {
            log.info("Task ID:{} - the previous synchronization task has not finished yet", taskId);
            return;
        }

        try {
            String config = sourceConfigService.getConfig(configTaskDO.getDataId(), getGroupNameOrDefault(configTaskDO.getGroupName()), 2000);
            destConfigService.publishConfig(configTaskDO.getDataId(), getGroupNameOrDefault(configTaskDO.getGroupName()), config);
        } finally {
            syncTaskTap.remove(taskId);
        }
    }


}
