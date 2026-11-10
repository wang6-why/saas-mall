package com.saas.mall.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 租户拦截器必须放在最前（在分页等其他拦截器之前）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new SaasTenantLineHandler()));
        return interceptor;
    }
}
