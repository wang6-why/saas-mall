<script setup>
import { ref } from 'vue'
import { login } from './api'

const username = ref('')
const password = ref('')
const tenantName = ref(localStorage.getItem('tenantName') || '')
const err = ref('')
const tab = ref('login')
const regName = ref('')

async function doLogin() {
  err.value = ''
  try {
    const res = await login(username.value, password.value)
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('tenantName', res.data.tenantName)
    tenantName.value = res.data.tenantName
    location.reload()
  } catch (e) {
    err.value = (e && e.msg) || '登录失败'
  }
}

async function doRegister() {
  err.value = ''
  try {
    await register(regName.value, username.value, password.value)
    tab.value = 'login'
    err.value = '注册成功，请登录'
  } catch (e) {
    err.value = (e && e.msg) || '注册失败'
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="panel login-panel">
      <h1 class="neon" style="text-align:center; margin-bottom:24px;">SaaS MALL</h1>
      <div class="tabs">
        <span :class="{active: tab==='login'}" @click="tab='login'">登录</span>
        <span :class="{active: tab==='reg'}" @click="tab='reg'">注册新租户</span>
      </div>
      <template v-if="tab==='login'">
        <input v-model="username" placeholder="管理员账号 (adminA / adminB)" />
        <input v-model="password" type="password" placeholder="密码 (123456)" @keyup.enter="doLogin" />
      </template>
      <template v-else>
        <input v-model="regName" placeholder="店铺名称" />
        <input v-model="username" placeholder="管理员账号" />
        <input v-model="password" type="password" placeholder="密码" />
      </template>
      <p v-if="err" class="err">{{ err }}</p>
      <button class="btn" style="margin-top:16px; width:100%;" @click="tab==='login' ? doLogin() : doRegister()">
        {{ tab==='login' ? '登录' : '注册' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.login-wrap { display:flex; align-items:center; justify-content:center; min-height:100vh; }
.login-panel { width: 380px; display:flex; flex-direction:column; gap:12px; padding:32px; }
.tabs { display:flex; gap:16px; margin-bottom:8px; }
.tabs span { cursor:pointer; color:#8888aa; padding-bottom:4px; }
.tabs span.active { color:var(--neon-pink); text-shadow:0 0 8px var(--neon-pink); border-bottom:2px solid var(--neon-pink); }
.err { color:#ff4444; font-size:13px; }
</style>
