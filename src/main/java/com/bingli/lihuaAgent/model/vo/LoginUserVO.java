package com.bingli.lihuaAgent.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LoginUserVO implements Serializable {

    private Long id;

    private String username;

    private String userAccount;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    private LocalDateTime vipExpireTime;

    private static final long serialVersionUID = 1L;
}
