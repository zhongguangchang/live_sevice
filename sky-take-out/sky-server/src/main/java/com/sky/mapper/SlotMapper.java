package com.sky.mapper;

import com.sky.dto.SlotQueryDTO;
import com.sky.entity.Slot;
import com.sky.vo.SlotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 服务时段排期 Mapper
 * <p>
 * 本项目最关键的两个 SQL 在这里：deductStock 和 releaseStock。
 * <p>
 * 库存分两层，职责不同：
 * Redis 层（见 slot_deduct.lua）承接并发抢购，完全不碰数据库；
 * MySQL 层（本 Mapper）真正落账，作为最终事实。
 * 两者通过「Redis 预扣 - 支付成功后 MySQL 落账 - 定时对账」串起来。
 */
@Mapper
public interface SlotMapper {

    /**
     * 批量插入排期
     */
    void insertBatch(@Param("slots") List<Slot> slots);

    @Select("select * from slot where id = #{id}")
    Slot getById(Long id);

    /**
     * 查询可预约时段（用户端下单页用），联表带出师傅信息
     */
    List<SlotVO> listAvailable(SlotQueryDTO slotQueryDTO);

    /**
     * 管理端条件查询排期
     */
    List<SlotVO> listByCondition(SlotQueryDTO slotQueryDTO);

    /**
     * 库存落账：乐观锁扣减已预约数
     * <p>
     * 关键在于 where 里的 booked_count &lt; total_stock：
     * 把「判断还有没有名额」和「扣减」合并成一条原子 SQL，
     * 由数据库行锁保证不会超卖。
     * <p>
     * 返回值是影响行数，1 表示扣减成功，0 表示已被抢完。
     * Service 层必须判断这个返回值，不能想当然认为一定成功。
     * <p>
     * 这里没有用 version 字段做乐观锁，而是直接用库存条件作为判据，
     * 也就是 CAS 更新，比 version 更直接——version 会因为无关字段的
     * 修改而白白失败重试。
     *
     * @return 影响行数，1 成功 0 失败
     */
    @Update("update slot set booked_count = booked_count + 1, version = version + 1, update_time = now() " +
            "where id = #{slotId} and booked_count < total_stock and status = 1")
    int deductStock(Long slotId);

    /**
     * 库存释放：取消订单 / 超时未支付 / 师傅拒单时回补
     * <p>
     * where 里的 booked_count &gt; 0 是防御性写法，
     * 保证 booked_count 永远不会被减成负数（比如重复调用时）。
     *
     * @return 影响行数，1 成功 0 无需释放
     */
    @Update("update slot set booked_count = booked_count - 1, version = version + 1, update_time = now() " +
            "where id = #{slotId} and booked_count > 0")
    int releaseStock(Long slotId);

    /**
     * 修改时段状态（关闭/开放）
     */
    @Update("update slot set status = #{status}, update_time = now() where id = #{slotId}")
    void updateStatus(@Param("slotId") Long slotId, @Param("status") Integer status);

    /**
     * 查询指定日期区间内的全部排期（缓存预热和对账用）
     */
    @Select("select * from slot where service_date between #{begin} and #{end} and status = 1")
    List<Slot> listByDateRange(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 统计同一师傅在指定时段是否已有冲突排期（派单时做档期过滤）
     */
    @Select("select count(id) from slot where provider_id = #{providerId} and service_date = #{serviceDate} " +
            "and id != #{excludeSlotId} and status = 1 " +
            "and start_time < #{endTime} and end_time > #{startTime}")
    Integer countConflict(@Param("providerId") Long providerId,
                          @Param("serviceDate") LocalDate serviceDate,
                          @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime,
                          @Param("excludeSlotId") Long excludeSlotId);

    /**
     * 对账用：查出未来仍有预约占用的排期，
     * 定时任务拿这些记录去比对 Redis 里的实际值
     */
    @Select("select * from slot where service_date >= #{today} and booked_count > 0")
    List<Slot> listForReconcile(LocalDate today);

    /**
     * 对账时以 MySQL 为准强制修正
     */
    @Update("update slot set booked_count = #{bookedCount}, update_time = now() where id = #{slotId}")
    void forceSetBookedCount(@Param("slotId") Long slotId, @Param("bookedCount") Integer bookedCount);
}
