package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.entity.Category;

import java.util.List;


/**
 *分类
 */
public interface CategoryService extends IService<Category> {
    List<Category> listAllCategories();

    /**
     * 新增分类
     */
    boolean addCategory(Category category);

    /**
     * 更新分类
     */
    boolean updateCategory(Category category);

    /**
     * 删除分类
     */
    boolean deleteCategory(Integer id);

    /**
     * 根据ID获取分类
     */
    Category getById(Integer id);

    /**
     * 更新分类状态
     */
    boolean updateCategoryStatus(Integer id, Integer status);
}