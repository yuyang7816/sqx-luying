<script setup>
	import { computed, ref } from 'vue';
	import { onLoad, onShow } from '@dcloudio/uni-app';
	import { ensureLogin } from '@/common/auth';
	import {
		fetchApplyProgress,
		submitApply,
		canAccessMerchantWorkbench,
		shouldShowApplyStatusPage,
		canResubmitMerchantApply
	} from '@/common/api/merchant';
	import { chooseAndUploadImages } from '@/common/upload';

	const LICENSE_DOCS = [
		{ key: 'license', label: '营业执照', index: 0 },
		{ key: 'idFront', label: '身份证正面', index: 1 },
		{ key: 'idBack', label: '身份证反面', index: 2 }
	];

	const pageMode = ref('loading');
	const forceFormMode = ref(false);
	const applySummary = ref({
		status: 'pending',
		statusLabel: '加载中',
		storeName: '—',
		contactName: '—',
		contactPhone: '—',
		applyTime: '—',
		rejectReason: ''
	});
	const applyTimeline = ref([]);
	const applyDetail = ref({});

	const form = ref({
		contactName: '',
		contactPhone: '',
		storeName: '',
		address: '',
		province: '',
		city: '',
		district: '',
		latitude: null,
		longitude: null,
		serviceArea: '',
		baseDeliveryFee: '',
		freeDeliveryThreshold: ''
	});
	const hourStart = ref('09:00');
	const hourEnd = ref('21:00');
	const licenseUrls = ref([]);
	const facadeCover = ref('');
	const carouselUrls = ref([]);
	const uploadingKey = ref('');
	const submitting = ref(false);

	const businessHours = computed(() => `${hourStart.value}-${hourEnd.value}`);
	const isReviewing = computed(() => shouldShowApplyStatusPage(applySummary.value.status));
	const isRejected = computed(() => canResubmitMerchantApply(applySummary.value.status));
	const showStatusView = computed(() => pageMode.value === 'status');
	const showFormView = computed(() => pageMode.value === 'form');

	const getLicenseUrl = (index) => licenseUrls.value[index] || '';

	const parseBusinessHours = (value = '') => {
		const parts = String(value || '').split('-');
		if (parts.length === 2) {
			hourStart.value = parts[0].trim() || '09:00';
			hourEnd.value = parts[1].trim() || '21:00';
		}
	};

	const fillFormFromProgress = (data = {}) => {
		const summary = data.summary || {};
		form.value.storeName = summary.storeName === '—' ? '' : (summary.storeName || data.storeName || '');
		form.value.contactName = summary.contactName === '—' ? '' : (summary.contactName || data.contactName || '');
		form.value.contactPhone = summary.contactPhoneRaw || data.contactPhone || '';
		form.value.address = data.storeAddress || form.value.address;
		form.value.province = data.province || form.value.province;
		form.value.city = data.city || form.value.city;
		form.value.district = data.district || form.value.district;
		form.value.latitude = data.latitude ?? form.value.latitude;
		form.value.longitude = data.longitude ?? form.value.longitude;
		form.value.serviceArea = data.serviceArea || form.value.serviceArea;
		form.value.baseDeliveryFee = data.baseDeliveryFee != null && data.baseDeliveryFee !== ''
			? String(data.baseDeliveryFee)
			: form.value.baseDeliveryFee;
		form.value.freeDeliveryThreshold = data.freeDeliveryThreshold != null && data.freeDeliveryThreshold !== ''
			? String(data.freeDeliveryThreshold)
			: form.value.freeDeliveryThreshold;
		parseBusinessHours(data.businessHours || summary.businessHours);
		if (Array.isArray(data.licenseUrls) && data.licenseUrls.length) {
			licenseUrls.value = [...data.licenseUrls];
		}
		if (Array.isArray(data.extraUrls) && data.extraUrls.length) {
			facadeCover.value = data.extraUrls[0] || '';
			carouselUrls.value = data.extraUrls.slice(1).filter(Boolean);
		}
	};

	const resolvePageMode = (data = {}) => {
		const status = data.summary?.status || data.status || 'pending';
		applySummary.value = data.summary || applySummary.value;
		applyTimeline.value = data.timeline || [];
		applyDetail.value = data;

		if (canAccessMerchantWorkbench(status)) {
			uni.redirectTo({ url: '/packageMerchant/dashboard' });
			return;
		}
		if (shouldShowApplyStatusPage(status)) {
			fillFormFromProgress(data);
			pageMode.value = 'status';
			return;
		}
		if (canResubmitMerchantApply(status) && !forceFormMode.value) {
			fillFormFromProgress(data);
			pageMode.value = 'status';
			return;
		}
		fillFormFromProgress(data);
		pageMode.value = 'form';
	};

	const loadProgress = async () => {
		if (!ensureLogin('/packageMerchant/apply')) return;
		try {
			const data = await fetchApplyProgress();
			resolvePageMode(data);
		} catch {
			pageMode.value = 'form';
		}
	};

	const startResubmit = () => {
		forceFormMode.value = true;
		pageMode.value = 'form';
	};

	const uploadLicenseDoc = async (item) => {
		if (uploadingKey.value) return;
		uploadingKey.value = item.key;
		try {
			uni.showLoading({ title: '上传中' });
			const uploadedUrls = await chooseAndUploadImages({ count: 1 });
			const url = uploadedUrls[0];
			if (!url) return;
			const urls = [...licenseUrls.value];
			urls[item.index] = url;
			licenseUrls.value = urls;
		} catch (error) {
			uni.showToast({ title: '上传失败', icon: 'none' });
		} finally {
			uploadingKey.value = '';
			uni.hideLoading();
		}
	};

	const uploadFacadeCover = async () => {
		if (uploadingKey.value) return;
		uploadingKey.value = 'facade';
		try {
			uni.showLoading({ title: '上传中' });
			const uploadedUrls = await chooseAndUploadImages({ count: 1 });
			if (uploadedUrls[0]) facadeCover.value = uploadedUrls[0];
		} catch (error) {
			uni.showToast({ title: '上传失败', icon: 'none' });
		} finally {
			uploadingKey.value = '';
			uni.hideLoading();
		}
	};

	const uploadCarousel = async () => {
		if (uploadingKey.value) return;
		if (carouselUrls.value.length >= 6) {
			uni.showToast({ title: '最多上传 6 张轮播图', icon: 'none' });
			return;
		}
		uploadingKey.value = 'carousel';
		try {
			uni.showLoading({ title: '上传中' });
			const remain = 6 - carouselUrls.value.length;
			const uploadedUrls = await chooseAndUploadImages({ count: Math.min(remain, 3) });
			carouselUrls.value = [...carouselUrls.value, ...uploadedUrls.filter(Boolean)];
		} catch (error) {
			uni.showToast({ title: '上传失败', icon: 'none' });
		} finally {
			uploadingKey.value = '';
			uni.hideLoading();
		}
	};

	const removeCarousel = (index) => {
		carouselUrls.value = carouselUrls.value.filter((_, idx) => idx !== index);
	};

	const chooseStoreLocation = () => {
		uni.chooseLocation({
			success: (res) => {
				form.value.address = res.address || res.name || '';
				form.value.latitude = res.latitude;
				form.value.longitude = res.longitude;
			},
			fail: (error) => {
				if (error?.errMsg?.includes('cancel')) return;
				uni.showToast({ title: '选择位置失败', icon: 'none' });
			}
		});
	};

	const onHourStartChange = (event) => {
		hourStart.value = event.detail.value;
	};

	const onHourEndChange = (event) => {
		hourEnd.value = event.detail.value;
	};

	const submit = async () => {
		if (!ensureLogin('/packageMerchant/apply')) return;
		if (!form.value.storeName?.trim()) {
			uni.showToast({ title: '请输入门店名称', icon: 'none' });
			return;
		}
		if (!form.value.contactName?.trim()) {
			uni.showToast({ title: '请输入负责人姓名', icon: 'none' });
			return;
		}
		if (!form.value.contactPhone?.trim()) {
			uni.showToast({ title: '请输入联系电话', icon: 'none' });
			return;
		}
		if (!form.value.address?.trim() || form.value.latitude == null || form.value.longitude == null) {
			uni.showToast({ title: '请点击选择门店地图位置', icon: 'none' });
			return;
		}
		if (!getLicenseUrl(0) || !getLicenseUrl(1) || !getLicenseUrl(2)) {
			uni.showToast({ title: '请上传营业执照与身份证正反面', icon: 'none' });
			return;
		}
		if (!facadeCover.value) {
			uni.showToast({ title: '请上传门头封面图', icon: 'none' });
			return;
		}
		if (!carouselUrls.value.length) {
			uni.showToast({ title: '请至少上传 1 张门店轮播图', icon: 'none' });
			return;
		}
		submitting.value = true;
		try {
			await submitApply({
				...form.value,
				businessHours: businessHours.value,
				storeAddress: form.value.address,
				licenseUrls: licenseUrls.value.filter(Boolean),
				extraUrls: [facadeCover.value, ...carouselUrls.value.filter(Boolean)],
				baseDeliveryFee: form.value.baseDeliveryFee ? Number(form.value.baseDeliveryFee) : undefined,
				freeDeliveryThreshold: form.value.freeDeliveryThreshold ? Number(form.value.freeDeliveryThreshold) : undefined
			});
			forceFormMode.value = false;
			uni.showToast({ title: '入驻申请已提交', icon: 'success' });
			setTimeout(loadProgress, 400);
		} catch (error) {
			uni.showToast({ title: error?.msg || error?.message || '提交失败', icon: 'none' });
		} finally {
			submitting.value = false;
		}
	};

	onLoad((options) => {
		if (options?.resubmit === '1') {
			forceFormMode.value = true;
		}
	});

	onShow(loadProgress);
</script>

<template>
	<view class="page">
		<view v-if="pageMode === 'loading'" class="loading-card">
			<text class="loading-text">加载中...</text>
		</view>

		<template v-else-if="showStatusView">
			<view class="status-hero-card">
				<text class="status-hero-title">{{ applySummary.statusLabel }}</text>
				<text class="status-hero-desc">
					{{ isReviewing ? '您的入驻申请已提交，平台正在审核中，请耐心等待。审核通过后方可进入商户工作台。' : '申请时间：' + applySummary.applyTime + '，可在下方查看审核进度与申请信息。' }}
				</text>
			</view>

			<view v-if="isRejected && applySummary.rejectReason" class="card reject-card">
				<text class="section-title">驳回原因</text>
				<text class="reject-reason">{{ applySummary.rejectReason }}</text>
				<text class="reject-tip">请根据驳回原因修改资料后重新提交申请。</text>
			</view>

			<view class="card">
				<text class="section-title">申请信息</text>
				<view class="line"><text>门店名称</text><text>{{ applySummary.storeName }}</text></view>
				<view class="line"><text>负责人</text><text>{{ applySummary.contactName }}</text></view>
				<view class="line"><text>联系电话</text><text>{{ applySummary.contactPhone }}</text></view>
				<view v-if="applyDetail.storeAddress" class="line"><text>门店地址</text><text>{{ applyDetail.storeAddress }}</text></view>
				<view v-if="applyDetail.businessHours || applySummary.businessHours" class="line"><text>营业时间</text><text>{{ applyDetail.businessHours || applySummary.businessHours }}</text></view>
				<view v-if="applyDetail.serviceArea" class="line"><text>配送范围</text><text>{{ applyDetail.serviceArea }}</text></view>
				<view v-if="applyDetail.baseDeliveryFee != null" class="line"><text>基础配送费</text><text>¥{{ applyDetail.baseDeliveryFee }}</text></view>
				<view v-if="applyDetail.freeDeliveryThreshold != null" class="line"><text>满额免配送</text><text>¥{{ applyDetail.freeDeliveryThreshold }}（仅租金，不含押金）</text></view>
			</view>

			<view class="card">
				<text class="section-title">审核进度</text>
				<view v-for="(item, index) in applyTimeline" :key="item.title + index" class="progress-row">
					<view class="progress-index">{{ index + 1 }}</view>
					<view class="progress-main">
						<text class="progress-title">{{ item.title }}</text>
						<text class="progress-time">{{ item.time }}</text>
						<text class="progress-desc">{{ item.desc }}</text>
					</view>
				</view>
			</view>

			<view class="card action-card">
				<button v-if="isRejected" class="primary-btn full-btn" @click="startResubmit">重新提交申请</button>
				<button v-else class="ghost-btn full-btn" disabled>审核中，请耐心等待</button>
			</view>
		</template>

		<template v-else-if="showFormView">
			<view v-if="isRejected" class="card reject-banner">
				<text class="reject-banner-title">上次申请被驳回</text>
				<text class="reject-banner-desc">{{ applySummary.rejectReason || '请修改资料后重新提交' }}</text>
			</view>

			<view class="card">
				<text class="section-title">基本信息</text>
				<view class="field"><text class="label">门店名称</text><input v-model="form.storeName" class="input" placeholder="请输入门店名称" /></view>
				<view class="field"><text class="label">负责人</text><input v-model="form.contactName" class="input" placeholder="请输入负责人姓名" /></view>
				<view class="field"><text class="label">联系电话</text><input v-model="form.contactPhone" class="input" type="number" maxlength="11" placeholder="请输入联系电话" /></view>
			</view>

			<view class="card">
				<text class="section-title">门店与履约信息</text>
				<view class="field" @click="chooseStoreLocation">
					<text class="label">门店地址</text>
					<view class="textarea location-box">{{ form.address || '点击选择地图位置（自动获取经纬度）' }}</view>
				</view>
				<view class="field">
					<text class="label">营业时间</text>
					<view class="hour-row">
						<picker mode="time" :value="hourStart" @change="onHourStartChange">
							<view class="input hour-input">{{ hourStart }}</view>
						</picker>
						<text class="hour-sep">至</text>
						<picker mode="time" :value="hourEnd" @change="onHourEndChange">
							<view class="input hour-input">{{ hourEnd }}</view>
						</picker>
					</view>
				</view>
				<view class="field"><text class="label">配送范围</text><textarea v-model="form.serviceArea" class="textarea short" maxlength="80" placeholder="请输入支持配送的区域范围"></textarea></view>
				<view class="field"><text class="label">基础配送费（元）</text><input v-model="form.baseDeliveryFee" class="input" type="digit" placeholder="留空则使用平台默认配送费" /></view>
				<view class="field"><text class="label">满额免配送（元）</text><input v-model="form.freeDeliveryThreshold" class="input" type="digit" placeholder="仅按租金计，不含押金" /></view>
				<text class="tip-text">配送费由门店自行设置；未配置时回落平台默认价。满额免配送仅按租金金额计算，不含押金。用户选择同城配送时，确认订单页会展示配送费及规则说明。</text>
			</view>

			<view class="card">
				<text class="section-title">资质资料</text>

				<view class="doc-section">
					<text class="doc-section-title">营业执照</text>
					<view class="doc-upload-box" :class="{ selected: getLicenseUrl(0) }" @click="uploadLicenseDoc(LICENSE_DOCS[0])">
						<image v-if="getLicenseUrl(0)" class="doc-image" :src="getLicenseUrl(0)" mode="aspectFill"></image>
						<view v-else class="upload-guide">
							<text class="guide-action">{{ uploadingKey === 'license' ? '上传中...' : '点击上传营业执照' }}</text>
						</view>
					</view>
				</view>

				<view class="doc-section">
					<text class="doc-section-title">身份证</text>
					<view class="id-card-row">
						<view class="id-card-item">
							<text class="id-card-label">人像面</text>
							<view class="doc-upload-box" :class="{ selected: getLicenseUrl(1) }" @click="uploadLicenseDoc(LICENSE_DOCS[1])">
								<image v-if="getLicenseUrl(1)" class="doc-image" :src="getLicenseUrl(1)" mode="aspectFill"></image>
								<view v-else class="upload-guide compact">
									<text class="guide-action">{{ uploadingKey === 'idFront' ? '上传中...' : '上传人像面' }}</text>
								</view>
							</view>
						</view>
						<view class="id-card-item">
							<text class="id-card-label">国徽面</text>
							<view class="doc-upload-box" :class="{ selected: getLicenseUrl(2) }" @click="uploadLicenseDoc(LICENSE_DOCS[2])">
								<image v-if="getLicenseUrl(2)" class="doc-image" :src="getLicenseUrl(2)" mode="aspectFill"></image>
								<view v-else class="upload-guide compact">
									<text class="guide-action">{{ uploadingKey === 'idBack' ? '上传中...' : '上传国徽面' }}</text>
								</view>
							</view>
						</view>
					</view>
				</view>

				<view class="doc-section">
					<text class="doc-section-title">门头封面图</text>
					<view class="doc-upload-box wide" :class="{ selected: facadeCover }" @click="uploadFacadeCover">
						<image v-if="facadeCover" class="doc-image wide-image" :src="facadeCover" mode="aspectFill"></image>
						<view v-else class="upload-guide wide-guide">
							<text class="guide-action">{{ uploadingKey === 'facade' ? '上传中...' : '点击上传门头封面' }}</text>
						</view>
					</view>
				</view>

				<view class="doc-section">
					<text class="doc-section-title">门店轮播图</text>
					<scroll-view scroll-x class="carousel-scroll">
						<view class="carousel-row">
							<view v-for="(url, index) in carouselUrls" :key="`${url}-${index}`" class="carousel-item">
								<image class="carousel-image" :src="url" mode="aspectFill"></image>
								<text class="carousel-remove" @click.stop="removeCarousel(index)">删除</text>
							</view>
							<view v-if="carouselUrls.length < 6" class="carousel-add" @click="uploadCarousel">
								<text>{{ uploadingKey === 'carousel' ? '上传中...' : '+ 添加' }}</text>
							</view>
						</view>
					</scroll-view>
				</view>
				<text class="tip-text">请上传清晰完整的资料：营业执照、身份证正反面、门头封面与门店轮播图（至少 1 张）。</text>
			</view>

			<button class="submit-btn" :loading="submitting" :disabled="submitting" @click="submit">
				{{ isRejected ? '重新提交入驻申请' : '提交入驻申请' }}
			</button>
		</template>
	</view>
</template>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; padding: 28rpx; background: #f6f8f3; overflow-x: hidden; }
	.loading-card { padding: 80rpx 32rpx; text-align: center; }
	.loading-text { font-size: 28rpx; color: #829279; }
	.status-hero-card { box-sizing: border-box; padding: 32rpx; border-radius: 32rpx; margin-bottom: 28rpx; background: linear-gradient(135deg, var(--theme-hero-start, #7ed321), var(--theme-hero-end, #6bc01a)); }
	.status-hero-title, .status-hero-desc { display: block; }
	.status-hero-title { font-size: 40rpx; font-weight: 700; color: #ffffff; }
	.status-hero-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.92); }
	.card { box-sizing: border-box; width: 100%; margin-bottom: 28rpx; padding: 32rpx; border-radius: 32rpx; background: #ffffff; box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04); overflow: hidden; }
	.section-title, .label, .tip-text, .reject-reason, .reject-tip, .reject-banner-title, .reject-banner-desc { display: block; }
	.section-title { font-size: 32rpx; font-weight: 800; color: #1a2114; letter-spacing: 1rpx; margin-bottom: 8rpx; }
	.reject-card { background: #fff7f7; }
	.reject-reason { margin-top: 16rpx; font-size: 28rpx; line-height: 1.7; color: #c0392b; font-weight: 600; }
	.reject-tip { margin-top: 12rpx; font-size: 22rpx; color: #a06a6a; }
	.reject-banner { background: #fff8f0; border: 2rpx solid #ffd9b3; }
	.reject-banner-title { font-size: 28rpx; font-weight: 700; color: #d97706; }
	.reject-banner-desc { margin-top: 10rpx; font-size: 24rpx; line-height: 1.7; color: #92400e; }
	.line { display: flex; justify-content: space-between; align-items: flex-start; margin-top: 20rpx; font-size: 24rpx; line-height: 1.6; color: #4b5563; }
	.line text:first-child { width: 132rpx; flex-shrink: 0; color: #8f97a5; }
	.line text:last-child { flex: 1; min-width: 0; text-align: right; word-break: break-all; color: #1f2937; }
	.progress-row { display: flex; margin-top: 24rpx; }
	.progress-index { width: 48rpx; height: 48rpx; line-height: 48rpx; border-radius: 50%; text-align: center; font-size: 24rpx; font-weight: 700; color: #ffffff; background: #7ed321; }
	.progress-main { flex: 1; min-width: 0; margin-left: 16rpx; }
	.progress-title, .progress-time, .progress-desc { display: block; }
	.progress-title { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.progress-time { margin-top: 8rpx; font-size: 20rpx; color: #9aa2b1; }
	.progress-desc { margin-top: 8rpx; font-size: 22rpx; line-height: 1.6; color: #6b7280; }
	.action-card { margin-bottom: 40rpx; }
	.primary-btn, .ghost-btn { box-sizing: border-box; height: 84rpx; line-height: 84rpx; margin: 0; border-radius: 42rpx; font-size: 28rpx; }
	.full-btn { width: 100%; }
	.primary-btn { color: #ffffff; background: #7ed321; }
	.ghost-btn { color: #829279; background: #eef3ea; }
	.field + .field { margin-top: 28rpx; }
	.label { font-size: 24rpx; color: #6b7a5e; font-weight: 600; margin-bottom: 12rpx; }
	.input { height: 88rpx; padding: 0 28rpx; border-radius: 20rpx; font-size: 28rpx; color: #1a2114; background: #f6f8f3; font-weight: 500; }
	.textarea { display: block; height: 180rpx; padding: 24rpx 28rpx; border-radius: 20rpx; font-size: 26rpx; line-height: 1.7; color: #1a2114; background: #f6f8f3; font-weight: 500; }
	.textarea.short { height: 140rpx; }
	.location-box { min-height: 140rpx; }
	.hour-row { display: flex; align-items: center; gap: 16rpx; }
	.hour-input { flex: 1; line-height: 88rpx; text-align: center; }
	.hour-sep { font-size: 24rpx; color: #6b7a5e; }
	.doc-section { margin-top: 28rpx; }
	.doc-section-title { display: block; margin-bottom: 16rpx; font-size: 26rpx; font-weight: 700; color: #3f4f36; }
	.id-card-row { display: flex; gap: 20rpx; }
	.id-card-item { flex: 1; min-width: 0; }
	.id-card-label { display: block; margin-bottom: 12rpx; font-size: 22rpx; color: #829279; }
	.doc-upload-box { overflow: hidden; border-radius: 20rpx; background: #f6f8f3; border: 2rpx solid transparent; }
	.doc-upload-box.wide { width: 100%; }
	.doc-upload-box.selected { background: #f6ffea; border-color: rgba(103, 174, 40, 0.35); }
	.doc-image, .upload-guide { display: block; width: 100%; height: 220rpx; }
	.doc-image.wide-image, .upload-guide.wide-guide { height: 240rpx; }
	.upload-guide.compact { height: 220rpx; }
	.doc-image { background: #edf1e9; }
	.upload-guide { display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f2f6ed; }
	.guide-action { margin-top: 12rpx; font-size: 22rpx; color: #829279; }
	.carousel-scroll { width: 100%; white-space: nowrap; margin-top: 8rpx; }
	.carousel-row { display: inline-flex; gap: 16rpx; padding-bottom: 8rpx; }
	.carousel-item, .carousel-add { width: 180rpx; flex-shrink: 0; }
	.carousel-image { width: 180rpx; height: 180rpx; border-radius: 16rpx; background: #edf1e9; }
	.carousel-remove { display: block; margin-top: 8rpx; text-align: center; font-size: 22rpx; color: #67ae28; }
	.carousel-add { height: 180rpx; border-radius: 16rpx; background: #f2f6ed; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #829279; }
	.tip-text { margin-top: 20rpx; font-size: 22rpx; line-height: 1.7; color: #8a9a7a; padding: 20rpx 24rpx; background: #f9fbf6; border-radius: 16rpx; }
	.submit-btn { box-sizing: border-box; width: 100%; height: 96rpx; line-height: 96rpx; margin-top: 12rpx; margin-bottom: 40rpx; border-radius: 48rpx; font-size: 32rpx; font-weight: 600; color: #ffffff; background: linear-gradient(135deg, #7ed321, #6bc01a); box-shadow: 0 8rpx 28rpx rgba(126, 211, 33, 0.35); border: none; }
</style>
