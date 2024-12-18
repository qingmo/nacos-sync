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

import com.alibaba.nacossync.dao.ConfigTaskAccessService;
import com.alibaba.nacossync.pojo.QueryCondition;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;
import com.alibaba.nacossync.pojo.request.ConfigTaskListQueryRequest;
import com.alibaba.nacossync.pojo.result.ConfigTaskListQueryResult;
import com.alibaba.nacossync.pojo.view.ConfigTaskModel;
import com.alibaba.nacossync.template.Processor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author NacosSync
 * @version $Id: TaskListQueryProcessor.java, v 0.1 2018-09-30 PM1:01 NacosSync Exp $$
 */
@Service
@Slf4j
public class ConfigTaskListQueryProcessor implements Processor<ConfigTaskListQueryRequest, ConfigTaskListQueryResult> {

    private final ConfigTaskAccessService configTaskAccessService;

    public ConfigTaskListQueryProcessor(ConfigTaskAccessService configTaskAccessService) {
        this.configTaskAccessService = configTaskAccessService;
    }
    
    @Override
    public void process(ConfigTaskListQueryRequest configTaskListQueryRequest, ConfigTaskListQueryResult configTaskListQueryResult,
            Object... others) {
        
        Page<ConfigTaskDO> taskDOPage;
        
        if (StringUtils.isNotBlank(configTaskListQueryRequest.getDataId())) {
            
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setDataId(configTaskListQueryRequest.getDataId());
            taskDOPage = configTaskAccessService.findPageCriteria(configTaskListQueryRequest.getPageNum() - 1,
                    configTaskListQueryRequest.getPageSize(), queryCondition);
        } else {
            
            taskDOPage = configTaskAccessService.findPageNoCriteria(configTaskListQueryRequest.getPageNum() - 1,
                    configTaskListQueryRequest.getPageSize());
            
        }
        
        List<ConfigTaskModel> taskList = taskDOPage.stream().map(ConfigTaskModel::from).toList();

        configTaskListQueryResult.setTaskModels(taskList);
        configTaskListQueryResult.setTotalPage(taskDOPage.getTotalPages());
        configTaskListQueryResult.setTotalSize(taskDOPage.getTotalElements());
        configTaskListQueryResult.setCurrentSize(taskList.size());
    }
}
