import request from '@/utils/request'
/**
 * 评价管理
 * 后端接口前缀 /admin/review
 **/

export const getReviewPage = (params: any) => {
  return request({
    url: '/review/page',
    method: 'get',
    params
  })
}

// 回复评价
export const replyReview = (params: any) => {
  return request({
    url: '/review/reply',
    method: 'put',
    data: { ...params }
  })
}

// 显示 / 隐藏评价 status: 1显示 0隐藏
export const reviewStatusByStatus = (params: any) => {
  return request({
    url: `/review/status/${params.status}`,
    method: 'post',
    params: { id: params.id }
  })
}
