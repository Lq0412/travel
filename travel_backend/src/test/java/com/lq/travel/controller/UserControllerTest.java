package com.lq.travel.controller;

import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.model.dto.user.UserLoginRequest;
import com.lq.travel.model.dto.user.UserRegisterRequest;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.LoginUserVO;
import com.lq.travel.service.QuotaService;
import com.lq.travel.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserController controller;
    private UserService userService;
    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        controller = new UserController();
        userService = mock(UserService.class);
        quotaService = mock(QuotaService.class);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "quotaService", quotaService);
    }

    @Test
    void userRegisterShouldThrowWhenRequestIsNull() {
        BusinessException exception = assertThrows(BusinessException.class, () -> controller.userRegister(null));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void userRegisterShouldDelegateToService() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount("traveler");
        request.setUserPassword("12345678");
        request.setCheckPassword("12345678");
        when(userService.userRegister("traveler", "12345678", "12345678")).thenReturn(12L);

        var response = controller.userRegister(request);

        assertEquals(0, response.getCode());
        assertEquals(12L, response.getData());
    }

    @Test
    void userLoginShouldThrowWhenRequestIsNull() {
        BusinessException exception = assertThrows(BusinessException.class, () -> controller.userLogin(null, mock(HttpServletRequest.class)));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void userLoginShouldDelegateToService() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserAccount("traveler");
        request.setUserPassword("12345678");

        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(9L);
        loginUserVO.setUserName("测试用户");

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(userService.userLogin("traveler", "12345678", servletRequest)).thenReturn(loginUserVO);

        var response = controller.userLogin(request, servletRequest);

        assertEquals(0, response.getCode());
        assertEquals(9L, response.getData().getId());
    }

    @Test
    void getUserQuotaShouldReturnLoginErrorWhenUserMissing() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(userService.getLoginUser(servletRequest)).thenReturn(null);

        var response = controller.getUserQuota(servletRequest);

        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), response.getCode());
    }

    @Test
    void rechargeUserQuotaShouldRejectInvalidTokens() {
        var response = controller.rechargeUserQuota(1L, 0);

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), response.getCode());
        assertEquals("充值数量必须大于0", response.getMessage());
    }

    @Test
    void getUserQuotaShouldReturnRemainingQuotaForCurrentUser() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        User user = new User();
        user.setId(3L);
        when(userService.getLoginUser(servletRequest)).thenReturn(user);
        when(quotaService.getRemainingQuota(3L)).thenReturn(456);

        var response = controller.getUserQuota(servletRequest);

        assertEquals(0, response.getCode());
        assertEquals(456, response.getData());
        verify(quotaService).getRemainingQuota(3L);
    }
}
