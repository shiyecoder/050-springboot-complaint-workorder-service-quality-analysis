<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>个人中心</h1>
        <div class="desc">维护个人资料与登录密码，查看最近站内消息；头像与姓名会同步到顶栏显示</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="$router.push('/message')">全部消息</el-button>
      </div>
    </div>

    <div class="prof-grid">
      <!-- 左：个人信息 -->
      <div class="card prof-card">
        <img v-if="info.avatar" class="ava-big" :src="info.avatar" alt="头像" />
        <span v-else class="ava-big ph">{{ (info.realName || info.username || 'U')[0] }}</span>
        <div class="p-name">{{ info.realName || info.username || '—' }}</div>
        <div class="p-role"><span class="tag t-blue">{{ roleShort }}</span></div>
        <div class="kv p-kv">
          <span class="k">登录账号</span><span>{{ info.username || '—' }}</span>
          <span class="k">所属组织</span><span>{{ info.orgName || '—' }}</span>
          <span class="k">数据范围</span><span>{{ scopeText(info.dataScope) }}</span>
          <span class="k">联系电话</span><span>{{ info.phone || '—' }}</span>
          <span class="k">电子邮箱</span><span class="ellipsis">{{ info.email || '—' }}</span>
          <span class="k">最后登录</span><span class="num">{{ fmtTime(info.lastLoginTime) }}</span>
        </div>
      </div>

      <!-- 右：资料 / 密码 / 消息 -->
      <div class="card" style="margin-bottom:0">
        <el-tabs v-model="tab">
          <el-tab-pane label="修改资料" name="profile">
            <el-form label-width="88px" style="max-width:520px;margin-top:8px">
              <el-form-item label="头像">
                <div style="display:flex;align-items:center;gap:14px">
                  <img v-if="form.avatar" class="ava-mid" :src="form.avatar" alt="头像预览" />
                  <span v-else class="ava-mid ph">{{ (form.realName || 'U')[0] }}</span>
                  <el-upload action="/api/file/upload" :headers="uploadHeaders" :show-file-list="false"
                             accept="image/jpeg,image/png,image/webp,image/gif"
                             :before-upload="beforeAvatar" :on-success="onAvatarDone" :on-error="onAvatarError">
                    <el-button size="small" :loading="uploading">上传新头像</el-button>
                  </el-upload>
                  <span class="tip">支持 jpg/png/webp，不超过 2MB</span>
                </div>
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="form.realName" maxlength="20" placeholder="请输入姓名" />
              </el-form-item>
              <el-form-item label="联系电话">
                <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="电子邮箱">
                <el-input v-model="form.email" maxlength="50" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
                <el-button @click="fillForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-form label-width="88px" style="max-width:460px;margin-top:8px">
              <el-form-item label="原密码">
                <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="pwd.newPassword" type="password" show-password placeholder="不少于 6 位" />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input v-model="pwd.confirm" type="password" show-password placeholder="再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="savingPwd" @click="savePassword">确认修改</el-button>
                <span class="tip" style="margin-left:10px">修改成功后下次登录使用新密码</span>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="我的消息" name="message">
            <div class="msg-list" v-loading="msgLoading">
              <div v-for="m in msgs" :key="m.id" class="msg-i" @click="$router.push('/message')">
                <span class="tag" :class="m.isRead ? 't-gray' : 't-amber'">{{ m.msgType || '通知' }}</span>
                <div class="msg-b">
                  <div class="msg-t">{{ m.title }}</div>
                  <div class="msg-c">{{ m.content }}</div>
                </div>
                <span class="msg-tm num">{{ fmtTime(String(m.createTime || '').replace('T', ' ')) }}</span>
              </div>
              <div v-if="!msgs.length && !msgLoading" class="empty">暂无站内消息</div>
              <div class="more">
                <el-link type="primary" @click="$router.push('/message')">查看全部消息 →</el-link>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { fmtTime } from '../utils/format'
import { store, roleShort } from '../store/user'

const tab = ref('profile')
const info = reactive({})
const form = reactive({ realName: '', phone: '', email: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const msgs = ref([])
const msgLoading = ref(false)
const savingProfile = ref(false)
const savingPwd = ref(false)
const uploading = ref(false)

const SCOPE = { ALL: '全部数据', ORG: '本组织', SELF: '本人', DEPT: '本部门' }
function scopeText(v) {
  return SCOPE[v] || v || '—'
}

const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` }))

function fillForm() {
  form.realName = info.realName || ''
  form.phone = info.phone || ''
  form.email = info.email || ''
  form.avatar = info.avatar || ''
}

function syncUser(patch) {
  const clean = {}
  Object.entries(patch || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') clean[k] = v
  })
  const next = { ...store.user, ...clean }
  store.user = next
  localStorage.setItem('user', JSON.stringify(next))
}

function beforeAvatar(file) {
  const ok = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)
  if (!ok) {
    ElMessage.warning('仅支持 jpg / png / webp / gif 图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return false
  }
  uploading.value = true
  return true
}

async function onAvatarDone(resp) {
  uploading.value = false
  const d = resp?.data || resp
  if (!d?.url) {
    ElMessage.error(resp?.message || '上传失败')
    return
  }
  form.avatar = d.url
  try {
    await api.updateProfile({ realName: form.realName, phone: form.phone, email: form.email, avatar: d.url })
    info.avatar = d.url
    syncUser({ avatar: d.url })
    ElMessage.success('头像已更新')
  } catch (e) {
    ElMessage.warning('头像已上传，但资料保存失败，请点击「保存资料」重试')
  }
}

function onAvatarError() {
  uploading.value = false
  ElMessage.error('头像上传失败，请稍后重试')
}

async function saveProfile() {
  if (!form.realName || !form.realName.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  savingProfile.value = true
  try {
    await api.updateProfile({
      realName: form.realName.trim(), phone: form.phone, email: form.email, avatar: form.avatar
    })
    Object.assign(info, { realName: form.realName.trim(), phone: form.phone, email: form.email, avatar: form.avatar })
    syncUser({ realName: form.realName.trim(), phone: form.phone, email: form.email, avatar: form.avatar })
    ElMessage.success('资料已保存')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwd.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于 6 位')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await api.updatePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改')
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
  } finally {
    savingPwd.value = false
  }
}

async function loadMsgs() {
  msgLoading.value = true
  try {
    const r = await api.messagePage({ page: 1, size: 3 })
    msgs.value = r.records || []
  } finally {
    msgLoading.value = false
  }
}

onMounted(async () => {
  Object.assign(info, store.user || {})
  try {
    const me = await api.me()
    Object.assign(info, me || {})
    syncUser({ realName: me?.realName, phone: me?.phone, avatar: me?.avatar, username: me?.username, roleCode: me?.roleCode, roleName: me?.roleName })
  } catch (e) { /* 读不到时用本地缓存 */ }
  fillForm()
  loadMsgs()
})
</script>

<style scoped>
.prof-grid { display: grid; grid-template-columns: 300px 1fr; gap: 14px; align-items: start; }
.prof-card { display: flex; flex-direction: column; align-items: center; text-align: left; }
.ava-big { width: 76px; height: 76px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); }
.ava-big.ph { display: grid; place-items: center; background: var(--tint); color: var(--text-sub); font-size: 26px; }
.p-name { font-size: 16px; font-weight: 600; margin-top: 10px; }
.p-role { margin: 6px 0 14px; }
.p-kv { width: 100%; grid-template-columns: 82px 1fr; gap: 8px 10px; }
.ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ava-mid { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); }
.ava-mid.ph { display: grid; place-items: center; background: var(--tint); color: var(--text-sub); font-size: 15px; }
.tip { font-size: 11px; color: var(--text-sub); }
.msg-i {
  display: flex; align-items: flex-start; gap: 10px; padding: 10px 0; border-bottom: 1px dashed var(--line);
  cursor: pointer;
}
.msg-i:hover .msg-t { color: var(--secondary); }
.msg-b { flex: 1; min-width: 0; }
.msg-t { font-size: 13px; font-weight: 600; margin-bottom: 2px; }
.msg-c { font-size: 12px; color: var(--text-sub); line-height: 18px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg-tm { font-size: 11px; color: var(--text-sub); white-space: nowrap; }
.more { text-align: right; padding-top: 10px; font-size: 12px; }
.empty { padding: 26px; text-align: center; color: var(--text-sub); font-size: 13px; }
:deep(.el-tabs__header) { margin-bottom: 4px; }
@media (max-width: 1100px) {
  .prof-grid { grid-template-columns: 1fr; }
}
</style>
