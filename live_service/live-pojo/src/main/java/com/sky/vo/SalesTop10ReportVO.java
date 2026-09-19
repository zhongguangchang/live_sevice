package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 服务销量排名 Top10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTop10ReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 服务名称列表（按销量从高到低） */
    private List<String> nameList;

    /** 对应的销量 */
    private List<Integer> numberList;
}
