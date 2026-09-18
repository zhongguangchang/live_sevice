package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 师傅拒单入参
 */
@Data
public class ServiceOrderRejectionDTO implements Serializable {

    //订单id
    private Long id;

    //拒单原因
    private String rejectionReason;
}
