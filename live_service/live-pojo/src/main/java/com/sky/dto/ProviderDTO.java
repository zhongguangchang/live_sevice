package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务人员（师傅）新增/编辑入参
 * <p>
 * 师傅的技能是一组服务分类 id，保存时写入 provider_skill 表。
 */
@Data
public class ProviderDTO implements Serializable {

    private Long id;

    //关联的后台账号id（可为空）
    private Long employeeId;

    //师傅姓名
    private String name;

    //联系电话
    private String phone;

    //头像url
    private String avatar;

    //性别 0女 1男
    private String sex;

    //身份证号
    private String idNumber;

    //资质证书图片url
    private String certImage;

    //从业年限
    private Integer workYears;

    //个人简介
    private String intro;

    //可提供的服务方式 1上门 2到店 3都支持
    private Integer serviceMode;

    //接单状态 1可接单 2忙碌 3休息中 4已离职
    private Integer status;

    //擅长的服务分类id列表
    private List<Long> categoryIds = new ArrayList<>();
}
