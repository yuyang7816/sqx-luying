let pcasDataPromise = null;

function loadPcasData() {
	if (!pcasDataPromise) {
		pcasDataPromise = import('./data/pca-code.json').then((module) => module.default || module);
	}
	return pcasDataPromise;
}

export function normalizeRegionName(name = '') {
	return String(name)
		.trim()
		.replace(/(省|市|自治区|回族|维吾尔|壮族|特别行政区|地区|自治州|盟|区|县)$/g, '');
}

export function matchRegionName(left = '', right = '') {
	const a = normalizeRegionName(left);
	const b = normalizeRegionName(right);
	if (!a || !b) return false;
	return a === b || String(left).trim() === String(right).trim() || String(left).trim().includes(String(right).trim()) || String(right).trim().includes(String(left).trim());
}

function mapRegionNodes(list = []) {
	return list.map(({ code, name }) => ({ code, name }));
}

export function getCityNodes(provinceNode) {
	return mapRegionNodes(provinceNode?.children || []);
}

export function getDistrictNodes(provinceNode, cityNode) {
	return mapRegionNodes(cityNode?.children || []);
}

function findNodeIndex(list = [], name = '') {
	if (!name) return 0;
	const index = list.findIndex((item) => matchRegionName(item.name, name));
	return index >= 0 ? index : 0;
}

function findProvinceNode(data = [], provinceName = '') {
	if (!provinceName) return data[0];
	return data.find((item) => matchRegionName(item.name, provinceName)) || data[0];
}

function findCityNode(provinceNode, cityName = '') {
	const cities = provinceNode?.children || [];
	if (!cityName) return cities[0];
	return cities.find((item) => matchRegionName(item.name, cityName)) || cities[0];
}

function findDistrictNode(cityNode, districtName = '') {
	const districts = cityNode?.children || [];
	if (!districtName) return districts[0];
	return districts.find((item) => matchRegionName(item.name, districtName)) || districts[0];
}

/** 构建省市区三列数据与默认索引 */
export async function buildRegionPickerColumns(selected = {}) {
	const data = await loadPcasData();
	const provinces = mapRegionNodes(data);
	const provinceNode = findProvinceNode(data, selected.province);
	const cityNode = findCityNode(provinceNode, selected.city);
	const cities = getCityNodes(provinceNode);
	const districts = getDistrictNodes(provinceNode, cityNode);

	return {
		columns: [provinces, cities, districts],
		indexes: [
			findNodeIndex(provinces, selected.province || provinceNode?.name),
			findNodeIndex(cities, selected.city || cityNode?.name),
			findNodeIndex(districts, selected.district || cityNode?.children?.[0]?.name)
		],
		data
	};
}

/** 联动更新市、区列 */
export async function buildRegionColumnsByChange(data, columns, columnIndex, pickerIndexs = []) {
	const provinces = columns[0] || [];
	const province = provinces[pickerIndexs[0] || 0] || provinces[0];
	const provinceNode = findProvinceNode(data, province?.name);

	if (columnIndex === 0) {
		const cities = getCityNodes(provinceNode);
		const cityNode = cities[0];
		return {
			cities,
			districts: getDistrictNodes(provinceNode, cityNode)
		};
	}

	const cities = getCityNodes(provinceNode);
	const city = cities[pickerIndexs[1] || 0] || cities[0];
	const cityNode = findCityNode(provinceNode, city?.name);
	return {
		districts: getDistrictNodes(provinceNode, cityNode)
	};
}

export function formatRegionText(province = '', city = '', district = '') {
	return [province, city, district].filter(Boolean).join(' ');
}
