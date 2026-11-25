<template>
  <div class="login-wrap">
    <!-- 左栏：品牌 + 投诉流转链路，背景为本地生成的园区实景图（断网可见） -->
    <section class="login-left">
      <div class="lg-brand">
        <span class="logo">客</span>
        <span>客诉工单与服务质量分析系统</span>
      </div>
      <div>
        <h2>把每一条投诉<br />变成可追溯的服务改进</h2>
        <p class="sub">
          受理、分派、处理、审核、确认、回访全流程留痕，超时预警自动触发；
          结构化共性问题聚类帮助质量经理定位真正的系统性缺陷。
        </p>
        <div class="flow">
          <template v-for="(s, i) in flow" :key="s">
            <span class="st">{{ s }}</span>
            <span v-if="i < flow.length - 1" class="arrow">→</span>
          </template>
        </div>
      </div>
      <div class="ft">© 2026 客户服务中心 · 服务质量数字化平台</div>
    </section>

    <!-- 右栏：登录表单 -->
    <section class="login-right">
      <div class="login-card">
        <h3>账号登录</h3>
        <div class="hint">请使用工号或客户账号登录，不同角色进入不同工作台</div>
        <el-form :model="form" @submit.prevent="submit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                      show-password :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%;height:40px" :loading="loading" @click="submit">
            登录系统
          </el-button>
        </el-form>
        <div class="demo-acc">
          演示账号（点击填入）：<br />
          <b v-for="a in accounts" :key="a.u" style="margin-right:12px" @click="fill(a)">{{ a.n }}</b>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { setLogin, store } from '../store/user'

const router = useRouter()
const loading = ref(false)
const flow = ['待受理', '已分派', '处理中', '待审核', '待客户确认', '待回访', '已归档']
const form = reactive({ username: 'admin', password: 'admin123' })
const accounts = [
  { n: '质量经理 leader01', u: 'leader01' },
  { n: '客服 cs01', u: 'cs01' },
  { n: '部门 dept01', u: 'dept01' },
  { n: '客户 cust01', u: 'cust01' }
]

function fill(a) {
  form.username = a.u
  form.password = '123456'
}

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名与密码')
  loading.value = true
  try {
    const d = await api.login({ username: form.username.trim(), password: form.password })
    if (!d || !d.token) return ElMessage.error('登录返回异常')
    setLogin(d)
    ElMessage.success(`欢迎回来，${d.user.realName || d.user.username}`)
    router.replace(store.user.roleCode === 'CUSTOMER' ? '/portal' : '/dashboard')
  } catch (e) {
    /* 错误提示由拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>
