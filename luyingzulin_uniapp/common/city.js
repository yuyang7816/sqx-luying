import { reverseGeocode } from '@/common/api/map';

export const SELECTED_CITY_STORAGE_KEY = 'app-selected-city';
export const LOCATED_CITY_STORAGE_KEY = 'app-located-city';

export const HOT_CITY_NAMES = ['北京', '上海', '广州', '深圳', '杭州', '成都', '西安', '重庆', '昆明', '三亚'];

export const CITY_ENTRIES = [
	{ name: '北京', letter: 'B' },
	{ name: '保定', letter: 'B' },
	{ name: '成都', letter: 'C' },
	{ name: '重庆', letter: 'C' },
	{ name: '长沙', letter: 'C' },
	{ name: '常州', letter: 'C' },
	{ name: '大连', letter: 'D' },
	{ name: '东莞', letter: 'D' },
	{ name: '佛山', letter: 'F' },
	{ name: '福州', letter: 'F' },
	{ name: '广州', letter: 'G' },
	{ name: '贵阳', letter: 'G' },
	{ name: '杭州', letter: 'H' },
	{ name: '合肥', letter: 'H' },
	{ name: '哈尔滨', letter: 'H' },
	{ name: '海口', letter: 'H' },
	{ name: '济南', letter: 'J' },
	{ name: '嘉兴', letter: 'J' },
	{ name: '昆明', letter: 'K' },
	{ name: '兰州', letter: 'L' },
	{ name: '洛阳', letter: 'L' },
	{ name: '南京', letter: 'N' },
	{ name: '南昌', letter: 'N' },
	{ name: '南宁', letter: 'N' },
	{ name: '宁波', letter: 'N' },
	{ name: '青岛', letter: 'Q' },
	{ name: '泉州', letter: 'Q' },
	{ name: '上海', letter: 'S' },
	{ name: '深圳', letter: 'S' },
	{ name: '苏州', letter: 'S' },
	{ name: '沈阳', letter: 'S' },
	{ name: '石家庄', letter: 'S' },
	{ name: '三亚', letter: 'S' },
	{ name: '天津', letter: 'T' },
	{ name: '太原', letter: 'T' },
	{ name: '武汉', letter: 'W' },
	{ name: '无锡', letter: 'W' },
	{ name: '温州', letter: 'W' },
	{ name: '乌鲁木齐', letter: 'W' },
	{ name: '西安', letter: 'X' },
	{ name: '厦门', letter: 'X' },
	{ name: '西宁', letter: 'X' },
	{ name: '烟台', letter: 'Y' },
	{ name: '扬州', letter: 'Y' },
	{ name: '郑州', letter: 'Z' },
	{ name: '珠海', letter: 'Z' },
	{ name: '中山', letter: 'Z' }
];

export function getSelectedCity() {
	return String(uni.getStorageSync(SELECTED_CITY_STORAGE_KEY) || '').trim();
}

/** 当前展示/筛选用的城市：优先手动选择，否则 GPS 定位缓存 */
export function getAppCity() {
	return getSelectedCity() || getLocatedCity() || '';
}

export function setSelectedCity(cityName) {
	const normalized = String(cityName || '').trim();
	if (!normalized) return '';
	uni.setStorageSync(SELECTED_CITY_STORAGE_KEY, normalized);
	return normalized;
}

export function clearSelectedCity() {
	uni.removeStorageSync(SELECTED_CITY_STORAGE_KEY);
}

export function getLocatedCity() {
	return String(uni.getStorageSync(LOCATED_CITY_STORAGE_KEY) || '').trim();
}

export function setLocatedCity(cityName) {
	const normalized = String(cityName || '').trim();
	if (!normalized) return '';
	uni.setStorageSync(LOCATED_CITY_STORAGE_KEY, normalized);
	return normalized;
}

/** 官方定位 API 坐标系：统一 gcj02，H5 由 uni-app 结合 manifest 腾讯 Key 自动转换 */
export function getLocationCoordinateType() {
	return 'gcj02';
}

/** 腾讯逆地理编码 coord_type：1=GPS(wgs84)，5=国测局(gcj02) */
export function getReverseGeocodeCoordType() {
	return getLocationCoordinateType() === 'wgs84' ? '1' : '5';
}

const LOCATION_SCOPE = 'scope.userLocation';

function requireWeixinPrivacyAuthorize() {
	// #ifdef MP-WEIXIN
	return new Promise((resolve, reject) => {
		if (typeof wx === 'undefined' || typeof wx.requirePrivacyAuthorize !== 'function') {
			resolve(true);
			return;
		}
		wx.requirePrivacyAuthorize({
			success: () => resolve(true),
			fail: (error) => reject(error || new Error('用户未同意隐私协议'))
		});
	});
	// #endif
	return Promise.resolve(true);
}

/** 小程序/App 需先申请定位权限，H5 直接放行 */
export function ensureLocationPermission(options = {}) {
	const { guideToSetting = true } = options;
	// #ifdef H5
	return Promise.resolve(true);
	// #endif

	return requireWeixinPrivacyAuthorize().then(() => new Promise((resolve, reject) => {
		uni.getSetting({
			success: (res) => {
				const authSetting = res?.authSetting || {};
				if (authSetting[LOCATION_SCOPE]) {
					resolve(true);
					return;
				}
				if (authSetting[LOCATION_SCOPE] === false) {
					if (!guideToSetting) {
						reject(new Error('用户未授权位置权限'));
						return;
					}
					uni.showModal({
						title: '位置权限',
						content: '需要获取您的位置信息以展示所在城市',
						confirmText: '去设置',
						success: (modalRes) => {
							if (!modalRes.confirm) {
								reject(new Error('用户拒绝授权位置权限'));
								return;
							}
							uni.openSetting({
								success: (settingRes) => {
									if (settingRes?.authSetting?.[LOCATION_SCOPE]) {
										resolve(true);
										return;
									}
									reject(new Error('用户未授权位置权限'));
								},
								fail: reject
							});
						}
					});
					return;
				}
				uni.authorize({
					scope: LOCATION_SCOPE,
					success: () => resolve(true),
					fail: () => {
						if (!guideToSetting) {
							reject(new Error('用户拒绝授权位置权限'));
							return;
						}
						uni.showModal({
							title: '位置权限',
							content: '需要获取您的位置信息以展示所在城市',
							confirmText: '去设置',
							success: (modalRes) => {
								if (!modalRes.confirm) {
									reject(new Error('用户拒绝授权位置权限'));
									return;
								}
								uni.openSetting({
									success: (settingRes) => {
										if (settingRes?.authSetting?.[LOCATION_SCOPE]) {
											resolve(true);
											return;
										}
										reject(new Error('用户未授权位置权限'));
									},
									fail: reject
								});
							}
						});
					}
				});
			},
			fail: reject
		});
	}));
}

/** 调用 uni.getLocation 获取当前经纬度 */
export function requestDeviceLocation(options = {}) {
	return ensureLocationPermission(options).then(() => new Promise((resolve, reject) => {
		uni.getLocation({
			type: getLocationCoordinateType(),
			isHighAccuracy: true,
			success: resolve,
			fail: reject
		});
	}));
}

export function buildCityLocateData(entries = CITY_ENTRIES, hotCityNames = HOT_CITY_NAMES) {
	const indexList = ['🔥'];
	const cityList = [hotCityNames.map((name) => ({ name }))];
	const letters = [...new Set(entries.map((item) => item.letter))].sort();

	letters.forEach((letter) => {
		indexList.push(letter);
		cityList.push(
			entries
				.filter((item) => item.letter === letter)
				.map((item) => ({ name: item.name }))
		);
	});

	return { indexList, cityList };
}

export function normalizeLocatedCity(address = {}) {
	const city = String(address.city || address.district || address.province || '').trim();
	return city.replace(/(市|地区|自治州|盟)$/, '');
}

/** 定位当前城市：uni.getLocation + 逆地理编码（H5 用 manifest 腾讯 Key，其余走后端） */
export async function locateCurrentCity(options = {}) {
	const { force = false, useCache = true } = options;
	if (useCache && !force) {
		const cached = getLocatedCity();
		if (cached) return cached;
	}

	const location = await requestDeviceLocation();
	const address = await reverseGeocode(
		location.latitude,
		location.longitude,
		getReverseGeocodeCoordType()
	);
	const cityName = normalizeLocatedCity(address);
	if (!cityName) {
		throw new Error('未能识别当前城市');
	}
	setLocatedCity(cityName);
	return cityName;
}
