<template>
  <div class="fab" @click="visible = true">
    <span class="fab-icon">🤖</span>
    <span class="fab-text">AI 客服</span>
  </div>

  <el-drawer v-model="visible" title="AI 智能客服" size="400px">
    <div class="chat">
      <div v-for="(m, idx) in messages" :key="idx" class="msg" :class="m.role">
        <div class="bubble">{{ m.content }}</div>
      </div>
      <div v-if="loading" class="msg assistant">
        <div class="bubble">思考中…</div>
      </div>
    </div>
    <div class="quick">
      <el-tag v-for="q in quickAsk" :key="q" @click="ask(q)" class="tag">{{ q }}</el-tag>
    </div>
    <div class="input">
      <el-input v-model="text" placeholder="问问订单进度、口味推荐…" @keyup.enter="send" />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
    <p class="cb-muted" style="margin-top: 10px">
      由 DeepSeek 大模型驱动，回答仅供参考
    </p>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import http from '../api/http'

const visible = ref(false)
const loading = ref(false)
const text = ref('')
const messages = ref([
  { role: 'assistant', content: '您好，我是 CloudBite 智能客服，可以帮您查订单进度、推荐菜品～' }
])
const quickAsk = ['我的订单做到哪了？', '有什么不辣的推荐？', '怎么取消订单？']

async function ask(q) {
  text.value = q
  await send()
}

async function send() {
  const content = text.value.trim()
  if (!content || loading.value) return
  messages.value.push({ role: 'user', content })
  text.value = ''
  loading.value = true
  try {
    const orderNo = content.match(/CB\d{16,20}/)?.[0]
    const data = await http.post('/ai/chat', { message: content, orderNo })
    messages.value.push({ role: 'assistant', content: data.answer })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，AI 服务暂时不可用。' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.fab {
  position: fixed;
  right: 26px;
  bottom: 28px;
  background: linear-gradient(135deg, #ff8a5c, #ff5f2e);
  color: #fff;
  border-radius: 999px;
  padding: 12px 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(255, 95, 46, 0.35);
  z-index: 30;
}
.fab-icon { font-size: 20px; }
.fab-text { font-weight: 600; }
.chat { height: 56vh; overflow: auto; padding-bottom: 8px; }
.msg { display: flex; margin-bottom: 10px; }
.msg.user { justify-content: flex-end; }
.bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f3f4f8;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.msg.user .bubble { background: var(--cb-primary); color: #fff; }
.quick { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0; }
.tag { cursor: pointer; }
.input { display: flex; gap: 8px; }
</style>