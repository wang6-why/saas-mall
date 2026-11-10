package com.saas.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.mall.common.JwtUtil;
import com.saas.mall.entity.SysUser;
import com.saas.mall.entity.Tenant;
import com.saas.mall.mapper.SysUserMapper;
import com.saas.mall.mapper.TenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;

    public AuthService(TenantMapper tenantMapper, SysUserMapper sysUserMapper, JwtUtil jwtUtil) {
        this.tenantMapper = tenantMapper;
        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
    }

    /** 登录：用租户管理员账号登录，返回 token */
    public Map<String, Object> login(String username, String password) {
        Tenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getAdminUser, username).eq(Tenant::getAdminPwd, password));
        if (tenant == null) {
            throw new RuntimeException("账号或密码错误");
        }
        if (tenant.getStatus() == null || tenant.getStatus() != 1) {
            throw new RuntimeException("租户已被禁用");
        }
        String token = jwtUtil.createToken(tenant.getId(), tenant.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tenantId", tenant.getId());
        result.put("tenantName", tenant.getName());
        return result;
    }

    /** 平台注册新租户：创建租户 + 管理员用户（事务） */
    @Transactional
    public Tenant register(String name, String adminUser, String adminPwd) {
        Long exist = tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getAdminUser, adminUser));
        if (exist > 0) {
            throw new RuntimeException("管理员账号已存在");
        }
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setAdminUser(adminUser);
        tenant.setAdminPwd(adminPwd);
        tenant.setStatus(1);
        tenantMapper.insert(tenant);

        SysUser user = new SysUser();
        user.setTenantId(tenant.getId());
        user.setUsername(adminUser);
        user.setPassword(adminPwd);
        user.setRole("admin");
        sysUserMapper.insert(user);
        return tenant;
    }
}
