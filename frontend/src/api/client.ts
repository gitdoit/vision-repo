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
  (response) => response.data,
  (error) => {
    // Unified error handling
    console.error('[API Error]', error?.response?.status, error?.message)
    return Promise.reject(error)
  }
)

export default client
