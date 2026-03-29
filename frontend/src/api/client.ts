import axios from 'axios'

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.request.use((config) => {
  // Future: attach auth token
  return config
})

client.interceptors.response.use(
  (response) => {
    const res = response.data
    // Unwrap R<T> wrapper: { code, message, data }
    if (res && typeof res === 'object' && res.code !== undefined) {
      if (res.code !== 200) {
        console.error('[API Biz Error]', res.code, res.message)
        return Promise.reject(new Error(res.message || 'Request failed'))
      }
      return res.data
    }
    return res
  },
  (error) => {
    // Unified error handling
    console.error('[API Error]', error?.response?.status, error?.message)
    return Promise.reject(error)
  }
)

export default client
