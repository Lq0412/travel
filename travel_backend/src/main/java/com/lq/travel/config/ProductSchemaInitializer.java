package com.lq.travel.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
public class ProductSchemaInitializer implements CommandLineRunner {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `product` (
                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
                  `user_id` bigint NOT NULL COMMENT '用户ID',
                  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
                  `city` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属城市',
                  `address` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '详细地址',
                  `tags_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品标签JSON',
                  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品描述',
                  `is_recommendable` tinyint NOT NULL DEFAULT 1 COMMENT '是否可推荐',
                  `is_purchasable` tinyint NOT NULL DEFAULT 1 COMMENT '是否可购买',
                  `cover` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品封面',
                  `latitude` decimal(10,7) NULL COMMENT '纬度',
                  `longitude` decimal(10,7) NULL COMMENT '经度',
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                  `is_delete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
                  PRIMARY KEY (`id`) USING BTREE,
                  INDEX `idx_product_user_id`(`user_id` ASC) USING BTREE,
                  INDEX `idx_product_city`(`city` ASC) USING BTREE,
                  INDEX `idx_product_is_delete`(`is_delete` ASC) USING BTREE
                ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品表'
                """);
        jdbcTemplate.execute("ALTER TABLE product ADD COLUMN IF NOT EXISTS address varchar(512) NULL COMMENT '详细地址' AFTER city");
        jdbcTemplate.execute("ALTER TABLE product ADD COLUMN IF NOT EXISTS latitude decimal(10,7) NULL COMMENT '纬度' AFTER cover");
        jdbcTemplate.execute("ALTER TABLE product ADD COLUMN IF NOT EXISTS longitude decimal(10,7) NULL COMMENT '经度' AFTER latitude");
        log.info("商品表检查完成");
    }
}
