package com.alibaba.nacossync.dao;

import com.alibaba.nacossync.constant.SkyWalkerConstants;
import com.alibaba.nacossync.dao.repository.ConfigTaskRepository;
import com.alibaba.nacossync.pojo.QueryCondition;
import com.alibaba.nacossync.pojo.model.ConfigTaskDO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Service
public class ConfigTaskAccessService implements PageQueryService<ConfigTaskDO> {

    private final ConfigTaskRepository configTaskRepository;

    public ConfigTaskAccessService(ConfigTaskRepository configTaskRepository) {
        this.configTaskRepository = configTaskRepository;
    }
    
    public ConfigTaskDO findByTaskId(String taskId) {
        
        return configTaskRepository.findByTaskId(taskId);
    }
    
    public void deleteConfigTaskById(String taskId) {
        configTaskRepository.deleteByTaskId(taskId);
    }
    
    /**
     * batch delete tasks by taskIds
     *
     * @param taskIds
     * @author yongchao9
     */
    public void deleteConfigTaskInBatch(List<String> taskIds) {
        List<ConfigTaskDO> tds = configTaskRepository.findAllByTaskIdIn(taskIds);
        configTaskRepository.deleteAllInBatch(tds);
    }
    
    public Iterable<ConfigTaskDO> findAll() {
        
        return configTaskRepository.findAll();
    }
    
    public void addConfigTask(ConfigTaskDO taskDO) {
        
        configTaskRepository.save(taskDO);
        
    }
    
    public int countByDestClusterIdOrSourceClusterId(String destClusterId, String sourceClusterId) {
        return configTaskRepository.countByDestClusterIdOrSourceClusterId(destClusterId, sourceClusterId);
    }
    
    private Predicate getPredicate(CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        Predicate[] p = new Predicate[predicates.size()];
        return criteriaBuilder.and(predicates.toArray(p));
    }
    
    private List<Predicate> getPredicates(Root<ConfigTaskDO> root, CriteriaBuilder criteriaBuilder,
            QueryCondition queryCondition) {
        
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.like(root.get("dataId"), "%" + queryCondition.getDataId() + "%"));
        
        return predicates;
    }
    
    @Override
    public Page<ConfigTaskDO> findPageNoCriteria(Integer pageNum, Integer size) {
        
        Pageable pageable = PageRequest.of(pageNum, size, Sort.Direction.DESC, "id");
        
        return configTaskRepository.findAll(pageable);
    }
    
    @Override
    public Page<ConfigTaskDO> findPageCriteria(Integer pageNum, Integer size, QueryCondition queryCondition) {
        
        Pageable pageable = PageRequest.of(pageNum, size, Sort.Direction.DESC, "id");
        
        return getConfigTaskDOS(queryCondition, pageable);
    }
    
    private Page<ConfigTaskDO> getConfigTaskDOS(QueryCondition queryCondition, Pageable pageable) {
        return configTaskRepository.findAll((Specification<ConfigTaskDO>) (root, criteriaQuery, criteriaBuilder) -> {
            
            List<Predicate> predicates = getPredicates(root, criteriaBuilder, queryCondition);
            
            return getPredicate(criteriaBuilder, predicates);
            
        }, pageable);
    }
    
    public List<ConfigTaskDO> findAllByServiceNameEqualAll() {
        // TODO
        return configTaskRepository.findAllByDataIdEqualsIgnoreCase(SkyWalkerConstants.NACOS_ALL_SERVICE_NAME);
    }
    
    public List<ConfigTaskDO> findAllByServiceNameNotEqualAll() {
        return configTaskRepository.findAllByDataIdNotIgnoreCase(SkyWalkerConstants.NACOS_ALL_SERVICE_NAME);
    }
    
}
