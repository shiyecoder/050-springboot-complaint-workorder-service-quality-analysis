<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>新建投诉登记</h1>
        <div class="desc">客服代客户登记投诉，选择分类后自动带出问题类型、服务环节、建议级别与 SLA 时限</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="$router.push('/intake')">返回受理台</el-button>
        <el-button size="small" type="primary" :loading="saving" @click="submit">提交登记</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-h">
        <h2>投诉基本信息</h2>
        <span class="sub">带 * 为必填项，提交后生成唯一客诉编号</span>
      </div>

      <el-form :model="form" label-width="96px" label-position="right" size="default">
        <div class="grid2">
          <el-form-item label="客户 *">
            <el-select v-model="form.customerId" filterable placeholder="输入姓名或电话筛选客户"
                       style="width:100%" @change="onCustomer">
              <el-option v-for="c in opt.customers" :key="c.id" :label="`${c.customerName}（${c.phone || '无电话'}）`" :value="c.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="选择客户后自动带出" />
          </el-form-item>

          <el-form-item label="投诉对象 *">
            <el-select v-model="form.objectId" filterable placeholder="选择产品或服务对象" style="width:100%">
              <el-option v-for="o in opt.objects" :key="o.id" :label="`${o.objectName}（${o.objectType}）`" :value="o.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="投诉类别 *">
            <el-select v-model="form.categoryId" filterable placeholder="选择业务分类" style="width:100%" @change="onCategory">
              <el-option v-for="c in opt.categories" :key="c.id" :label="c.categoryName" :value="c.id" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 选择分类后自动带出的规则提示 -->
        <div class="rule-tip" v-if="cat">
          <span>问题类型：<b>{{ cat.problemType }}</b></span>
          <span>服务环节：<b>{{ cat.serviceStage }}</b></span>
          <span>建议级别：<b>{{ cat.defaultLevel }}</b></span>
          <span>响应时限：<b>{{ cat.responseHours }} 小时</b></span>
          <span>处理时限：<b>{{ cat.handleHours }} 小时</b></span>
        </div>

        <div class="grid2">
          <el-form-item label="受理渠道">
            <el-select v-model="form.channel" placeholder="选择渠道" style="width:100%">
              <el-option v-for="c in opt.channels" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>

          <el-form-item label="严重级别">
            <el-select v-model="form.levelCode" placeholder="选择级别" style="width:100%">
              <el-option v-for="l in opt.levels" :key="l.levelCode" :label="`${l.levelCode} ${l.levelName}`" :value="l.levelCode" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="投诉标题 *">
          <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="一句话概括问题，例如「报修三天无人联系」" />
        </el-form-item>

        <el-form-item label="投诉内容">
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="500" show-word-limit
                    placeholder="记录客户表述的事实经过、沟通情况与关键时间点" />
        </el-form-item>

        <el-form-item label="客户诉求">
          <el-input v-model="form.demand" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="例如：要求更换新机并致歉" />
        </el-form-item>

        <div class="grid2">
          <el-form-item label="发生时间">
            <el-date-picker v-model="form.occurTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                            placeholder="选择问题发生时间" style="width:100%" />
          </el-form-item>

          <el-form-item label="发生地点">
            <el-input v-model="form.occurPlace" placeholder="例如：上海浦东 客户地址" />
          </el-form-item>
        </div>

        <el-form-item label="证据图片">
          <div class="evi-wrap">
            <div class="evi-actions">
              <el-upload action="/api/file/upload" name="file" :headers="uploadHeaders" :show-file-list="false"
                         accept=".jpg,.jpeg,.png,.webp,.gif" :before-upload="beforeUpload"
                         :on-success="onUploaded" :on-error="onUploadError">
                <el-button size="small">
                  <el-icon><Upload /></el-icon>上传图片
                </el-button>
              </el-upload>
              <span class="hint">支持 jpg / png / webp，单张不超过 20MB</span>
            </div>

            <div class="seed">
              <span class="hint">或从示例证据图选择：</span>
              <img v-for="s in seedImages" :key="s" class="seed-img" :class="{ on: form.coverImage === s }"
                   :src="s" alt="示例证据" @click="form.coverImage = s" />
            </div>

            <div class="preview" v-if="form.coverImage">
              <el-image class="prev-img" :src="form.coverImage" :preview-src-list="[form.coverImage]"
                        fit="cover" preview-teleported />
              <el-button link type="danger" @click="form.coverImage = ''">移除</el-button>
            </div>
          </div>
        </el-form-item>

        <div class="foot">
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" :loading="saving" @click="submit">提交登记</el-button>
        </div>
      </el-form>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'

const router = useRouter()
const opt = reactive({ customers: [], objects: [], categories: [], channels: [], levels: [] })
const saving = ref(false)

// 初始值集中定义，重置时复用
const blank = () => ({
  customerId: null,
  phone: '',
  objectId: null,
  categoryId: null,
  channel: '电话',
  levelCode: 'P3',
  title: '',
  content: '',
  demand: '',
  occurTime: '',
  occurPlace: '',
  coverImage: ''
})
const form = reactive(blank())

const seedImages = ['01', '02', '03', '04', '05', '06', '07', '08'].map((n) => `/uploads/seed/evidence/${n}.jpg`)

// 上传接口需要携带登录 token，el-upload 不经过 axios 拦截器，这里手动取
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` }))

const cat = computed(() => (opt.categories || []).find((c) => c.id === form.categoryId) || null)

function onCustomer(id) {
  const c = (opt.customers || []).find((x) => x.id === id)
  form.phone = c?.phone || ''
}

function onCategory(id) {
  const c = (opt.categories || []).find((x) => x.id === id)
  // 分类自带建议级别，客服仍可手动调整（调整原因在流转记录中体现）
  if (c?.defaultLevel) form.levelCode = c.defaultLevel
}

function beforeUpload(file) {
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('单张图片不能超过 20MB')
    return false
  }
  return true
}

function onUploaded(res) {
  // 后端统一返回 { code, message, data: { url, name, size } }
  if (res?.code === 200 && res.data?.url) {
    form.coverImage = res.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(res?.message || '图片上传失败')
  }
}

function onUploadError() {
  ElMessage.error('图片上传失败，请检查网络后重试')
}

function reset() {
  Object.assign(form, blank())
}

async function submit() {
  if (!form.customerId) return ElMessage.warning('请选择客户')
  if (!form.objectId) return ElMessage.warning('请选择投诉对象')
  if (!form.categoryId) return ElMessage.warning('请选择投诉类别')
  if (!form.title || !form.title.trim()) return ElMessage.warning('请填写投诉标题')

  saving.value = true
  try {
    const r = await api.createTicket({
      customerId: form.customerId,
      objectId: form.objectId,
      categoryId: form.categoryId,
      channel: form.channel,
      levelCode: form.levelCode,
      title: form.title.trim(),
      content: form.content,
      demand: form.demand,
      occurTime: form.occurTime,
      occurPlace: form.occurPlace,
      coverImage: form.coverImage
    })
    ElMessage.success(`投诉登记成功，编号 ${r?.ticketNo || ''}`)
    router.push(`/ticket/detail/${r.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)
})
</script>

<style scoped>
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 20px; }
.rule-tip {
  display: flex; flex-wrap: wrap; gap: 18px; margin: 0 0 16px 96px; padding: 8px 12px;
  background: var(--tint); border-radius: 6px; font-size: 12px; color: var(--text-sub);
}
.rule-tip b { color: var(--text); font-weight: 600; }
.evi-wrap { width: 100%; }
.evi-actions { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 11px; color: var(--text-sub); }
.seed { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 10px; }
.seed-img { width: 56px; height: 42px; object-fit: cover; border-radius: 4px; border: 1px solid var(--line); cursor: pointer; }
.seed-img.on { border-color: var(--secondary); box-shadow: 0 0 0 1px var(--secondary); }
.preview { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.prev-img { width: 132px; height: 96px; border-radius: 6px; border: 1px solid var(--line); }
.foot { display: flex; justify-content: flex-end; gap: 10px; padding-top: 6px; border-top: 1px dashed var(--line); }
@media (max-width: 1100px) {
  .grid2 { grid-template-columns: 1fr; }
  .rule-tip { margin-left: 0; }
}
</style>
