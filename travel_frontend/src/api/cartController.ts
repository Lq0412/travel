// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /cart/add */
export async function addToCart(body: API.Cart, options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/cart/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /cart/clear */
export async function clearCart(options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/cart/clear', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /cart/delete */
export async function deleteCart(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/cart/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /cart/list */
export async function getCartList(options?: { [key: string]: any }) {
  return request<API.ResponseDTOListCartVO>('/cart/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /cart/update */
export async function updateCart(body: API.Cart, options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/cart/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
