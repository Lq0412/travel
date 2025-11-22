package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.model.dto.picture.*;
import com.lq.travel.model.entity.Picture;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;


/**
 * @author Lq304
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-25 17:15:05
 */
public interface PictureService {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    void validPicture(Picture picture);

    /**
     * 获取图片封装类（单条）
     *
     * @param picture 图片信息
     * @param request HTTP请求
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片封装类（多条）
     *
     * @param picturePage 图片分页信息
     * @param request     HTTP请求
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);


    /**
     * 构建查询参数
     *
     * @param pictureQueryRequest
     * @return
     */
    Map<String, Object> buildQueryParams(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 删除图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 检查图片权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);


    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 根据ID获取图片
     *
     * @param id 图片ID
     * @return 图片信息
     */
    Picture getById(Long id);

    /**
     * 更新图片
     *
     * @param picture 图片信息
     * @return 更新结果
     */
    boolean updateById(Picture picture);

    /**
     * 分页查询图片
     *
     * @param page 分页参数
     * @param params 查询参数
     * @return 分页结果
     */
    Page<Picture> page(Page<Picture> page, Map<String, Object> params);

    /**
     * 分页获取图片列表（带缓存）
     * 使用本地缓存和Redis缓存提升查询性能，支持多条件查询和排序
     *
     * @param pictureQueryRequest 图片查询请求参数
     * @param request             HTTP请求对象
     * @return 图片分页列表（封装类）
     */
    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 清除图片缓存
     *
     * @param pictureId 图片ID
     */
    void clearPictureCache(Long pictureId);

    /**
     * 清除所有图片缓存
     */
    void clearAllPictureCache();
}
