/** 客服配置默认值（管理端未配置时的兜底展示，非 Mock 业务数据） */
const defaultServiceConfig = {
	activeChannel: 'wechat',
	channels: {
		onlineIm: {
			enabled: false,
			title: '在线聊天 IM',
			desc: '适合咨询下单、续租、归还、售后问题，由平台官方客服接待。',
			actionText: '立即会话'
		},
		wechat: {
			enabled: true,
			title: '添加客服微信',
			wechatId: 'LYZL-CAMP',
			qrCode: '',
			desc: '可复制微信号添加，也可直接扫码联系。'
		},
		phone: {
			enabled: false,
			title: '电话客服',
			phoneNumber: '400-000-0000',
			serviceHours: '08:30-20:30',
			desc: '适合紧急改期、设备异常、取还货协调。'
		},
		wecom: {
			enabled: false,
			title: '企业微信客服',
			corpName: '露营设备租赁企业客服',
			corpLink: 'https://work.weixin.qq.com/',
			desc: '面向企业团建和长期客户。'
		}
	}
};

export const getServiceConfig = () => JSON.parse(JSON.stringify(defaultServiceConfig));

export const getActiveServiceChannel = (config = defaultServiceConfig) => {
	const activeKey = config.activeChannel
		|| Object.entries(config.channels || {}).find(([, item]) => item.enabled)?.[0]
		|| 'wechat';
	const channel = config.channels?.[activeKey];
	if (!channel) return null;
	return { key: activeKey, ...channel };
};

export const getVisibleServiceChannels = () => {
	const channel = getActiveServiceChannel(defaultServiceConfig);
	return channel ? [channel] : [];
};

export const SERVICE_CENTER_PAGE = '/packageUser/service';
export const PLATFORM_IM_CHAT_PAGE = '/packageUser/platform-im-chat';

/** 根据当前客服配置解析「客服中心」入口应跳转的页面 */
export const resolveServiceCenterUrl = (config = defaultServiceConfig) => {
	const action = resolveServiceCenterAction(config);
	if (action.type === 'navigate') {
		return action.url;
	}
	return SERVICE_CENTER_PAGE;
};

/** 根据当前客服配置解析「客服中心」入口行为（跳转或直拨电话） */
export const resolveServiceCenterAction = (config = defaultServiceConfig) => {
	const channel = getActiveServiceChannel(config);
	if (channel?.key === 'onlineIm') {
		return { type: 'navigate', url: PLATFORM_IM_CHAT_PAGE, needLogin: true };
	}
	if (channel?.key === 'phone') {
		const phoneNumber = String(channel.phoneNumber || '').replace(/[\s-]/g, '');
		if (phoneNumber) {
			return { type: 'phone', phoneNumber };
		}
	}
	return { type: 'navigate', url: SERVICE_CENTER_PAGE, needLogin: false };
};
