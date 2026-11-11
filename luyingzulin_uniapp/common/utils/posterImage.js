import { getApiBaseUrl } from '@/common/env';

const DEV_API_HOSTS = new Set(['luyingzl.xianmaxiong.com']);
const CDN_HOSTS = new Set(['tu.mianyazuju.com']);

function isDataUrl(src = '') {
	return /^data:image\//i.test(src);
}

function isBlobUrl(src = '') {
	return /^blob:/i.test(src);
}

function isRelativePath(src = '') {
	return src.startsWith('/') && !src.startsWith('//');
}

function toAbsoluteUrl(src = '') {
	if (!src) return '';
	if (/^https?:\/\//i.test(src) || isDataUrl(src) || isBlobUrl(src)) return src;
	const base = getApiBaseUrl().replace(/\/+$/, '');
	return src.startsWith('/') ? `${base}${src}` : `${base}/${src}`;
}

/** H5：将跨域图片 URL 改写为 dev server 同源代理路径 */
function toSameOriginFetchUrl(url = '') {
	if (!url || isDataUrl(url) || isBlobUrl(url)) return url;
	if (isRelativePath(url)) return url;

	// #ifdef H5
	try {
		const parsed = new URL(url);
		if (DEV_API_HOSTS.has(parsed.host)) {
			return `${parsed.pathname}${parsed.search}`;
		}

		const apiBase = getApiBaseUrl();
		if (/^https?:\/\//i.test(apiBase)) {
			const apiOrigin = new URL(apiBase).origin;
			if (parsed.origin === apiOrigin) {
				return `${parsed.pathname}${parsed.search}`;
			}
		}

		if (CDN_HOSTS.has(parsed.host)) {
			return `/media-proxy${parsed.pathname}${parsed.search}`;
		}
	} catch (error) {
		// ignore invalid url
	}
	// #endif

	return url;
}

function arrayBufferToBase64(buffer) {
	const bytes = new Uint8Array(buffer);
	let binary = '';
	const chunkSize = 0x8000;
	for (let i = 0; i < bytes.length; i += chunkSize) {
		binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
	}
	if (typeof btoa === 'function') {
		return btoa(binary);
	}
	if (typeof uni !== 'undefined' && typeof uni.arrayBufferToBase64 === 'function') {
		return uni.arrayBufferToBase64(buffer);
	}
	throw new Error('base64 编码不可用');
}

async function fetchAsDataUrlH5(url) {
	const fetchUrl = toSameOriginFetchUrl(toAbsoluteUrl(url));
	const response = await fetch(fetchUrl, { credentials: 'include' });
	if (!response.ok) {
		throw new Error(`图片请求失败(${response.status})`);
	}
	const contentType = response.headers.get('content-type') || 'image/png';
	const buffer = await response.arrayBuffer();
	const base64 = arrayBufferToBase64(buffer);
	return `data:${contentType.split(';')[0]};base64,${base64}`;
}

function downloadToLocalPath(url) {
	return new Promise((resolve, reject) => {
		uni.downloadFile({
			url,
			success: (res) => {
				if (res.statusCode === 200 && res.tempFilePath) {
					resolve(res.tempFilePath);
					return;
				}
				reject(new Error('图片下载失败'));
			},
			fail: (error) => reject(error || new Error('图片下载失败'))
		});
	});
}

/** 将远程图片转为 canvas 可安全导出的本地路径或 dataURL */
export async function resolveCanvasSafeImageSrc(src) {
	if (!src) return '';
	if (isDataUrl(src) || isBlobUrl(src)) return src;

	// #ifdef H5
	return fetchAsDataUrlH5(src);
	// #endif

	// #ifndef H5
	return downloadToLocalPath(toAbsoluteUrl(src));
	// #endif
}
