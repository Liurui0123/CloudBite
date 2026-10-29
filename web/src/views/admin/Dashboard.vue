<template>
  <div>
    <!-- 顶部：实时状态 -->
    <div class="topbar">
      <div class="live" :class="{ on: connected }">
        <span class="dot"></span>
        {{ connected ? '实时监测中' : '连接中断，重连中…' }}
        <span class="cb-muted" v-if="updatedAt">· 更新于 {{ updatedAt }}</span>
      </div>
      <div class="cb-muted">每 3 秒自动推送一次（SSE）</div>
    </div>

    <!-- 指标卡 -->
    <div class="cards">
      <div class="stat cb-card">
        <div class="label">今日订单</div>
        <div class="value">{{ stats.todayOrders ?? '-' }}<span class="unit">单</span></div>
        <div class="hint">实时</div>
      </div>
      <div class="stat cb-card accent">
        <div class="label">今日营业额</div>
        <div class="value">￥{{ Number(stats.todayRevenue ?? 0).toFixed(2) }}</div>
        <div class="hint">已支付 / 已完成</div>
      </div>
      <div class="stat cb-card">
        <div class="label">累计订单</div>
        <div class="value">{{ stats.totalOrders ?? '-' }}<span class="unit">单</span></div>
        <div class="hint">全部订单</div>
      </div>
      <div class="stat cb-card">
        <div class="label">注册用户</div>
        <div class="value">{{ stats.totalUsers ?? '-' }}<span class="unit">人</span></div>
        <div class="hint">学生账号</div>
      </div>
      <div class="stat cb-card">
        <div class="label">待处理</div>
        <div class="value">{{ pendingCount }}<span class="unit">单</span></div>
        <div class="hint">待支付 + 制作中</div>
      </div>
    </div>

    <div class="grid-2">
      <div class="cb-card panel">
        <div class="panel-title">近 7 天订单与营业额</div>
        <div ref="trendRef" style="height: 300px"></div>
      </div>
      <div class="cb-card panel">
        <div class="panel-title">订单状态分布</div>
        <div ref="pieRef" style="height: 300px"></div>
      </div>
    </div>

    <div class="grid-2 second">
      <div class="cb-card panel">
        <div class="panel-title">热销菜品 Top 5</div>
        <div ref="barRef" style="height: 280px"></div>
      </div>
      <div class="cb-card panel">
        <div class="panel-title">
          最新订单
          <span class="live-mini" :class="{ on: connected }">● 实时</span>
        </div>
        <div class="latest">
          <div v-for="o in stats.latestOrders || []" :key="o.orderNo" class="latest-item">
            <div class="no">{{ o.orderNo }}</div>
            <div class="meta">
              <span class="cb-price">{{ Number(o.amount).toFixed(2) }}</span>
              <el-tag size="small" :type="statusType(o.status)">{{ statusText(o.status) }}</el-tag>
              <span class="cb-muted">{{ o.createdAt?.slice(5, 16) }}</span>
            </div>
          </div>
          <el-empty v-if="!stats.latestOrders || stats.latestOrders.length === 0" description="暂无订单" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElNotification } from 'element-plus'
import { statusText, statusType } from '../../utils/status'

const stats = ref({})
const connected = ref(false)
const updatedAt = ref('')
const trendRef = ref()
const pieRef = ref()
const barRef = ref()

let trendChart = null
let pieChart = null
let barChart = null
let source = null
let lastOrderNo = null

const pendingCount = computed(() => {
  const c = stats.value.statusCount || {}
  return (c.PENDING_PAYMENT || 0) + (c.COOKING || 0)
})

function initCharts() {
  trendChart = echarts.init(trendRef.value)
  pieChart = echarts.init(pieRef.value)
  barChart = echarts.init(barRef.value)
}

function render() {
  const trend = stats.value.weeklyTrend || []
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '营业额'], bottom: 0 },
    grid: { left: 50, right: 60, top: 30, bottom: 50 },
    xAxis: { type: 'category', data: trend.map((t) => t.date), axisTick: { show: false } },
    yAxis: [
      { type: 'value', name: '订单', splitLine: { lineStyle: { color: '#f0f1f5' } } },
      { type: 'value', name: '营业额', splitLine: { show: false } }
    ],
    series: [
      {
        name: '订单数', type: 'bar', data: trend.map((t) => t.orders),
        barWidth: 18, itemStyle: { color: '#ffb08a', borderRadius: [6, 6, 0, 0] }
      },
      {
        name: '营业额', type: 'line', yAxisIndex: 1, smooth: true,
        data: trend.map((t) => Number(t.revenue)),
        symbolSize: 7, lineStyle: { width: 3, color: '#ff6b35' },
        itemStyle: { color: '#ff6b35' },
        areaStyle: { color: 'rgba(255,107,53,0.10)' }
      }
    ]
  })

  const statusCount = stats.value.statusCount || {}
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
    series: [{
      type: 'pie', radius: ['46%', '70%'], center: ['50%', '45%'],
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{c}' },
      data: Object.entries(statusCount).map(([k, v]) => ({ name: statusText(k), value: v }))
    }]
  })

  const top = stats.value.topDishes || []
  barChart.setOption({
    grid: { left: 100, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f1f5' } } },
    yAxis: { type: 'category', data: top.map((d) => d.name).reverse(), axisTick: { show: false } },
    series: [{
      type: 'bar', data: top.map((d) => d.count).reverse(),
      itemStyle: { color: '#ff6b35', borderRadius: [0, 6, 6, 0] },
      barWidth: 16, label: { show: true, position: 'right' }
    }]
  })
}

function applySnapshot(data) {
  stats.value = data
  updatedAt.value = (data.updatedAt || '').slice(11, 19)
  render()

  const latest = (data.latestOrders || [])[0]
  if (latest) {
    if (lastOrderNo && latest.orderNo !== lastOrderNo && latest.status === 'PENDING_PAYMENT') {
      ElNotification({
        title: '新订单',
        message: `${latest.orderNo}　￥${Number(latest.amount).toFixed(2)}`,
        type: 'success',
        duration: 4000,
        position: 'bottom-right'
      })
    }
    lastOrderNo = latest.orderNo
  }
}

function connect() {
  if (source) source.close()
  const token = localStorage.getItem('cb_token') || ''
  source = new EventSource(`/api/monitor/stream?token=${encodeURIComponent(token)}`)
  source.addEventListener('stats', (e) => {
    connected.value = true
    try {
      applySnapshot(JSON.parse(e.data))
    } catch (err) {
      /* 忽略单次解析失败 */
    }
  })
  source.onopen = () => { connected.value = true }
  source.onerror = () => {
    connected.value = false
    source.close()
    setTimeout(connect, 3000)
  }
}

function onResize() {
  trendChart?.resize()
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  initCharts()
  connect()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  source?.close()
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.live {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 13px; color: #8a90a2;
  background: #fff; border-radius: 999px; padding: 6px 14px;
  border: 1px solid #eef0f5;
}
.live.on { color: #18a058; border-color: #c8f0d8; background: #f4fdf8; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #c9cedb; }
.live.on .dot { background: #18a058; animation: pulse 1.4s infinite; }
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(24, 160, 88, 0.5); }
  70% { box-shadow: 0 0 0 8px rgba(24, 160, 88, 0); }
  100% { box-shadow: 0 0 0 0 rgba(24, 160, 88, 0); }
}
.cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.stat { padding: 16px 18px; position: relative; overflow: hidden; }
.stat.accent { background: linear-gradient(135deg, #fff, #fff4ee); border: 1px solid #ffe1d3; }
.stat .label { color: var(--cb-muted); font-size: 13px; }
.stat .value { font-size: 26px; font-weight: 800; margin: 6px 0 2px; color: #1f2430; }
.stat .unit { font-size: 13px; font-weight: 500; color: #9aa3b8; margin-left: 3px; }
.stat .hint { font-size: 12px; color: #a3a9bb; }
.grid-2 { display: grid; grid-template-columns: 2fr 1fr; gap: 14px; margin-top: 14px; }
.grid-2.second { grid-template-columns: 1fr 1fr; }
.panel { padding: 16px 18px; }
.panel-title { font-weight: 700; font-size: 15px; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.live-mini { font-size: 12px; color: #c9cedb; font-weight: 500; }
.live-mini.on { color: #18a058; }
.latest { display: flex; flex-direction: column; gap: 10px; max-height: 280px; overflow: auto; }
.latest-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: 10px; background: #fafbfd; border: 1px solid #f0f1f5;
}
.latest-item .no { font-family: ui-monospace, monospace; font-size: 13px; color: #4a5060; }
.latest-item .meta { display: flex; align-items: center; gap: 10px; }
</style>
