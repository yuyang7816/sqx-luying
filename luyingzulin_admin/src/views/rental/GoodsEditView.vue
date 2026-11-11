<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import ImageUpload from '@/components/common/ImageUpload.vue'
import VideoUpload from '@/components/common/VideoUpload.vue'
import ImagesUpload from '@/components/common/ImagesUpload.vue'
import { getRentCategoryTree } from '@/api/rental/category'
import { getRentGoodsDetail, saveRentGoods, updateRentGoods } from '@/api/rental/goods'
import { flattenRentCategories } from '@/utils/rentalCategory'
import { getMerchantOptions } from '@/api/merchant/merchant'
import { useAuthStore } from '@/stores/auth'
import { validateForm } from '@/utils/form'
import { GOODS_FORM_RULES } from '@/views/rental/goodsFormRules'
import { createEmptyGoodsForm, detailToForm, formToPayload } from '@/views/rental/goodsFormUtils'
import { showError, showSuccess } from '@/utils/modal'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const categoryOptions = ref([])
const merchantOptions = ref([])

const goodsId = computed(() => {
  const raw = route.params.id
  return raw ? Number(raw) : null
})

const isEdit = computed(() => Boolean(goodsId.value))

const form = reactive(createEmptyGoodsForm())
const rules = GOODS_FORM_RULES

async function loadMerchantOptions() {
  try {
    merchantOptions.value = await getMerchantOptions()
  } catch {
    merchantOptions.value = []
  }
}

async function loadCategoryOptions() {
  try {
    const tree = await getRentCategoryTree()
    categoryOptions.value = flattenRentCategories(Array.isArray(tree) ? tree : [])
  } catch {
    categoryOptions.value = []
  }
}

async function loadDetail() {
  if (!goodsId.value) return
  loading.value = true
  try {
    const detail = await getRentGoodsDetail(goodsId.value)
    Object.assign(form, createEmptyGoodsForm(), detailToForm(detail))
  } catch (error) {
    showError(error?.msg || error?.message || '加载商品详情失败')
    router.replace({ name: 'rental-goods' })
  } finally {
    loading.value = false
  }
}

function addParamRow() {
  form.params.push({ paramName: '', paramValue: '', sort: form.params.length + 1 })
}

function removeParamRow(index) {
  form.params.splice(index, 1)
}

function addPackageRow() {
  form.packages.push({ packageName: '', packageDesc: '', price: 0, sort: form.packages.length + 1 })
}

function removePackageRow(index) {
  form.packages.splice(index, 1)
}

async function handleSubmit() {
  const valid = await validateForm(formRef)
  if (!valid) return

  saving.value = true
  try {
    const payload = formToPayload(form, { includeStatus: !isEdit.value })
    if (isEdit.value) {
      await updateRentGoods({ ...payload, id: goodsId.value })
      showSuccess('商品已更新')
    } else {
      await saveRentGoods(payload)
      showSuccess('商品已创建')
    }
    router.push({ name: 'rental-goods' })
  } catch (error) {
    showError(error?.msg || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push({ name: 'rental-goods' })
}

onMounted(async () => {
  if (!isEdit.value && !authStore.canSuperviseCreateGoods) {
    showError('无监管新建商品权限')
    router.replace({ name: 'rental-goods' })
    return
  }
  await Promise.all([loadCategoryOptions(), loadMerchantOptions()])
  if (isEdit.value) {
    await loadDetail()
  }
})
</script>

<template>
  <el-card v-loading="loading" shadow="never" class="page-card goods-edit">
    <template #header>
      <div class="card-header">
        <div class="card-header__title">
          <strong>{{ isEdit ? '编辑商品' : '新增商品' }}</strong>
          <p class="card-header__desc">按区块依次填写，带 <span class="required-mark">*</span> 为必填项</p>
        </div>
        <el-space>
          <el-button @click="handleCancel">返回列表</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
        </el-space>
      </div>
    </template>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="fill-guide"
      title="填写顺序建议"
    >
      <ol class="fill-guide__list">
        <li>选择<strong>所属分类</strong>与<strong>关联商户</strong>（监管新建时须指定商户）</li>
        <li>填写<strong>商品名称</strong>、<strong>日租价</strong>与<strong>押金</strong></li>
        <li>上传<strong>封面图</strong>（列表与详情首图）</li>
        <li>上传<strong>使用教程视频</strong>（订单核销后通过 IM 自动发送给用户）</li>
        <li>按需补充商品参数、配件套餐、轮播图与详情描述</li>
      </ol>
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="goods-form">
      <section class="form-section">
        <header class="form-section__head">
          <h3 class="form-section__title">基本信息</h3>
          <p class="form-section__desc">商品展示与归属信息</p>
        </header>
        <el-row :gutter="24">
          <el-col :xs="24" :sm="12">
            <el-form-item label="所属分类" prop="categoryId" required>
              <el-select v-model="form.categoryId" filterable placeholder="请选择分类" class="full-width">
                <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="关联商户">
              <el-select v-model="form.merchantId" clearable filterable placeholder="请选择商户" class="full-width">
                <el-option
                  v-for="item in merchantOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <div class="field-tip">商家发布时会自动绑定；监管新建时请指定所属商户</div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="商品名称" prop="name" required>
              <el-input v-model="form.name" placeholder="如：双人自动帐篷" maxlength="80" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="副标题">
              <el-input v-model="form.subtitle" placeholder="简短卖点，选填" maxlength="120" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="标签">
              <el-select
                v-model="form.tags"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入后回车添加"
                class="full-width"
              />
              <div class="field-tip">如「免押金」「新手友好」，选填</div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :step="1" controls-position="right" class="number-field" />
              <div class="field-tip">数值越大越靠前，默认 0</div>
            </el-form-item>
          </el-col>
          <el-col v-if="!isEdit" :xs="24" :sm="12">
            <el-form-item label="上架状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">上架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </section>

      <section class="form-section">
        <header class="form-section__head">
          <h3 class="form-section__title">租赁定价</h3>
          <p class="form-section__desc">价格与库存，日租价与押金为必填</p>
        </header>
        <el-row :gutter="24">
          <el-col :xs="24" :sm="8">
            <el-form-item label="日租价" prop="rentPriceDay" required>
              <el-input-number
                v-model="form.rentPriceDay"
                :min="0"
                :precision="2"
                :step="1"
                controls-position="right"
                class="full-width"
              />
              <div class="field-tip">单位：元/天</div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="时租价">
              <el-input-number
                v-model="form.rentPriceHour"
                :min="0"
                :precision="2"
                :step="1"
                controls-position="right"
                class="full-width"
              />
              <div class="field-tip">单位：元/小时，选填</div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="过夜租价">
              <el-input-number
                v-model="form.rentPriceOvernight"
                :min="0"
                :precision="2"
                :step="1"
                controls-position="right"
                class="full-width"
              />
              <div class="field-tip">单位：元/晚，选填</div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="押金" prop="depositAmount" required>
              <el-input-number
                v-model="form.depositAmount"
                :min="0"
                :precision="2"
                :step="1"
                controls-position="right"
                class="full-width"
              />
              <div class="field-tip">单位：元</div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="总库存">
              <el-input-number
                v-model="form.totalStock"
                :min="0"
                :step="1"
                controls-position="right"
                class="full-width"
              />
              <div class="field-tip">可租数量，默认 0</div>
            </el-form-item>
          </el-col>
        </el-row>
      </section>

      <section class="form-section">
        <header class="form-section__head">
          <h3 class="form-section__title">商品图片与教程</h3>
          <p class="form-section__desc">封面图为必填；教程视频选填，用户取货核销后自动通过 IM 发送</p>
        </header>
        <el-row :gutter="24">
          <el-col :xs="24" :md="10">
            <el-form-item label="封面图" prop="coverImage" required label-position="top">
              <ImageUpload v-model="form.coverImage" tip="列表与详情首图，建议 750×750" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="14">
            <el-form-item label="轮播图" label-position="top">
              <ImagesUpload v-model="form.images" tip="详情页轮播，最多 9 张，选填" />
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="使用教程视频" label-position="top">
              <VideoUpload
                v-model="form.tutorialVideoUrl"
                tip="支持 mp4 等常见格式，不超过 50MB；订单核销后自动推送给用户"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </section>

      <section class="form-section">
        <header class="form-section__head form-section__head--toolbar">
          <div>
            <h3 class="form-section__title">商品参数</h3>
            <p class="form-section__desc">如尺寸、材质等，选填，仅详情展示</p>
          </div>
          <el-button type="primary" plain @click="addParamRow">添加参数</el-button>
        </header>
        <el-table :data="form.params" border empty-text="暂无参数，点击右上角「添加参数」">
          <el-table-column label="参数名" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.paramName" placeholder="如：尺寸" />
            </template>
          </el-table-column>
          <el-table-column label="参数值" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.paramValue" placeholder="如：210×140×110cm" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="88" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeParamRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="form-section">
        <header class="form-section__head form-section__head--toolbar">
          <div>
            <h3 class="form-section__title">配件套餐</h3>
            <p class="form-section__desc">可选加价配件包，选填</p>
          </div>
          <el-button type="primary" plain @click="addPackageRow">添加套餐</el-button>
        </header>
        <el-table :data="form.packages" border empty-text="暂无套餐，点击右上角「添加套餐」">
          <el-table-column label="套餐名称" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.packageName" placeholder="如：地钉防风绳加强包" />
            </template>
          </el-table-column>
          <el-table-column label="套餐说明" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.packageDesc" placeholder="套餐内容说明" />
            </template>
          </el-table-column>
          <el-table-column label="加价(元)" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :precision="2" controls-position="right" class="full-width" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="88" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removePackageRow($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="form-section">
        <header class="form-section__head">
          <h3 class="form-section__title">商品详情</h3>
          <p class="form-section__desc">图文详情描述，选填</p>
        </header>
        <el-form-item label-width="0">
          <QuillEditor v-model:content="form.detail" content-type="html" theme="snow" class="goods-detail-editor" />
        </el-form-item>
      </section>
    </el-form>

    <div class="footer-actions">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">保存商品</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.goods-edit :deep(.el-card__body) {
  padding-top: 20px;
}

.card-header__title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header__desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 400;
}

.required-mark {
  color: var(--el-color-danger);
}

.fill-guide {
  margin-bottom: 20px;
}

.fill-guide__list {
  margin: 4px 0 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.8;
}

.goods-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-section {
  padding: 20px 20px 4px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-fill-color-blank) 92%, var(--el-fill-color-light));
}

.form-section__head {
  margin-bottom: 16px;
}

.form-section__head--toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.form-section__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.form-section__desc {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.full-width {
  width: 100%;
}

.number-field {
  width: 160px;
}

.goods-form :deep(.el-input-number.full-width) {
  width: 100%;
}

.goods-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.goods-form :deep(.el-form-item__label) {
  font-weight: 500;
}

.field-tip {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.goods-detail-editor {
  width: 100%;
  min-height: 240px;
}

.goods-detail-editor :deep(.ql-toolbar) {
  border-radius: 8px 8px 0 0;
}

.goods-detail-editor :deep(.ql-container) {
  min-height: 200px;
  border-radius: 0 0 8px 8px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
