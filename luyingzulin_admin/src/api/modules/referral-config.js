import {
  getSystemConfigListApi,
  refreshSystemConfigApi,
  saveSystemConfigApi,
  updateSystemConfigApi,
} from '@/api/modules/system-config'

/** 分销配置分组 */
export const REFERRAL_CONFIG_TYPE = 'biz'

export const COMMISSION_RATE_LEVEL1_KEY = 'biz_referral_commission_rate_level1'
export const COMMISSION_RATE_LEVEL2_KEY = 'biz_referral_commission_rate_level2'
export const COMMISSION_RATE_LEGACY_KEY = 'biz_referral_commission_rate'

const DEFAULT_LEVEL1 = '0.05'
const DEFAULT_LEVEL2 = '0.02'

function findConfigInGroups(groups, paramKey) {
  for (const group of groups || []) {
    const list = group.sysConfigEntityList || group.list || []
    const hit = list.find((item) => item.paramKey === paramKey)
    if (hit) return hit
  }
  return null
}

function percentFromRate(rateStr) {
  const num = Number(rateStr)
  if (Number.isNaN(num)) return 0
  return Number((num * 100).toFixed(2))
}

function rateFromPercent(percent) {
  return (Number(percent) / 100).toFixed(4)
}

/** 读取一/二级分销比例配置 */
export async function getReferralCommissionConfig() {
  const groups = await getSystemConfigListApi()
  const bizGroup = (groups || []).find((g) => g.paramType === REFERRAL_CONFIG_TYPE)
  const level1Entity = findConfigInGroups(groups, COMMISSION_RATE_LEVEL1_KEY)
  const level2Entity = findConfigInGroups(groups, COMMISSION_RATE_LEVEL2_KEY)
  const legacyEntity = findConfigInGroups(groups, COMMISSION_RATE_LEGACY_KEY)

  const level1Rate = level1Entity?.paramValue ?? legacyEntity?.paramValue ?? DEFAULT_LEVEL1
  const level2Rate = level2Entity?.paramValue ?? DEFAULT_LEVEL2

  return {
    paramType: REFERRAL_CONFIG_TYPE,
    paramTypeDesc: bizGroup?.paramTypeDesc || '业务配置',
    level1: {
      id: level1Entity?.id ?? null,
      paramKey: COMMISSION_RATE_LEVEL1_KEY,
      percent: percentFromRate(level1Rate),
      status: level1Entity?.status ?? '1',
      remark: level1Entity?.remark ?? '一级（直推）分销佣金比例',
    },
    level2: {
      id: level2Entity?.id ?? null,
      paramKey: COMMISSION_RATE_LEVEL2_KEY,
      percent: percentFromRate(level2Rate),
      status: level2Entity?.status ?? '1',
      remark: level2Entity?.remark ?? '二级（间推）分销佣金比例',
    },
    legacyKey: COMMISSION_RATE_LEGACY_KEY,
    legacyPercent: legacyEntity ? percentFromRate(legacyEntity.paramValue) : null,
  }
}

async function upsertConfig({ id, paramKey, percent, remark }) {
  const payload = {
    id,
    paramKey,
    paramValue: rateFromPercent(percent),
    paramType: REFERRAL_CONFIG_TYPE,
    paramTypeDesc: '业务配置',
    status: '1',
    remark,
  }
  if (id) {
    await updateSystemConfigApi(payload)
  } else {
    await saveSystemConfigApi(payload)
  }
}

/** 保存分销比例并刷新缓存 */
export async function saveReferralCommissionConfig({ level1Percent, level2Percent }) {
  const current = await getReferralCommissionConfig()
  await upsertConfig({
    id: current.level1.id,
    paramKey: COMMISSION_RATE_LEVEL1_KEY,
    percent: level1Percent,
    remark: '一级（直推）分销佣金比例，如 0.05 表示 5%',
  })
  await upsertConfig({
    id: current.level2.id,
    paramKey: COMMISSION_RATE_LEVEL2_KEY,
    percent: level2Percent,
    remark: '二级（间推）分销佣金比例，如 0.02 表示 2%',
  })
  await refreshSystemConfigApi()
  return getReferralCommissionConfig()
}
