import request from '@/utils/request'
/**
 * 服务订单管理
 * 后端接口前缀 /admin/serviceOrder
 **/

// 订单条件搜索
export const getServiceOrderPage = (params: any) => {
  return request({
    url: '/serviceOrder/conditionSearch',
    method: 'get',
    params
  })
}

// 各状态订单数量统计
export const getServiceOrderStatistics = () => {
  return request({
    url: '/serviceOrder/statistics',
    method: 'get'
  })
}

// 订单详情（含明细和师傅信息）
export const queryServiceOrderDetail = (id: string | (string | null)[]) => {
  return request({
    url: `/serviceOrder/details/${id}`,
    method: 'get'
  })
}

// 派单，providerId 为空则走自动派单算法
export const dispatchServiceOrder = (params: any) => {
  return request({
    url: '/serviceOrder/dispatch',
    method: 'put',
    data: { ...params }
  })
}

// 取消订单
export const cancelServiceOrder = (params: any) => {
  return request({
    url: '/serviceOrder/cancel',
    method: 'put',
    data: { ...params }
  })
}

// 完成订单
export const completeServiceOrder = (id: string | (string | null)[]) => {
  return request({
    url: `/serviceOrder/complete/${id}`,
    method: 'put'
  })
}
