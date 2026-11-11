/**
 * H5 腾讯地图 Key 由 uni-app 在构建时从 manifest.json -> h5.sdkConfigs.maps.tencent.key
 * 注入全局 __uniConfig.qqMapKey，H5 运行时直接读取，勿在此重复维护。
 * 小程序/App 仍走后端 app/map/reverseGeocode + sys_config.map_tencent_key
 */

/** H5 腾讯地图 WebService JSONP 基址（逆地理编码，避免浏览器 CORS） */
export const TENCENT_MAP_WS_API = 'https://apis.map.qq.com/ws/geocoder/v1/';

/** 获取 H5 腾讯地图 Key（仅 H5 有效，来自 manifest Web 配置） */
export function getH5TencentMapKey() {
	// #ifdef H5
	if (typeof __uniConfig !== 'undefined' && __uniConfig.qqMapKey) {
		return __uniConfig.qqMapKey;
	}
	// #endif
	return '';
}
