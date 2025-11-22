import axios from 'axios'
import { apiLogger } from '@/utils/logger'

const instance = axios.create({
  baseURL: '/api',
  timeout: 60000,
  withCredentials: true,
  transformResponse: [
    function (data) {
      // ? JSON ?????????????? JSON ????? HTML ????
      if (typeof data === 'string') {
        try {
          const processedData = data.replace(/"id":(\d{16,})/g, '"id":"$1"')
          return JSON.parse(processedData)
        } catch (error) {
          apiLogger.warn('??? JSON ??????????', error)
          return { code: -1, message: '?JSON??', raw: data }
        }
      }
      return data
    },
  ],
})

// ???????
instance.interceptors.request.use(
  function (config) {
    // ?? Content-Type ??????????????????? Content-Type?
    if (config.data && typeof config.data === 'object' && !Array.isArray(config.data)) {
      const contentType = config.headers?.['Content-Type'] || config.headers?.['content-type']
      if (!contentType && config.headers) {
        config.headers['Content-Type'] = 'application/json'
      }
    }

    apiLogger.debug('????', {
      method: config.method?.toUpperCase(),
      url: config.url,
      withCredentials: config.withCredentials,
      contentType: config.headers?.['Content-Type'] || config.headers?.['content-type'] || '???',
    })

    return config
  },
  function (error) {
    apiLogger.error('???????', error)
    return Promise.reject(error)
  },
)

// ???????
instance.interceptors.response.use(
  function (response) {
    apiLogger.debug('????', {
      method: response.config.method?.toUpperCase(),
      url: response.config.url,
      status: response.status,
      code: response.data?.code,
    })
    return response
  },
  function (error) {
    apiLogger.error('????', {
      method: error.config?.method?.toUpperCase(),
      url: error.config?.url,
      status: error.response?.status,
      message: error.response?.data?.message,
    })
    return Promise.reject(error)
  },
)

export default instance
