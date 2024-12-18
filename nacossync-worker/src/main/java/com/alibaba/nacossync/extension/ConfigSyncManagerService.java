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
package com.alibaba.nacossync.extension;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.extension.annotation.NacosConfigSyncService;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.util.StringUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import static com.alibaba.nacossync.util.SkyWalkerUtil.generateSyncKey;

/**
 * @author NacosSync
 * @version $Id: SyncManagerService.java, v 0.1 2018-09-25 PM5:17 NacosSync Exp $$
 */
@Slf4j
@Service
public class ConfigSyncManagerService implements InitializingBean, ApplicationContextAware {

    protected final SkyWalkerCacheServices skyWalkerCacheServices;

    private final ConcurrentHashMap<String, ConfigSyncService> configSyncServiceMap = new ConcurrentHashMap<String, ConfigSyncService>();

    private ApplicationContext applicationContext;

    public ConfigSyncManagerService(
        SkyWalkerCacheServices skyWalkerCacheServices) {
        this.skyWalkerCacheServices = skyWalkerCacheServices;
    }

    public boolean delete(ConfigTaskDO taskDO) throws NacosException {

        return getConfigSyncService(taskDO.getSourceClusterId(), taskDO.getDestClusterId()).delete(taskDO);

    }

    public boolean sync(ConfigTaskDO taskDO, Integer index) {

        return getConfigSyncService(taskDO.getSourceClusterId(), taskDO.getDestClusterId()).sync(taskDO, index);

    }

    @Override
    public void afterPropertiesSet() {
        this.applicationContext.getBeansWithAnnotation(NacosConfigSyncService.class).forEach((key, value) -> {
            NacosConfigSyncService nacosSyncService = value.getClass().getAnnotation(NacosConfigSyncService.class);
            ClusterTypeEnum sourceCluster = nacosSyncService.sourceCluster();
            ClusterTypeEnum destinationCluster = nacosSyncService.destinationCluster();
            configSyncServiceMap.put(generateSyncKey(sourceCluster, destinationCluster), (ConfigSyncService) value);
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ConfigSyncService getConfigSyncService(String sourceClusterId, String destClusterId) {
        if (StringUtils.isEmpty(sourceClusterId) || StringUtils.isEmpty(destClusterId)) {
            throw new IllegalArgumentException("Source cluster id and destination cluster id must not be null or empty");
        }
        ClusterTypeEnum sourceClusterType = this.skyWalkerCacheServices.getClusterType(sourceClusterId);
        ClusterTypeEnum destClusterType = this.skyWalkerCacheServices.getClusterType(destClusterId);

        return configSyncServiceMap.get(generateSyncKey(sourceClusterType, destClusterType));
    }

}
