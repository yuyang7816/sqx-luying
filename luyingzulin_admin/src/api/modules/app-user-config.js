import {
  getSystemConfigListApi,
  refreshSystemConfigApi,
  saveSystemConfigApi,
  updateSystemConfigApi,
} from '@/api/modules/system-config'
import {
  APP_CONFIG_TYPE,
  DEFAULT_INVITE_POSTER_CONFIG,
  DEFAULT_SERVICE_CONFIG,
  INVITE_POSTER_CONFIG_KEY,
  SERVICE_CHANNEL_KEYS,
  SERVICE_CONFIG_KEY,
} from '@/constants/appUserConfig'

function findConfigInGroups(groups, paramKey) {
  for (const group of groups || []) {
    const list = group.sysConfigEntityList || group.list || []
    const hit = list.find((item) => item.paramKey === paramKey)
    if (hit) return hit
  }
  return null
}

function parseJsonValue(raw, fallback) {
  if (!raw) return JSON.parse(JSON.stringify(fallback))
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return parsed && typeof parsed === 'object' ? parsed : JSON.parse(JSON.stringify(fallback))
  } catch {
    return JSON.parse(JSON.stringify(fallback))
  }
}

function resolveActiveChannel(raw = {}, channels = {}) {
  if (raw.activeChannel && SERVICE_CHANNEL_KEYS.includes(raw.activeChannel)) {
    return raw.activeChannel
  }
  const enabledKey = SERVICE_CHANNEL_KEYS.find((key) => channels[key]?.enabled)
  return enabledKey || DEFAULT_SERVICE_CONFIG.activeChannel
}

function applyExclusiveChannels(channels = {}, activeChannel = DEFAULT_SERVICE_CONFIG.activeChannel) {
  const next = JSON.parse(JSON.stringify(channels))
  SERVICE_CHANNEL_KEYS.forEach((key) => {
    if (next[key]) {
      next[key].enabled = key === activeChannel
    }
  })
  return next
}

function mergeServiceConfig(raw = {}) {
  const defaults = JSON.parse(JSON.stringify(DEFAULT_SERVICE_CONFIG))
  const channels = { ...defaults.channels }
  Object.keys(defaults.channels).forEach((key) => {
    const item = raw.channels?.[key] || {}
    const fallback = defaults.channels[key]
    if (key === 'phone') {
      channels[key] = {
        ...fallback,
        ...item,
        phoneNumber: item.phoneNumber || item.value || fallback.phoneNumber,
        title: item.title || item.label || fallback.title,
      }
      return
    }
    if (key === 'wechat') {
      channels[key] = {
        ...fallback,
        ...item,
        wechatId: item.wechatId || item.value || fallback.wechatId,
        title: item.title || item.label || fallback.title,
      }
      return
    }
    channels[key] = { ...fallback, ...item }
  })
  const activeChannel = resolveActiveChannel(raw, channels)
  return {
    activeChannel,
    channels: applyExclusiveChannels(channels, activeChannel),
  }
}

function mergeInvitePosterConfig(raw = {}) {
  const merged = { ...DEFAULT_INVITE_POSTER_CONFIG, ...raw }
  return {
    brandName: merged.brandName || DEFAULT_INVITE_POSTER_CONFIG.brandName,
    inviterText: merged.inviterText || DEFAULT_INVITE_POSTER_CONFIG.inviterText,
    posterImage: merged.posterImage || '',
  }
}

async function upsertJsonConfig({ id, paramKey, value, remark }) {
  const payload = {
    id,
    paramKey,
    paramValue: JSON.stringify(value),
    paramType: APP_CONFIG_TYPE,
    paramTypeDesc: '用户端配置',
    status: '1',
    remark,
  }
  if (id) {
    await updateSystemConfigApi(payload)
  } else {
    await saveSystemConfigApi(payload)
  }
}

/** 读取客服中心配置 */
export async function getServiceConfigApi() {
  const groups = await getSystemConfigListApi()
  const entity = findConfigInGroups(groups, SERVICE_CONFIG_KEY)
  return {
    id: entity?.id ?? null,
    paramKey: SERVICE_CONFIG_KEY,
    config: mergeServiceConfig(parseJsonValue(entity?.paramValue, DEFAULT_SERVICE_CONFIG)),
  }
}

/** 保存客服中心配置 */
export async function saveServiceConfigApi(config) {
  const current = await getServiceConfigApi()
  const merged = mergeServiceConfig(config)
  await upsertJsonConfig({
    id: current.id,
    paramKey: SERVICE_CONFIG_KEY,
    value: merged,
    remark: '用户端客服中心：联系渠道（四选一）',
  })
  await refreshSystemConfigApi()
  return getServiceConfigApi()
}

/** 读取邀请海报配置 */
export async function getInvitePosterConfigApi() {
  const groups = await getSystemConfigListApi()
  const entity = findConfigInGroups(groups, INVITE_POSTER_CONFIG_KEY)
  return {
    id: entity?.id ?? null,
    paramKey: INVITE_POSTER_CONFIG_KEY,
    config: mergeInvitePosterConfig(parseJsonValue(entity?.paramValue, DEFAULT_INVITE_POSTER_CONFIG)),
  }
}

/** 保存邀请海报配置 */
export async function saveInvitePosterConfigApi(config) {
  const current = await getInvitePosterConfigApi()
  await upsertJsonConfig({
    id: current.id,
    paramKey: INVITE_POSTER_CONFIG_KEY,
    value: mergeInvitePosterConfig(config),
    remark: '用户端邀请海报背景与分享文案',
  })
  await refreshSystemConfigApi()
  return getInvitePosterConfigApi()
}
