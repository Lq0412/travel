package com.lq.travel.controller;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.DeleteRequest;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.constant.UserConstant;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.exception.ThrowUtils;
import com.lq.travel.model.dto.picture.*;
import com.lq.travel.model.entity.Picture;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.enums.PictureReviewStatusEnum;
import com.lq.travel.model.vo.PictureTagCategory;
import com.lq.travel.model.vo.PictureVO;
import com.lq.travel.service.PictureService;
import com.lq.travel.service.UserService;
import com.lq.travel.annotation.AuthCheck;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 图片管理接口
 * 提供图片上传、下载、编辑、审核、查询等功能，支持文件上传和URL上传两种方式
 *
 * @author Lq304
 * @createDate 2025-08-25
 */
@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;


    // 1. 上传图片（文件上传）
    @PostMapping("/upload")
    @AuthCheck
    public ResponseDTO<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 2. 执行图片上传
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResponseUtils.success(pictureVO);
    }

    // 2. 删除图片
    @PostMapping("/delete")
    @AuthCheck
    public ResponseDTO<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 1. 参数校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        pictureService.deletePicture(id, loginUser);
        return ResponseUtils.success(true);
    }

    // 3. 更新图片信息（仅管理员）
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        // 1. 参数校验
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. DTO转实体对象
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);

        // 3. 处理标签字段（list转string）
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 4. 数据校验
        pictureService.validPicture(picture);

        // 5. 校验图片是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 6. 获取当前登录用户并填充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);

        // 7. 执行更新操作
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResponseUtils.success(true);
    }

    // 4. 根据ID获取图片详情（仅管理员）
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Picture> getPictureById(long id, HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 2. 查询图片信息
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        return ResponseUtils.success(picture);
    }

    // 5. 根据ID获取图片详情（封装类）
    @GetMapping("/get/vo")
    public ResponseDTO<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 3. 查询图片信息
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 5. 转换为封装类并返回
        return ResponseUtils.success(pictureService.getPictureVO(picture, request));
    }

    // 6. 分页获取图片列表（仅管理员）
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 1. 提取分页参数
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 2. 执行分页查询
        Map<String, Object> queryParams = pictureService.buildQueryParams(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), queryParams);
        return ResponseUtils.success(picturePage);
    }

    // 7. 分页获取图片列表（封装类）
    @PostMapping("/list/page/vo")
    public ResponseDTO<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        // 1. 提取分页参数
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 2. 限制爬虫访问
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 3. 设置默认审核状态（普通用户只能查看已过审的数据）
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());


        // 4. 执行分页查询
        Map<String, Object> queryParams = pictureService.buildQueryParams(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), queryParams);

        // 5. 转换为封装类并返回
        return ResponseUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    // 8. 分页获取图片列表（带缓存）
    @PostMapping("/list/page/vo/cache")
    public ResponseDTO<Page<PictureVO>> listPictureVOByPageWithCache(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        // 1. 限制爬虫访问
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR);

        // 2. 调用Service层缓存方法
        Page<PictureVO> pictureVOPage = pictureService.listPictureVOByPageWithCache(pictureQueryRequest, request);
        return ResponseUtils.success(pictureVOPage);
    }



    // 9. 编辑图片信息
    @PostMapping("/edit")
    @AuthCheck
    public ResponseDTO<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        // 1. 参数校验
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 6. 获取当前登录用户并填充审核参数
        User loginUser = userService.getLoginUser(request);

        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResponseUtils.success(true);
    }

    // 10. 获取图片标签和分类列表
    @GetMapping("/tag_category")
    public ResponseDTO<PictureTagCategory> listPictureTagCategory() {
        // 1. 创建标签分类对象
        PictureTagCategory pictureTagCategory = new PictureTagCategory();

        // 2. 设置预定义标签列表
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        pictureTagCategory.setTagList(tagList);

        // 3. 设置预定义分类列表
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setCategoryList(categoryList);

        return ResponseUtils.success(pictureTagCategory);
    }

    // 11. 图片审核（仅管理员）
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3. 执行审核操作
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResponseUtils.success(true);
    }

    // 12. 通过URL上传图片
    @PostMapping("/upload/url")
    @AuthCheck
    public ResponseDTO<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 2. 获取图片URL
        String fileUrl = pictureUploadRequest.getFileUrl();

        // 3. 执行URL上传
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResponseUtils.success(pictureVO);
    }

    // 13. 批量上传图片（仅管理员）
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        // 1. 参数校验
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3. 执行批量上传
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResponseUtils.success(uploadCount);
    }

    // 14. 清除图片缓存（仅管理员）
    @PostMapping("/cache/clear/{pictureId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> clearPictureCache(@PathVariable Long pictureId, HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);

        // 2. 执行缓存清除
        pictureService.clearPictureCache(pictureId);
        return ResponseUtils.success(true);
    }

    // 15. 清除所有图片缓存（仅管理员）
    @PostMapping("/cache/clear/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> clearAllPictureCache(HttpServletRequest request) {
        // 1. 执行缓存清除
        pictureService.clearAllPictureCache();
        return ResponseUtils.success(true);
    }

}
