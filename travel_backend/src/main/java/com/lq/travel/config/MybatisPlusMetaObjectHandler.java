package com.lq.travel.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * MyBatis Plus 自动填充配置
 * 用于自动填充createTime和updateTime字段
 */
@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入时自动填充创建时间和更新时间
        fillTimeFields(metaObject, true);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时自动填充更新时间
        fillTimeFields(metaObject, false);
    }
    
    /**
     * 填充时间字段
     * @param metaObject 元对象
     * @param isInsert 是否为插入操作
     */
    private void fillTimeFields(MetaObject metaObject, boolean isInsert) {
        // 检查并填充createTime字段（仅插入时）
        if (isInsert) {
            if (metaObject.hasSetter("createTime")) {
                Object createTimeValue = metaObject.getValue("createTime");
                if (createTimeValue == null) {
                    // 根据字段类型选择合适的时间类型
                    if (metaObject.getSetterType("createTime").equals(Date.class)) {
                        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
                    } else if (metaObject.getSetterType("createTime").equals(LocalDateTime.class)) {
                        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                    }
                }
            }
        }
        
        // 检查并填充editTime字段
        if (metaObject.hasSetter("editTime")) {
            Object editTimeValue = metaObject.getValue("editTime");
            if (editTimeValue == null) {
                // 根据字段类型选择合适的时间类型
                if (metaObject.getSetterType("editTime").equals(Date.class)) {
                    if (isInsert) {
                        this.strictInsertFill(metaObject, "editTime", Date.class, new Date());
                    } else {
                        this.strictUpdateFill(metaObject, "editTime", Date.class, new Date());
                    }
                } else if (metaObject.getSetterType("editTime").equals(LocalDateTime.class)) {
                    if (isInsert) {
                        this.strictInsertFill(metaObject, "editTime", LocalDateTime.class, LocalDateTime.now());
                    } else {
                        this.strictUpdateFill(metaObject, "editTime", LocalDateTime.class, LocalDateTime.now());
                    }
                }
            }
        }
        
        // 检查并填充updateTime字段
        if (metaObject.hasSetter("updateTime")) {
            Object updateTimeValue = metaObject.getValue("updateTime");
            if (updateTimeValue == null) {
                // 根据字段类型选择合适的时间类型
                if (metaObject.getSetterType("updateTime").equals(Date.class)) {
                    if (isInsert) {
                        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
                    } else {
                        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
                    }
                } else if (metaObject.getSetterType("updateTime").equals(LocalDateTime.class)) {
                    if (isInsert) {
                        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                    } else {
                        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                    }
                }
            }
        }
    }
}

