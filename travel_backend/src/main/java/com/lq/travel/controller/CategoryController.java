package com.lq.travel.controller;

import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.entity.Category;
import com.lq.travel.service.CategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    // 1. 获取所有分类列表
    @GetMapping("/list")
    public ResponseDTO<List<Category>> listAllCategories() {
        List<Category> categories = categoryService.listAllCategories();
        return ResponseUtils.success(categories);
    }

    // 2. 根据ID获取分类详情
    @GetMapping("/get/{id}")
    public ResponseDTO<Category> getCategoryById(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            throw new RuntimeException("参数错误");
        }
        Category category = categoryService.getById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        return ResponseUtils.success(category);
    }

    // 3. 新增分类
    @PostMapping("/add")
    public ResponseDTO<Integer> addCategory(@RequestBody Category category) {
        boolean result = categoryService.addCategory(category);
        return ResponseUtils.success(result ? category.getId() : null);
    }

    // 4. 更新分类
    @PostMapping("/update")
    public ResponseDTO<Boolean> updateCategory(@RequestBody Category category) {
        boolean result = categoryService.updateCategory(category);
        return ResponseUtils.success(result);
    }

    // 5. 删除分类
    @PostMapping("/delete/{id}")
    public ResponseDTO<Boolean> deleteCategory(@PathVariable Integer id) {
        boolean result = categoryService.deleteCategory(id);
        return ResponseUtils.success(result);
    }

}