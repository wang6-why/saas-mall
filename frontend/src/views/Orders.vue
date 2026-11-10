<script setup>
import { ref, onMounted } from 'vue'
import { listOrders, payOrder } from '../api'

const orders = ref([])
const msg = ref('')
const STATUS = { 1: ['待支付', 'wait'], 2: ['已支付', 'ok'], 3: ['已取消', 'no'] }

async function load() {
  const res = await listOrders()
  orders.value = res.data
}

async function pay(o) {
  await payOrder(o.id)
  msg.value = `订单 ${o.orderNo} 已支付`
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <p v-if="msg" class="msg">{{ msg }}</p>
    <div class="panel">
      <table>
        <thead><tr><th>订单号</th><th>金额</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="o in orders" :key="o.id">
            <td>{{ o.orderNo }}</td>
            <td class="price">¥{{ o.amount }}</td>
            <td><span class="tag" :class="(STATUS[o.status] || ['', ''])[1]">{{ (STATUS[o.status] || ['未知'])[0] }}</span></td>
            <td>
              <button v-if="o.status === 1" class="btn cyan" @click="pay(o)">支付</button>
            </td>
          </tr>
          <tr v-if="!orders.length"><td colspan="4">暂无订单</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.price { color: var(--neon-yellow); }
.msg { color: var(--neon-cyan); margin-bottom: 12px; font-size: 14px; }
</style>
