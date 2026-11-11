/** 用户端客服配置默认值（与 uniapp common/serviceDefaults.js 对齐） */
export const SERVICE_CHANNEL_KEYS = ['onlineIm', 'wechat', 'phone', 'wecom']

export const DEFAULT_SERVICE_CONFIG = {
  activeChannel: 'wechat',
  channels: {
    onlineIm: {
      enabled: false,
      title: '在线聊天 IM',
      desc: '适合咨询下单、续租、归还、售后问题，由平台官方客服接待。',
      actionText: '立即会话',
    },
    wechat: {
      enabled: true,
      title: '添加客服微信',
      wechatId: 'LYZL-CAMP',
      qrCode: '',
      desc: '可复制微信号添加，也可直接扫码联系。',
    },
    phone: {
      enabled: false,
      title: '电话客服',
      phoneNumber: '400-000-0000',
      serviceHours: '08:30-20:30',
      desc: '适合紧急改期、设备异常、取还货协调。',
    },
    wecom: {
      enabled: false,
      title: '企业微信客服',
      corpName: '露营设备租赁企业客服',
      corpLink: 'https://work.weixin.qq.com/',
      desc: '面向企业团建和长期客户。',
    },
  },
}

/** 邀请海报配置默认值（与 uniapp common/auth.js 对齐） */
export const DEFAULT_INVITE_POSTER_CONFIG = {
  brandName: '露营装备租赁',
  inviterText: '邀你一起露营',
  posterImage: '',
}

export const SERVICE_CONFIG_KEY = 'app_service_channels_json'
export const INVITE_POSTER_CONFIG_KEY = 'app_invite_poster_json'
export const APP_CONFIG_TYPE = 'app'
