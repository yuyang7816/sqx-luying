<script setup>
import { ref } from 'vue'
import AppDialog from '@/components/common/AppDialog.vue'
import BasePagination from '@/components/common/BasePagination.vue'
import { useCrudPage } from '@/composables/useCrudPage'
import { validateForm } from '@/utils/form'

const props = defineProps({
  title: { type: String, required: true },
  api: { type: Object, required: true },
  columns: { type: Array, required: true },
  idKey: { type: String, default: 'id' },
  createDefaults: { type: Object, default: () => ({}) },
  showCreate: { type: Boolean, default: true },
  showDetail: { type: Boolean, default: false },
  detailWidth: { type: String, default: '720px' },
})

const formRef = ref()
const {
  loading,
  dialogVisible,
  dialogLoading,
  detailVisible,
  detailLoading,
  detailData,
  tableData,
  total,
  pageNum,
  pageSize,
  editingId,
  form,
  loadData,
  openCreate,
  openDetail,
  openEdit,
  submitForm,
  removeRow,
} = useCrudPage(props.api, { idKey: props.idKey })

function handleCreate() {
  openCreate({ ...props.createDefaults })
}

async function handleSubmit() {
  const valid = await validateForm(formRef)
  if (!valid) return
  await submitForm()
}

defineExpose({ loadData, openDetail })
</script>

<template>
  <el-card shadow="never" class="page-card">
    <template #header>
      <div class="card-header">
        <strong>{{ title }}</strong>
        <el-button v-if="showCreate" type="primary" @click="handleCreate">新增</el-button>
      </div>
    </template>
    <slot name="toolbar" :load-data="loadData" />
    <el-table v-loading="loading" :data="tableData" border>
      <el-table-column
        v-for="column in columns"
        :key="column.prop || column.label"
        v-bind="column"
      >
        <template v-if="$slots[`col-${column.prop}`]" #default="scope">
          <slot :name="`col-${column.prop}`" v-bind="scope" />
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="220" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <slot name="actions" :row="row" :open-detail="() => openDetail(row)" :open-edit="() => openEdit(row)" :remove-row="() => removeRow(row)" />
            <el-button v-if="showDetail" size="small" type="primary" @click="openDetail(row)">详情</el-button>
            <el-button size="small" type="warning" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeRow(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <BasePagination
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      @update:current-page="pageNum = $event; loadData()"
      @update:page-size="pageSize = $event; pageNum = 1; loadData()"
    />
    <AppDialog v-model="dialogVisible" :title="editingId ? `编辑${title}` : `新增${title}`" :loading="dialogLoading" @confirm="handleSubmit">
      <el-form ref="formRef" :model="form" label-width="110px">
        <slot name="form" :form="form" :editing-id="editingId" />
      </el-form>
    </AppDialog>
    <el-dialog v-if="showDetail" v-model="detailVisible" :title="`${title}详情`" :width="detailWidth" destroy-on-close>
      <div v-loading="detailLoading">
        <slot v-if="detailData" name="detail" :data="detailData" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
