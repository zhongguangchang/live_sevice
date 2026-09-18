import request from '@/utils/request'
/**
 * 服务时段排期管理
 * 后端接口前缀 /admin/slot
 **/

// 批量生成排期（日期区间 x 师傅 x 服务 x 时段）
export const batchCreateSlot = (params: any) => {
  return request({
    url: '/slot/batch',
    method: 'post',
    data: { ...params }
  })
}

// 条件查询排期
export const querySlotList = (params: any) => {
  return request({
    url: '/slot/list',
    method: 'get',
    params
  })
}

// 关闭或开放时段  status: 1可预约 0已关闭
export const slotStatusByStatus = (params: any) => {
  return request({
    url: `/slot/status/${params.status}`,
    method: 'post',
    params: { id: params.id }
  })
}

// 手动把排期库存预热到 Redis
export const warmUpSlotStock = (params: any) => {
  return request({
    url: '/slot/warmup',
    method: 'post',
    params
  })
}

// 手动触发 Redis 与 MySQL 的库存对账
export const reconcileSlotStock = () => {
  return request({
    url: '/slot/reconcile',
    method: 'post'
  })
}
