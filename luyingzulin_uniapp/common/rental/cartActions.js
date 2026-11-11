import { addToCart } from '@/common/api/cart';
import { ensureLogin } from '@/common/auth';
import { resolveErrorMessage } from '@/common/errorCode';
import { buildCartAddPayload, isGoodsAvailableForCart } from '@/common/rental/constants';

/**
 * 列表/详情页一键加购（使用默认租期占位，真实租期在购物车/结算页调整）
 */
export async function quickAddToCart({
	goods,
	rentModeKey = 'day',
	qty = 1,
	specId = 0,
	packageId = 0,
	loginRedirect = '/pages/cart/index',
	loadingRef = null,
	successText = ''
} = {}) {
	if (!goods?.id) return false;
	if (!ensureLogin(loginRedirect)) return false;
	if (!isGoodsAvailableForCart(goods)) {
		uni.showToast({ title: '暂无库存', icon: 'none' });
		return false;
	}
	if (loadingRef?.value) return false;
	if (loadingRef) loadingRef.value = true;
	try {
		await addToCart(buildCartAddPayload(goods.id, rentModeKey, qty, goods, specId, packageId), { silentError: true });
		uni.showToast({
			title: successText || `已加入租物车：${goods.name || '商品'}`,
			icon: 'none'
		});
		return true;
	} catch (error) {
		uni.showToast({
			title: resolveErrorMessage(error) || '加入租物车失败',
			icon: 'none'
		});
		return false;
	} finally {
		if (loadingRef) loadingRef.value = false;
	}
}
