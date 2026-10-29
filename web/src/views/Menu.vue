<template>
  <div class="cb-page">
    <section class="hero cb-card">
      <div>
        <h1>今天想吃点什么？</h1>
        <p class="cb-muted">下单后厨房立即接单，平均 15 分钟出餐</p>
        <el-input v-model="keyword" placeholder="搜索菜品，例如：宫保鸡丁" size="large" clearable
                  class="search" @keyup.enter="loadDishes">
          <template #prefix><el-icon><Search /></el-icon></template>
          <template #append><el-button @click="loadDishes">搜索</el-button></template>
        </el-input>
      </div>
      <div class="hero-emoji">🍱</div>
    </section>

    <section class="ai-card cb-card">
      <div class="ai-head">
        <div>
          <div class="ai-title">🤖 AI 智能推荐</div>
          <div class="cb-muted">告诉 AI 你的口味偏好，帮你选菜</div>
        </div>
        <div class="ai-actions">
          <el-input v-model="preference" placeholder="比如：想吃辣、预算 40 以内" style="width: 240px" />
          <el-button type="primary" :loading="aiLoading" @click="recommend">帮我选</el-button>
        </div>
      </div>
      <div v-if="aiAnswer" class="ai-answer">
        <pre>{{ aiAnswer }}</pre>
        <div class="ai-dishes">
          <div v-for="d in aiDishes" :key="d.id" class="ai-dish" @click="cart.add(d)">
            <span class="emoji">{{ d.emoji }}</span>
            <span>{{ d.name }}</span>
            <span class="cb-price">{{ Number(d.price).toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="hotList.length" class="hot-strip cb-card">
      <div class="hot-title">🔥 实时热销 <span class="cb-muted">（Redis ZSET 排行）</span></div>
      <div class="hot-list">
        <div v-for="(h, i) in hotList" :key="h.dishId" class="hot-item">
          <span class="rank" :class="'r' + (i + 1)">{{ i + 1 }}</span>
          <span class="emoji">{{ h.emoji }}</span>
          <span class="name">{{ h.name }}</span>
          <span class="sold">已售 {{ h.sold }}</span>
          <span class="cb-price">{{ Number(h.price).toFixed(2) }}</span>
        </div>
      </div>
    </section>

    <section class="categories">
      <el-radio-group v-model="activeCategory" @change="loadDishes">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button v-for="c in categories" :key="c.id" :value="c.id">
          {{ c.icon }} {{ c.name }}
        </el-radio-button>
      </el-radio-group>
    </section>

    <section v-loading="loading" class="grid">
      <div v-for="dish in dishes" :key="dish.id" class="dish cb-card">
        <div class="dish-cover">
          <img v-if="dish.imageUrl" :src="dish.imageUrl" class="dish-img" :alt="dish.name" />
          <span v-else class="dish-emoji">{{ dish.emoji || '🍽️' }}</span>
          <el-tag v-if="dish.tag" class="tag" type="danger" effect="dark" size="small">{{ dish.tag }}</el-tag>
        </div>
        <div class="dish-body">
          <div class="dish-name">{{ dish.name }}</div>
          <div class="dish-desc">{{ dish.description }}</div>
          <div class="dish-foot">
            <div>
              <span class="cb-price">{{ Number(dish.price).toFixed(2) }}</span>
              <span class="cb-muted">　已售 {{ dish.sales }}</span>
            </div>
            <el-button type="primary" circle @click="cart.add(dish)">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && dishes.length === 0" description="没有找到菜品" style="grid-column: 1 / -1" />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import http from '../api/http'
import { useCartStore } from '../store/cart'

const cart = useCartStore()
const categories = ref([])
const dishes = ref([])
const activeCategory = ref(null)
const keyword = ref('')
const loading = ref(false)
const preference = ref('')
const aiAnswer = ref('')
const aiDishes = ref([])
const aiLoading = ref(false)
const hotList = ref([])

async function loadHot() {
  try {
    hotList.value = await http.get('/menu/public/hot', { params: { top: 5 } })
  } catch (e) {
    hotList.value = []
  }
}

async function loadCategories() {
  categories.value = await http.get('/menu/public/categories')
}

async function loadDishes() {
  loading.value = true
  try {
    const params = {}
    if (activeCategory.value) params.categoryId = activeCategory.value
    if (keyword.value) params.keyword = keyword.value
    dishes.value = await http.get('/menu/public/dishes', { params })
  } finally {
    loading.value = false
  }
}

async function recommend() {
  aiLoading.value = true
  try {
    const data = await http.post('/ai/recommend', { preference: preference.value })
    aiAnswer.value = data.answer
    aiDishes.value = data.dishes || []
    if (!data.aiEnabled) ElMessage.info('未配置大模型 Key，当前为规则推荐')
  } finally {
    aiLoading.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  await loadDishes()
  await loadHot()
})
</script>

<style scoped>
.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28px 30px;
  margin-bottom: 18px;
  background: linear-gradient(120deg, #fff 55%, #fff3ec 100%);
}
.hero h1 { margin: 0 0 8px; font-size: 26px; }
.search { margin-top: 16px; max-width: 460px; }
.hero-emoji { font-size: 86px; opacity: .9; }
.ai-card { padding: 18px 20px; margin-bottom: 18px; border: 1px solid #ffe1d3; background: #fffaf7; }
.ai-head { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
.ai-title { font-weight: 700; margin-bottom: 4px; }
.ai-actions { display: flex; gap: 10px; }
.ai-answer { margin-top: 14px; background: #fff; border-radius: 12px; padding: 14px; border: 1px dashed #ffd4c0; }
.ai-answer pre { margin: 0; white-space: pre-wrap; font-family: inherit; line-height: 1.8; font-size: 14px; }
.ai-dishes { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
.ai-dish {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 999px; background: #fff3ec; cursor: pointer; font-size: 13px;
}
.ai-dish:hover { background: #ffe6d9; }
.hot-strip { padding: 14px 18px; margin-bottom: 16px; }
.hot-title { font-weight: 700; margin-bottom: 10px; font-size: 14px; }
.hot-list { display: flex; flex-wrap: wrap; gap: 10px; }
.hot-item {
  display: flex; align-items: center; gap: 8px;
  background: #fff7f2; border: 1px solid #ffe3d5; border-radius: 999px;
  padding: 6px 14px; font-size: 13px;
}
.hot-item .rank {
  width: 18px; height: 18px; border-radius: 50%; background: #ffd8c5; color: #b34a20;
  font-size: 11px; display: flex; align-items: center; justify-content: center; font-weight: 700;
}
.hot-item .rank.r1 { background: #ff6b35; color: #fff; }
.hot-item .rank.r2 { background: #ffa26b; color: #fff; }
.hot-item .rank.r3 { background: #ffc59b; color: #7a3a16; }
.hot-item .sold { color: var(--cb-muted); }
.categories { margin: 6px 0 18px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 18px; min-height: 200px; }
.dish { overflow: hidden; transition: transform .15s ease, box-shadow .15s ease; }
.dish:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgba(22, 28, 45, 0.1); }
.dish-cover {
  height: 118px; position: relative;
  background: linear-gradient(135deg, #fff1e8, #ffe0d0);
  display: flex; align-items: center; justify-content: center;
}
.dish-emoji { font-size: 54px; }
.dish-img { width: 100%; height: 118px; object-fit: cover; }
.tag { position: absolute; top: 10px; left: 10px; }
.dish-body { padding: 14px 16px 16px; }
.dish-name { font-weight: 700; font-size: 16px; margin-bottom: 6px; }
.dish-desc { color: var(--cb-muted); font-size: 12px; height: 34px; overflow: hidden; }
.dish-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
</style>