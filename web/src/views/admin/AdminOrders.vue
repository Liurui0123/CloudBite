<template>
  <div>
    <div class="bar">
      <el-radio-group v-model="filter">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="PENDING_PAYMENT">待支付</el-radio-button>
        <el-radio-button value="PAID">已支付</el-radio-button>
        <el-radio-button value="COOKING">制作中</el-radio-button>
        <el-radio-button value="COMPLETED">已完成</el-radio-button>
      </el-radio-group>
      <div class="grow"></div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="filtered" v-loading="loading" class="cb-card" stripe>
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column label="菜品" min-width="220">
        <template #default="{ row }">
          <span v-for="it in row.items" :key="it.id" class="chip">{{ it.emoji }} {{ it.dishName }} ×{{ it.quantity }}</span>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="110">
        <template #default="{ row }"><span class="cb-price">{{ Number(row.totalAmount).toFixed(2) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="160">
        <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PAID'" type="primary" size="small" @click="advance(row, 'COOKING')">开始制作</el-button>
          <el-button v-else-if="row.status === 'COOKING'" type="success" size="small" @click="advance(row, 'COMPLETED')">出餐完成</el-button>
          <span v-else class="cb-muted">-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { statusText, statusType } from '../../utils/status'

const orders = ref([])
const loading = ref(false)
const filter = ref('ALL')

const filtered = computed(() =>
  filter.value === 'ALL' ? orders.value : orders.value.filter((o) => o.status === filter.value)
)

async function load() {
  loading.value = true
  try {
    orders.value = await http.get('/order/admin/list')
  } finally {
    loading.value = false
  }
}

async function advance(row, target) {
  await http.post(`/order/admin/${row.orderNo}/status/${target}`)
  ElMessage.success('状态已更新')
  load()
}

onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.grow { flex: 1; }
.chip { display: inline-block; background: #f5f6fa; border-radius: 8px; padding: 2px 8px; margin: 2px 4px 2px 0; font-size: 12px; }
</style>