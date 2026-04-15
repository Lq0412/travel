package com.lq.travel.controller;

import com.lq.travel.common.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.exception.ThrowUtils;
import com.lq.travel.manager.upload.FilePictureUpload;
import com.lq.travel.model.dto.file.UploadPictureResult;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {

    private static final String PRODUCT_COVER_UPLOAD_PATH = "product_cover";

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @PostMapping("/upload/product-cover")
    public ResponseDTO<String> uploadProductCover(@RequestPart("file") MultipartFile file,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "请上传商品封面");

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(file, PRODUCT_COVER_UPLOAD_PATH);
        return ResponseUtils.success(uploadPictureResult.getUrl());
    }
}
