import request from '@/utils/request'
/**
 * 服务人员（师傅）管理
 * 后端接口前缀 /admin/provider
 **/

export const getProviderPage = (params: any) => {
  return request({
    url: '/provider/page',
    method: 'get',
    params
  })
}

export const queryProviderById = (id: string | (string | null)[]) => {
  return request({
    url: `/provider/${id}`,
    method: 'get'
  })
}

export const addProvider = (params: any) => {
  return request({
    url: '/provider',
    method: 'post',
    data: { ...params }
  })
}

export const editProvider = (params: any) => {
  return request({
    url: '/provider',
    method: 'put',
    data: { ...params }
  })
}

// 接单状态 1可接单 2忙碌 3休息中 4已离职
export const providerStatusByStatus = (params: any) => {
  return request({
    url: `/provider/status/${params.status}`,
    method: 'post',
    params: { id: params.id }
  })
}

export const queryProviderList = (params: any) => {
  return request({
    url: '/provider/list',
    method: 'get',
    params
  })
}

// 按技能分类查询可接单的师傅，派单时用
export const queryAvailableProvider = (params: any) => {
  return request({
    url: '/provider/available',
    method: 'get',
    params
  })
}
