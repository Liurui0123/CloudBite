<template>
  <div>
    <div class="bar">
      <span class="cb-muted">分类决定点餐页的筛选顺序，删除前会检查该分类下是否还有菜品</span>
      <div class="grow"></div>
      <el-button type="primary" @click="openCreate">新增分类</el-button>
    </div>

    <el-table :data="categories" v-loading="loading" class="cb-card" stripe>
      <el-table-column label="图标" width="90">
        <template #default="{ row }"><span class="icon">{{ row.icon || '🍽️' }}</span></template>
      </el-table-column>
      <el-table-column prop="name" label="分类名称" min-width="160" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="菜品数" width="120">
        <template #default="{ row }">{{ dishCount(row.id) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如：甜品" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="一个 emoji，例如 🍰" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="cb-muted" style="margin-left: 8px">数字越小越靠前</span>
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

const categories = ref([])
const dishes = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    categories.value = await http.get('/menu/categories')
    dishes.value = await http.get('/menu/dishes')
  } finally {
    loading.value = false
  }
}

const dishCount = (id) => dishes.value.filter((d) => d.categoryId === id).length

function openCreate() {
  form.value = { name: '', icon: '🍽️', sort: (categories.value.length + 1) }
  dialog.value = true
}

function openEdit(row) {
  form.value = { ...row }
  dialog.value = true
}

async function save() {
  if (!form.value.name) {
    ElMessage.warning('请填写分类名称')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await http.put(`/menu/categories/${form.value.id}`, form.value)
    } else {
      await http.post('/menu/categories', form.value)
    }
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
  await http.delete(`/menu/categories/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.grow { flex: 1; }
.icon { font-size: 20px; }
</style>
