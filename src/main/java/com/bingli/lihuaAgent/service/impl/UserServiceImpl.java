package com.bingli.lihuaAgent.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bingli.lihuaAgent.common.ErrorCode;
import com.bingli.lihuaAgent.constant.CommonConstant;
import com.bingli.lihuaAgent.exception.BusinessException;
import com.bingli.lihuaAgent.exception.ThrowUtils;
import com.bingli.lihuaAgent.model.dto.user.UserQueryRequest;
import com.bingli.lihuaAgent.model.dto.user.UserUpdatePasswordRequest;
import com.bingli.lihuaAgent.model.vo.UserVo;
import com.bingli.lihuaAgent.model.dto.user.UserUpdateRequest;
import com.bingli.lihuaAgent.model.entity.User;
import com.bingli.lihuaAgent.model.vo.LoginUserVO;
import com.bingli.lihuaAgent.service.UserService;
import com.bingli.lihuaAgent.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
  * 用户服务实现
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {




    /**
     * 用户注册
     *
     * @return 新用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 6 || checkPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 账户不能重复
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);

        long count = this.count(queryWrapper);

        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }


        String encryptPassword = BCrypt.hashpw(userPassword, BCrypt.gensalt(10));

        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUsername(userAccount);

        boolean saveResult = this.save(user);

        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }

        return user.getId();
    }

    /**
     * 用户登录
     *
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword) {
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        if (userPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserAccount, userAccount);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        if (!BCrypt.checkpw(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUsername(user.getUsername());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserAvatar(user.getUserAvatar());
        loginUserVO.setUserProfile(user.getUserProfile());
        loginUserVO.setUserRole(user.getUserRole());
        loginUserVO.setVipExpireTime(user.getVipExpireTime());

        StpUtil.login(user.getId());

        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     *
     * @return 登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(Long loginId) {
        User user = this.getById(loginId);
        if (user != null) {
            return getUserToLoginUserVO(user);
        }
        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
    }


    /**
     * 修改密码
     *
     * @return
     */
    @Override
    public boolean updatePassword(UserUpdatePasswordRequest req) {

        Long loginId = StpUtil.getLoginIdAsLong();

        User user = this.getById(loginId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (!BCrypt.checkpw(req.getOldPassword(), user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }
        if (Objects.equals(req.getOldPassword(), req.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与原密码相同");
        }
        String encryptedPassword = BCrypt.hashpw(
                req.getNewPassword(),
                BCrypt.gensalt(10)
        );

        User updateUser = new User();
        updateUser.setId(loginId);
        updateUser.setUserPassword(encryptedPassword);

        boolean success = this.updateById(updateUser);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改密码失败");
        }
        return true;
    }
    /**
     * 更新用户信息
     */
    @Override
    public void updateUser(UserUpdateRequest request) {

        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long id = request.getId();
        String userName = request.getUserName();
        String userAvatar = request.getUserAvatar();
        String userProfile = request.getUserProfile();

        Long loginId = StpUtil.getLoginIdAsLong();

        ThrowUtils.throwIf(!Objects.equals(id, loginId), new BusinessException(ErrorCode.NO_AUTH_ERROR));

        User user = this.getById(loginId);
        ThrowUtils.throwIf(user == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在"));
        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setUsername(userName);
        updateUser.setUserAvatar(userAvatar);
        updateUser.setUserProfile(userProfile);

        boolean result = this.updateById(updateUser);

        ThrowUtils.throwIf(!result, new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败"));

    }



    /**
     * 允许排序的字段白名单（前端字段名 -> 数据库字段名）
     */
    private static final Map<String, SFunction<User, ?>> SORT_FIELD_MAP = Map.of(
            "userName", User::getUsername,
            "createTime", User::getCreateTime,
            "updateTime", User::getUpdateTime,
            "vipExpireTime", User::getVipExpireTime
    );

    /**
     * 获取用户列表（管理员）
     */
    @Override
    public Page<UserVo> listUsers(UserQueryRequest userQueryRequest) {
        long current = Optional.ofNullable(userQueryRequest.getCurrent()).orElse(1);
        long size = Optional.ofNullable(userQueryRequest.getPageSize()).orElse(10);
        // 分页参数边界校验
        ThrowUtils.throwIf(current <= 0, ErrorCode.PARAMS_ERROR, "页号必须大于 0");
        ThrowUtils.throwIf(size <= 0, ErrorCode.PARAMS_ERROR, "每页条数必须大于 0");
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "每页条数不能超过 20");

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        // 构建查询条件
        queryWrapper.like(StringUtils.isNotBlank(userQueryRequest.getUserName()),
                User::getUsername, escapeSqlWildcard(userQueryRequest.getUserName()));
        queryWrapper.eq(StringUtils.isNotBlank(userQueryRequest.getUserRole()),
                User::getUserRole, userQueryRequest.getUserRole());
        queryWrapper.eq(StringUtils.isNotBlank(userQueryRequest.getUnionId()),
                User::getUnionId, userQueryRequest.getUnionId());
        queryWrapper.eq(StringUtils.isNotBlank(userQueryRequest.getMpOpenId()),
                User::getMpOpenId, userQueryRequest.getMpOpenId());
        queryWrapper.eq(userQueryRequest.getId() != null,
                User::getId, userQueryRequest.getId());
        queryWrapper.like(StringUtils.isNotBlank(userQueryRequest.getUserProfile()),
                User::getUserProfile, escapeSqlWildcard(userQueryRequest.getUserProfile()));

        // 排序：支持前端传入的排序字段
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        if (StringUtils.isNotBlank(sortField) && SORT_FIELD_MAP.containsKey(sortField)) {
            SFunction<User, ?> dbField = SORT_FIELD_MAP.get(sortField);
            if (CommonConstant.SORT_ORDER_ASC.equals(sortOrder)) {
                queryWrapper.orderByAsc(dbField);
            } else {
                queryWrapper.orderByDesc(dbField);
            }
        } else {
            queryWrapper.orderByDesc(User::getCreateTime);
        }

        Page<User> userPage = this.page(new Page<>(current, size), queryWrapper);
        // 转换为脱敏 VO
        Page<UserVo> userVoPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVo> userVoList = userPage.getRecords().stream()
                .map(this::getUserVo)
                .collect(Collectors.toList());
        userVoPage.setRecords(userVoList);
        return userVoPage;
    }

    /**
     * 转义 SQL 通配符（% 和 _），防止 LIKE 查询被注入
     */
    private String escapeSqlWildcard(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 删除用户（管理员）
     */
    @Override
    public boolean deleteUser(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return this.removeById(id);
    }

    /**
     * User 转 UserVo（脱敏）
     */
    private UserVo getUserVo(User user) {
        UserVo userVo = new UserVo();
        userVo.setId(user.getId());
        userVo.setUsername(user.getUsername());
        userVo.setUserAccount(user.getUserAccount());
        userVo.setUserAvatar(user.getUserAvatar());
        userVo.setUserProfile(user.getUserProfile());
        userVo.setUserRole(user.getUserRole());
        userVo.setVipExpireTime(user.getVipExpireTime());
        userVo.setCreateTime(user.getCreateTime());
        return userVo;
    }

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    private LoginUserVO getUserToLoginUserVO(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        LoginUserVO vo = new LoginUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserProfile(user.getUserProfile());
        vo.setUserRole(user.getUserRole());
        vo.setVipExpireTime(user.getVipExpireTime());
        return vo;
    }

}


