package com.lq.travel.controller;

import com.lq.travel.common.ResponseDTO;
import com.lq.travel.manager.upload.FilePictureUpload;
import com.lq.travel.model.dto.file.UploadPictureResult;
import com.lq.travel.model.entity.User;
import com.lq.travel.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileControllerTest {

    @Test
    void shouldUploadProductCoverByCosAndReturnUrl() {
        UserService userService = mock(UserService.class);
        FilePictureUpload filePictureUpload = mock(FilePictureUpload.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl("https://cos.example.com/product_cover/demo.png");
        when(filePictureUpload.uploadPicture(any(MockMultipartFile.class), eq("product_cover")))
                .thenReturn(uploadPictureResult);

        FileController fileController = new FileController();
        ReflectionTestUtils.setField(fileController, "userService", userService);
        ReflectionTestUtils.setField(fileController, "filePictureUpload", filePictureUpload);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                "fake-cover".getBytes()
        );

        ResponseDTO<String> response = fileController.uploadProductCover(file, request);

        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertEquals("https://cos.example.com/product_cover/demo.png", response.getData());
    }
}
