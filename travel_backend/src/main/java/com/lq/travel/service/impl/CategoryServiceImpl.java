package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.CategoryMapper;
import com.lq.travel.model.entity.Category;
import com.lq.travel.service.CategoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> listAllCategories() {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort");
        // 注意：数据库中的category表没有is_delete和status字段，所以移除这些条件
        return categoryMapper.selectList(queryWrapper);
    }

    @Override
    public boolean addCategory(Category category) {
        if (category == null) {
            throw new RuntimeException("分类信息不能为空");
        }
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new RuntimeException("分类名称不能为空");
        }

        // 设置默认值
        if (category.getSort() == null) {
            category.setSort(0);
        }

        // 检查名称是否已存在
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", category.getName());
        Long count = categoryMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("分类名称已存在");
        }

        return categoryMapper.insert(category) > 0;
    }

    @Override
    public boolean updateCategory(Category category) {
        if (category == null || category.getId() == null) {
            throw new RuntimeException("参数错误");
        }

        // 检查分类是否存在
        Category existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            throw new RuntimeException("分类不存在");
        }

        // 如果修改了名称，检查名称是否重复
        if (category.getName() != null && !category.getName().equals(existing.getName())) {
            QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", category.getName());
            queryWrapper.ne("id", category.getId());
            Long count = categoryMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new RuntimeException("分类名称已存在");
            }
        }

        return categoryMapper.updateById(category) > 0;
    }

    @Override
    public boolean deleteCategory(Integer id) {
        if (id == null || id <= 0) {
            throw new RuntimeException("参数错误");
        }

        // 硬删除：直接删除记录（因为数据库中没有is_delete字段）
        return categoryMapper.deleteById(id) > 0;
    }

    @Override
    public Category getById(Integer id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public boolean updateCategoryStatus(Integer id, Integer status) {
        // 注意：数据库中的category表没有status字段，此方法暂时不实现
        throw new RuntimeException("分类状态更新功能暂不支持，因为数据库表中没有status字段");
    }

}