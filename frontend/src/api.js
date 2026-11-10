import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

api.interceptors.response.use(
  res => res.data,
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('tenantName')
      location.reload()
    }
    return Promise.reject(err.response ? err.response.data : err)
  }
)

export const login = (username, password) => api.post('/auth/login', { username, password })
export const register = (name, username, password) => api.post('/auth/register', { name, username, password })
export const listProducts = keyword => api.get('/product/list', { params: { keyword } })
export const saveProduct = p => api.post('/product', p)
export const updateProduct = p => api.put('/product', p)
export const deleteProduct = id => api.delete(`/product/${id}`)
export const listOrders = () => api.get('/order/list')
export const createOrder = (productId, qty) => api.post('/order', { productId, qty })
export const payOrder = id => api.put(`/order/${id}/pay`)
