<template>
  <el-container class="admin">
    <el-aside width="216px" class="aside">
      <div class="brand">
        <span class="logo">🍜</span>
        <div>
          <div class="brand-name">CloudBite</div>
          <div class="brand-sub">商家后台</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="menu">
        <el-menu-item index="/admin">
          <el-icon><DataLine /></el-icon><span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/dishes">
          <el-icon><Dish /></el-icon><span>菜品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Menu /></el-icon><span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon><span>订单管理</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-foot">
        <el-button text @click="logout">退出登录</el-button>
      </div>
    </el-aside>
    <el-container>
      <el-header class="top">
        <div class="top-title">{{ title }}</div>
        <el-dropdown @command="(c) => c === 'logout' && logout()">
          <span class="user">
            <el-avatar :size="28" style="background: var(--cb-primary)">{{ user.displayName.slice(0, 1) }}</el-avatar>
            <span>{{ user.displayName }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataLine, Dish, List, Menu } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const title = computed(() => {
  if (route.path === '/admin') return '数据看板'
  if (route.path.includes('dishes')) return '菜品管理'
  if (route.path.includes('categories')) return '分类管理'
  if (route.path.includes('orders')) return '订单管理'
  return '商家后台'
})

function logout() {
  user.logout()
  setTimeout(() => { window.location.href = '/admin/login' }, 200)
}
</script>

<style scoped>
.admin { min-height: 100vh; }
.aside { background: #1f2430; color: #fff; display: flex; flex-direction: column; }
.brand { display: flex; gap: 10px; align-items: center; padding: 20px; }
.logo { font-size: 26px; }
.brand-name { font-weight: 800; font-size: 17px; color: #fff; }
.brand-sub { font-size: 12px; color: #8b93a7; }
.menu { border-right: none; background: transparent; flex: 1; }
.menu :deep(.el-menu-item) { color: #c3c9d8; }
.menu :deep(.el-menu-item:hover) { background: #2a3040; }
.menu :deep(.el-menu-item.is-active) { background: var(--cb-primary); color: #fff; }
.aside-foot { padding: 16px; }
.top { background: #fff; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eef0f5; }
.top-title { font-weight: 700; font-size: 16px; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.main { background: var(--cb-bg); padding: 20px; }
</style>