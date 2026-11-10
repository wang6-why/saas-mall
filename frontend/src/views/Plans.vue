<script setup>
import { ref, onMounted } from 'vue'
import { listPlans, planUsage, subscribePlan } from '../api'

const plans = ref([])
const usage = ref({})
const msg = ref('')
const busy = ref(false)

async function load() {
  const [p, u] = await Promise.all([listPlans(), planUsage()])
  plans.value = p.data
  usage.value = u.data
}

async function choose(p) {
  if (usage.value.planName === p.name) return
  if (!confirm(`确认订阅「${p.name}」1 个月？费用 ¥${p.price}`)) return
  busy.value = true
  try {
    await subscribePlan(p.id, 1)
    msg.value = `已开通 ${p.name}，有效期 1 个月`
    await load()
  } catch (e) { msg.value = (e && e.msg) || '订阅失败' }
  busy.value = false
}

function quotaPct() {
  const max = usage.value.maxProducts
  if (!max || max < 0) return 0
  return Math.min(100, Math.round((usage.value.usedProducts / max) * 100))
}

onMounted(load)
</script>

<template>
  <div>
    <p v-if="msg" class="msg">{{ msg }}</p>

    <div class="panel usage">
      <div class="u-head">
        <h3 class="neon">当前套餐：{{ usage.planName }}</h3>
        <span class="tag" :class="usage.valid ? 'ok' : 'no'">{{ usage.valid ? '有效' : '已过期' }}</span>
      </div>
      <div class="bar-wrap">
        <div class="bar-label">
          <span>商品配额</span>
          <span>{{ usage.usedProducts }} / {{ usage.maxProducts < 0 ? '不限' : usage.maxProducts }}</span>
        </div>
        <div class="bar"><div class="bar-inner" :style="{ width: quotaPct() + '%' }"></div></div>
      </div>
      <div class="meta">
        <span>秒杀功能：<b :class="usage.seckillEnabled ? 'cyan-txt' : 'red-txt'">{{ usage.seckillEnabled ? '已开放' : '未开放' }}</b></span>
        <span>到期时间：{{ usage.expireTime ? usage.expireTime.replace('T', ' ') : '长期有效' }}</span>
      </div>
    </div>

    <div class="plans">
      <div v-for="p in plans" :key="p.id" class="panel plan" :class="{ current: usage.planName === p.name }">
        <h4>{{ p.name }}</h4>
        <div class="plan-price">¥{{ p.price }}<span class="unit">/月</span></div>
        <ul>
          <li>商品上限：{{ p.maxProducts < 0 ? '不限' : p.maxProducts + ' 个' }}</li>
          <li>秒杀功能：{{ p.seckillEnabled ? '✅ 开放' : '❌ 不开放' }}</li>
          <li>{{ p.remark }}</li>
        </ul>
        <button class="btn" :disabled="busy || usage.planName === p.name" @click="choose(p)">
          {{ usage.planName === p.name ? '当前套餐' : '订阅 / 续费' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.usage { margin-bottom: 20px; display: flex; flex-direction: column; gap: 14px; }
.u-head { display: flex; align-items: center; gap: 12px; }
.bar-label { display: flex; justify-content: space-between; font-size: 13px; color: #9d9dcc; margin-bottom: 6px; }
.bar { height: 8px; background: rgba(0,0,0,.5); border-radius: 4px; overflow: hidden; }
.bar-inner { height: 100%; background: linear-gradient(90deg, var(--neon-cyan), var(--neon-pink)); box-shadow: 0 0 10px var(--neon-cyan); transition: width .4s; }
.meta { display: flex; gap: 24px; font-size: 13px; color: #9d9dcc; }
.cyan-txt { color: var(--neon-cyan); }
.red-txt { color: #ff4444; }
.plans { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.plan { display: flex; flex-direction: column; gap: 10px; }
.plan.current { border-color: var(--neon-pink); box-shadow: 0 0 18px rgba(255,46,196,.25); }
.plan h4 { color: var(--neon-yellow); }
.plan-price { font-size: 26px; color: var(--neon-cyan); text-shadow: 0 0 10px var(--neon-cyan); }
.unit { font-size: 13px; color: #8888aa; }
.plan ul { list-style: none; display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #c9c9e6; }
.msg { color: var(--neon-cyan); margin-bottom: 14px; font-size: 14px; }
</style>
