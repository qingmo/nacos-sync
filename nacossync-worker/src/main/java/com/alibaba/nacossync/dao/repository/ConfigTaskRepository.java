package com.alibaba.nacossync.dao.repository;

import com.alibaba.nacossync.pojo.model.ConfigTaskDO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

import javax.transaction.Transactional;

/**
 * @author NacosSync
 * @version $Id: TaskRepository.java, v 0.1 2018-09-25 AM12:04 NacosSync Exp $$
 */
public interface ConfigTaskRepository extends CrudRepository<ConfigTaskDO, Integer>, JpaRepository<ConfigTaskDO, Integer>,
        JpaSpecificationExecutor<ConfigTaskDO> {

    ConfigTaskDO findByTaskId(String taskId);

    @Transactional
    void deleteByTaskId(String taskId);
    
    List<ConfigTaskDO> findAllByTaskIdIn(List<String> taskIds);
    /**
     * query service is all，use ns leven sync data
     */
    List<ConfigTaskDO> findAllByDataIdEqualsIgnoreCase(String dataId);
    List<ConfigTaskDO> findAllByDataIdNotIgnoreCase(String dataId);
    
    int countByDestClusterIdOrSourceClusterId(String destClusterId,String sourceClusterId);

}
