import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { side: 'user' } },
  { path: '/admin/login', component: () => import('./views/Login.vue'), meta: { side: 'admin' } },
  {
    path: '/',
    component: () => import('./layouts/UserLayout.vue'),
    meta: { side: 'user' },
    children: [
      { path: '', name: 'menu', component: () => import('./views/Menu.vue') },
      { path: 'orders', name: 'orders', component: () => import('./views/Orders.vue') },
      { path: 'orders/:orderNo', name: 'orderDetail', component: () => import('./views/OrderDetail.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('./layouts/AdminLayout.vue'),
    meta: { side: 'admin' },
    children: [
      { path: '', name: 'dashboard', component: () => import('./views/admin/Dashboard.vue') },
      { path: 'dishes', name: 'adminDishes', component: () => import('./views/admin/Dishes.vue') },
      { path: 'categories', name: 'adminCategories', component: () => import('./views/admin/Categories.vue') },
      { path: 'orders', name: 'adminOrders', component: () => import('./views/admin/AdminOrders.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('cb_token')
  const user = JSON.parse(localStorage.getItem('cb_user') || 'null')
  const isAdmin = !!user && user.role === 'ADMIN'

  // 管理端登录页
  if (to.path === '/admin/login') {
    return token && isAdmin ? '/admin' : true
  }
  // 管理端页面：必须是管理员
  if (to.path.startsWith('/admin')) {
    if (!token || !isAdmin) return '/admin/login'
    return true
  }
  // 用户端登录页
  if (to.path === '/login') {
    if (!token) return true
    return isAdmin ? '/admin' : '/'
  }
  // 用户端页面：必须先登录；管理员不进用户端
  if (!token) return '/login'
  if (isAdmin) return '/admin'
  return true
})

export default router