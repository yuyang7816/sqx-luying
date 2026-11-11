import { post, postJson, resolveBusinessResponse, get } from '@/common/httpRequest';
import { getAuthPlatform, getAuthPlatformLabel } from '@/common/auth';
import { fetchWalletSummary, fetchWalletRechargePayStatus } from '@/common/api/wallet';
import { fetchPayStatus } from '@/common/api/order';
import { fetchDepositPayStatus } from '@/common/api/deposit';
import { fetchVipPayStatus } from '@/common/api/vip';
import { fetchMerchantSecurityDepositPayStatus } from '@/common/api/merchantWallet';
import { isOaOAuthEnabled } from '@/common/oaOAuthConfig';

const PENDING_PAY_STORAGE_KEY = 'camp-pending-pay';

const balanceMethod = (balance = 0) => ({
	key: 'balance',
	label: '余额支付',
	desc: `可用余额 ¥${Number(balance || 0).toFixed(2)}`,
	icon: 'red-packet'
});

const baseMethods = {
	wechatMini: { key: 'wechat-mini', label: '微信支付', desc: '', icon: 'weixin-fill' },
	wechatJsapi: { key: 'wechat-jsapi', label: '微信支付', desc: '', icon: 'weixin-fill' },
	alipayH5: { key: 'alipay-h5', label: '支付宝支付', desc: '', icon: 'zhifubao' },
	wechatApp: { key: 'wechat-app', label: '微信支付', desc: '', icon: 'weixin-fill' },
	alipayApp: { key: 'alipay-app', label: '支付宝支付', desc: '', icon: 'zhifubao' }
};

/** 提交支付宝 H5 表单 */
const submitAlipayForm = (html) => {
	// #ifdef H5
	const payWindow = window.open('', '_self');
	if (payWindow) {
		payWindow.document.write(html);
		payWindow.document.close();
		return;
	}
	const div = document.createElement('div');
	div.innerHTML = html;
	div.style.display = 'none';
	document.body.appendChild(div);
	const form = div.querySelector('form');
	if (form) {
		form.submit();
		return;
	}
	throw new Error('支付宝支付表单无效');
	// #endif
	throw new Error('当前环境不支持支付宝 H5 支付');
};

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const isPaidStatus = (statusVo = {}) => (
	statusVo.paid === true
	|| statusVo.payStatus === 'success'
	|| statusVo.status === 'completed'
	|| statusVo.status === 'paid'
);

const payStatusFetchers = {
	wallet_recharge: (orderNo) => fetchWalletRechargePayStatus(orderNo),
	rent_order: (orderNo) => fetchPayStatus(orderNo),
	deposit_pay: (orderNo) => fetchDepositPayStatus(orderNo),
	vip: (orderNo) => fetchVipPayStatus(orderNo),
	merchant_deposit_pay: (orderNo) => fetchMerchantSecurityDepositPayStatus(orderNo)
};

/** 轮询支付结果（适用于支付宝 H5 回跳等异步确认场景） */
export const pollPaymentStatus = async ({
	businessType,
	orderNo,
	maxAttempts = 6,
	intervalMs = 1500
} = {}) => {
	const fetcher = payStatusFetchers[businessType];
	if (!fetcher || !orderNo) {
		return 'pending';
	}
	for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
		try {
			const statusVo = await fetcher(orderNo);
			if (isPaidStatus(statusVo)) {
				return 'success';
			}
		} catch (error) {
			// 忽略单次轮询失败
		}
		if (attempt < maxAttempts - 1) {
			await sleep(intervalMs);
		}
	}
	return 'pending';
};

export const savePendingPayment = ({ businessType, orderNo, scene } = {}) => {
	if (!businessType || !orderNo) return;
	uni.setStorageSync(PENDING_PAY_STORAGE_KEY, {
		businessType,
		orderNo,
		scene: scene || businessType,
		savedAt: Date.now()
	});
};

export const consumePendingPayment = () => {
	const pending = uni.getStorageSync(PENDING_PAY_STORAGE_KEY);
	if (!pending?.orderNo) {
		return null;
	}
	if (Date.now() - Number(pending.savedAt || 0) > 30 * 60 * 1000) {
		uni.removeStorageSync(PENDING_PAY_STORAGE_KEY);
		return null;
	}
	return pending;
};

export const clearPendingPayment = () => {
	uni.removeStorageSync(PENDING_PAY_STORAGE_KEY);
};

/** 检查支付通道是否已配置（需登录） */
export const fetchPayConfigCheck = () => get('app/base/pay/config-check').then(resolveBusinessResponse);

const paymentLabelMap = Object.values(baseMethods).reduce((map, item) => {
	map[item.key] = item.label;
	map.balance = '余额支付';
	return map;
}, {});

/** 支付方式 → 统一支付 tradeType */
const resolveTradeType = (methodKey = '') => {
	if (methodKey === 'balance') return 'BALANCE';
	if (methodKey.includes('jsapi') || methodKey.includes('mini')) return 'JSAPI';
	if (methodKey.includes('app')) return 'APP';
	return 'H5';
};

/** 支付方式 → 支付渠道（与后端 PayChannel 对齐：wx / ali） */
const resolvePayChannel = (methodKey = '') => {
	if (methodKey.includes('alipay')) return 'ali';
	if (methodKey === 'balance') return 'balance';
	return 'wx';
};

const getWxAppScene = () => {
	const platform = getAuthPlatform();
	if (platform === 'mp-weixin') return 'ma';
	if (platform === 'h5-wechat') return 'mp';
	if (platform === 'app') return 'app';
	return '';
};

const resolveOnlinePaymentMethods = () => {
	const platform = getAuthPlatform();
	if (platform === 'mp-weixin') {
		return [baseMethods.wechatMini];
	}
	if (platform === 'h5-wechat') {
		return isOaOAuthEnabled() ? [baseMethods.wechatJsapi] : [];
	}
	if (platform === 'app') {
		return [baseMethods.wechatApp, baseMethods.alipayApp];
	}
	if (platform === 'h5') {
		return [baseMethods.alipayH5];
	}
	return [baseMethods.alipayH5];
};

/** 获取在线支付方式（不含余额） */
export const getOnlinePaymentMethods = () => [...resolveOnlinePaymentMethods()];

export const getPaymentPlatform = getAuthPlatform;
export const getPaymentPlatformLabel = getAuthPlatformLabel;

/** 获取可用支付方式（含余额，余额>0 时展示） */
export const getAvailablePaymentMethods = async () => {
	const methods = [...resolveOnlinePaymentMethods()];
	try {
		const wallet = await fetchWalletSummary();
		if (Number(wallet?.balance || wallet?.amount || 0) > 0) {
			methods.unshift(balanceMethod(wallet.balance || wallet.amount));
		}
	} catch (error) {
		// 余额查询失败不影响在线支付
	}
	return methods;
};

export const getDefaultPaymentMethod = async () => {
	const methods = await getAvailablePaymentMethods();
	return methods[0]?.key || 'alipay-h5';
};

export const getPaymentMethodLabel = (methodKey) => paymentLabelMap[methodKey] || '在线支付';

const parsePayPayload = (payResult) => {
	if (!payResult) return null;
	if (typeof payResult === 'object') return payResult;
	if (typeof payResult === 'string') {
		const trimmed = payResult.trim();
		if (/^https?:\/\//.test(trimmed)) {
			return { mwebUrl: trimmed, payUrl: trimmed };
		}
		try {
			return JSON.parse(trimmed);
		} catch (error) {
			return { raw: trimmed };
		}
	}
	return null;
};

const normalizeJsapiParams = (payload = {}) => ({
	appId: payload.appId || payload.appid,
	timeStamp: String(payload.timeStamp || payload.timestamp || ''),
	nonceStr: payload.nonceStr || payload.noncestr || '',
	package: payload.packageValue || payload.package || (payload.prepayId ? `prepay_id=${payload.prepayId}` : ''),
	signType: payload.signType || 'MD5',
	paySign: payload.paySign || payload.sign || ''
});

/** H5 微信内置浏览器 JSAPI 支付 */
const invokeJsapiPay = (params) => new Promise((resolve, reject) => {
	const payRequest = {
		appId: params.appId,
		timeStamp: params.timeStamp,
		nonceStr: params.nonceStr,
		package: params.package,
		signType: params.signType,
		paySign: params.paySign
	};
	const onBridgeReady = () => {
		// eslint-disable-next-line no-undef
		WeixinJSBridge.invoke('getBrandWCPayRequest', payRequest, (res) => {
			if (res.err_msg === 'get_brand_wcpay_request:ok') {
				resolve({ result: 'success' });
				return;
			}
			if (res.err_msg === 'get_brand_wcpay_request:cancel') {
				reject(new Error('已取消支付'));
				return;
			}
			reject(new Error(res.err_msg || '微信支付失败'));
		});
	};
	// #ifdef H5
	if (typeof WeixinJSBridge === 'undefined') {
		document.addEventListener('WeixinJSBridgeReady', onBridgeReady, false);
	} else {
		onBridgeReady();
	}
	return;
	// #endif
	reject(new Error('当前环境不支持 JSAPI 支付'));
});

/** 小程序 / App uni.requestPayment */
const invokeUniPayment = (params, provider = 'wxpay') => new Promise((resolve, reject) => {
	uni.requestPayment({
		provider,
		timeStamp: params.timeStamp,
		nonceStr: params.nonceStr,
		package: params.package,
		signType: params.signType,
		paySign: params.paySign,
		orderInfo: params.orderInfo || params.raw,
		success: () => resolve({ result: 'success' }),
		fail: (error) => reject(new Error(error?.errMsg || '支付失败'))
	});
});

/** 根据后端返回拉起真实支付 */
export const invokeNativePayment = async ({ payResult, methodKey }) => {
	const payload = parsePayPayload(payResult);
	if (!payload) {
		throw new Error('支付参数为空');
	}

	const mwebUrl = payload.mwebUrl || payload.h5Url || payload.payUrl;
	if (mwebUrl && /^https?:\/\//.test(mwebUrl)) {
		// #ifdef H5
		window.location.href = mwebUrl;
		return { result: 'pending', payUrl: mwebUrl };
		// #endif
	}

	const jsapi = normalizeJsapiParams(payload);
	if (jsapi.appId && jsapi.paySign && jsapi.package) {
		const platform = getAuthPlatform();
		if (platform === 'h5-wechat') {
			return invokeJsapiPay(jsapi);
		}
		if (platform === 'mp-weixin') {
			return invokeUniPayment(jsapi, 'wxpay');
		}
		if (platform === 'app') {
			return invokeUniPayment({ ...jsapi, orderInfo: payload.raw || payResult }, 'wxpay');
		}
	}

	if (methodKey.includes('alipay')) {
		// #ifdef H5
		const html = payload.raw || (typeof payResult === 'string' ? payResult : '');
		if (html && html.includes('<form')) {
			submitAlipayForm(html);
			return { result: 'pending' };
		}
		if (mwebUrl) {
			window.location.href = mwebUrl;
			return { result: 'pending', payUrl: mwebUrl };
		}
		// #endif
	}

	return { result: 'pending', payData: payload };
};

const paymentSuccessRoutes = {
	rent_order: '/packageOrder/list',
	rent_renew: '/packageOrder/list',
	deposit_pay: '/packageContent/deposit',
	wallet_recharge: '/packageUser/wallet',
	vip: '/packageUser/vip',
	merchant_deposit_pay: '/packageMerchant/wallet-deposit'
};

const paymentSceneRoutes = {
	pay: '/packageOrder/list',
	renew: '/packageOrder/list',
	deposit: '/packageContent/deposit',
	wallet: '/packageUser/wallet',
	vip: '/packageUser/vip',
	'merchant-deposit': '/packageMerchant/wallet-deposit'
};

/** 支付完成后应跳转的目标页 */
export const resolvePaymentSuccessUrl = ({ businessType, scene } = {}) => (
	paymentSuccessRoutes[businessType] || paymentSceneRoutes[scene] || '/packageOrder/list'
);

/** 支付完成后跳转目标页（不再进入 pay-result） */
export const navigateAfterPayment = async ({ payRes, businessType, scene, orderNo } = {}) => {
	const url = resolvePaymentSuccessUrl({ businessType, scene });
	let result = payRes?.result || 'pending';
	const tradeNo = orderNo || payRes?.orderId || payRes?.tradeNo;

	if (result === 'pending' && tradeNo && payStatusFetchers[businessType]) {
		result = await pollPaymentStatus({ businessType, orderNo: tradeNo, maxAttempts: 4, intervalMs: 1200 });
		if (result === 'success') {
			clearPendingPayment();
		}
	}

	if (result === 'success') {
		clearPendingPayment();
		uni.showToast({ title: '支付成功', icon: 'success' });
	} else if (result === 'pending') {
		uni.showToast({ title: '支付确认中，请稍后刷新', icon: 'none' });
	}
	uni.redirectTo({
		url,
		fail: () => {
			uni.reLaunch({ url });
		}
	});
};

/** 构建 H5 外部支付回跳地址 */
const buildPayRedirectUri = (businessType = 'rent_order') => {
	const targetPath = resolvePaymentSuccessUrl({ businessType });
	// #ifdef H5
	if (typeof window !== 'undefined' && window.location) {
		return `${window.location.origin}/#${targetPath}`;
	}
	// #endif
	return targetPath;
};

const buildPaymentOrderMeta = ({ amount, orderId, methodKey }) => ({
	amount,
	orderId,
	methodKey,
	methodLabel: getPaymentMethodLabel(methodKey),
	platform: getPaymentPlatform(),
	platformLabel: getPaymentPlatformLabel(),
	tradeNo: String(orderId || '')
});

/** 发起统一支付（真实接口 + 多端拉起） */
export const launchPayment = async ({ amount, orderNo, methodKey, businessType = 'rent_order' }) => {
	const paymentOrder = buildPaymentOrderMeta({ amount, orderId: orderNo, methodKey });

	if (methodKey === 'balance') {
		await postJson('app/userWallet/payWithBalance', { orderNo, businessType }).then(resolveBusinessResponse);
		return {
			result: 'success',
			...paymentOrder
		};
	}

	const mpOpenId = uni.getStorageSync('mpOpenId') || '';
	const maOpenId = uni.getStorageSync('openId') || '';
	const payPayload = {
		orderNo,
		businessType,
		tradeType: resolveTradeType(methodKey),
		redirectUri: buildPayRedirectUri(businessType),
		amount,
		channel: resolvePayChannel(methodKey),
		userId: uni.getStorageSync('userId') || '',
		openId: getAuthPlatform() === 'h5-wechat' ? mpOpenId : maOpenId,
		wxAppScene: getWxAppScene()
	};

	try {
		const payResult = await postJson('app/base/pay/pay', payPayload).then(resolveBusinessResponse);
		savePendingPayment({ businessType, orderNo, scene: businessType });
		const invokeResult = await invokeNativePayment({ payResult, methodKey });
		return {
			result: invokeResult.result || 'pending',
			...paymentOrder,
			payUrl: invokeResult.payUrl,
			payData: invokeResult.payData || parsePayPayload(payResult)
		};
	} catch (error) {
		if (error instanceof Error) {
			throw error;
		}
		throw new Error(error?.message || error?.msg || '支付失败');
	}
};
