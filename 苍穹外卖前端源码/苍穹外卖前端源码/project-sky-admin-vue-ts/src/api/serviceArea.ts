import request from '@/utils/request'
/**
 * 服务区域管理
 * 后端接口前缀 /admin/serviceArea
 **/

export const queryServiceAreaList = (params: any) => {
  return request({
    url: '/serviceArea/list',
    method: 'get',
    params
  })
}

export const addServiceArea = (params: any) => {
  return request({
    url: '/serviceArea',
    method: 'post',
    data: { ...params }
  })
}

export const editServiceArea = (params: any) => {
  return request({
    url: '/serviceArea',
    method: 'put',
    data: { ...params }
  })
}

export const deleteServiceArea = (id: string | (string | null)[]) => {
  return request({
    url: `/serviceArea/${id}`,
    method: 'delete'
  })
}

// 开通 / 停用 status: 1已开通 0未开通
export const serviceAreaStatusByStatus = (params: any) => {
  return request({
    url: `/serviceArea/status/${params.status}`,
    method: 'post',
    params: { id: params.id }
  })
}
