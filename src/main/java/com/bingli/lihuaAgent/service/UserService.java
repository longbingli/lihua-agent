package com.bingli.lihuaAgent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bingli.lihuaAgent.model.dto.user.UserQueryRequest;
import com.bingli.lihuaAgent.model.dto.user.UserUpdatePasswordRequest;
import com.bingli.lihuaAgent.model.vo.UserVo;
import com.bingli.lihuaAgent.model.dto.user.UserUpdateRequest;
import com.bingli.lihuaAgent.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bingli.lihuaAgent.model.vo.LoginUserVO;

/**
 * 用户服务
 *
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword);

    /**
     * 获取当前登录用户
     *
     * @return 登录用户信息
     */
    LoginUserVO getLoginUserVO(Long loginId);


    /**
     * 修改密码
     *
     * @param userUpdatePasswordRequest
     * @return
     */
    boolean updatePassword(UserUpdatePasswordRequest userUpdatePasswordRequest);

    /**
     * 更新用户信息
     */
    void updateUser(UserUpdateRequest request);

    /**
     * 获取用户列表（管理员）
     *
     * @param userQueryRequest 查询请求
     * @return 用户分页列表
     */
    Page<UserVo> listUsers(UserQueryRequest userQueryRequest);

    /**
     * 删除用户（管理员）
     *
     * @param id 用户id
     * @return 是否删除成功
     */
    boolean deleteUser(long id);


}

