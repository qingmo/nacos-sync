package com.alibaba.nacossync.dao;

import com.alibaba.nacossync.dao.repository.SystemConfigRepository;
import com.alibaba.nacossync.pojo.QueryCondition;
import com.alibaba.nacossync.pojo.model.SystemConfigDO;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class SystemConfigAccessService implements PageQueryService<SystemConfigDO> {

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigAccessService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    public SystemConfigDO findByConfigKey(String configKey) {
        return this.systemConfigRepository.findByConfigKey(configKey);
    }


    @Override
    public Page<SystemConfigDO> findPageNoCriteria(Integer pageNum, Integer size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<SystemConfigDO> findPageCriteria(Integer pageNum, Integer size, QueryCondition queryCondition) {
        throw new UnsupportedOperationException();
    }
}
