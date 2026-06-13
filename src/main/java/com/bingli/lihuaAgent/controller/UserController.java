package com.bingli.lihuaAgent.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bingli.lihuaAgent.common.BaseResponse;
import com.bingli.lihuaAgent.common.DeleteRequest;
import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.common.ResultUtils;
import com.bingli.lihuaAgent.constant.UserConstant;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.model.dto.user.UserLoginRequest;
import com.bingli.lihuaAgent.model.dto.user.UserQueryRequest;
import com.bingli.lihuaAgent.model.dto.user.UserRegisterRequest;
import com.bingli.lihuaAgent.model.dto.user.UserUpdatePasswordRequest;
import com.bingli.lihuaAgent.model.dto.user.UserUpdateRequest;
import com.bingli.lihuaAgent.model.vo.LoginUserVO;
import com.bingli.lihuaAgent.model.vo.UserVo;
import com.bingli.lihuaAgent.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return 新用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result, "注册成功");
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @return 登录用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        LoginUserVO result = userService.userLogin(userAccount, userPassword);
        return ResultUtils.success(result, "登录成功");
    }


    /**
     * 用户注销
     *
     * @return 响应结果
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout() {

        // 当前登录用户 id
        Object loginId = StpUtil.getLoginIdDefaultNull();

        // 未登录
        if (loginId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 退出当前会话
        StpUtil.logout();
        return ResultUtils.success(true, "注销成功");
    }


    /**
     * 修改密码
     *
     * @param  userUpdatePasswordRequest
     * @return 响应结果
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updatePassword(
            @RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest) {

        if (userUpdatePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = userService.updatePassword(userUpdatePasswordRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改密码失败");
        }
        return ResultUtils.success(true, "修改密码成功");
    }



    /**
     * 获取当前登录用户
     *
     * @return 登录用户信息
     */
    @GetMapping("/current")
    public BaseResponse<LoginUserVO> getCurrentUser() {

        Long loginId = StpUtil.getLoginIdAsLong();

        LoginUserVO loginUserVO = userService.getLoginUserVO(loginId);

        return ResultUtils.success(loginUserVO);
    }

     /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(
            @RequestBody UserUpdateRequest request) {

        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        userService.updateUser(request);

        return ResultUtils.success(true);
    }


    /**
     * 获取用户列表（管理员）
     */
    @GetMapping("/list")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVo>> listUsers(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<UserVo> userVoPage = userService.listUsers(userQueryRequest);
        return ResultUtils.success(userVoPage);
    }

    /**
     * 删除用户（管理员）
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        boolean result = userService.deleteUser(id);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        return ResultUtils.success(true, "删除成功");
    }


}
