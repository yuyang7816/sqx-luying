<script setup>
import { onMounted, reactive, ref } from 'vue'
import ImageUpload from '@/components/common/ImageUpload.vue'
import { getInvitePosterConfigApi, saveInvitePosterConfigApi } from '@/api/modules/app-user-config'
import {
  COMMISSION_RATE_LEGACY_KEY,
  getReferralCommissionConfig,
  saveReferralCommissionConfig,
} from '@/api/modules/referral-config'
import { validateForm } from '@/utils/form'
import { showError, showSuccess } from '@/utils/modal'

const loading = ref(false)
const savingPoster = ref(false)
const savingCommission = ref(false)
const posterFormRef = ref()
const commissionFormRef = ref()
const legacyPercent = ref(null)

const posterForm = reactive({
  brandName: '',
  inviterText: '',
  posterImage: '',
})

const commissionForm = reactive({
  level1Percent: 5,
  level2Percent: 2,
})

const posterRules = {
  brandName: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }],
  posterImage: [{ required: true, message: '请上传邀请海报图', trigger: 'change' }],
}

const commissionRules = {
  level1Percent: [{ required: true, message: '请输入一级比例', trigger: 'blur' }],
  level2Percent: [{ required: true, message: '请输入二级比例', trigger: 'blur' }],
}

async function loadPosterConfig() {
  const { config } = await getInvitePosterConfigApi()
  Object.assign(posterForm, config)
}

async function loadCommissionConfig() {
  const config = await getReferralCommissionConfig()
  commissionForm.level1Percent = config.level1.percent
  commissionForm.level2Percent = config.level2.percent
  legacyPercent.value = config.legacyPercent
}

async function loadData() {
  loading.value = true
  try {
    await Promise.all([loadPosterConfig(), loadCommissionConfig()])
  } catch (error) {
    showError(error?.msg || error?.message || '加载邀请配置失败')
  } finally {
    loading.value = false
  }
}

async function handleSavePoster() {
  const valid = await validateForm(posterFormRef)
  if (!valid) return
  savingPoster.value = true
  try {
    await saveInvitePosterConfigApi({ ...posterForm })
    showSuccess('邀请海报已保存并刷新缓存')
    await loadPosterConfig()
  } catch (error) {
    showError(error?.msg || error?.message || '保存失败')
  } finally {
    savingPoster.value = false
  }
}

async function handleSaveCommission() {
  const valid = await validateForm(commissionFormRef)
  if (!valid) return
  if (commissionForm.level1Percent < 0 || commissionForm.level2Percent < 0) {
    showError('比例不能为负数')
    return
  }
  savingCommission.value = true
  try {
    await saveReferralCommissionConfig({
      level1Percent: commissionForm.level1Percent,
      level2Percent: commissionForm.level2Percent,
    })
    showSuccess('分销比例已保存并刷新缓存')
    await loadCommissionConfig()
  } catch (error) {
    showError(error?.msg || error?.message || '保存失败')
  } finally {
    savingCommission.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="invite-config-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <strong>邀请配置</strong>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="config-tip"
        title="一二级分销说明"
      >
        <p>本项目采用<strong>二级分销</strong>模型：用户 A 邀请 B 为一级（直推），B 邀请 C 时 A 获得二级（间推）佣金。</p>
        <p>一级比例为直推佣金，二级比例为间推佣金。保存后立即生效，并刷新系统配置缓存。</p>
        <p>
          若存在旧配置键 <code>{{ COMMISSION_RATE_LEGACY_KEY }}</code>
          <template v-if="legacyPercent !== null">（当前 {{ legacyPercent }}%）</template>，
          仅在一级比例未配置时作为兜底；请优先维护下方「一级比例」。
        </p>
      </el-alert>

      <el-form
        ref="commissionFormRef"
        :model="commissionForm"
        :rules="commissionRules"
        label-width="120px"
        class="config-form"
      >
        <el-form-item label="一级比例" prop="level1Percent">
          <el-input-number v-model="commissionForm.level1Percent" :min="0" :max="100" :precision="2" :step="0.5" />
          <span class="field-suffix">%（直推）</span>
        </el-form-item>
        <el-form-item label="二级比例" prop="level2Percent">
          <el-input-number v-model="commissionForm.level2Percent" :min="0" :max="100" :precision="2" :step="0.5" />
          <span class="field-suffix">%（间推）</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingCommission" @click="handleSaveCommission">保存分销比例</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="page-card">
      <template #header>
        <strong>邀请海报</strong>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="config-tip"
        title="海报说明"
      >
        <p>对应用户端 /packageUser/invite。海报图作为全屏背景，底部展示用户头像、昵称、邀请码与二维码。</p>
        <p>好友注册成功后消费，邀请人获得<strong>分销佣金</strong>（非优惠券），比例由上方分销配置维护。</p>
      </el-alert>

      <el-form ref="posterFormRef" :model="posterForm" :rules="posterRules" label-width="140px" class="config-form">
        <el-form-item label="邀请海报图" prop="posterImage">
          <ImageUpload v-model="posterForm.posterImage" tip="建议 750×1000，作为海报背景" />
        </el-form-item>

        <div v-if="posterForm.posterImage" class="preview-wrap">
          <div class="preview-label">背景预览</div>
          <div class="preview-shell">
            <img class="preview-image" :src="posterForm.posterImage" alt="邀请海报预览">
            <div class="preview-footer">
              <div class="preview-avatar"></div>
              <div class="preview-info">
                <div class="preview-name">用户昵称</div>
                <div class="preview-code">邀请码：XXXXXX</div>
              </div>
              <div class="preview-qr"></div>
            </div>
          </div>
        </div>

        <el-form-item label="品牌名称" prop="brandName">
          <el-input v-model="posterForm.brandName" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="邀请人话术">
          <el-input v-model="posterForm.inviterText" maxlength="20" placeholder="如：邀你一起露营（用于复制文案）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPoster" @click="handleSavePoster">保存海报配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.invite-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-tip {
  margin-bottom: 20px;
}

.config-tip p {
  margin: 4px 0;
  line-height: 1.6;
}

.config-form {
  max-width: 640px;
}

.field-suffix {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}

.preview-wrap {
  margin: 0 0 24px 140px;
}

.preview-label {
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.preview-shell {
  position: relative;
  width: 100%;
  max-width: 280px;
  aspect-ratio: 690 / 980;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  background: #f6f8f3;
}

.preview-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-footer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #ffffff;
}

.preview-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #eefadb;
  flex-shrink: 0;
}

.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-size: 12px;
  font-weight: 700;
  color: #1f2937;
}

.preview-code {
  margin-top: 4px;
  font-size: 10px;
  color: #6b7280;
}

.preview-qr {
  width: 40px;
  height: 40px;
  background: repeating-conic-gradient(#111 0% 25%, #fff 0% 50%) 50% / 8px 8px;
  flex-shrink: 0;
}
</style>
