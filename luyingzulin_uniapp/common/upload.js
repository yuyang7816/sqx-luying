import { getApiBaseUrl } from '@/common/env';
import { isBusinessSuccess, resolveErrorMessage } from '@/common/errorCode';

const buildUploadUrl = () => `${getApiBaseUrl()}file/upload`;

const parseUploadResponse = (raw) => {
	const payload = typeof raw === 'string' ? JSON.parse(raw) : raw;
	if (!isBusinessSuccess(payload)) {
		throw payload;
	}
	const data = payload.data || {};
	const url = resolveUploadUrl(data);
	return {
		url,
		path: data.path || '',
		relativePath: data.relativePath || '',
		extension: data.extension || '',
		size: data.size || 0
	};
};

/** 解析 /file/upload 返回的可访问 URL */
export function resolveUploadUrl(result = {}) {
	const path = String(result.path || result.url || '').trim();
	if (path) return path;
	const relativePath = String(result.relativePath || '').trim();
	if (!relativePath) return '';
	if (/^https?:\/\//i.test(relativePath)) return relativePath;
	return relativePath.startsWith('/') ? relativePath : `/${relativePath}`;
}

/** 转为页面 image 组件可加载的完整 URL */
export function resolveDisplayImageUrl(src = '') {
	const value = String(src || '').trim();
	if (!value) return '';
	if (/^https?:\/\//i.test(value) || /^data:/i.test(value) || /^blob:/i.test(value)) return value;
	const base = getApiBaseUrl().replace(/\/+$/, '');
	return value.startsWith('/') ? `${base}${value}` : `${base}/${value}`;
}

/** 上传本地临时文件，返回可访问 URL */
export const uploadFile = (filePath, options = {}) => {
	const {
		name = 'file',
		hasToken = true,
		silentError = false
	} = options;
	const token = hasToken ? uni.getStorageSync('token') : '';

	return new Promise((resolve, reject) => {
		uni.uploadFile({
			url: buildUploadUrl(),
			filePath,
			name,
			header: hasToken && token ? { token } : {},
			success: (result) => {
				try {
					resolve(parseUploadResponse(result.data));
				} catch (error) {
					if (!silentError) {
						uni.showToast({ title: resolveErrorMessage(error) || '上传失败', icon: 'none' });
					}
					reject(error);
				}
			},
			fail: (error) => {
				if (!silentError) {
					uni.showToast({ title: error?.errMsg || '上传失败', icon: 'none' });
				}
				reject(error);
			}
		});
	});
};

const MAX_VIDEO_SIZE_MB = 50;

/** 选择视频并上传，返回 URL */
export const chooseAndUploadVideo = ({
	maxDuration = 300,
	sourceType = ['album', 'camera'],
	...uploadOptions
} = {}) => new Promise((resolve, reject) => {
	uni.chooseVideo({
		sourceType,
		maxDuration,
		compressed: true,
		success: async (result) => {
			try {
				const sizeMb = Number(result.size || 0) / 1024 / 1024;
				if (sizeMb > MAX_VIDEO_SIZE_MB) {
					uni.showToast({ title: `视频大小不能超过 ${MAX_VIDEO_SIZE_MB}MB`, icon: 'none' });
					reject(new Error('video too large'));
					return;
				}
				const upload = await uploadFile(result.tempFilePath, uploadOptions);
				resolve(upload.url);
			} catch (error) {
				reject(error);
			}
		},
		fail: reject
	});
});

/** 选择图片并上传，返回 URL 列表 */
export const chooseAndUploadImages = ({
	count = 1,
	sourceType = ['album', 'camera'],
	...uploadOptions
} = {}) => new Promise((resolve, reject) => {
	uni.chooseImage({
		count,
		sourceType,
		success: async (result) => {
			try {
				const uploads = await Promise.all(
					result.tempFilePaths.map((filePath) => uploadFile(filePath, uploadOptions))
				);
				resolve(uploads.map((item) => item.url).filter(Boolean));
			} catch (error) {
				reject(error);
			}
		},
		fail: reject
	});
});

export default {
	uploadFile,
	chooseAndUploadVideo,
	chooseAndUploadImages,
	resolveUploadUrl,
	resolveDisplayImageUrl
};
