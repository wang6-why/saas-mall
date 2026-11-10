<script setup>
import { ref } from 'vue'
import Login from './views/Login.vue'
import Products from './views/Products.vue'
import Orders from './views/Orders.vue'

const token = localStorage.getItem('token')
const tenantName = localStorage.getItem('tenantName') || ''
const tab = ref('products')

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('tenantName')
  location.reload()
}
</script>

<template>
  <Login v-if="!token" />
  <div v-else class="layout">
    <aside class="sidebar">
      <h2 class="neon">SaaS MALL</h2>
      <p class="tenant">🛰️ {{ tenantName }}</p>
      <nav>
        <span :class="{active: tab==='products'}" @click="tab='products'">🛒 商品管理</span>
        <span :class="{active: tab==='orders'}" @click="tab='orders'">📦 订单</span>
      </nav>
      <button class="btn" @click="logout">退出登录</button>
    </aside>
    <main class="main">
      <Products v-if="tab==='products'" />
      <Orders v-else />
    </main>
  </div>
</template>

<style scoped>
.layout { display:flex; min-height:100vh; }
.sidebar {
  width: 220px;
  background: var(--panel);
  border-right: 1px solid rgba(0,240,255,.3);
  padding: 24px 16px;
  display:flex; flex-direction:column; gap:16px;
}
.tenant { color: var(--neon-pink); font-size: 13px; text-shadow: 0 0 8px var(--neon-pink); }
nav { display:flex; flex-direction:column; gap:8px; }
nav span { cursor:pointer; padding:10px 12px; border-radius:6px; color:#9d9dcc; }
nav span:hover { background: rgba(0,240,255,.08); }
nav span.active { color: var(--neon-cyan); background: rgba(0,240,255,.12); text-shadow:0 0 6px var(--neon-cyan); }
.main { flex:1; padding: 24px; max-width: 900px; }
</style>
