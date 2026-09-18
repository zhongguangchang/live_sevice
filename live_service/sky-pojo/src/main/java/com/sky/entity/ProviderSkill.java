package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务人员技能（师傅擅长的服务分类，多对多）
 * <p>
 * 派单时的第一层过滤条件：只把订单派给掌握了该服务所属分类的师傅。
 * level 可用于派单优先级排序（高级师傅优先接大单）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSkill implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //服务人员id
    private Long providerId;

    //擅长的服务分类id
    private Long categoryId;

    //技能等级 1初级 2中级 3高级
    private Integer level;
}
