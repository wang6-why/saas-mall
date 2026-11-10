package com.saas.mall.config;

import com.saas.mall.common.TenantContext;
import com.saas.mall.entity.AuditLog;
import com.saas.mall.mapper.AuditLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 操作审计：企业客户最看重的合规能力之一（谁、什么时候、改了什么）。
 * 只记录写操作（POST/PUT/DELETE），异步化在本项目简化为同步写入。
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    public AuditAspect(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Around("execution(* com.saas.mall.controller..*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String result = "success";
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            result = "error";
            throw t;
        } finally {
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String method = req.getMethod();
                if (!"GET".equals(method)) {
                    save(req, pjp, result);
                }
            }
        }
    }

    private void save(HttpServletRequest req, ProceedingJoinPoint pjp, String result) {
        try {
            Long tenantId = TenantContext.get();
            if (tenantId == null) return;
            AuditLog log = new AuditLog();
            log.setTenantId(tenantId);
            log.setMethod(req.getMethod());
            log.setPath(req.getRequestURI());
            String args = Arrays.toString(pjp.getArgs());
            log.setBody(args.length() > 1000 ? args.substring(0, 1000) : args);
            log.setResult(result);
            auditLogMapper.insert(log);
        } catch (Exception ignore) {
            // 审计失败不能影响主流程
        }
    }
}
