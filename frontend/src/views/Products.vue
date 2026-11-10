<script setup>
import { ref, onMounted } from 'vue'
import { listProducts, saveProduct, deleteProduct, createOrder } from '../api'

const products = ref([])
const keyword = ref('')
const form = ref({ name: '', price: null, stock: null })
const showForm = ref(false)
const msg = ref('')

async function load() {
  const res = await listProducts(keyword.value)
  products.value = res.data
}

async function save() {
  try {
    await saveProduct(form.value)
    showForm.value = false
    form.value = { name: '', price: null, stock: null }
    load()
  } catch (e) { msg.value = (e && e.msg) || '保存失败' }
}

async function del(p) {
  if (!confirm(`确认下架删除「${p.name}」？`)) return
  await deleteProduct(p.id)
  load()
}

async function buy(p) {
  try {
    await createOrder(p.id, 1)
    msg.value = `已下单：${p.name}`
    load()
  } catch (e) { msg.value = (e && e.msg) || '下单失败' }
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex; gap:12px; margin-bottom:16px; align-items:center;">
      <input v-model="keyword" placeholder="搜索商品" @keyup.enter="load" />
      <button class="btn cyan" @click="load">搜索</button>
      <button class="btn" @click="showForm = !showForm">{{ showForm ? '取消' : '+ 新增商品' }}</button>
    </div>
    <p v-if="msg" class="msg">{{ msg }}</p>
    <div v-if="showForm" class="panel" style="margin-bottom:16px; display:flex; gap:12px;">
      <input v-model="form.name" placeholder="商品名" />
      <input v-model.number="form.price" type="number" placeholder="价格" />
      <input v-model.number="form.stock" type="number" placeholder="库存" />
      <button class="btn" @click="save">保存</button>
    </div>
    <div class="panel">
      <table>
        <thead>
          <tr><th>商品</th><th>价格</th><th>库存</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in products" :key="p.id">
            <td>{{ p.name }}</td>
            <td class="price">¥{{ p.price }}</td>
            <td>{{ p.stock }}</td>
            <td>
              <button class="btn cyan" @click="buy(p)">下单</button>
              <button class="btn" style="margin-left:8px;" @click="del(p)">删除</button>
            </td>
          </tr>
          <tr v-if="!products.length"><td colspan="4">暂无商品</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.price { color: var(--neon-yellow); text-shadow: 0 0 6px rgba(249,240,2,.4); }
.msg { color: var(--neon-cyan); margin-bottom: 12px; font-size: 14px; }
</style>
