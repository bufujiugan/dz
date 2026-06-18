package com.dz.tavern.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class ProductEntity extends BaseEntity {
    private Long storeId;
    private Long categoryId;
    private String name;
    private String mainImage;
    private String images;
    private String description;
    private Integer status;
    private Integer recommended;
}
