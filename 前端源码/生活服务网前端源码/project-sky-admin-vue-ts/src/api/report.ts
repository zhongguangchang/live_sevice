import request from '@/utils/request'

/**
 * 统计报表接口
 * 后端前缀 /admin/report（前端经 /api 代理过去）
 */

// 经营数据概览（工作台首屏）
export const getBusinessOverview = () => {
  return request({
    url: '/report/overview',
    method: 'get'
  })
}

// 营业额统计
export const getTurnoverStatistics = (params: any) => {
  return request({
    url: '/report/turnoverStatistics',
    method: 'get',
    params
  })
}

// 用户统计（新增 + 累计）
export const getUserStatistics = (params: any) => {
  return request({
    url: '/report/userStatistics',
    method: 'get',
    params
  })
}

// 订单统计
export const getOrdersStatistics = (params: any) => {
  return request({
    url: '/report/ordersStatistics',
    method: 'get',
    params
  })
}

// 服务销量 Top10
export const getSalesTop10 = (params: any) => {
  return request({
    url: '/report/top10',
    method: 'get',
    params
  })
}

/**
 * 导出运营数据报表。
 * 注意 responseType 必须是 blob：后端返回的是 Excel 二进制流，
 * 按默认的 json 解析会把文件内容当字符串硬拆，直接报错。
 */
export const exportReport = (params: any) => {
  return request({
    url: '/report/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
