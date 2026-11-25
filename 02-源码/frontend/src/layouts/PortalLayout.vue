<template>
  <div style="min-height:100vh;background:var(--bg)">
    <!-- 客户门户：无侧栏，浅色前台风格 -->
    <header class="portal-top">
      <div style="display:flex;align-items:center;gap:10px">
        <span style="width:26px;height:26px;border-radius:6px;background:var(--secondary);color:#fff;display:grid;place-items:center;font-size:13px;font-weight:700">客</span>
        <b style="font-size:15px">客户服务门户</b>
        <span style="font-size:12px;color:var(--text-sub);margin-left:6px">投诉提交 · 进度跟踪 · 结果确认</span>
      </div>
      <nav style="display:flex;align-items:center;gap:18px;font-size:13px">
        <span v-for="n in navs" :key="n.path" style="cursor:pointer"
              :style="{ color: route.path === n.path ? 'var(--secondary)' : 'var(--text-sub)', fontWeight: route.path === n.path ? 600 : 400 }"
              @click="router.push(n.path)">{{ n.name }}</span>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="avatar-box">
            <img :src="store.user.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
            <span style="font-size:13px">{{ store.user.realName || store.user.username }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>
    </header>
    <div class="portal-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { store, clearLogin } from '../store/user'

const route = useRoute()
const router = useRouter()
const navs = [
  { name: '我的投诉', path: '/portal' },
  { name: '提交投诉', path: '/portal/new' },
  { name: '站内消息', path: '/message' },
  { name: '个人中心', path: '/profile' }
]
function onCommand(cmd) {
  if (cmd === 'logout') {
    clearLogin()
    router.push('/login')
  } else router.push('/profile')
}
</script>
