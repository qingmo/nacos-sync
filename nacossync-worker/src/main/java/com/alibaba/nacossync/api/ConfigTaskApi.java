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

package com.alibaba.nacossync.api;

import com.alibaba.nacossync.pojo.request.ConfigTaskAddRequest;
import com.alibaba.nacossync.pojo.request.ConfigTaskDeleteRequest;
import com.alibaba.nacossync.pojo.request.ConfigTaskDetailQueryRequest;
import com.alibaba.nacossync.pojo.request.ConfigTaskListQueryRequest;
import com.alibaba.nacossync.pojo.request.ConfigTaskUpdateRequest;
import com.alibaba.nacossync.pojo.request.TaskDeleteInBatchRequest;
import com.alibaba.nacossync.pojo.result.BaseResult;
import com.alibaba.nacossync.pojo.result.ConfigTaskAddResult;
import com.alibaba.nacossync.pojo.result.ConfigTaskDetailQueryResult;
import com.alibaba.nacossync.pojo.result.ConfigTaskListQueryResult;
import com.alibaba.nacossync.template.SkyWalkerTemplate;
import com.alibaba.nacossync.template.processor.ConfigTaskAddProcessor;
import com.alibaba.nacossync.template.processor.ConfigTaskDeleteInBatchProcessor;
import com.alibaba.nacossync.template.processor.ConfigTaskDeleteProcessor;
import com.alibaba.nacossync.template.processor.ConfigTaskDetailProcessor;
import com.alibaba.nacossync.template.processor.ConfigTaskListQueryProcessor;
import com.alibaba.nacossync.template.processor.ConfigTaskUpdateProcessor;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * @author NacosSync
 * @version $Id: Task.java, v 0.1 2018-09-24 PM3:43 NacosSync Exp $$
 */
@Slf4j
@RestController
public class ConfigTaskApi {

    private final ConfigTaskUpdateProcessor configTaskUpdateProcessor;

    private final ConfigTaskAddProcessor configTaskAddProcessor;

//    private final TaskAddAllProcessor taskAddAllProcessor;

    private final ConfigTaskDeleteProcessor configTaskDeleteProcessor;

    private final ConfigTaskDeleteInBatchProcessor configTaskDeleteInBatchProcessor;

    private final ConfigTaskListQueryProcessor configTaskListQueryProcessor;

    private final ConfigTaskDetailProcessor configTaskDetailProcessor;

    public ConfigTaskApi(ConfigTaskUpdateProcessor configTaskUpdateProcessor, ConfigTaskAddProcessor configTaskAddProcessor,
//                         TaskAddAllProcessor taskAddAllProcessor,
                         ConfigTaskDeleteProcessor configTaskDeleteProcessor,
                         ConfigTaskDeleteInBatchProcessor configTaskDeleteInBatchProcessor,
                         ConfigTaskListQueryProcessor configTaskListQueryProcessor,
                         ConfigTaskDetailProcessor configTaskDetailProcessor) {
        this.configTaskUpdateProcessor = configTaskUpdateProcessor;
        this.configTaskAddProcessor = configTaskAddProcessor;
//        this.taskAddAllProcessor = taskAddAllProcessor;
        this.configTaskDeleteProcessor = configTaskDeleteProcessor;
        this.configTaskDeleteInBatchProcessor = configTaskDeleteInBatchProcessor;
        this.configTaskListQueryProcessor = configTaskListQueryProcessor;
        this.configTaskDetailProcessor = configTaskDetailProcessor;
    }
    
    @RequestMapping(path = "/v1/config/task/list", method = RequestMethod.GET)
    public ConfigTaskListQueryResult tasks(ConfigTaskListQueryRequest configTaskListQueryRequest) {
        
        return SkyWalkerTemplate.run(configTaskListQueryProcessor, configTaskListQueryRequest, new ConfigTaskListQueryResult());
    }
    
    @RequestMapping(path = "/v1/config/task/detail", method = RequestMethod.GET)
    public ConfigTaskDetailQueryResult getByTaskId(ConfigTaskDetailQueryRequest configTaskDetailQueryRequest) {
        return SkyWalkerTemplate.run(configTaskDetailProcessor, configTaskDetailQueryRequest, new ConfigTaskDetailQueryResult());
    }
    
    @RequestMapping(path = "/v1/config/task/delete", method = RequestMethod.DELETE)
    public BaseResult deleteTask(ConfigTaskDeleteRequest configTaskDeleteRequest) {
        return SkyWalkerTemplate.run(configTaskDeleteProcessor, configTaskDeleteRequest, new BaseResult());
    }
    
    /**
     * @param taskBatchDeleteRequest
     */
    @RequestMapping(path = "/v1/config/task/deleteInBatch", method = RequestMethod.DELETE)
    public BaseResult batchDeleteTask(TaskDeleteInBatchRequest taskBatchDeleteRequest) {
        return SkyWalkerTemplate.run(configTaskDeleteInBatchProcessor, taskBatchDeleteRequest, new BaseResult());
    }
    
    @RequestMapping(path = "/v1/config/task/add", method = RequestMethod.POST)
    public BaseResult taskAdd(@RequestBody ConfigTaskAddRequest addConfigTaskRequest) {
        return SkyWalkerTemplate.run(configTaskAddProcessor, addConfigTaskRequest, new ConfigTaskAddResult());
    }
    
//    /**
//     * TODO 目前仅支持 Nacos 为源的同步类型，待完善更多类型支持.
//     * <p>
//     * 支持从 sourceCluster 获取所有 service，然后生成同步到 destCluster 的任务。
//     * </p>
//     */
//    @RequestMapping(path = "/v1/config/task/addAll", method = RequestMethod.POST)
//    public BaseResult taskAddAll(@RequestBody TaskAddAllRequest addAllRequest) {
//        // TODO:
//        return SkyWalkerTemplate.run(taskAddAllProcessor, addAllRequest, new TaskAddResult());
//    }
    
    @RequestMapping(path = "/v1/config/task/update", method = RequestMethod.POST)
    public BaseResult updateTask(@RequestBody ConfigTaskUpdateRequest configTaskUpdateRequest) {
        return SkyWalkerTemplate.run(configTaskUpdateProcessor, configTaskUpdateRequest, new BaseResult());
    }
}
