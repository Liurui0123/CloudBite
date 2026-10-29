<template>
  <div>
    <div class="bar">
      <el-input v-model="keyword" placeholder="搜索菜品" style="width: 220px" clearable />
      <div class="grow"></div>
      <el-button type="primary" @click="openCreate">新增菜品</el-button>
    </div>

    <el-table :data="filtered" v-loading="loading" class="cb-card" stripe>
      <el-table-column label="图片" width="90">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="cover" class="thumb" :preview-src-list="[row.imageUrl]" />
          <span v-else class="emoji">{{ row.emoji }}</span>
        </template>
      </el-table-column>
      <el-table-column label="菜品" min-width="180">
        <template #default="{ row }">
          <span class="name">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="110">
        <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="价格" width="100">
        <template #default="{ row }"><span class="cb-price">{{ Number(row.price).toFixed(2) }}</span></template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column prop="sales" label="销量" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'">
            {{ row.status === 'ON_SALE' ? '在售' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button text @click="toggle(row)">{{ row.status === 'ON_SALE' ? '下架' : '上架' }}</el-button>
          <el-button text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑菜品' : '新增菜品'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.emoji" placeholder="一个 emoji，如 🍗" /></el-form-item>
        <el-form-item label="图片">
          <el-upload
            class="uploader"
            :show-file-list="false"
            :http-request="uploadImage"
            accept="image/png,image/jpeg,image/gif,image/webp"
          >
            <img v-if="form.imageUrl" :src="form.imageUrl" class="upload-img" />
            <div v-else class="upload-placeholder">
              <div style="font-size: 22px">＋</div>
              <div>上传图片（≤5MB）</div>
            </div>
          </el-upload>
          <el-button v-if="form.imageUrl" text type="danger" @click="form.imageUrl = ''">移除图片</el-button>
          <span class="cb-muted" style="margin-left: 8px">不传则展示 emoji</span>
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tag" placeholder="招牌 / 新品 / 推荐" /></el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
          <el-button text type="primary" :loading="aiLoading" @click="aiDescribe">✨ AI 生成文案</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../../api/http'

const dishes = ref([])
const categories = ref([])
const keyword = ref('')
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const aiLoading = ref(false)
const form = ref({})

const filtered = computed(() =>
  dishes.value.filter((d) => !keyword.value || d.name.includes(keyword.value))
)

function categoryName(id) {
  return categories.value.find((c) => c.id === id)?.name || '-'
}

async function load() {
  loading.value = true
  try {
    dishes.value = await http.get('/menu/dishes')
    categories.value = await http.get('/menu/categories')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', price: 0, stock: 100, emoji: '🍽️', status: 'ON_SALE', categoryId: categories.value[0]?.id }
  dialog.value = true
}

function openEdit(row) {
  form.value = { ...row, price: Number(row.price) }
  dialog.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) await http.put(`/menu/dishes/${form.value.id}`, form.value)
    else await http.post('/menu/dishes', form.value)
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  await http.patch(`/menu/dishes/${row.id}/toggle`)
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
  await http.delete(`/menu/dishes/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function uploadImage({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await http.post('/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.value.imageUrl = res.url
    ElMessage.success('图片已上传')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

async function aiDescribe() {
  if (!form.value.name) {
    ElMessage.warning('先填写菜名')
    return
  }
  aiLoading.value = true
  try {
    const data = await http.post('/ai/description', {
      dishName: form.value.name,
      ingredients: form.value.description || ''
    })
    form.value.description = data.answer
  } finally {
    aiLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.grow { flex: 1; }
.emoji { font-size: 20px; margin-right: 6px; }
.thumb { width: 56px; height: 56px; border-radius: 8px; }
.uploader :deep(.el-upload) { border: 1px dashed #dcdfe6; border-radius: 10px; cursor: pointer; }
.upload-img { width: 120px; height: 90px; object-fit: cover; border-radius: 10px; display: block; }
.upload-placeholder {
  width: 120px; height: 90px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #9aa3b8; font-size: 12px; gap: 4px;
}
.name { font-weight: 600; }
</style>