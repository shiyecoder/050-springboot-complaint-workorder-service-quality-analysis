<template>
  <PortalLayout>
    <div class="head">
      <h1>提交投诉</h1>
      <div class="sub">填写问题经过与诉求，我们会在 2 小时内响应并安排专人跟进</div>
    </div>

    <!-- 3 步引导：让客户明确当前处于流程的哪一步 -->
    <div class="steps card">
      <el-steps :active="0" align-center finish-status="success">
        <el-step title="填写投诉" description="描述问题并上传证据" />
        <el-step title="客服受理" description="核实后分派责任部门" />
        <el-step title="跟踪确认" description="查看进度并确认结果" />
      </el-steps>
    </div>

    <div class="card">
      <el-form :model="form" label-width="88px" label-position="top" size="large">
        <div class="grid2">
          <el-form-item label="投诉类别 *">
            <el-select v-model="form.categoryId" placeholder="请选择问题类别" style="width:100%" @change="onCategory">
              <el-option v-for="c in opt.categories" :key="c.id" :label="c.categoryName" :value="c.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="投诉对象 *">
            <el-select v-model="form.objectId" filterable placeholder="请选择产品或服务" style="width:100%">
              <el-option v-for="o in opt.objects" :key="o.id" :label="`${o.objectName}（${o.objectType}）`" :value="o.id" />
            </el-select>
          </el-form-item>
        </div>

        <div class="tip" v-if="cat">
          该问题属于「{{ cat.problemType }}」环节「{{ cat.serviceStage }}」，通常 {{ cat.responseHours }} 小时内响应、
          {{ cat.handleHours }} 小时内给出处理方案。
        </div>

        <el-form-item label="投诉标题 *">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="一句话说明问题，例如：安装后漏水无人处理" />
        </el-form-item>

        <el-form-item label="问题描述 *">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="500" show-word-limit
                    placeholder="请描述发生时间、现象、已联系过谁以及对方的答复" />
        </el-form-item>

        <el-form-item label="您的诉求">
          <el-input v-model="form.demand" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="例如：希望三天内上门更换并书面致歉" />
        </el-form-item>

        <div class="grid2">
          <el-form-item label="发生时间">
            <el-date-picker v-model="form.occurTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                            placeholder="选择问题发生时间" style="width:100%" />
          </el-form-item>
          <el-form-item label="发生地点">
            <el-input v-model="form.occurPlace" placeholder="例如：上海浦东 家中厨房" />
          </el-form-item>
        </div>

        <el-form-item label="现场照片">
          <div style="width:100%">
            <el-upload action="/api/file/upload" name="file" :headers="uploadHeaders" :show-file-list="false"
                       accept=".jpg,.jpeg,.png,.webp" :before-upload="beforeUpload"
                       :on-success="onUploaded" :on-error="onUploadError">
              <el-button><el-icon><Upload /></el-icon>上传照片</el-button>
            </el-upload>
            <div class="tip-plain">支持 jpg / png / webp，单张不超过 20MB；清晰的现场照片有助于快速定责</div>
            <div class="preview" v-if="form.coverImage">
              <el-image class="prev-img" :src="form.coverImage" :preview-src-list="[form.coverImage]"
                        fit="cover" preview-teleported />
              <el-button link type="danger" @click="form.coverImage = ''">移除</el-button>
            </div>
          </div>
        </el-form-item>

        <div class="foot">
          <el-button @click="$router.push('/portal')">返回我的投诉</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交投诉</el-button>
        </div>
      </el-form>
    </div>
  </PortalLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import PortalLayout from '../layouts/PortalLayout.vue'
import { api } from '../api'
import { store } from '../store/user'

const router = useRouter()
const opt = reactive({ categories: [], objects: [], customers: [] })
const saving = ref(false)
const customerId = ref(null)

const form = reactive({
  categoryId: null,
  objectId: null,
  levelCode: 'P3',
  title: '',
  content: '',
  demand: '',
  occurTime: '',
  occurPlace: '',
  coverImage: ''
})

const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` }))
const cat = computed(() => (opt.categories || []).find((c) => c.id === form.categoryId) || null)

function onCategory(id) {
  const c = (opt.categories || []).find((x) => x.id === form.categoryId)
  form.levelCode = c?.defaultLevel || 'P3'
}

function beforeUpload(file) {
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('单张照片不能超过 20MB')
    return false
  }
  return true
}

function onUploaded(res) {
  if (res?.code === 200 && res.data?.url) {
    form.coverImage = res.data.url
    ElMessage.success('照片上传成功')
  } else {
    ElMessage.error(res?.message || '照片上传失败')
  }
}

function onUploadError() {
  ElMessage.error('照片上传失败，请稍后重试')
}

/**
 * 客户档案 ID：客户账号与客户档案通过 user_id 关联，前端没有单独接口，
 * 这里先用本人投诉记录带出，其次按姓名/电话匹配档案，最后才退化为第一条。
 */
async function resolveCustomer() {
  try {
    const r = await api.myTickets({ page: 1, size: 1 })
    const first = (r.records || [])[0]
    if (first?.customerId) {
      customerId.value = first.customerId
      return
    }
  } catch (e) { /* 忽略，继续用后备方案 */ }

  const list = opt.customers || []
  const byName = list.find((c) => c.customerName === store.user.realName)
  const byPhone = list.find((c) => c.phone && c.phone === store.user.phone)
  customerId.value = (byName || byPhone || list[0])?.id || null
}

async function submit() {
  if (!form.categoryId) return ElMessage.warning('请选择投诉类别')
  if (!form.objectId) return ElMessage.warning('请选择投诉对象')
  if (!form.title.trim()) return ElMessage.warning('请填写投诉标题')
  if (!form.content.trim()) return ElMessage.warning('请描述问题经过')
  if (!customerId.value) return ElMessage.warning('未能识别客户档案，请联系客服人员代录')

  saving.value = true
  try {
    const r = await api.createTicket({
      customerId: customerId.value,
      objectId: form.objectId,
      categoryId: form.categoryId,
      channel: '在线',
      levelCode: form.levelCode || cat.value?.defaultLevel || 'P3',
      title: form.title.trim(),
      content: form.content,
      demand: form.demand,
      occurTime: form.occurTime,
      occurPlace: form.occurPlace,
      coverImage: form.coverImage
    })
    ElMessage.success(`投诉已提交，编号 ${r?.ticketNo || ''}`)
    router.push('/portal')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)
  await resolveCustomer()
})
</script>

<style scoped>
.head { margin-bottom: 14px; }
.head h1 { font-size: 20px; margin: 0; font-weight: 600; }
.head .sub { font-size: 12px; color: var(--text-sub); margin-top: 4px; }
.steps { padding: 16px 8px; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.tip {
  background: var(--tint); border-radius: 6px; padding: 8px 12px; font-size: 12px;
  color: var(--text-sub); margin: 0 0 14px;
}
.tip-plain { font-size: 11px; color: var(--text-sub); margin-top: 6px; }
.preview { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.prev-img { width: 140px; height: 100px; border-radius: 6px; border: 1px solid var(--line); }
.foot { display: flex; justify-content: flex-end; gap: 12px; padding-top: 8px; border-top: 1px dashed var(--line); }
@media (max-width: 860px) {
  .grid2 { grid-template-columns: 1fr; }
}
</style>
