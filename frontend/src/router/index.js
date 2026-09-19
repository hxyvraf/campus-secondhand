import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { guest: true } },
  { path: '/products/:id', name: 'product-detail', component: () => import('../views/ProductDetailView.vue') },
  { path: '/users/:id', name: 'user-home', component: () => import('../views/UserHomeView.vue') },
  { path: '/publish', name: 'publish', component: () => import('../views/PublishView.vue'), meta: { auth: true } },
  { path: '/my-products', name: 'my-products', component: () => import('../views/MyProductsView.vue'), meta: { auth: true } },
  { path: '/favorites', name: 'favorites', component: () => import('../views/FavoritesView.vue'), meta: { auth: true } },
  { path: '/orders', name: 'orders', component: () => import('../views/OrdersView.vue'), meta: { auth: true } },
  { path: '/messages', name: 'messages', component: () => import('../views/MessagesView.vue'), meta: { auth: true } },
  { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
  { path: '/admin/products', name: 'admin-products', component: () => import('../views/admin/AdminProductsView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/users', name: 'admin-users', component: () => import('../views/admin/AdminUsersView.vue'), meta: { auth: true, admin: true } },
  { path: '/admin/categories', name: 'admin-categories', component: () => import('../views/admin/AdminCategoriesView.vue'), meta: { auth: true, admin: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.auth && !userStore.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && !userStore.isAdmin) {
    return { path: '/' }
  }
  if (to.meta.guest && userStore.isLogin) {
    return { path: '/' }
  }
  return true
})

export default router
