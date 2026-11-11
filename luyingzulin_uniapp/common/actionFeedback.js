export const showActionError = (fallbackText = '操作失败，请稍后重试', error = null) => {
	const message = error?.message || fallbackText;
	uni.showToast({ title: message, icon: 'none' });
};

export const runAction = async ({ action, successText = '', errorText = '操作失败，请稍后重试', onSuccess = null, onError = null }) => {
	try {
		const result = await action();
		if (successText) {
			uni.showToast({ title: successText, icon: 'success' });
		}
		if (typeof onSuccess === 'function') {
			onSuccess(result);
		}
		return { ok: true, result };
	} catch (error) {
		showActionError(errorText, error);
		if (typeof onError === 'function') {
			onError(error);
		}
		return { ok: false, error };
	}
};
