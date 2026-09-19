package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.ReviewPageQueryDTO;
import com.sky.entity.Review;
import com.sky.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 服务评价 Mapper
 */
@Mapper
public interface ReviewMapper {

    void insert(Review review);

    /**
     * 根据订单id查评价
     * <p>
     * 提交评价前先用它做一次校验。虽然 review 表上 order_id 有唯一索引
     * 能挡住重复插入，但先用 SQL 查一次能给出更友好的提示语
     * （「该订单已评价过了」比数据库唯一键冲突报错友好得多）。
     */
    @Select("select * from review where order_id = #{orderId}")
    Review getByOrderId(Long orderId);

    /**
     * 分页查询评价，联表带出用户昵称、头像、师傅姓名、服务名称
     */
    Page<ReviewVO> pageQuery(ReviewPageQueryDTO reviewPageQueryDTO);

    /**
     * 按条件统计评价数量，map 可传 providerId、status、score
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 统计某师傅的平均分（用于评分回写）
     */
    @Select("select round(avg(score), 2) from review where provider_id = #{providerId} and status = 1")
    java.math.BigDecimal avgScoreByProvider(Long providerId);

    /**
     * 统计某师傅的好评率（4 分及以上算好评）
     */
    @Select("select round(sum(case when score >= 4 then 1 else 0 end) * 100.0 / count(*), 2) " +
            "from review where provider_id = #{providerId} and status = 1")
    java.math.BigDecimal goodRateByProvider(Long providerId);

    /**
     * 统计某服务项目的平均分（用于评分回写）
     */
    @Select("select round(avg(score), 2) from review where service_id = #{serviceId} and status = 1")
    java.math.BigDecimal avgScoreByService(Long serviceId);

    /**
     * 平台/商家回复评价
     */
    @Update("update review set reply = #{reply}, reply_time = now() where id = #{id}")
    void updateReply(@Param("id") Long id, @Param("reply") String reply);

    /**
     * 显示/隐藏评价（运营下架恶意评价用）
     */
    @Update("update review set status = #{status} where id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 查询某个用户的全部评价（个人中心用）
     */
    List<Review> listByUserId(Long userId);
}
