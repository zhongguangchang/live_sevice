package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 评价回复入参
 */
@Data
public class ReviewReplyDTO implements Serializable {

    //评价id
    private Long id;

    //回复内容
    private String reply;
}
