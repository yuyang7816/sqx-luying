import { get, resolveBusinessResponse } from '@/common/httpRequest';
import { getH5TencentMapKey } from '@/common/mapConfig';

function mapTencentAddressComponent(component = {}) {
	return {
		country: component.nation || component.country,
		province: component.province,
		city: component.city,
		district: component.district
	};
}

function reverseGeocodeByBackend(lat, lng, coordType) {
	const params = {
		lat: String(lat),
		lng: String(lng)
	};
	if (coordType) {
		params.coordType = String(coordType);
	}
	return get('app/map/reverseGeocode', params, { hasToken: false, silentError: true }).then(resolveBusinessResponse);
}

/** H5：通过 manifest Web 配置 Key + JSONP 调用腾讯逆地理（与 uni.getLocation 一致，避免 CORS） */
function reverseGeocodeByTencentH5(lat, lng, coordType = '5') {
	const mapKey = getH5TencentMapKey();
	if (!mapKey) {
		return Promise.reject(new Error('未配置腾讯地图 Key，请在 manifest.json -> h5 -> sdkConfigs -> maps -> tencent -> key 中设置'));
	}

	return new Promise((resolve, reject) => {
		if (typeof window === 'undefined') {
			reject(new Error('当前环境不支持腾讯地图'));
			return;
		}

		const callbackName = `__tencentGeocode_${Date.now()}`;
		const script = document.createElement('script');

		const cleanup = () => {
			delete window[callbackName];
			script.remove();
		};

		window[callbackName] = (result) => {
			cleanup();
			if (result?.status === 0 && result.result?.address_component) {
				resolve(mapTencentAddressComponent(result.result.address_component));
				return;
			}
			reject(new Error(result?.message || '腾讯地图解析失败'));
		};

		const location = `${Number(lat)},${Number(lng)}`;
		script.src = [
			'https://apis.map.qq.com/ws/geocoder/v1/',
			`?output=jsonp&callback=${callbackName}`,
			`&location=${encodeURIComponent(location)}`,
			`&key=${encodeURIComponent(mapKey)}`,
			`&coord_type=${encodeURIComponent(String(coordType || '5'))}`
		].join('');
		script.onerror = () => {
			cleanup();
			reject(new Error('腾讯地图解析失败'));
		};
		document.head.appendChild(script);
	});
}

/** 根据经纬度逆地理编码 */
export async function reverseGeocode(lat, lng, coordType) {
	// #ifdef H5
	try {
		return await reverseGeocodeByTencentH5(lat, lng, coordType);
	} catch (error) {
		return reverseGeocodeByBackend(lat, lng, coordType);
	}
	// #endif
	return reverseGeocodeByBackend(lat, lng, coordType);
}
