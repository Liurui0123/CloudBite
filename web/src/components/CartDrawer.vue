<template>
  <el-drawer :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
             title="购物车" size="380px">
    <div v-if="cart.items.length === 0" class="empty">
      <div style="font-size: 40px">🛒</div>
      <p class="cb-muted">购物车还是空的，去点几个菜吧</p>
    </div>
    <template v-else>
      <div class="items">
        <div v-for="item in cart.items" :key="item.dishId" class="item">
          <div class="emoji">{{ item.emoji || '🍽️' }}</div>
          <div class="info">
            <div class="name">{{ item.name }}</div>
            <div class="cb-price">{{ item.price.toFixed(2) }}</div>
          </div>
          <div class="qty">
            <el-button circle size="small" @click="cart.dec(item)">-</el-button>
            <span class="num">{{ item.quantity }}</span>
            <el-button circle size="small" type="primary" @click="cart.inc(item)">+</el-button>
          </div>
        </div>
      </div>

      <el-form label-position="top" class="form">
        <el-form-item label="送餐地址">
          <el-input v-model="address" placeholder="例如：3 号宿舍楼 502" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="remark" placeholder="少辣 / 不要香菜 …" />
        </el-form-item>
      </el-form>

      <div class="footer">
        <div class="total">合计 <span class="cb-price">{{ cart.total.toFixed(2) }}</span></div>
        <el-button type="primary" size="large" :loading="submitting" @click="submit">
          提交订单（{{ cart.count }} 件）
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { useCartStore } from '../store/cart'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'checkout'])

const cart = useCartStore()
const router = useRouter()
const address = ref('3 号宿舍楼 502')
const remark = ref('')
const submitting = ref(false)

async function submit() {
  if (!address.value.trim()) {
    ElMessage.warning('请填写送餐地址')
    return
  }
  submitting.value = true
  try {
    const order = await http.post('/order', {
      items: cart.items.map((i) => ({ dishId: i.dishId, quantity: i.quantity })),
      address: address.value,
      remark: remark.value
    })
    cart.clear()
    emit('update:modelValue', false)
    emit('checkout')
    try {
      await ElMessageBox.confirm(`订单 ${order.orderNo} 已创建，共 ￥${order.totalAmount}，是否立即支付？`, '下单成功', {
        confirmButtonText: '立即支付',
        cancelButtonText: '稍后支付',
        type: 'success'
      })
      await http.post(`/order/${order.orderNo}/pay`)
      ElMessage.success('支付成功，厨房已收到订单')
    } catch (e) {
      /* 用户选择稍后支付 */
    }
    router.push(`/orders/${order.orderNo}`)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.empty { text-align: center; padding: 60px 0; }
.items { max-height: 40vh; overflow: auto; }
.item { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px dashed #eef0f5; }
.emoji { font-size: 26px; width: 40px; text-align: center; }
.info { flex: 1; }
.name { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.qty { display: flex; align-items: center; gap: 8px; }
.num { min-width: 18px; text-align: center; }
.form { margin-top: 16px; }
.footer { margin-top: 16px; display: flex; align-items: center; justify-content: space-between; }
.total { font-size: 15px; }
</style>