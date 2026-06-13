package com.bingli.lihuaAgent.config.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.bingli.lihuaAgent.model.entity.User;
import com.bingli.lihuaAgent.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserService userService;

    /**
     * 返回权限列表
     */
    @Override
    public List<String> getPermissionList(
            Object loginId,
            String loginType) {

        return new ArrayList<>();
    }

    /**
     * 返回角色列表
     */
    @Override
    public List<String> getRoleList(
            Object loginId,
            String loginType) {

        Long userId = Long.valueOf(loginId.toString());

        User user = userService.getById(userId);

        if (user == null) {
            return new ArrayList<>();
        }

        return Collections.singletonList(user.getUserRole());
    }
}