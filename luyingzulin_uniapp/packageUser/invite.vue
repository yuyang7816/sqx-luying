<template>
	<view class="page">
		<up-poster ref="posterRef" :json="exportPosterJson || posterJson" class="poster-hidden"></up-poster>

		<view class="invite-card">
			<view class="poster-shell">
				<image
					v-if="posterConfig.posterImage"
					class="poster-bg"
					:src="posterConfig.posterImage"
					mode="aspectFill"
				></image>
				<view v-else class="poster-fallback">
					<text class="fallback-brand">{{ posterConfig.brandName }}</text>
					<text class="fallback-title">邀请好友</text>
				</view>

				<view class="poster-footer">
					<view class="user-left">
						<image class="user-avatar" :src="userAvatar" mode="aspectFill"></image>
						<view class="user-info">
							<text class="user-name">{{ inviteProfile?.nickname || '用户' }}</text>
							<text class="code-text">邀请码：{{ inviteProfile?.inviteCode || '--' }}</text>
						</view>
					</view>
					<view class="qr-box">
						<image v-if="posterQrImage" class="qr-image" :src="posterQrImage" mode="aspectFit"></image>
						<up-qrcode
							v-else
							:val="inviteProfile?.inviteUrl || ''"
							:size="qrPixelSize"
							background="#ffffff"
							foreground="#000000"
						></up-qrcode>
					</view>
				</view>
			</view>
		</view>

		<view class="action-row">
			<button class="action-btn copy-btn" @click="copyPromoText">复制文案</button>
			<button class="action-btn poster-btn" :loading="posterLoading" :disabled="posterLoading" @click="savePoster">生成海报</button>
		</view>

		<view class="footer-links">
			<text class="footer-link" @click="goInviteUsers">我的团队</text>
			<text class="footer-dot">·</text>
			<text class="footer-link" @click="goCommissionList">收益明细</text>
		</view>

		<view v-if="posterPreviewVisible" class="poster-preview-mask" @click="closePosterPreview">
			<view class="poster-preview-panel" @click.stop>
				<text class="poster-preview-title">海报预览</text>
				<scroll-view scroll-y class="poster-preview-scroll">
					<image
						class="poster-preview-image"
						:src="posterPreviewPath"
						mode="widthFix"
						show-menu-by-longpress
					></image>
				</scroll-view>
				<view class="poster-preview-actions">
					<button class="preview-btn preview-close-btn" @click="closePosterPreview">关闭</button>
					<button class="preview-btn preview-save-btn" :loading="posterSaving" :disabled="posterSaving" @click="downloadPoster">
						保存海报
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
	import { computed, nextTick, ref } from 'vue';
	import { onShareAppMessage, onShareTimeline, onShow } from '@dcloudio/uni-app';
	import { buildInvitePromoText, buildInviteRegisterUrl, ensureLogin, getStoredSession } from '@/common/auth';
	import { fetchInvitePosterConfigRemote } from '@/common/api/content';
	import { fetchInviteWxQrCode } from '@/common/api/auth';
	import { fetchInviteProfile } from '@/common/api/invite';
	import { resolveCanvasSafeImageSrc } from '@/common/utils/posterImage';
	import { DEFAULT_AVATAR_URL, resolveAvatarUrl } from '@/common/constants/avatar';

	const POSTER_WIDTH = '690rpx';
	const POSTER_HEIGHT = '980rpx';
	const FOOTER_TOP = '812rpx';
	const FOOTER_HEIGHT = '168rpx';

	const posterRef = ref(null);
	const posterLoading = ref(false);
	const posterSaving = ref(false);
	const posterPreviewVisible = ref(false);
	const posterPreviewPath = ref('');
	const posterImagePath = ref('');
	const posterQrImage = ref('');
	const exportPosterJson = ref(null);
	const inviteProfile = ref(null);
	const userAvatar = ref(DEFAULT_AVATAR_URL);
	const qrPixelSize = uni.upx2px(112);
	const posterConfig = ref({
		brandName: '露营装备租赁',
		inviterText: '邀你一起露营',
		posterImage: ''
	});

	const loadPosterQrImage = async (inviteCode) => {
		if (!inviteCode) {
			posterQrImage.value = '';
			return;
		}
		// #ifdef MP-WEIXIN
		try {
			const base64 = await fetchInviteWxQrCode(inviteCode);
			posterQrImage.value = base64 ? `data:image/png;base64,${base64}` : '';
			if (posterQrImage.value) return;
		} catch (error) {
			console.warn('小程序码生成失败，回退 H5 链接二维码', error);
		}
		// #endif
		posterQrImage.value = '';
	};

	const syncInviteData = async () => {
		if (!ensureLogin('/packageUser/invite')) return;
		const session = getStoredSession();
		userAvatar.value = resolveAvatarUrl(session?.userInfo?.avatar);
		try {
			const [profile, remotePosterConfig] = await Promise.all([
				fetchInviteProfile(),
				fetchInvitePosterConfigRemote()
			]);
			inviteProfile.value = {
				...profile,
				inviteUrl: profile.inviteUrl || buildInviteRegisterUrl(profile.inviteCode)
			};
			posterConfig.value = remotePosterConfig;
			posterImagePath.value = '';
			posterPreviewPath.value = '';
			posterPreviewVisible.value = false;
			exportPosterJson.value = null;
			await loadPosterQrImage(profile.inviteCode);
		} catch (error) {
			uni.showToast({ title: error.message || '加载失败', icon: 'none' });
		}
	};

	onShow(syncInviteData);

	const buildPosterQrView = (qrSrc = '', css = {}) => {
		const qrCss = {
			left: '538rpx',
			top: '828rpx',
			width: '120rpx',
			height: '120rpx',
			...css
		};
		if (qrSrc) {
			return { type: 'image', src: qrSrc, css: qrCss };
		}
		return {
			type: 'qrcode',
			text: inviteProfile.value?.inviteUrl || buildInviteRegisterUrl(inviteProfile.value?.inviteCode || ''),
			css: qrCss
		};
	};

	const buildPosterJson = ({ bgSrc = '', avatarSrc = '', qrSrc = '', forExport = false } = {}) => {
		const views = [];
		const backgroundSrc = forExport ? bgSrc : (bgSrc || posterConfig.value.posterImage);

		if (backgroundSrc) {
			views.push({
				type: 'image',
				src: backgroundSrc,
				css: { left: '0', top: '0', width: POSTER_WIDTH, height: POSTER_HEIGHT }
			});
		} else {
			views.push({
				type: 'view',
				css: {
					left: '0',
					top: '0',
					width: POSTER_WIDTH,
					height: POSTER_HEIGHT,
					background: 'linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%)'
				}
			});
		}

		views.push({
			type: 'view',
			css: {
				left: '0',
				top: FOOTER_TOP,
				width: POSTER_WIDTH,
				height: FOOTER_HEIGHT,
				background: '#ffffff'
			}
		});

		const avatar = forExport ? avatarSrc : (avatarSrc || userAvatar.value);
		if (avatar) {
			views.push({
				type: 'image',
				src: avatar,
				css: { left: '32rpx', top: '836rpx', width: '96rpx', height: '96rpx', radius: '48rpx' }
			});
		}

		views.push(
			{
				type: 'text',
				text: inviteProfile.value?.nickname || '用户',
				css: {
					left: '144rpx',
					top: '848rpx',
					width: '360rpx',
					fontSize: '30rpx',
					color: '#1f2937',
					fontWeight: 'bold'
				}
			},
			{
				type: 'text',
				text: `邀请码：${inviteProfile.value?.inviteCode || '--'}`,
				css: {
					left: '144rpx',
					top: '892rpx',
					width: '360rpx',
					fontSize: '24rpx',
					color: '#6b7280'
				}
			},
			buildPosterQrView(qrSrc)
		);

		return {
			css: {
				width: POSTER_WIDTH,
				height: POSTER_HEIGHT,
				background: '#f6f8f3'
			},
			views
		};
	};

	const posterJson = computed(() => buildPosterJson());

	const prepareExportPosterJson = async () => {
		let bgSrc = '';
		if (posterConfig.value.posterImage) {
			try {
				bgSrc = await resolveCanvasSafeImageSrc(posterConfig.value.posterImage);
			} catch (error) {
				console.warn('海报背景图转换失败', error);
			}
		}

		let avatarSrc = '';
		if (userAvatar.value) {
			try {
				avatarSrc = await resolveCanvasSafeImageSrc(userAvatar.value);
			} catch (error) {
				console.warn('头像转换失败，将跳过头像绘制', error);
			}
		}

		let qrSrc = '';
		if (posterQrImage.value) {
			try {
				qrSrc = await resolveCanvasSafeImageSrc(posterQrImage.value);
			} catch (error) {
				console.warn('海报二维码图片转换失败，将回退在线二维码', error);
			}
		}

		return buildPosterJson({ bgSrc, avatarSrc, qrSrc, forExport: true });
	};

	const exportPoster = async () => {
		if (posterImagePath.value) return posterImagePath.value;
		if (!posterRef.value) throw new Error('海报组件尚未准备好');
		exportPosterJson.value = await prepareExportPosterJson();
		await nextTick();
		await new Promise((resolve) => setTimeout(resolve, 120));
		try {
			const result = await posterRef.value.exportImage();
			posterImagePath.value = result.path;
			return result.path;
		} finally {
			exportPosterJson.value = null;
		}
	};

	const ensureAlbumPermission = () => new Promise((resolve, reject) => {
		// #ifdef APP-PLUS || MP-WEIXIN
		uni.getSetting({
			success: (settingRes) => {
				if (settingRes.authSetting?.['scope.writePhotosAlbum']) {
					resolve();
					return;
				}
				uni.authorize({
					scope: 'scope.writePhotosAlbum',
					success: resolve,
					fail: () => {
						uni.showModal({
							title: '需要相册权限',
							content: '保存海报需要访问您的相册，请在设置中开启权限。',
							confirmText: '去设置',
							success: (modalRes) => {
								if (!modalRes.confirm) {
									reject(new Error('未授权相册权限'));
									return;
								}
								uni.openSetting({
									success: (openRes) => {
										if (openRes.authSetting?.['scope.writePhotosAlbum']) {
											resolve();
										} else {
											reject(new Error('未授权相册权限'));
										}
									},
									fail: () => reject(new Error('打开设置失败'))
								});
							}
						});
					}
				});
			},
			fail: () => reject(new Error('获取权限状态失败'))
		});
		// #endif
		// #ifndef APP-PLUS || MP-WEIXIN
		resolve();
		// #endif
	});

	const savePoster = async () => {
		if (posterLoading.value) return;
		posterLoading.value = true;
		try {
			const path = await exportPoster();
			posterPreviewPath.value = path;
			posterPreviewVisible.value = true;
		} catch (error) {
			uni.showToast({ title: error.message || error.errMsg || '海报生成失败', icon: 'none' });
		} finally {
			posterLoading.value = false;
		}
	};

	const closePosterPreview = () => {
		posterPreviewVisible.value = false;
	};

	const downloadPoster = async () => {
		if (posterSaving.value || !posterPreviewPath.value) return;
		posterSaving.value = true;
		try {
			const path = posterPreviewPath.value;
			// #ifdef H5
			const link = document.createElement('a');
			link.href = path;
			link.download = `invite-poster-${inviteProfile.value?.inviteCode || 'camp'}.png`;
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
			uni.showToast({ title: '海报已开始下载', icon: 'none' });
			// #endif
			// #ifndef H5
			await ensureAlbumPermission();
			await new Promise((resolve, reject) => {
				uni.saveImageToPhotosAlbum({ filePath: path, success: resolve, fail: reject });
			});
			uni.showToast({ title: '海报已保存到相册', icon: 'success' });
			// #endif
		} catch (error) {
			uni.showToast({ title: error.message || error.errMsg || '海报保存失败', icon: 'none' });
		} finally {
			posterSaving.value = false;
		}
	};

	const copyPromoText = () => {
		if (!inviteProfile.value) return;
		const text = buildInvitePromoText(inviteProfile.value, posterConfig.value);
		uni.setClipboardData({
			data: text,
			success: () => uni.showToast({ title: '推广文案已复制', icon: 'none' })
		});
	};

	const goInviteUsers = () => uni.navigateTo({ url: '/packageUser/invite-users' });
	const goCommissionList = () => uni.navigateTo({ url: '/packageUser/invite-commission' });

	onShareAppMessage(() => ({
		title: `${posterConfig.value.brandName} · 邀请你注册`,
		path: inviteProfile.value?.invitePath || '/packageUser/register',
		imageUrl: posterConfig.value.posterImage || '/static/logo.png'
	}));

	onShareTimeline(() => ({
		title: `${posterConfig.value.brandName} · 邀请你注册`,
		query: `inviteCode=${inviteProfile.value?.inviteCode || ''}`,
		imageUrl: posterConfig.value.posterImage || '/static/logo.png'
	}));
</script>

<style scoped>
	.page {
		box-sizing: border-box;
		min-height: 100vh;
		padding: 24rpx 24rpx 48rpx;
		background: #f6f8f3;
		overflow-x: hidden;
	}

	.poster-hidden {
		position: fixed;
		left: -9999px;
		top: -9999px;
		opacity: 0;
		pointer-events: none;
	}

	.invite-card {
		box-sizing: border-box;
		width: 100%;
		border-radius: 28rpx;
		overflow: hidden;
		box-shadow: 0 10rpx 36rpx rgba(126, 211, 33, 0.08);
	}

	.poster-shell {
		position: relative;
		width: 100%;
		height: 980rpx;
		background: #eef2e8;
	}

	.poster-bg {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
	}

	.poster-fallback {
		position: absolute;
		left: 0;
		top: 0;
		width: 100%;
		height: 100%;
		display: flex;
		flex-direction: column;
		justify-content: center;
		padding: 48rpx 36rpx;
		background: linear-gradient(160deg, #7ed321 0%, #6bc01a 50%, #5aad15 100%);
	}

	.fallback-brand,
	.fallback-title {
		display: block;
		color: #ffffff;
	}

	.fallback-brand {
		font-size: 24rpx;
		opacity: 0.92;
	}

	.fallback-title {
		margin-top: 16rpx;
		font-size: 56rpx;
		font-weight: 800;
	}

	.poster-footer {
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 24rpx;
		padding: 28rpx 32rpx calc(28rpx + env(safe-area-inset-bottom));
		background: #ffffff;
		box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.06);
	}

	.user-left {
		display: flex;
		align-items: center;
		flex: 1;
		min-width: 0;
	}

	.user-avatar {
		width: 112rpx;
		height: 112rpx;
		border-radius: 50%;
		background: #eefadb;
		flex-shrink: 0;
	}

	.user-info {
		flex: 1;
		min-width: 0;
		margin-left: 24rpx;
	}

	.user-name,
	.code-text {
		display: block;
	}

	.user-name {
		font-size: 34rpx;
		font-weight: 700;
		color: #1f2937;
	}

	.code-text {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #6b7280;
	}

	.qr-box {
		width: 112rpx;
		height: 112rpx;
		background: #ffffff;
		border: 1rpx solid #eef2f7;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.qr-image {
		width: 112rpx;
		height: 112rpx;
	}

	.action-row {
		display: flex;
		gap: 24rpx;
		margin-top: 36rpx;
	}

	.action-btn {
		flex: 1;
		height: 92rpx;
		line-height: 92rpx;
		border-radius: 999rpx;
		font-size: 30rpx;
		font-weight: 700;
		border: none;
	}

	.copy-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #ffb347, #ff9f1c);
		box-shadow: 0 8rpx 24rpx rgba(255, 159, 28, 0.28);
	}

	.poster-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
		box-shadow: 0 8rpx 24rpx rgba(126, 211, 33, 0.28);
	}

	.footer-links {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 12rpx;
		margin-top: 28rpx;
	}

	.footer-link {
		font-size: 24rpx;
		color: #6b7a90;
	}

	.footer-dot {
		font-size: 24rpx;
		color: #c5cad3;
	}

	.poster-preview-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		z-index: 999;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 32rpx;
		background: rgba(0, 0, 0, 0.72);
		box-sizing: border-box;
	}

	.poster-preview-panel {
		box-sizing: border-box;
		width: 100%;
		max-width: 640rpx;
		max-height: 88vh;
		padding: 28rpx;
		border-radius: 24rpx;
		background: #ffffff;
		display: flex;
		flex-direction: column;
	}

	.poster-preview-title {
		display: block;
		margin-bottom: 20rpx;
		font-size: 32rpx;
		font-weight: 700;
		color: #1f2937;
		text-align: center;
	}

	.poster-preview-scroll {
		flex: 1;
		min-height: 0;
		max-height: calc(88vh - 220rpx);
	}

	.poster-preview-image {
		display: block;
		width: 100%;
	}

	.poster-preview-actions {
		display: flex;
		gap: 20rpx;
		margin-top: 24rpx;
	}

	.preview-btn {
		flex: 1;
		height: 84rpx;
		line-height: 84rpx;
		border-radius: 999rpx;
		font-size: 28rpx;
		font-weight: 700;
		border: none;
	}

	.preview-close-btn {
		color: #6b7280;
		background: #f3f4f6;
	}

	.preview-save-btn {
		color: #ffffff;
		background: linear-gradient(135deg, #7ed321, #6bc01a);
	}
</style>
