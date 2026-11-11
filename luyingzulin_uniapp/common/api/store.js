import { get, resolveBusinessResponse } from '@/common/httpRequest';
import { mapPickupTypeToApi } from '@/common/rental/constants';

/** 取货点位 VO → 确认页展示结构 */
export const mapPickupStoreVoToUi = (vo = {}) => ({
	id: vo.id,
	merchantId: vo.merchantId,
	storeNo: vo.storeNo,
	name: vo.name || '',
	address: vo.address || '',
	contact: vo.contactPhone || '',
	lat: vo.latitude,
	lng: vo.longitude,
	baseDeliveryFee: vo.baseDeliveryFee,
	freeDeliveryThreshold: vo.freeDeliveryThreshold,
	pickupType: vo.pickupType
});

/**
 * 取货点位列表
 * @param {'store'|'station'|'delivery'} pickupType UI 取货方式
 */
export async function fetchPickupStores(pickupType = 'store', options = {}) {
	if (pickupType === 'delivery') {
		return [];
	}
	const apiType = mapPickupTypeToApi(pickupType);
	const params = { pickupType: apiType };
	if (options.merchantId) {
		params.merchantId = options.merchantId;
	}
	const list = await get('app/rental/store/list', params, options).then(resolveBusinessResponse);
	return (list || []).map(mapPickupStoreVoToUi);
}
