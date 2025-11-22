// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /orders/${param0}/items */
export async function listOrderItems(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listOrderItemsParams,
  options?: { [key: string]: any }
) {
  const { orderId: param0, ...queryParams } = params
  return request<API.ResponseDTOListOrderItem>(`/orders/${param0}/items`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /orders/cancel */
export async function cancelOrder(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.cancelOrderParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOBoolean>('/orders/cancel', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /orders/create */
export async function createOrder(body: API.CreateOrderRequest, options?: { [key: string]: any }) {
  return request<API.ResponseDTOLong>('/orders/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /orders/merchant/${param0} */
export async function getMerchantOrderDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMerchantOrderDetailParams,
  options?: { [key: string]: any }
) {
  const { orderId: param0, ...queryParams } = params
  return request<API.ResponseDTOOrder>(`/orders/merchant/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /orders/merchant/${param0}/cancel */
export async function merchantCancelOrder(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.merchantCancelOrderParams,
  options?: { [key: string]: any }
) {
  const { orderId: param0, ...queryParams } = params
  return request<API.ResponseDTOBoolean>(`/orders/merchant/${param0}/cancel`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /orders/merchant/${param0}/ship */
export async function shipOrder(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.shipOrderParams,
  options?: { [key: string]: any }
) {
  const { orderId: param0, ...queryParams } = params
  return request<API.ResponseDTOBoolean>(`/orders/merchant/${param0}/ship`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /orders/merchant/list */
export async function listMerchantOrders(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMerchantOrdersParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOIPageOrder>('/orders/merchant/list', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',
      // size has a default value: 10
      size: '10',

      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /orders/my */
export async function listMyOrders(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMyOrdersParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOIPageOrder>('/orders/my', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',
      // size has a default value: 10
      size: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /orders/my/${param0} */
export async function getMyOrderDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMyOrderDetailParams,
  options?: { [key: string]: any }
) {
  const { orderId: param0, ...queryParams } = params
  return request<API.ResponseDTOOrder>(`/orders/my/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /orders/pay */
export async function payOrder(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.payOrderParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOBoolean>('/orders/pay', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
