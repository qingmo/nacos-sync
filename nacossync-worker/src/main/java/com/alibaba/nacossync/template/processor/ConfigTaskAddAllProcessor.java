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

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.http.ServerHttpAgent;
import com.alibaba.nacos.client.naming.utils.UtilAndComs;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.alibaba.nacos.common.utils.StringUtils;
import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.ClusterAccessService;
import com.alibaba.nacossync.dao.ConfigTaskAccessService;
import com.alibaba.nacossync.exception.SkyWalkerException;
import com.alibaba.nacossync.extension.ConfigSyncManagerService;
import com.alibaba.nacossync.extension.holder.NacosConfigServerHolder;
import com.alibaba.nacossync.pojo.model.ClusterDO;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.pojo.request.ConfigTaskAddRequest;
import com.alibaba.nacossync.pojo.request.TaskAddAllRequest;
import com.alibaba.nacossync.pojo.result.ConfigTaskAddResult;
import com.alibaba.nacossync.template.Processor;
import com.alibaba.nacossync.util.SkyWalkerUtil;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.DATA_ID_PARAM;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.GROUP_PARAM;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.PAGE_NO;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.PAGE_SIZE;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SEARCH_PARAM;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.TENANT_PARAM;

/**
 * @author NacosSync
 * @version $Id: TaskAddAllProcessor.java, v 0.1 2022-03-23 PM11:40 NacosSync Exp $$
 */
@Slf4j
@Service
public class ConfigTaskAddAllProcessor implements Processor<TaskAddAllRequest, ConfigTaskAddResult> {

    @Resource
    private NacosConfigServerHolder nacosConfigServerHolder;

    @Resource
    private ConfigSyncManagerService configSyncManagerService;

    @Resource
    private ConfigTaskAccessService configTaskAccessService;

    @Resource
    private ClusterAccessService clusterAccessService;

    @Resource
    private SkyWalkerCacheServices skyWalkerCacheServices;

    @Override
    public void process(TaskAddAllRequest addAllRequest, ConfigTaskAddResult taskAddResult, Object... others)
            throws Exception {
        
        ClusterDO destCluster = clusterAccessService.findByClusterId(addAllRequest.getDestClusterId());
        
        ClusterDO sourceCluster = clusterAccessService.findByClusterId(addAllRequest.getSourceClusterId());
        
        if (Objects.isNull(destCluster) || Objects.isNull(sourceCluster)) {
            throw new SkyWalkerException("Please check if the source or target cluster exists.");
        }
        
        if (Objects.isNull(configSyncManagerService.getConfigSyncService(sourceCluster.getClusterId(), destCluster.getClusterId()))) {
            throw new SkyWalkerException("current sync type not supported.");
        }
        // TODO 目前仅支持 Nacos 为源的同步类型，待完善更多类型支持。
        final ConfigService sourceConfigService = nacosConfigServerHolder.get(sourceCluster.getClusterId());
        if (sourceConfigService == null) {
            throw new SkyWalkerException("only support sync type that the source of the Nacos.");
        }
        
        final EnhanceConfigService enhanceConfigService = new EnhanceConfigService(sourceConfigService, sourceCluster, skyWalkerCacheServices);
        final CatalogDataIdsResult catalogServiceResult = enhanceConfigService.catalogDataIds(null, addAllRequest.getGroup());
        if (catalogServiceResult == null || catalogServiceResult.getTotalCount() <= 0) {
            throw new SkyWalkerException("sourceCluster dataIds empty");
        }
        // TODO 是否需要自动创建命名空间？
        for (DataIdView serviceView : catalogServiceResult.getPageItems()) {
            ConfigTaskAddRequest taskAddRequest = new ConfigTaskAddRequest();
            taskAddRequest.setSourceClusterId(sourceCluster.getClusterId());
            taskAddRequest.setDestClusterId(destCluster.getClusterId());
            taskAddRequest.setDataId(serviceView.getDataId());
            taskAddRequest.setGroupName(serviceView.getGroup());
            taskAddRequest.setContentType(serviceView.getType());
            taskAddRequest.setNameSpace(serviceView.getTenant());
            this.dealTask(addAllRequest, taskAddRequest);
        }
        log.info("process added total:{} config tasks", catalogServiceResult.getTotalCount());
    }
    
    private void dealTask(TaskAddAllRequest addAllRequest, ConfigTaskAddRequest taskAddRequest) throws Exception {
        
        String taskId = SkyWalkerUtil.generateConfigTaskId(taskAddRequest);
        ConfigTaskDO taskDO = configTaskAccessService.findByTaskId(taskId);
        if (null == taskDO) {
            taskDO = new ConfigTaskDO();
            taskDO.setTaskId(taskId);
            taskDO.setDestClusterId(addAllRequest.getDestClusterId());
            taskDO.setSourceClusterId(addAllRequest.getSourceClusterId());
            taskDO.setDataId(taskAddRequest.getDataId());
            taskDO.setContentType(taskAddRequest.getContentType());
            taskDO.setGroupName(taskAddRequest.getGroupName());
            taskDO.setNameSpace(taskAddRequest.getNameSpace());
            taskDO.setTaskStatus(TaskStatusEnum.SYNC.getCode());
            taskDO.setWorkerIp(SkyWalkerUtil.getLocalIp());
            taskDO.setOperationId(SkyWalkerUtil.generateOperationId());
            
        } else {
            taskDO.setTaskStatus(TaskStatusEnum.SYNC.getCode());
            taskDO.setOperationId(SkyWalkerUtil.generateOperationId());
        }
        configTaskAccessService.addConfigTask(taskDO);
    }
    
    static class EnhanceConfigService {

        private final ClusterDO sourceCluster;
        private final SkyWalkerCacheServices skyWalkerCacheServices;
        protected ConfigService delegate;
        
        protected ServerHttpAgent serverProxy;
        
        protected EnhanceConfigService(ConfigService configService,
                                       ClusterDO sourceCluster,
                                       SkyWalkerCacheServices skyWalkerCacheServices) throws NacosException {
            this.sourceCluster = sourceCluster;
            this.skyWalkerCacheServices = skyWalkerCacheServices;
            if (!(configService instanceof ConfigService)) {
                throw new IllegalArgumentException(
                        "configService only support instance of com.alibaba.nacos.client.naming.NacosNamingService.");
            }
            this.delegate = configService;
            List<String> allClusterConnectKey = this.skyWalkerCacheServices.getAllClusterConnectKey(this.sourceCluster.getClusterId());
            String serverList = Joiner.on(",").join(allClusterConnectKey);
            Properties properties = new Properties();
            properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverList);
            properties.setProperty(PropertyKeyConst.NAMESPACE, Optional.ofNullable(sourceCluster.getNamespace()).orElse(
                    Strings.EMPTY));
            Optional.ofNullable(sourceCluster.getUserName()).ifPresent(value ->
                    properties.setProperty(PropertyKeyConst.USERNAME, value)
            );

            Optional.ofNullable(sourceCluster.getPassword()).ifPresent(value ->
                    properties.setProperty(PropertyKeyConst.PASSWORD, value)
            );
            serverProxy = new ServerHttpAgent(properties);
        }
        
        public CatalogDataIdsResult catalogDataIds(@Nullable String dataId, @Nullable String group)
                throws Exception {
            int pageNo = 1; // start with 1
            int pageSize = 100;
            
            final CatalogDataIdsResult result = catalogDataIds(dataId, group, pageNo, pageSize);
            
            CatalogDataIdsResult tmpResult = result;
            
            while (Objects.nonNull(tmpResult) && tmpResult.getPagesAvailable() >= pageNo) {
                pageNo++;
                tmpResult = catalogDataIds(dataId, group, pageNo, pageSize);
                
                if (tmpResult != null) {
                    result.pageItems.addAll(tmpResult.pageItems);
                }
            }
            
            return result;
        }
        
        public CatalogDataIdsResult catalogDataIds(@Nullable String dataId, @Nullable String group, int pageNo,
                                                   int pageSize) throws Exception {
            
            // pageNo
            // pageSize
            // serviceNameParam
            // groupNameParam
            final Map<String, String> params = new HashMap<>(8);

            params.put(DATA_ID_PARAM, org.apache.commons.lang3.StringUtils.defaultString(dataId));
            params.put(GROUP_PARAM, org.apache.commons.lang3.StringUtils.defaultString(group));
            params.put(PAGE_NO, String.valueOf(pageNo));
            params.put(PAGE_SIZE, String.valueOf(pageSize));
            params.put(TENANT_PARAM, sourceCluster.getNamespace());
            params.put(SEARCH_PARAM, "accurate");

            HttpRestResult<String> response = this.serverProxy.httpGet("/v1/cs/configs", Maps.newHashMap(), params, "UTF-8", 2000);
            if (!response.ok()) {
                throw new SkyWalkerException("catalogDataIds failed" + response);
            }

            String data = response.getData();
            if (StringUtils.isNotEmpty(data)) {
                return JacksonUtils.toObj(data, CatalogDataIdsResult.class);
            }
            return null;
        }
        
    }
    
    /**
     * Copy from Nacos Server.
     */
    @Data
    static class DataIdView {
        
        private String id;
        
        private String dataId;
        
        private String group;
        
        private String tenant;

        private String type;

    }
    
    @Data
    static class CatalogDataIdsResult {
        
        private int totalCount;
        private int pageNumber;
        private int pagesAvailable;

        private List<DataIdView> pageItems;
        
    }
    
}
