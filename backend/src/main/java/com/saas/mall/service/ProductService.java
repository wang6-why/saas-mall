package com.saas.mall.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saas.mall.entity.Product;
import com.saas.mall.mapper.ProductMapper;
import org.springframework.stereotype.Service;

@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {
}
