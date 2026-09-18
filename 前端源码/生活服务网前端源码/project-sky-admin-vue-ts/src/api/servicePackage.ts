import request from '@/utils/request'
/**
 * 服务套餐管理
 * 后端接口前缀 /admin/servicePackage
 **/

export const getServicePackagePage = (params: any) => {
  return request({
    url: '/servicePackage/page',
    method: 'get',
    params
  })
}

export const queryServicePackageById = (id: string | (string | null)[]) => {
  return request({
    url: `/servicePackage/${id}`,
    method: 'get'
  })
}

export const addServicePackage = (params: any) => {
  return request({
    url: '/servicePackage',
    method: 'post',
    data: { ...params }
  })
}

export const editServicePackage = (params: any) => {
  return request({
    url: '/servicePackage',
    method: 'put',
    data: { ...params }
  })
}

export const deleteServicePackage = (ids: string) => {
  return request({
    url: '/servicePackage',
    method: 'delete',
    params: { ids }
  })
}

export const servicePackageStatusByStatus = (params: any) => {
  return request({
    url: `/servicePackage/status/${params.status}`,
    method: 'post',
    params: { id: params.id }
  })
}

export const queryServicePackageList = (params: any) => {
  return request({
    url: '/servicePackage/list',
    method: 'get',
    params
  })
}
