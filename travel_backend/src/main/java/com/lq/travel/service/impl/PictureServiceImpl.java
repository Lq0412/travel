package com.lq.travel.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.exception.ThrowUtils;
import com.lq.travel.manager.CosManager;
import com.lq.travel.manager.FileManager;
import com.lq.travel.manager.upload.FilePictureUpload;
import com.lq.travel.manager.upload.PictureUploadTemplate;
import com.lq.travel.manager.upload.UrlPictureUpload;
import com.lq.travel.mapper.PictureMapper;
import com.lq.travel.model.dto.file.UploadPictureResult;
import com.lq.travel.model.dto.picture.*;
import com.lq.travel.model.entity.Picture;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.enums.PictureReviewStatusEnum;
import com.lq.travel.model.vo.PictureVO;
import com.lq.travel.model.vo.UserVO;
import com.lq.travel.service.PictureService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图片服务实现类
 * 提供图片上传、下载、编辑、审核、查询等核心功能，支持文件上传和URL上传两种方式
 *
 * @author Lq304
 * @createDate 2025-08-25
 */
@Service
@Slf4j
public class PictureServiceImpl implements PictureService {

    @Resource
    private PictureMapper pictureMapper;
    
    @Resource
    private FileManager fileManager;
    
    @Resource
    private UserService userService;
    
    @Resource
    private FilePictureUpload filePictureUpload;
    
    @Resource
    private UrlPictureUpload urlPictureUpload;
    
    @Resource
    private CosManager cosManager;
    @Resource
    private TransactionTemplate transactionTemplate;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    // 本地缓存配置
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /**
     * 上传图片（支持文件上传和URL上传）
     * 根据输入源类型自动选择上传方式，支持新增和更新操作
     *
     * @param inputSource          输入源（MultipartFile文件 或 String URL）
     * @param pictureUploadRequest 上传请求参数
     * @param loginUser            当前登录用户
     * @return 图片VO对象
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 1. 基础参数校验
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 2. 判断操作类型（新增/更新）
        final Long pictureId = pictureUploadRequest != null ? pictureUploadRequest.getId() : null;

        // 3. 更新操作的权限校验
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = pictureMapper.selectById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }


        // ==================== 第三步：选择上传策略并执行上传 ====================
        // 3.1 构建上传路径前缀（按用户ID划分目录，便于管理）
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());


        // 3.2 根据输入源类型选择上传策略（策略模式）
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload; // 默认文件上传
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload; // URL上传
        }

        // 3.3 执行图片上传，获取上传结果
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // ==================== 第四步：构建图片实体对象 ====================
        // 4.1 创建图片实体对象
        Picture picture = new Picture();
        // 4.2 设置基本信息
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());

        // 4.3 设置图片名称（优先使用用户指定的名称）
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);

        // 4.4 设置图片属性信息
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        
        // 4.5 设置时间字段（确保不为null）
        Date now = new Date();
        picture.setCreateTime(now);
        picture.setEditTime(now);
        picture.setUpdateTime(now);
        
        // 4.6 设置逻辑删除字段（确保不为null）
        picture.setIsDelete(0);

        // ==================== 第五步：设置审核参数 ====================
        // 5.1 根据用户角色设置审核状态
        fillReviewParams(picture, loginUser);

        // ==================== 第六步：数据库操作 ====================
        // 6.1 如果是更新操作，设置更新相关字段
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        transactionTemplate.execute(status -> {
            int result;
            if (pictureId != null) {
                // 更新操作
                result = pictureMapper.updateById(picture);
            } else {
                // 新增操作
                result = pictureMapper.insert(picture);
            }
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR, "图片上传失败");
            return picture;
        });
        //如果是更新操作，删除临时文件，并且释放额度

        return PictureVO.objToVo(picture);
    }

    /**
     * 校验
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 获取图片封装类
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装类
     * 批量转换图片实体列表为VO列表，并关联查询用户信息
     *
     * @param picturePage 图片分页信息
     * @param request     HTTP请求对象
     * @return 图片VO分页信息
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 1. 提取图片列表
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        
        // 2. 空列表处理
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        
        // 3. 对象列表转封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        
        // 4. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        
        // 5. 填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    /**
     * 构建查询参数Map
     * 支持多字段动态查询、模糊搜索、精确匹配、排序等功能
     *
     * @param pictureQueryRequest 查询请求参数
     * @return 查询参数Map
     */
    @Override
    public Map<String, Object> buildQueryParams(PictureQueryRequest pictureQueryRequest) {
        Map<String, Object> params = new HashMap<>();
        
        // 空参数处理
        if (pictureQueryRequest == null) {
            return params;
        }

        // 提取查询参数
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        Long userId = pictureQueryRequest.getUserId();

        // 图片属性查询参数
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();

        // 审核相关查询参数
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();


        // 搜索和排序参数
        String searchText = pictureQueryRequest.getSearchText();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 构建查询参数
        if (ObjUtil.isNotEmpty(id)) {
            params.put("id", id);
        }
        if (ObjUtil.isNotEmpty(userId)) {
            params.put("userId", userId);
        }
        if (StrUtil.isNotBlank(name)) {
            params.put("name", name);
        }
        if (StrUtil.isNotBlank(introduction)) {
            params.put("introduction", introduction);
        }
        if (StrUtil.isNotBlank(category)) {
            params.put("category", category);
        }
        if (ObjUtil.isNotEmpty(picWidth)) {
            params.put("picWidth", picWidth);
        }
        if (ObjUtil.isNotEmpty(picHeight)) {
            params.put("picHeight", picHeight);
        }
        if (ObjUtil.isNotEmpty(picSize)) {
            params.put("picSize", picSize);
        }
        if (ObjUtil.isNotEmpty(picScale)) {
            params.put("picScale", picScale);
        }
        if (StrUtil.isNotBlank(picFormat)) {
            params.put("picFormat", picFormat);
        }
        if (ObjUtil.isNotEmpty(reviewStatus)) {
            params.put("reviewStatus", reviewStatus);
        }
        if (ObjUtil.isNotEmpty(reviewerId)) {
            params.put("reviewerId", reviewerId);
        }
        if (StrUtil.isNotBlank(reviewMessage)) {
            params.put("reviewMessage", reviewMessage);
        }
        if (StrUtil.isNotBlank(searchText)) {
            params.put("searchText", searchText);
        }
        if (StrUtil.isNotBlank(sortField)) {
            params.put("sortField", sortField);
        }
        if (StrUtil.isNotBlank(sortOrder)) {
            params.put("sortOrder", sortOrder);
        }
        if (CollUtil.isNotEmpty(tags)) {
            params.put("tags", tags);
        }
        
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        if (ObjUtil.isNotEmpty(startEditTime)) {
            params.put("startEditTime", startEditTime);
        }
        if (ObjUtil.isNotEmpty(endEditTime)) {
            params.put("endEditTime", endEditTime);
        }

        return params;
    }

    /**
     * 执行图片审核操作
     *
     * @param pictureReviewRequest 审核请求参数
     * @param loginUser            当前登录用户（审核人）
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // ==================== 第一步：参数解析与校验 ====================
        // 1.1 获取图片ID和审核状态
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();

        // 1.2 将审核状态转换为枚举对象，便于后续处理
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        // 1.3 参数校验：ID不能为空，审核状态必须有效，不能设置为"审核中"状态
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // ==================== 第二步：检查图片是否存在 ====================
        // 2.1 根据ID查询原图片信息
        Picture oldPicture = pictureMapper.selectById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // ==================== 第三步：防止重复审核 ====================
        // 3.1 检查当前审核状态是否与要设置的状态相同（避免无效操作）
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // ==================== 第四步：更新审核状态 ====================
        // 4.1 创建更新对象，复制请求参数
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);

        // 4.2 设置审核人ID和审核时间
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());

        // 4.3 执行数据库更新操作
        int result = pictureMapper.updateById(updatePicture);
        ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 设置审核状态
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量上传图片
     * 通过必应图片搜索获取图片链接，然后批量上传到系统
     *
     * @param pictureUploadByBatchRequest 批量上传请求参数
     * @param loginUser                   当前登录用户
     * @return 成功上传的图片数量
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 1. 参数提取与校验
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        
        // 2. 设置图片名称前缀
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = "picture_" + loginUser.getId() + "_" + System.currentTimeMillis();
        }

        // 3. 验证数量限制
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");

        // 4. 网络爬虫获取图片链接
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;

        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        // 5. HTML解析提取图片链接
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;

        // 6. 批量处理图片上传
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");

            // 7. 跳过空链接
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }

            // 8. 处理图片上传地址
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 9. 构建上传请求参数
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);

            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }

            // 10. 执行单个图片上传
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }

            // 11. 数量控制
            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = pictureMapper.countByUrl(pictureUrl);
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        // 检查是否为本人或管理员
        if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = pictureMapper.selectById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            int result = pictureMapper.deleteById(pictureId);
            ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR);
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);

    }

    /**
     *
     * 编辑图片
     * @param pictureEditRequest
     * @param loginUser
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureMapper.selectById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        int result = pictureMapper.updateById(picture);
        ThrowUtils.throwIf(result <= 0, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 根据ID获取图片
     *
     * @param id 图片ID
     * @return 图片信息
     */
    @Override
    public Picture getById(Long id) {
        return pictureMapper.selectById(id);
    }

    /**
     * 更新图片
     *
     * @param picture 图片信息
     * @return 更新结果
     */
    @Override
    public boolean updateById(Picture picture) {
        int result = pictureMapper.updateById(picture);
        return result > 0;
    }

    /**
     * 分页查询图片
     *
     * @param page 分页参数
     * @param params 查询参数
     * @return 分页结果
     */
    @Override
    public Page<Picture> page(Page<Picture> page, Map<String, Object> params) {
        // 设置分页参数
        params.put("offset", (int) ((page.getCurrent() - 1) * page.getSize()));
        params.put("limit", (int) page.getSize());
        
        // 查询数据
        List<Picture> records = pictureMapper.selectByCondition(params);
        long total = pictureMapper.countByCondition(params);
        
        // 设置结果
        page.setRecords(records);
        page.setTotal(total);
        
        return page;
    }

    /**
     * 分页获取图片列表（带缓存）
     * 使用本地缓存和Redis缓存提升查询性能，支持多条件查询和排序
     *
     * @param pictureQueryRequest 图片查询请求参数
     * @param request             HTTP请求对象
     * @return 图片分页列表（封装类）
     */
    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 1. 提取分页参数
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 2. 限制爬虫访问
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 3. 设置默认审核状态（普通用户只能查看已过审的数据）
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 4. 构建缓存key
        String queryCondition;
        try {
            queryCondition = new ObjectMapper().writeValueAsString(pictureQueryRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = "yupicture:listPictureVOByPage:" + hashKey;
        String localCacheKey = "listPictureVOByPage:" + hashKey;

        try {
            ObjectMapper mapper = new ObjectMapper();

            // 5. 查询本地缓存 (Caffeine)
            String cachedValue = LOCAL_CACHE.getIfPresent(localCacheKey);
            if (cachedValue != null) {
                Page<PictureVO> cachedPage = mapper.readValue(
                        cachedValue,
                        new TypeReference<Page<PictureVO>>() {
                        }
                );
                return cachedPage;
            }

            // 6. 本地未命中 -> 查询Redis
            String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
            if (redisValue != null) {
                Page<PictureVO> cachedPage = mapper.readValue(
                        redisValue,
                        new TypeReference<Page<PictureVO>>() {
                        }
                );
                // 回填本地缓存，避免下次还去Redis
                LOCAL_CACHE.put(localCacheKey, redisValue);
                return cachedPage;
            }

            // 7. Redis未命中 -> 查询数据库
            Map<String, Object> queryParams = buildQueryParams(pictureQueryRequest);
            Page<Picture> picturePage = page(new Page<>(current, size), queryParams);
            Page<PictureVO> pictureVOPage = getPictureVOPage(picturePage, request);

            // 8. 写入缓存
            String cacheValue = mapper.writeValueAsString(pictureVOPage);
            int cacheExpireTime = 300 + new Random().nextInt(301); // Redis随机5~10分钟
            stringRedisTemplate.opsForValue().set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

            LOCAL_CACHE.put(localCacheKey, cacheValue);

            return pictureVOPage;

        } catch (Exception e) {
            // 9. 异常降级：直接查询数据库
            log.error("缓存查询失败，降级到数据库查询", e);
            Map<String, Object> queryParams = buildQueryParams(pictureQueryRequest);
            Page<Picture> picturePage = page(new Page<>(current, size), queryParams);
            return getPictureVOPage(picturePage, request);
        }
    }

    /**
     * 清除图片缓存
     *
     * @param pictureId 图片ID
     */
    @Override
    public void clearPictureCache(Long pictureId) {
        try {
            // 清除本地缓存
            LOCAL_CACHE.invalidateAll();
            
            // 清除Redis缓存（使用模式匹配）
            Set<String> keys = stringRedisTemplate.keys("yupicture:listPictureVOByPage:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            
            log.info("图片缓存清除成功: pictureId={}", pictureId);
        } catch (Exception e) {
            log.error("清除图片缓存失败: pictureId={}", pictureId, e);
        }
    }

    /**
     * 清除所有图片缓存
     */
    @Override
    public void clearAllPictureCache() {
        try {
            // 清除本地缓存
            LOCAL_CACHE.invalidateAll();
            
            // 清除Redis缓存
            Set<String> keys = stringRedisTemplate.keys("yupicture:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            
            log.info("所有图片缓存清除成功");
        } catch (Exception e) {
            log.error("清除所有图片缓存失败", e);
        }
    }

}




