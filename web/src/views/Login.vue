<template>
  <div class="login-page" :class="{ admin: isAdminSide }">
    <div class="left">
      <div class="brand">
        <span class="logo">{{ isAdminSide ? '📊' : '🍜' }}</span>
        <span class="name">CloudBite 云餐</span>
        <span class="badge">{{ isAdminSide ? '管理端' : '用户端' }}</span>
      </div>

      <template v-if="isAdminSide">
        <h1>餐厅管理后台<br />看数据，管菜品，盯订单</h1>
        <ul>
          <li>📈 营业额 / 订单量 / 注册用户，一屏掌握</li>
          <li>🔥 热销菜品 Top5、订单状态分布</li>
          <li>🍽️ 菜品上下架、库存与价格维护</li>
          <li>📦 订单接单、制作、出餐全流程</li>
        </ul>
        <p class="tip">管理员账号：admin / admin123</p>
      </template>

      <template v-else>
        <h1>校园点餐<br />从下单到出餐，全流程数字化</h1>
        <ul>
          <li>🤖 AI 智能推荐：说口味，帮你选菜</li>
          <li>🧾 实时热销榜，跟着大家点不会错</li>
          <li>💬 AI 客服：随时查订单进度</li>
          <li>⚡ 下单即通知后厨，平均 15 分钟出餐</li>
        </ul>
        <p class="tip">演示账号：xiaoming / 123456</p>
      </template>
    </div>

    <div class="right">
      <div class="card">
        <div class="card-head">
          <h2>{{ isAdminSide ? '管理端登录' : (formMode === 'register' ? '注册新账号' : '欢迎回来') }}</h2>
          <span class="sub">{{ isAdminSide ? '仅限餐厅管理员账号' : (formMode === 'register' ? '注册后即可点餐' : '点餐、查订单、看推荐') }}</span>
        </div>

        <el-form :model="form" label-position="top" @submit.prevent>
          <el-form-item label="用户名">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" @keyup.enter="submit" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" size="large" show-password
                      placeholder="请输入密码" @keyup.enter="submit" />
          </el-form-item>
          <el-form-item v-if="formMode === 'register'" label="昵称">
            <el-input v-model="form.nickname" size="large" placeholder="怎么称呼你" @keyup.enter="submit" />
          </el-form-item>
          <el-alert v-if="isAdminSide" type="info" :closable="false" show-icon class="hint"
                    title="该入口仅管理员可登录，学生账号请返回用户端" />

          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
            {{ formMode === 'register' ? '注 册' : '登 录' }}
          </el-button>
        </el-form>

        <div class="switch">
          <template v-if="isAdminSide">
            <router-link to="/login">← 返回用户端登录</router-link>
          </template>
          <template v-else-if="formMode === 'register'">
            已有账号？<a @click="formMode = 'login'">去登录</a>
            <span class="sep">·</span>
            <router-link to="/admin/login">管理员登录 →</router-link>
          </template>
          <template v-else>
            还没有账号？<a @click="formMode = 'register'">立即注册</a>
            <span class="sep">·</span>
            <router-link to="/admin/login">管理员登录 →</router-link>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const isAdminSide = computed(() => route.path === '/admin/login')

const loading = ref(false)
const formMode = ref('login')
const form = reactive({
  username: isAdminSide.value ? 'admin' : 'xiaoming',
  password: isAdminSide.value ? 'admin123' : '123456',
  nickname: ''
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = formMode.value === 'register'
      ? await user.register({ username: form.username, password: form.password, nickname: form.nickname })
      : await user.login({ username: form.username, password: form.password })

    // 管理端入口：只放行管理员
    if (isAdminSide.value && data.role !== 'ADMIN') {
      user.logout()
      ElMessage.error('该账号不是管理员，请返回用户端登录')
      return
    }
    // 用户端入口：管理员请走管理端
    if (!isAdminSide.value && data.role === 'ADMIN') {
      ElMessage.success('管理员账号，正在进入管理后台…')
      router.push('/admin')
      return
    }

    ElMessage.success('欢迎，' + (data.nickname || data.username))
    router.push(data.role === 'ADMIN' ? '/admin' : '/')
  } finally {
    loading.value = false
  }
}


</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: 1.15fr 1fr; }
.left {
  background: linear-gradient(150deg, #1f2430 0%, #2c3446 55%, #ff6b35 160%);
  color: #fff;
  padding: 64px 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.admin .left {
  background: linear-gradient(150deg, #16213e 0%, #1f2a44 55%, #3a7bd5 160%);
}
.brand { display: flex; align-items: center; gap: 10px; font-size: 22px; font-weight: 800; margin-bottom: 36px; }
.logo { font-size: 30px; }
.badge {
  font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 999px;
  background: rgba(255, 255, 255, 0.16); letter-spacing: 0.5px;
}
h1 { font-size: 34px; line-height: 1.35; margin: 0 0 26px; }
ul { list-style: none; padding: 0; margin: 0 0 30px; color: #cfd5e4; line-height: 2.1; font-size: 14px; }
.tip { color: #9aa3b8; font-size: 13px; }
.right { display: flex; align-items: center; justify-content: center; background: var(--cb-bg); }
.card { width: 400px; background: #fff; border-radius: 18px; padding: 34px 32px; box-shadow: 0 16px 40px rgba(22, 28, 45, 0.08); }
.card-head { margin-bottom: 18px; }
h2 { margin: 0 0 4px; font-size: 22px; }
.sub { color: var(--cb-muted); font-size: 13px; }
.hint { margin-bottom: 16px; }
.switch { margin-top: 16px; font-size: 13px; color: var(--cb-muted); text-align: center; }
.switch a { color: var(--cb-primary); cursor: pointer; }
.sep { margin: 0 6px; }
</style>
