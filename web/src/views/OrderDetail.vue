<template>
  <div class="cb-page" v-loading="loading">
    <el-page-header @back="$router.push('/orders')" content="订单详情" style="margin-bottom: 18px" />

    <div v-if="order" class="cb-card detail">
      <div class="top">
        <div>
          <div class="no">{{ order.orderNo }}</div>
          <div class="cb-muted">{{ formatTime(order.createdAt) }} 下单</div>
        </div>
        <el-tag :type="statusType(order.status)" size="large">{{ statusText(order.status) }}</el-tag>
      </div>

      <el-steps :active="step" finish-status="success" align-center class="steps">
        <el-step title="已下单" />
        <el-step title="已支付" />
        <el-step title="制作中" />
        <el-step title="已完成" />
      </el-steps>

      <div class="items">
        <div v-for="it in order.items" :key="it.id" class="item">
          <span class="emoji">{{ it.emoji || '🍽️' }}</span>
          <div class="grow">
            <div class="name">{{ it.dishName }}</div>
            <div class="cb-muted">￥{{ Number(it.price).toFixed(2) }} × {{ it.quantity }}</div>
          </div>
          <div class="cb-price">{{ (Number(it.price) * it.quantity).toFixed(2) }}</div>
        </div>
      </div>

      <div class="meta">
        <div><span class="label">送餐地址</span>{{ order.address || '-' }}</div>
        <div><span class="label">备注</span>{{ order.remark || '无' }}</div>
        <div><span class="label">合计</span><span class="cb-price" style="font-size: 22px">{{ Number(order.totalAmount).toFixed(2) }}</span></div>
      </div>

      <div class="ops">
        <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" size="large" @click="pay">立即支付</el-button>
        <el-button v-if="order.status === 'PENDING_PAYMENT'" size="large" @click="cancel">取消订单</el-button>
        <el-button size="large" @click="$router.push('/')">再来一单</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { statusText, statusType } from '../utils/status'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)

const step = computed(() => {
  const s = order.value?.status
  return { PENDING_PAYMENT: 1, PAID: 2, COOKING: 3, COMPLETED: 4, CANCELLED: 1 }[s] || 1
})

function formatTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    order.value = await http.get(`/order/${route.params.orderNo}`)
  } finally {
    loading.value = false
  }
}

async function pay() {
  order.value = await http.post(`/order/${order.value.orderNo}/pay`)
  ElMessage.success('支付成功')
}

async function cancel() {
  await ElMessageBox.confirm('确定取消这个订单吗？', '提示', { type: 'warning' })
  order.value = await http.post(`/order/${order.value.orderNo}/cancel`)
  ElMessage.success('已取消')
}

onMounted(load)
</script>

<style scoped>
.detail { padding: 24px 26px; max-width: 760px; margin: 0 auto; }
.top { display: flex; justify-content: space-between; align-items: center; }
.no { font-size: 18px; font-weight: 700; font-family: ui-monospace, monospace; }
.steps { margin: 26px 0 10px; }
.items { border-top: 1px dashed #eef0f5; margin-top: 12px; padding-top: 8px; }
.item { display: flex; align-items: center; gap: 12px; padding: 10px 0; }
.emoji { font-size: 24px; }
.grow { flex: 1; }
.name { font-weight: 600; }
.meta { background: #f7f8fb; border-radius: 12px; padding: 14px 16px; margin-top: 14px; display: flex; flex-direction: column; gap: 8px; font-size: 14px; }
.label { color: var(--cb-muted); display: inline-block; width: 72px; }
.ops { margin-top: 22px; display: flex; gap: 10px; justify-content: flex-end; }
</style>