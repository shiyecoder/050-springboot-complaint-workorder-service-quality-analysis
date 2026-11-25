<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <div class="head-line">
          <h1 class="num">{{ tk?.ticketNo || '投诉详情' }}</h1>
          <span v-if="tk" class="tag" :class="statusTag(tk.status)">{{ tk.status }}</span>
          <span v-if="tk" class="tag" :class="levelTag(tk.levelCode)">{{ tk.levelCode }} {{ levelName }}</span>
          <span v-if="tk?.overdueFlag" class="tag t-red">已超期</span>
          <span v-if="tk?.duplicateFlag" class="tag t-amber">疑似重复</span>
          <span v-if="tk?.invalidFlag" class="tag t-gray">无效</span>
        </div>
        <div class="desc">{{ tk?.title || '加载中…' }}</div>
      </div>
      <div class="acts">
        <el-button size="small" @click="$router.push('/ticket/list')">返回台账</el-button>
        <el-button v-if="can('dispatch')" size="small" type="primary" @click="openDispatch">分派</el-button>
        <el-button v-if="can('accept')" size="small" type="primary" @click="doAccept">接单</el-button>
        <el-button v-if="can('handle')" size="small" type="primary" @click="goHandle">提交方案</el-button>
        <el-button v-if="can('review')" size="small" type="primary" @click="openReview">审核</el-button>
        <el-button v-if="can('confirm')" size="small" type="primary" @click="openConfirm">客户确认</el-button>
        <el-button v-if="can('revisit')" size="small" type="primary" @click="openRevisit">回访登记</el-button>
        <el-button size="small" :loading="exporting" @click="doExport">导出</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div class="detail-grid" v-if="tk">
        <!-- 左栏：事实、证据、方案、聚类 -->
        <div>
          <div class="card">
            <div class="card-h"><h2>基本信息</h2><span class="sub">创建于 {{ fmtTime(tk.createTime) }}</span></div>
            <div class="kv2">
              <div class="k">客户</div><div>{{ tk.customerName || '—' }}</div>
              <div class="k">联系电话</div><div class="num">{{ tk.phone || '—' }}</div>
              <div class="k">投诉对象</div><div>{{ tk.objectName || '—' }}</div>
              <div class="k">投诉类别</div><div>{{ tk.categoryName || '—' }}</div>
              <div class="k">受理渠道</div><div>{{ tk.channel || '—' }}</div>
              <div class="k">严重级别</div><div>{{ tk.levelCode }} {{ levelName }}</div>
              <div class="k">发生时间</div><div class="num">{{ fmtTime(tk.occurTime) }}</div>
              <div class="k">发生地点</div><div>{{ tk.occurPlace || '—' }}</div>
              <div class="k">要求响应</div><div class="num">{{ fmtTime(tk.responseDeadline) }}</div>
              <div class="k">要求完成</div>
              <div class="num" :style="{ color: isOverdue(tk.deadlineTime, tk.finishTime) ? 'var(--danger)' : '' }">
                {{ fmtTime(tk.deadlineTime) }}
              </div>
              <div class="k">当前处理人</div><div>{{ tk.currentHandlerName || '待分派' }}</div>
              <div class="k">责任部门</div><div>{{ tk.deptName || '—' }}</div>
              <div class="k">服务环节</div><div>{{ tk.serviceStage || '—' }}</div>
              <div class="k">完成时间</div><div class="num">{{ fmtTime(tk.finishTime) }}</div>
            </div>
            <div class="tagline" v-if="tk.reasonTags || tk.productTag">
              <span class="tag t-gray" v-if="tk.productTag">产品：{{ tk.productTag }}</span>
              <span class="tag t-gray" v-for="r in reasonList" :key="r">原因：{{ r }}</span>
            </div>
          </div>

          <div class="card">
            <div class="card-h"><h2>投诉内容与诉求</h2></div>
            <div class="sub-title">客户诉求</div>
            <div class="desc-block">{{ tk.demand || '客户未填写明确诉求' }}</div>
            <div class="sub-title">事实描述</div>
            <div class="desc-block">{{ tk.content || '暂无详细描述' }}</div>
          </div>

          <div class="card" v-if="images.length">
            <div class="card-h"><h2>证据图片</h2><span class="sub">共 {{ images.length }} 张，点击可放大</span></div>
            <div class="img-row">
              <el-image v-for="(src, i) in images" :key="src" class="evi-img" :src="src" fit="cover"
                        :preview-src-list="images" :initial-index="i" preview-teleported />
            </div>
          </div>

          <div class="card">
            <div class="card-h">
              <h2>处理方案</h2>
              <span class="sub" v-if="order">{{ order.orderNo }} · {{ order.nodeName }}</span>
            </div>
            <template v-if="order">
              <div class="kv2 kv-narrow">
                <div class="k">处理人</div><div>{{ order.handlerName || '—' }}（{{ order.deptName || '—' }}）</div>
                <div class="k">优先级</div><div>{{ order.priority || '—' }}</div>
                <div class="k">分派时间</div><div class="num">{{ fmtTime(order.dispatchTime) }}</div>
                <div class="k">接单时间</div><div class="num">{{ fmtTime(order.acceptTime) }}</div>
                <div class="k">计划完成</div><div class="num">{{ fmtTime(order.planFinishTime) }}</div>
                <div class="k">实际完成</div><div class="num">{{ fmtTime(order.actualFinishTime) }}</div>
              </div>
              <div class="sub-title">解决方案</div>
              <div class="desc-block">{{ order.solution || '处理人尚未提交解决方案' }}</div>
              <div class="sub-title">处理结果</div>
              <div class="desc-block">{{ order.result || '暂无处理结果说明' }}</div>
              <div class="sub-title" v-if="handles.length">处理记录</div>
              <div class="tl" v-if="handles.length">
                <div class="tl-item" v-for="h in handles" :key="h.id">
                  <div><b>第 {{ h.stepNo }} 步</b> · {{ h.operatorName || '—' }}</div>
                  <div style="color:var(--text-sub)">{{ h.content }}</div>
                  <div class="tm num">{{ fmtTime(h.handleTime) }}</div>
                </div>
              </div>
            </template>
            <div v-else class="empty-d">工单尚未生成，完成分派后自动创建并进入 SLA 计时</div>
          </div>

          <!-- 创新点：结构化共性问题聚类结果 -->
          <div class="card cluster" v-if="score">
            <div class="card-h">
              <h2>共性问题聚类</h2>
              <span class="sub">规则版本 {{ score.ruleVersion || '—' }} · 计算于 {{ fmtTime(score.calcTime) }}</span>
            </div>

            <div class="cluster-head">
              <div>
                <div class="cn">{{ score.clusterName || '—' }}</div>
                <div class="sub">相似度 <b class="num">{{ score.similarity }}</b> 分</div>
              </div>
              <div class="cluster-right">
                <span class="tag" :class="clusterLabel(score.level)[1]">{{ clusterLabel(score.level)[0] }}</span>
                <span class="tag t-blue" v-if="score.manualLevel">人工修正：{{ clusterLabel(score.manualLevel)[0] }}</span>
              </div>
            </div>

            <div class="factor-row" v-for="f in factors" :key="f.name">
              <span>{{ f.name }}</span>
              <span class="bar"><i :style="{ width: f.pct + '%' }"></i></span>
              <span class="num">{{ f.score }} / {{ f.weight }}</span>
            </div>

            <div class="sub-title">判定依据</div>
            <div class="desc-block">{{ score.reasons || '—' }}</div>
            <div class="snap" v-if="score.inputSnapshot">输入快照：{{ score.inputSnapshot }}</div>
            <div class="snap" v-if="score.unmet">未满足项：{{ score.unmet }}</div>
            <div class="snap" v-if="score.improve">改进建议：{{ score.improve }}</div>
            <div class="snap" v-if="score.manualLevel">
              人工修正记录：{{ clusterLabel(score.manualLevel)[0] }} · {{ score.manualReason || '—' }}
              · {{ score.manualBy || '' }} {{ fmtTime(score.manualTime) }}
            </div>

            <div class="cluster-acts">
              <el-button size="small" @click="adoptScore">采纳系统判定</el-button>
              <el-button size="small" type="primary" @click="openFix">修正档位</el-button>
            </div>
          </div>

          <div class="card" v-if="attachments.length">
            <div class="card-h"><h2>附件</h2><span class="sub">{{ attachments.length }} 个文件</span></div>
            <el-table :data="attachments" size="small">
              <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
              <el-table-column prop="bizType" label="类型" width="110" />
              <el-table-column label="大小" width="100">
                <template #default="{ row }"><span class="num">{{ fmtSize(row.fileSize) }}</span></template>
              </el-table-column>
              <el-table-column label="上传时间" width="150">
                <template #default="{ row }"><span class="num">{{ fmtTime(row.createTime) }}</span></template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <a :href="row.filePath" target="_blank" rel="noopener">查看</a>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="card" v-if="coops.length">
            <div class="card-h"><h2>协同任务</h2><span class="sub">{{ coops.length }} 项</span></div>
            <el-table :data="coops" size="small">
              <el-table-column prop="deptName" label="协办部门" width="120" />
              <el-table-column prop="handlerName" label="协办人" width="90" />
              <el-table-column prop="taskContent" label="协办内容" min-width="220" show-overflow-tooltip />
              <el-table-column label="截止" width="140">
                <template #default="{ row }"><span class="num">{{ fmtTime(row.deadlineTime) }}</span></template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 右栏：流转与审批轨迹 -->
        <div>
          <div class="card">
            <div class="card-h"><h2>流转时间轴</h2><span class="sub">{{ timeline.length }} 条记录</span></div>
            <div class="tl" v-if="timeline.length">
              <div class="tl-item" v-for="(it, i) in timeline" :key="i">
                <div class="tl-head">
                  <span class="tag t-gray">{{ it.recordType || '流转' }}</span>
                  <span v-if="it.fromStatus" class="tl-move">{{ it.fromStatus }} → {{ it.toStatus }}</span>
                  <span v-else class="tl-move">→ {{ it.toStatus }}</span>
                </div>
                <div class="tl-body">{{ it.content || '—' }}</div>
                <div class="tm">{{ it.operatorName || '系统' }} · <span class="num">{{ fmtTime(it.operateTime) }}</span></div>
              </div>
            </div>
            <div v-else class="empty-d">暂无流转记录</div>
          </div>

          <div class="card" v-if="review">
            <div class="card-h"><h2>审核记录</h2><span class="sub">{{ review.reviewNo }}</span></div>
            <div class="kv">
              <div class="k">审核结论</div>
              <div><span class="tag" :class="statusTag(review.reviewResult)">{{ review.reviewResult }}</span></div>
              <div class="k">方案评分</div><div class="num">{{ review.planScore }} 分</div>
              <div class="k">审核人</div><div>{{ review.reviewerName || '—' }}</div>
              <div class="k">审核时间</div><div class="num">{{ fmtTime(review.reviewTime) }}</div>
            </div>
            <div class="sub-title">审核意见</div>
            <div class="desc-block">{{ review.opinion || '—' }}</div>
          </div>

          <div class="card" v-if="confirm">
            <div class="card-h"><h2>客户确认</h2></div>
            <div class="kv">
              <div class="k">确认结果</div>
              <div><span class="tag" :class="statusTag(confirm.confirmResult)">{{ confirm.confirmResult }}</span></div>
              <div class="k">确认时间</div><div class="num">{{ fmtTime(confirm.confirmTime) }}</div>
              <div class="k">确认期限</div><div class="num">{{ fmtTime(confirm.deadlineTime) }}</div>
            </div>
            <div class="sub-title">客户意见</div>
            <div class="desc-block">{{ confirm.opinion || '客户未填写意见' }}</div>
          </div>

          <div class="card" v-if="revisit">
            <div class="card-h"><h2>回访与评价</h2></div>
            <div class="kv">
              <div class="k">是否解决</div><div>{{ revisit.solvedFlag ? '已解决' : '未解决' }}</div>
              <div class="k">满意度</div><div class="num">{{ satisfactionText(revisit.satisfaction) }}</div>
              <div class="k">回访方式</div><div>{{ revisit.revisitType || '—' }}</div>
              <div class="k">回访时间</div><div class="num">{{ fmtTime(revisit.revisitTime) }}</div>
              <div class="k">回访人</div><div>{{ revisit.revisitorName || '—' }}</div>
            </div>
            <div class="sub-title">回访意见</div>
            <div class="desc-block">{{ revisit.opinion || '—' }}</div>
          </div>

          <div class="card" v-if="reopens.length">
            <div class="card-h"><h2>重开记录</h2></div>
            <div class="tl">
              <div class="tl-item" v-for="r in reopens" :key="r.id">
                <div class="tl-body">{{ r.reason || '客户要求重开' }}</div>
                <div class="tm">{{ r.status || '' }} · <span class="num">{{ fmtTime(r.createTime) }}</span></div>
              </div>
            </div>
          </div>

          <div class="card" v-if="delays.length">
            <div class="card-h"><h2>延时申请</h2></div>
            <el-table :data="delays" size="small">
              <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="86">
                <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
              </el-table-column>
            </el-table>
          </div>

          <div class="card" v-if="warnings.length">
            <div class="card-h"><h2>时限预警</h2></div>
            <div class="tl">
              <div class="tl-item" v-for="w in warnings" :key="w.id">
                <div class="tl-head">
                  <span class="tag" :class="w.warnLevel === '严重' ? 't-red' : 't-amber'">{{ w.warnType }}</span>
                  <span class="tm">{{ w.warnLevel }}</span>
                </div>
                <div class="tl-body">{{ w.handleNote || '系统自动扫描生成预警' }}</div>
                <div class="tm num">{{ fmtTime(w.warnTime) }}</div>
              </div>
            </div>
          </div>

          <div class="card" v-if="comms.length">
            <div class="card-h"><h2>客户沟通</h2><span class="sub">{{ comms.length }} 次</span></div>
            <div class="tl">
              <div class="tl-item" v-for="c in comms" :key="c.id">
                <div class="tl-head">
                  <span class="tag t-blue">{{ c.commType }}</span>
                  <span class="tm num">{{ fmtTime(c.commTime) }}</span>
                </div>
                <div class="tl-body">{{ c.summary }}</div>
                <div class="tm">{{ c.operatorName }} · 客户反馈：{{ c.customerFeedback || '—' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：按合法下一步推进 -->
      <div class="card actbar" v-if="tk">
        <div class="ab-l">
          <b>当前节点：{{ tk.status }}</b>
          <span class="sub">{{ nextHint }}</span>
        </div>
        <el-button v-if="nextStep" type="primary" @click="nextStep.run()">{{ nextStep.btn }}</el-button>
        <el-button v-else-if="tk.status === '已归档'" disabled>流程已归档</el-button>
      </div>
    </div>

    <!-- 分派 -->
    <el-dialog v-model="dlg.dispatch" title="分派工单" width="480px">
      <el-form label-width="88px" size="default">
        <el-form-item label="工单">
          <span class="num">{{ tk?.ticketNo }} · {{ tk?.title }}</span>
        </el-form-item>
        <el-form-item label="责任部门">
          <el-select v-model="fm.deptId" placeholder="请选择部门" style="width:100%" @change="fm.handlerId = null">
            <el-option v-for="d in opt.depts" :key="d.id" :label="d.orgName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人">
          <el-select v-model="fm.handlerId" placeholder="请选择处理人" filterable style="width:100%">
            <el-option v-for="h in deptHandlers" :key="h.id" :label="`${h.realName}（${h.orgName}）`" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分派说明">
          <el-input v-model="fm.note" type="textarea" :rows="3" placeholder="说明分派依据与要求" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.dispatch = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitDispatch">确认分派</el-button>
      </template>
    </el-dialog>

    <!-- 审核 -->
    <el-dialog v-model="dlg.review" title="结果审核" width="480px">
      <el-form label-width="88px">
        <el-form-item label="审核结论">
          <el-radio-group v-model="rv.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="方案评分">
          <el-input-number v-model="rv.planScore" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="rv.opinion" type="textarea" :rows="3" placeholder="不少于 5 个字，驳回时必须写明原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.review = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReview">提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 客户确认 -->
    <el-dialog v-model="dlg.confirm" title="客户确认登记" width="480px">
      <el-form label-width="88px">
        <el-form-item label="确认结果">
          <el-radio-group v-model="cf.accept">
            <el-radio :value="true">接受方案</el-radio>
            <el-radio :value="false">提出异议</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="客户意见">
          <el-input v-model="cf.opinion" type="textarea" :rows="3" placeholder="提出异议时不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.confirm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitConfirm">提交</el-button>
      </template>
    </el-dialog>

    <!-- 回访 -->
    <el-dialog v-model="dlg.revisit" title="回访登记" width="480px">
      <el-form label-width="88px">
        <el-form-item label="回访方式">
          <el-select v-model="vs.revisitType" style="width:100%">
            <el-option v-for="c in ['电话', '在线消息', '上门']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否解决">
          <el-switch v-model="vs.solvedFlag" :active-value="1" :inactive-value="0" active-text="已解决" inactive-text="未解决" />
        </el-form-item>
        <el-form-item label="满意度">
          <el-rate v-model="vs.satisfaction" />
        </el-form-item>
        <el-form-item label="回访意见">
          <el-input v-model="vs.opinion" type="textarea" :rows="3" placeholder="记录客户对处理结果的评价" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.revisit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRevisit">提交并归档</el-button>
      </template>
    </el-dialog>

    <!-- 聚类档位修正 -->
    <el-dialog v-model="dlg.fix" title="修正聚类档位" width="420px">
      <el-form label-width="80px">
        <el-form-item label="系统判定">
          <span class="tag" :class="clusterLabel(score?.level)[1]">{{ clusterLabel(score?.level)[0] }}</span>
        </el-form-item>
        <el-form-item label="修正为">
          <el-select v-model="fx.level" style="width:100%">
            <el-option label="高相似" value="HIGH" />
            <el-option label="中相似" value="MID" />
            <el-option label="低相似" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="修正原因">
          <el-input v-model="fx.reason" type="textarea" :rows="3" placeholder="说明人工判断依据，便于规则迭代" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg.fix = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFix">保存</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'
import { api } from '../api'
import { store } from '../store/user'
import { fmtTime, statusTag, levelTag, clusterLabel, satisfactionText, isOverdue } from '../utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const d = ref(null)
const opt = reactive({ levels: [], depts: [], handlers: [] })

const dlg = reactive({ dispatch: false, review: false, confirm: false, revisit: false, fix: false })
const fm = reactive({ deptId: null, handlerId: null, note: '' })
const rv = reactive({ pass: true, opinion: '', planScore: 85 })
const cf = reactive({ accept: true, opinion: '' })
const vs = reactive({ revisitType: '电话', solvedFlag: 1, satisfaction: 5, opinion: '' })
const fx = reactive({ level: 'HIGH', reason: '' })

const tk = computed(() => d.value?.ticket || null)
const order = computed(() => d.value?.order || null)
const timeline = computed(() => d.value?.timeline || [])
const attachments = computed(() => d.value?.attachments || [])
const handles = computed(() => d.value?.handles || [])
const comms = computed(() => d.value?.comms || [])
const review = computed(() => d.value?.review || null)
const confirm = computed(() => d.value?.confirm || null)
const revisit = computed(() => d.value?.revisit || null)
const reopens = computed(() => d.value?.reopens || [])
const delays = computed(() => d.value?.delays || [])
const coops = computed(() => d.value?.coops || [])
const warnings = computed(() => d.value?.warnings || [])
const score = computed(() => d.value?.score || null)

const levelName = computed(() => {
  const l = (opt.levels || []).find((x) => x.levelCode === tk.value?.levelCode)
  return l?.levelName || ''
})
const reasonList = computed(() => (tk.value?.reasonTags ? String(tk.value.reasonTags).split('/').filter(Boolean) : []))
const deptHandlers = computed(() => (opt.handlers || []).filter((h) => Number(h.orgId) === Number(fm.deptId)))

// 证据图 = 主图 + 图片类附件
const images = computed(() => {
  const list = []
  if (tk.value?.coverImage) list.push(tk.value.coverImage)
  attachments.value.forEach((a) => {
    if (a.filePath && /\.(jpg|jpeg|png|webp|gif)$/i.test(a.filePath) && !list.includes(a.filePath)) list.push(a.filePath)
  })
  return list
})

// 六大因子与权重：关联产品25 / 服务环节15 / 问题类型20 / 原因标签15 / 责任部门10 / 近30天次数15
const FACTORS = [
  ['关联产品', 'sProduct', 25],
  ['服务环节', 'sStage', 15],
  ['问题类型', 'sProblem', 20],
  ['原因标签', 'sReason', 15],
  ['责任部门', 'sDept', 10],
  ['近30天次数', 'sFreq', 15]
]
const factors = computed(() => {
  const s = score.value
  if (!s) return []
  return FACTORS.map(([name, key, weight]) => {
    const v = Number(s[key] ?? 0)
    return { name, weight, score: Math.round(v * 10) / 10, pct: Math.min(100, Math.round((v / weight) * 100)) }
  })
})

// 角色与状态共同决定按钮可见性
const role = computed(() => store.user.roleCode || '')
const inRole = (list) => list.includes(role.value)
function can(act) {
  const s = tk.value?.status
  if (!s) return false
  if (act === 'dispatch') return s === '待受理' && inRole(['CS', 'LEADER', 'ADMIN'])
  if (act === 'accept') return s === '已分派' && inRole(['DEPT', 'LEADER', 'ADMIN'])
  if (act === 'handle') return s === '处理中' && inRole(['DEPT', 'LEADER', 'ADMIN'])
  if (act === 'review') return s === '待审核' && inRole(['LEADER', 'ADMIN'])
  if (act === 'confirm') return s === '待客户确认' && inRole(['CS', 'LEADER', 'ADMIN'])
  if (act === 'revisit') return s === '待回访' && inRole(['CS', 'LEADER', 'ADMIN'])
  return false
}

// 状态机主流程：待受理→已分派→处理中→待审核→待客户确认→待回访→已归档
const NEXT = {
  待受理: { to: '已分派', btn: '受理分派', hint: '核实后选择责任部门与处理人，工单进入处理环节', act: 'dispatch' },
  已分派: { to: '处理中', btn: '接单处理', hint: '处理人确认接单后开始计时处理', act: 'accept' },
  处理中: { to: '待审核', btn: '提交处理方案', hint: '提交调查情况与解决方案，交由客服组长审核', act: 'handle' },
  待审核: { to: '待客户确认', btn: '审核通过', hint: '审核通过后转客户确认，驳回则退回处理中', act: 'review' },
  待客户确认: { to: '待回访', btn: '登记客户确认', hint: '客户接受后进入回访，提出异议则退回处理中', act: 'confirm' },
  待回访: { to: '已归档', btn: '回访并归档', hint: '回访登记满意度后归档，流程结束', act: 'revisit' }
}
const nextStep = computed(() => {
  const n = NEXT[tk.value?.status]
  if (!n || !can(n.act)) return null
  const run = { dispatch: openDispatch, accept: doAccept, handle: goHandle, review: openReview, confirm: openConfirm, revisit: openRevisit }[n.act]
  return { ...n, run }
})
const nextHint = computed(() => {
  const n = NEXT[tk.value?.status]
  if (!n) return '该工单已归档，仅可查看与导出'
  if (!can(n.act)) return `下一步为「${n.to}」，当前角色无操作权限，请联系对应角色处理`
  return n.hint
})

function fmtSize(n) {
  if (!n && n !== 0) return '—'
  const kb = Number(n) / 1024
  return kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(kb))} KB`
}

async function load() {
  loading.value = true
  try {
    d.value = await api.ticketDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

function openDispatch() {
  fm.deptId = tk.value?.deptId || null
  fm.handlerId = tk.value?.currentHandlerId || null
  fm.note = ''
  dlg.dispatch = true
}

// 接单：已分派 → 处理中
async function doAccept() {
  saving.value = true
  try {
    await api.acceptOrder(tk.value.id)
    ElMessage.success('已接单，请在时限内提交处理方案')
    await load()
  } finally {
    saving.value = false
  }
}

function goHandle() {
  router.push({ path: '/handle/edit', query: { ticketId: tk.value.id } })
}

function openReview() {
  rv.pass = true
  rv.opinion = ''
  rv.planScore = 85
  dlg.review = true
}
function openConfirm() {
  cf.accept = true
  cf.opinion = ''
  dlg.confirm = true
}
function openRevisit() {
  vs.revisitType = '电话'
  vs.solvedFlag = 1
  vs.satisfaction = 5
  vs.opinion = ''
  dlg.revisit = true
}
function openFix() {
  fx.level = score.value?.level || 'MID'
  fx.reason = ''
  dlg.fix = true
}

async function submitDispatch() {
  if (!fm.deptId) return ElMessage.warning('请选择责任部门')
  if (!fm.handlerId) return ElMessage.warning('请选择处理人')
  saving.value = true
  try {
    await api.transition(tk.value.id, {
      toStatus: '已分派',
      reason: fm.note || '客服核实后分派处理',
      deptId: fm.deptId,
      handlerId: fm.handlerId
    })
    ElMessage.success('工单已分派')
    dlg.dispatch = false
    await load()
  } finally {
    saving.value = false
  }
}

async function submitReview() {
  if ((rv.opinion || '').trim().length < 5) return ElMessage.warning('审核意见不少于 5 个字')
  saving.value = true
  try {
    // 通过 → 待客户确认；驳回 → 退回处理中，均由后端状态机控制
    await api.review(tk.value.id, { pass: rv.pass, opinion: rv.opinion, planScore: rv.planScore })
    ElMessage.success(rv.pass ? '审核通过，已转客户确认' : '已驳回，工单退回处理中')
    dlg.review = false
    await load()
  } finally {
    saving.value = false
  }
}

async function submitConfirm() {
  if (!cf.accept && (cf.opinion || '').trim().length < 5) return ElMessage.warning('提出异议需填写不少于 5 个字的原因')
  saving.value = true
  try {
    await api.confirm(tk.value.id, {
      accept: cf.accept,
      confirmResult: cf.accept ? '接受' : '提出异议',
      opinion: cf.opinion
    })
    ElMessage.success(cf.accept ? '客户已确认，进入待回访' : '已记录客户异议，工单退回处理中')
    dlg.confirm = false
    await load()
  } finally {
    saving.value = false
  }
}

async function submitRevisit() {
  saving.value = true
  try {
    await api.revisit({
      ticketId: tk.value.id,
      revisitType: vs.revisitType,
      solvedFlag: vs.solvedFlag,
      satisfaction: vs.satisfaction,
      opinion: vs.opinion || '客户对处理结果表示认可'
    })
    ElMessage.success('回访完成，工单已归档')
    dlg.revisit = false
    await load()
  } finally {
    saving.value = false
  }
}

async function adoptScore() {
  saving.value = true
  try {
    // 采纳 = 以系统判定档位写入人工修正字段，保留操作痕迹
    await api.manualLevel(score.value.id, {
      manualLevel: score.value.level,
      level: score.value.level,
      reason: '采纳系统聚类判定'
    })
    ElMessage.success('已采纳系统判定')
    await load()
  } finally {
    saving.value = false
  }
}

async function saveFix() {
  if ((fx.reason || '').trim().length < 5) return ElMessage.warning('请填写不少于 5 个字的修正原因')
  saving.value = true
  try {
    await api.manualLevel(score.value.id, { manualLevel: fx.level, level: fx.level, reason: fx.reason })
    ElMessage.success('档位已修正')
    dlg.fix = false
    await load()
  } finally {
    saving.value = false
  }
}

async function doExport() {
  exporting.value = true
  try {
    const r = await api.exportTickets({ keyword: tk.value?.ticketNo })
    ElMessage.success(`已生成导出文件：${r?.fileName || '投诉台账.xlsx'}（${r?.total || 0} 条）`)
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  const o = await api.options()
  Object.assign(opt, o)
  await load()
})
</script>

<style scoped>
.head-line { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.head-line h1 { font-size: 20px; line-height: 28px; margin: 0; font-weight: 600; }
.page-head .acts { flex-wrap: wrap; justify-content: flex-end; max-width: 620px; }

.kv2 { display: grid; grid-template-columns: 82px 1fr 82px 1fr; gap: 8px 10px; font-size: 13px; }
.kv2.kv-narrow { grid-template-columns: 82px 1fr; }
.kv2 .k { color: var(--text-sub); }
.tagline { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 10px; }
.sub-title { font-size: 13px; font-weight: 600; margin: 12px 0 6px; }
.img-row { display: flex; gap: 8px; flex-wrap: wrap; }
.evi-img { width: 108px; height: 80px; border-radius: 6px; border: 1px solid var(--line); }

.tl-head { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.tl-move { font-size: 12px; color: var(--text-sub); }
.tl-body { line-height: 20px; }
.tl .tm { font-size: 11px; color: var(--text-sub); }

.cluster-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 12px; }
.cluster-head .cn { font-size: 14px; font-weight: 600; }
.cluster-right { display: flex; gap: 6px; }
.snap { font-size: 11px; color: var(--text-sub); line-height: 18px; margin-top: 4px; }
.cluster-acts { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }

.actbar { display: flex; align-items: center; justify-content: space-between; margin-top: 14px; }
.ab-l { display: flex; align-items: center; gap: 12px; font-size: 13px; }
.ab-l .sub { font-size: 12px; color: var(--text-sub); }

.empty-d { font-size: 12px; color: var(--text-sub); padding: 10px 0; }

@media (max-width: 1200px) {
  .kv2 { grid-template-columns: 82px 1fr; }
  .page-head .acts { max-width: none; }
}
</style>
