/******************************************/
/*   DB name = nacos_sync   */
/*   Table name = cluster   */
/******************************************/
CREATE TABLE `cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cluster_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `cluster_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `cluster_type` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `connect_key_list` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `user_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `namespace` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `cluster_level` int default 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/******************************************/
/*   DB name = nacos_sync   */
/*   Table name = system_config   */
/******************************************/
CREATE TABLE `system_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `config_desc` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `config_key` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `config_value` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/******************************************/
/*   DB name = nacos_sync   */
/*   Table name = task   */
/******************************************/
CREATE TABLE `task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dest_cluster_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `group_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `name_space` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `operation_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `service_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `source_cluster_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `task_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `task_status` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `version` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `worker_ip` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `status` int default null ,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE `config_task` (
  `id` int NOT NULL AUTO_INCREMENT,
  `data_id` varchar(255) DEFAULT NULL,
  `dest_cluster_id` varchar(255) DEFAULT NULL,
  `group_name` varchar(255) DEFAULT NULL,
  `name_space` varchar(255) DEFAULT NULL,
  `operation_id` varchar(255) DEFAULT NULL,
  `source_cluster_id` varchar(255) DEFAULT NULL,
  `task_id` varchar(255) DEFAULT NULL,
  `task_status` varchar(255) DEFAULT NULL,
  `worker_ip` varchar(255) DEFAULT NULL,
  `content_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=436 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;


INSERT INTO system_config
(config_desc, config_key, config_value)
VALUES('开启配置同步', 'CONFIG_SYNC_ENABLE', '1'),
('在删除时需要执行注销动作', 'DEREGISTER_WHEN_DELETE', '0');
