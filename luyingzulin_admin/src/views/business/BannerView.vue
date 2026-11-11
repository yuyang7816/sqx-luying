<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import CrudPage from '@/components/common/CrudPage.vue'
import ImageUpload from '@/components/common/ImageUpload.vue'
import { bannerApi, updateBannerStateApi } from '@/api/modules/banner'
import { getRentCategoryTree } from '@/api/rental/category'
import { getRentGoodsPage } from '@/api/rental/goods'
import {
  BANNER_JUMP_TYPES,
  composeBannerJump,
  parseBannerJump,
} from '@/utils/bannerJump'
import StatusSwitch from '@/components/common/StatusSwitch.vue'
import { showError, showSuccess } from '@/utils/modal'

const crudRef = ref()
const categoryOptions = ref([])
const goodsOptions = ref([])
const goodsLoading = ref(false)

const filters = reactive({
  name: '',
  classify: null,
  state: null,
})

function preparePayload(raw = {}) {
  const payload = { ...raw }
  payload.url = composeBannerJump(payload.jumpType, payload.jumpTarget)
  payload.description = payload.remark ?? payload.description ?? ''
  delete payload.jumpType
  delete payload.jumpTarget
  delete payload.remark
  return payload
}

const bannerCrudApi = {
  ...bannerApi,
  async info(id) {
    const data = await bannerApi.info(id)
    const parsed = parseBannerJump(data?.url)
    return {
      ...data,
      jumpType: parsed.jumpType || 'goods',
      jumpTarget: parsed.jumpTarget || '',
      remark: data?.description || '',
    }
  },
  save(data) {
    return bannerApi.save(preparePayload(data))
  },
  update(data) {
    return bannerApi.update(preparePayload(data))
  },
}

const columns = [
  { prop: 'name', label: '名称', minWidth: 140 },
  {
    prop: 'imageUrl',
    label: '图片',
    width: 120,
  },
  {
    prop: 'jumpTarget',
    label: '跳转目标',
    minWidth: 160,
    formatter: (row) => parseBannerJump(row.url).jumpTarget || '-',
  },
  {
    prop: 'classify',
    label: '展示位',
    width: 100,
    formatter: (_row, _col, value) => (value === 2 ? '金刚区' : 'Banner图'),
  },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'state', label: '状态', width: 80 },
]

function flattenCategories(nodes = [], bucket = []) {
  nodes.forEach((node) => {
    bucket.push({ id: node.id, name: node.name })
    if (node.children?.length) flattenCategories(node.children, bucket)
  })
  return bucket
}

async function loadCategoryOptions() {
  try {
    const tree = await getRentCategoryTree()
    categoryOptions.value = flattenCategories(Array.isArray(tree) ? tree : [])
  } catch {
    categoryOptions.value = []
  }
}

async function searchGoods(keyword = '') {
  goodsLoading.value = true
  try {
    const page = await getRentGoodsPage({
      page: 1,
      limit: 20,
      name: keyword || undefined,
    })
    goodsOptions.value = page.list || []
  } catch {
    goodsOptions.value = []
  } finally {
    goodsLoading.value = false
  }
}

function reloadList() {
  crudRef.value?.loadData({
    name: filters.name || undefined,
    classify: filters.classify ?? undefined,
    state: filters.state ?? undefined,
  })
}

function resetFilters() {
  filters.name = ''
  filters.classify = null
  filters.state = null
  reloadList()
}

async function handleBannerStateChange(row, nextState) {
  try {
    await updateBannerStateApi({ id: row.id, state: nextState })
    row.state = nextState
    showSuccess('状态已更新')
  } catch (error) {
    showError(error?.msg || error?.message || '状态更新失败')
  }
}

const createDefaults = computed(() => ({
  classify: 1,
  sort: 1,
  state: 1,
  jumpType: 'goods',
  jumpTarget: '',
  remark: '',
}))

onMounted(() => {
  loadCategoryOptions()
  searchGoods()
})
</script>

<template>
  <CrudPage
    ref="crudRef"
    title="首页配置"
    :api="bannerCrudApi"
    :columns="columns"
    :create-defaults="createDefaults"
  >
    <template #toolbar>
      <el-form inline @submit.prevent="reloadList">
        <el-form-item label="名称">
          <el-input v-model="filters.name" clearable placeholder="轮播图名称" @keyup.enter="reloadList" />
        </el-form-item>
        <el-form-item label="展示位">
          <el-select v-model="filters.classify" clearable placeholder="全部" style="width: 130px">
            <el-option label="Banner图" :value="1" />
            <el-option label="金刚区" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.state" clearable placeholder="全部" style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="隐藏" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reloadList">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </template>

    <template #col-imageUrl="{ row }">
      <el-image
        v-if="row.imageUrl"
        :src="row.imageUrl"
        :preview-src-list="[row.imageUrl]"
        fit="cover"
        style="width: 88px; height: 44px; border-radius: 4px"
      />
      <span v-else>-</span>
    </template>

    <template #col-state="{ row }">
      <StatusSwitch
        :model-value="row.state"
        :active-value="1"
        :inactive-value="2"
        @change="(value) => handleBannerStateChange(row, value)"
      />
    </template>

    <template #form="{ form, editingId }">
      <el-form-item label="名称" prop="name" :rules="[{ required: true, message: '请输入名称' }]">
        <el-input v-model="form.name" placeholder="运营识别名称" />
      </el-form-item>

      <el-form-item label="图片" prop="imageUrl" :rules="[{ required: true, message: '请上传图片' }]">
        <ImageUpload v-model="form.imageUrl" tip="建议 750×360，支持 jpg/png，≤5MB" />
      </el-form-item>

      <el-form-item label="跳转类型" prop="jumpType" :rules="[{ required: true, message: '请选择跳转类型' }]">
        <el-select v-model="form.jumpType" placeholder="选择跳转类型" @change="form.jumpTarget = ''">
          <el-option v-for="item in BANNER_JUMP_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item
        v-if="form.jumpType === 'goods'"
        label="商品"
        prop="jumpTarget"
        :rules="[{ required: true, message: '请选择商品' }]"
      >
        <el-select
          v-model="form.jumpTarget"
          filterable
          remote
          reserve-keyword
          placeholder="按商品名称搜索"
          :remote-method="searchGoods"
          :loading="goodsLoading"
          style="width: 100%"
        >
          <el-option
            v-for="item in goodsOptions"
            :key="item.id"
            :label="item.name"
            :value="String(item.id)"
          />
        </el-select>
      </el-form-item>

      <el-form-item
        v-else-if="form.jumpType === 'category'"
        label="分类"
        prop="jumpTarget"
        :rules="[{ required: true, message: '请选择分类' }]"
      >
        <el-select v-model="form.jumpTarget" filterable placeholder="选择租赁分类" style="width: 100%">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="String(item.id)"
          />
        </el-select>
      </el-form-item>

      <el-form-item
        v-else-if="form.jumpType === 'link' || form.jumpType === 'h5'"
        label="链接地址"
        prop="jumpTarget"
        :rules="[{ required: true, message: '请输入链接地址' }]"
      >
        <el-input v-model="form.jumpTarget" placeholder="https://..." />
      </el-form-item>

      <el-form-item label="展示位">
        <el-select v-model="form.classify">
          <el-option label="Banner图（首页轮播）" :value="1" />
          <el-option label="金刚区（快捷入口）" :value="2" />
        </el-select>
      </el-form-item>

      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" />
      </el-form-item>

      <el-form-item label="状态">
        <div class="status-switch-row">
          <StatusSwitch
            :model-value="form.state"
            :active-value="1"
            :inactive-value="2"
            @change="(value) => { form.state = value }"
          />
          <span class="status-switch-row__text">{{ Number(form.state) === 1 ? '正常展示' : '隐藏不展示' }}</span>
        </div>
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="运营备注，不对 C 端展示" />
      </el-form-item>

      <el-form-item v-if="editingId" label="跳转预览">
        <el-text type="info">{{ composeBannerJump(form.jumpType, form.jumpTarget) || '未配置' }}</el-text>
      </el-form-item>
    </template>
  </CrudPage>
</template>

<style scoped>
.status-switch-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-switch-row__text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
