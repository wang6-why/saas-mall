<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { listSeckill, seckillBuy, seckillStatus, seckillStock } from '../api'

const activities = ref([])
const msg = ref('')
const STATUS_TXT = {
  0: { t: '⏳ 排队中...', c: 'wait' },
  1: { t: '✅ 抢购成功！', c: 'ok' },
  [-1]: { t: '⛔ 你已经抢过了', c: 'no' },
  [-2]: { t: '💥 已售罄', c: 'no' },
  [-3]: { t: '❌ 抢购失败，请重试', c: 'no' }
}

let pollTimer = null
let clockTimer = null

async function load() {
  const res = await listSeckill()
  activities.value = await Promise.all(res.data.map(async a => {
    const s = await seckillStock(a.id).catch(() => ({ data: -1 }))
    return { ...a, remain: s.data }
  }))
}

async function grab(a) {
  msg.value = ''
  try {
    const res = await seckillBuy(a.id)
    if (res.data === 0) {
      a.polling = true
      pollStatus(a)
    } else {
      msg.value = (STATUS_TXT[res.data] || {}).t || '状态异常'
      load()
    }
  } catch (e) { msg.value = '请求失败' }
}

function pollStatus(a) {
  if (pollTimer) clearInterval(pollTimer)
  let count = 0
  pollTimer = setInterval(async () => {
    count++
    try {
      const res = await seckillStatus(a.id)
      const st = STATUS_TXT[res.data]
      if (res.data !== 0 || count > 15) {
        clearInterval(pollTimer)
        pollTimer = null
        a.polling = false
        msg.value = st ? st.t : '下单超时'
        load()
      } else {
        msg.value = st.t
      }
    } catch (e) { /* 轮询失败忽略 */ }
  }, 1200)
}

function fmtTime(iso) {
  return iso ? iso.replace('T', ' ') : ''
}

function countdown(a) {
  const end = new Date(a.endTime).getTime()
  const diff = end - Date.now()
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff / 3600000), m = Math.floor(diff % 3600000 / 60000), s = Math.floor(diff % 60000 / 1000)
  return `🔥 ${h}h ${m}m ${s}s`
}

onMounted(() => {
  load()
  clockTimer = setInterval(() => {
    activities.value.forEach(a => a.cd = countdown(a))
  }, 1000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<template>
  <div>
    <p v-if="msg" class="msg">{{ msg }}</p>
    <div class="cards">
      <div v-for="a in activities" :key="a.id" class="panel card">
        <div class="ribbon">⚡ 限时秒杀</div>
        <h3 class="neon">活动 #{{ a.id }}</h3>
        <div class="row"><span class="label">秒杀价</span>
          <span class="price">¥{{ a.seckillPrice }}</span></div>
        <div class="row"><span class="label">剩余配额</span>
          <span :class="a.remain > 0 ? 'cyan-txt' : 'red-txt'">{{ a.remain < 0 ? '加载中' : a.remain }}</span></div>
        <div class="row"><span class="label">倒计时</span><span class="cd">{{ a.cd || '...' }}</span></div>
        <button class="btn grab" :disabled="a.polling" @click="grab(a)">
          {{ a.polling ? '⏳ 排队中' : '立即抢购' }}
        </button>
      </div>
      <p v-if="!activities.length" class="empty">暂无秒杀活动</p>
    </div>
  </div>
</template>

<style scoped>
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.card { position: relative; display: flex; flex-direction: column; gap: 10px; }
.ribbon {
  position: absolute; top: -1px; right: -1px;
  background: var(--neon-pink); color: #000;
  padding: 3px 10px; font-size: 12px; border-radius: 0 8px 0 8px;
  box-shadow: 0 0 10px var(--neon-pink);
}
.row { display: flex; justify-content: space-between; font-size: 14px; }
.label { color: #8888aa; }
.price { color: var(--neon-yellow); font-size: 20px; text-shadow: 0 0 8px rgba(249,240,2,.5); }
.cyan-txt { color: var(--neon-cyan); text-shadow: 0 0 6px var(--neon-cyan); }
.red-txt { color: #ff4444; }
.cd { color: var(--neon-pink); text-shadow: 0 0 6px var(--neon-pink); font-variant-numeric: tabular-nums; }
.grab { font-size: 15px; padding: 10px; }
.grab:disabled { opacity: .5; cursor: not-allowed; }
.msg { color: var(--neon-cyan); margin-bottom: 14px; font-size: 15px; }
.empty { color: #8888aa; }
</style>
