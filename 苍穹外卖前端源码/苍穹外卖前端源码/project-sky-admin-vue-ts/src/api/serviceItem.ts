import request from '@/utils/request'
/**
 * 服务项目管理
 * 后端接口前缀 /admin/serviceItem
 **/

// 分页查询
export const getServiceItemPage = (params: any) => {
  return request({
    url: '/serviceItem/page',
    method: 'get',
    params
  })
}

// 查询详情（含规格）
export const queryServiceItemById = (id: string | (string | null)[]) => {
  return request({
    url: `/serviceItem/${id}`,
    method: 'get'
  })
}

// 新增（含规格）
export const addServiceItem = (params: any) => {
  return request({
    url: '/serviceItem',
    method: 'post',
    data: { ...params }
  })
}

// 修改（规格全删重插）
export const editServiceItem = (params: any) => {
  return request({
    url: '/serviceItem',
    method: 'put',
    data: { ...params }
  })
}

// 批量删除
export const deleteServiceItem = (ids: string) => {
  return request({
    url: '/serviceItem',
    method: 'delete',
    params: { ids }
  })
}

// 起售停售
export const serviceItemStatusByStatus = (params: any) => {
  return request({
    url: `/serviceItem/status/${params.status}`,
    method: 'post',
    params: { id: params.id }
  })
}

// 按分类查询服务项目列表
export const queryServiceItemList = (params: any) => {
  return request({
    url: '/serviceItem/list',
    method: 'get',
    params
  })
}
