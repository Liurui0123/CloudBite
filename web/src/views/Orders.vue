<template>
  <div class="cb-page">
    <div class="head">
      <h2 class="cb-title" style="margin: 0">我的订单</h2>
      <el-button text @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="list">
      <div v-for="o in orders" :key="o.orderNo" class="order cb-card">
        <div class="order-top">
          <div>
            <span class="no">{{ o.orderNo }}</span>
            <span class="cb-muted">　{{ formatTime(o.createdAt) }}</span>
          </div>
          <el-tag :type="statusType(o.status)" effect="light">{{ statusText(o.status) }}</el-tag>
        </div>
        <div class="items">
          <div v-for="it in o.items" :key="it.id" class="item">
            <span class="emoji">{{ it.emoji || '🍽️' }}</span>
            <span class="name">{{ it.dishName }}</span>
            <span class="cb-muted">×{{ it.quantity }}</span>
          </div>
        </div>
        <div class="order-foot">
          <div class="cb-muted">共 {{ o.itemCount }} 件　合计 <span class="cb-price">{{ Number(o.totalAmount).toFixed(2) }}</span></div>
          <div class="ops">
            <el-button v-if="o.status === 'PENDING_PAYMENT'" type="primary" @click="pay(o)">去支付</el-button>
            <el-button v-if="o.status === 'PENDING_PAYMENT'" @click="cancel(o)">取消</el-button>
            <el-button text @click="$router.push(`/orders/${o.orderNo}`)">详情</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="还没有订单，去点一单吧" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { statusText, statusType } from '../utils/status'

const orders = ref([])
const loading = ref(false)

function formatTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    orders.value = await http.get('/order/my')
  } finally {
    loading.value = false
  }
}

async function pay(o) {
  await http.post(`/order/${o.orderNo}/pay`)
  ElMessage.success('支付成功')
  load()
}

async function cancel(o) {
  await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
  await http.post(`/order/${o.orderNo}/cancel`)
  ElMessage.success('订单已取消')
  load()
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.list { display: flex; flex-direction: column; gap: 14px; }
.order { padding: 16px 18px; }
.order-top { display: flex; align-items: center; justify-content: space-between; }
.no { font-weight: 600; font-family: ui-monospace, monospace; }
.items { display: flex; flex-wrap: wrap; gap: 14px; margin: 14px 0; }
.item { display: flex; align-items: center; gap: 6px; background: #f7f8fb; border-radius: 10px; padding: 8px 12px; }
.emoji { font-size: 18px; }
.name { font-size: 14px; }
.order-foot { display: flex; align-items: center; justify-content: space-between; border-top: 1px dashed #eef0f5; padding-top: 12px; }
.ops { display: flex; gap: 8px; align-items: center; }
</style>