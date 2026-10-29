<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <div class="brand" @click="$router.push('/')">
          <span class="logo">🍜</span>
          <span class="brand-name">CloudBite</span>
          <span class="brand-sub">云餐 · 点餐平台</span>
        </div>
        <nav class="nav">
          <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">点餐</router-link>
          <router-link to="/orders" class="nav-item" :class="{ active: $route.path.startsWith('/orders') }">我的订单</router-link>
        </nav>
        <div class="actions">
          <el-badge :value="cart.count" :hidden="cart.count === 0" class="cart-badge">
            <el-button round @click="cartVisible = true">
              <el-icon><ShoppingCart /></el-icon>
              <span style="margin-left: 6px">购物车</span>
            </el-button>
          </el-badge>
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="30" style="background: var(--cb-primary)">
                {{ user.displayName.slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ user.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>

                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <cart-drawer v-model="cartVisible" @checkout="cartVisible = false" />
    <ai-assistant />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart, ArrowDown } from '@element-plus/icons-vue'
import { useCartStore } from '../store/cart'
import { useUserStore } from '../store/user'
import CartDrawer from '../components/CartDrawer.vue'
import AiAssistant from '../components/AiAssistant.vue'

const cart = useCartStore()
const user = useUserStore()
const router = useRouter()
const cartVisible = ref(false)

function onCommand(cmd) {
  if (cmd === 'orders') router.push('/orders')
  if (cmd === 'admin') router.push('/admin')
  if (cmd === 'logout') {
    user.logout()
    ElMessage.success('已退出登录')
    // 用整页跳转，避免 SPA 内部状态（缓存、未完成的请求）把页面留在原处
    setTimeout(() => { window.location.href = '/login' }, 300)
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid #eef0f5;
}
.header-inner {
  max-width: 1180px;
  margin: 0 auto;
  height: 66px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.brand { display: flex; align-items: baseline; gap: 8px; cursor: pointer; }
.logo { font-size: 24px; }
.brand-name { font-size: 20px; font-weight: 800; color: var(--cb-primary); letter-spacing: .3px; }
.brand-sub { font-size: 12px; color: var(--cb-muted); }
.nav { display: flex; gap: 6px; flex: 1; }
.nav-item {
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 15px;
  color: #555b6e;
}
.nav-item:hover { background: #f4f5f9; }
.nav-item.active { color: var(--cb-primary); background: #fff1eb; font-weight: 600; }
.actions { display: flex; align-items: center; gap: 16px; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-name { font-size: 14px; }
</style>