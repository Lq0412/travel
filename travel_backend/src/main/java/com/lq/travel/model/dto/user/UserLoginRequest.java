package com.lq.travel.model.dto.user;


import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 用户账号
     */
    private String userAccount;


}
