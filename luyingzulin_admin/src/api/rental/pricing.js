import {
  getSystemConfigListApi,
  refreshSystemConfigApi,
  saveSystemConfigApi,
  updateSystemConfigApi,
} from '@/api/modules/system-config'

/** 租赁定价配置分组（系统配置 paramType） */
export const RENTAL_CONFIG_TYPE = 'rental'

/** 同城配送基础费用配置键（与 server 约定，单位：元） */
export const DELIVERY_BASE_FEE_KEY = 'rental.delivery.base_fee'

const DELIVERY_FEE_DEFAULT = '15.00'

function findConfigInGroups(groups, paramKey) {
  for (const group of groups || []) {
    const list = group.sysConfigEntityList || group.list || []
    const hit = list.find((item) => item.paramKey === paramKey)
    if (hit) return hit
  }
  return null
}

/** 读取配送费规则配置 */
export async function getDeliveryFeeConfig() {
  const groups = await getSystemConfigListApi()
  const rentalGroup = (groups || []).find((g) => g.paramType === RENTAL_CONFIG_TYPE)
  const entity = findConfigInGroups(groups, DELIVERY_BASE_FEE_KEY)
  return {
    paramType: RENTAL_CONFIG_TYPE,
    paramTypeDesc: rentalGroup?.paramTypeDesc || '租赁业务',
    id: entity?.id ?? null,
    paramKey: DELIVERY_BASE_FEE_KEY,
    paramValue: entity?.paramValue ?? DELIVERY_FEE_DEFAULT,
    status: entity?.status ?? '1',
    remark: entity?.remark ?? '同城配送基础费用（元），选择配送取货方式时计入订单',
  }
}

/** 保存配送费规则并刷新缓存 */
export async function saveDeliveryFeeConfig({ id, paramValue, status = '1', remark }) {
  const payload = {
    id,
    paramKey: DELIVERY_BASE_FEE_KEY,
    paramValue: String(paramValue),
    paramType: RENTAL_CONFIG_TYPE,
    paramTypeDesc: '租赁业务',
    status,
    remark: remark || '同城配送基础费用（元），选择配送取货方式时计入订单',
  }
  if (id) {
    await updateSystemConfigApi(payload)
  } else {
    await saveSystemConfigApi(payload)
  }
  await refreshSystemConfigApi()
  return getDeliveryFeeConfig()
}
