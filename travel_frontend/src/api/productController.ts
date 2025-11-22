// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /product/admin/add */
export async function adminAddProduct(body: API.Product, options?: { [key: string]: any }) {
  return request<API.ResponseDTOLong>('/product/admin/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /product/admin/delete */
export async function adminDeleteProduct(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOBoolean>('/product/admin/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /product/admin/get/${param0} */
export async function adminGetProduct(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.adminGetProductParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.ResponseDTOProduct>(`/product/admin/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /product/admin/list/page */
export async function adminListProducts(
  body: API.ProductQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOIPageProduct>('/product/admin/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /product/admin/update */
export async function adminUpdateProduct(body: API.Product, options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/product/admin/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /product/get/${param0} */
export async function getProductById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getProductByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.ResponseDTOProduct>(`/product/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /product/list */
export async function listProducts(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listProductsParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOIPageProduct>('/product/list', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',
      // pageSize has a default value: 12
      pageSize: '12',
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /product/merchant/add */
export async function merchantAddProduct(body: API.Product, options?: { [key: string]: any }) {
  return request<API.ResponseDTOLong>('/product/merchant/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /product/merchant/delete */
export async function merchantDeleteProduct(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOBoolean>('/product/merchant/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /product/merchant/list */
export async function merchantListProducts(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.merchantListProductsParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOIPageProduct>('/product/merchant/list', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',
      // pageSize has a default value: 10
      pageSize: '10',

      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /product/merchant/update */
export async function merchantUpdateProduct(body: API.Product, options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/product/merchant/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
