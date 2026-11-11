<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-title">租赁规则说明</text>
			<text class="hero-desc">下单前请阅读租赁协议、押金规则与损坏赔付说明，便于确认取货、使用和归还责任。</text>
		</view>

		<view class="tab-wrap">
			<u-tabs :list="tabs" keyName="label" :current="currentTabIndex" :scrollable="true" lineColor="#7ed321" :activeStyle="tabActiveStyle" :inactiveStyle="tabInactiveStyle" @change="handleTabChange"></u-tabs>
		</view>

		<view v-if="loading" class="card loading-card">
			<text class="loading-text">协议内容加载中...</text>
		</view>

		<view v-else-if="remoteHtml" class="card rich-card">
			<rich-text :nodes="remoteHtml"></rich-text>
		</view>

		<template v-else>
			<view class="card summary-card">
				<text class="section-title">{{ currentSection.title }}</text>
				<text class="section-desc">{{ currentSection.desc }}</text>
			</view>

			<view class="card">
				<view v-for="item in currentSection.items" :key="item.title" class="rule-item">
					<text class="rule-title">{{ item.title }}</text>
					<text class="rule-desc">{{ item.desc }}</text>
				</view>
			</view>
		</template>

		<view class="card tip-card">
			<text class="section-title">温馨提示</text>
			<text class="tip-text">实际退押、赔付与续租结果以订单状态、门店验收结果和平台通知为准。</text>
			<text class="tip-text">如需人工确认，可在订单详情页联系门店或进入客服中心。</text>
		</view>
	</view>
</template>

<script setup>
	import { computed, ref, watch } from 'vue';
	import { onLoad } from '@dcloudio/uni-app';
	import { fetchAgreementHtml } from '@/common/api/content';

	const tabs = [
		{ key: 'rental', label: '租赁协议' },
		{ key: 'deposit', label: '押金规则' },
		{ key: 'damage', label: '赔付说明' },
		{ key: 'privacy', label: '隐私协议' }
	];

	const sectionMap = {
		rental: {
			title: '租赁协议',
			desc: '明确订单生效、取货核验、租期使用、续租归还等基础规则。',
			items: [
				{ title: '订单生效', desc: '订单支付成功后方可锁定库存；未支付订单以系统自动释放为准。' },
				{ title: '取货核验', desc: '到店取货需核对联系人、手机号和订单信息；同城配送需保持电话畅通。' },
				{ title: '租期计算', desc: '租期以确认订单页选择的时段为准，超时未归还可能产生续租或超时费用。' },
				{ title: '续租与取消', desc: '库存允许时可申请续租；已锁定库存的订单在起租前取消可能按规则扣除服务费用。' }
			]
		},
		deposit: {
			title: '押金规则',
			desc: '说明押金收取、冻结、退回与免押使用条件。',
			items: [
				{ title: '押金收取', desc: '部分装备按品类和套餐收取押金；支持信用免押的订单会在下单页明确提示。' },
				{ title: '押金冻结', desc: '押金在支付成功后冻结，用于覆盖设备遗失、损坏或逾期未归还等风险。' },
				{ title: '押金退回', desc: '设备归还并验收完成后，平台会在 1-3 个工作日内原路退回可退押金。' },
				{ title: '异常扣除', desc: '如存在遗失、损坏、严重污损或逾期费用，平台会按实际费用从押金中扣除。' }
			]
		},
		damage: {
			title: '损坏赔付说明',
			desc: '针对常见的配件缺失、污损、破损和遗失情况说明赔付口径。',
			items: [
				{ title: '轻微污损', desc: '可清洁恢复的普通污渍一般不额外收费，但需避免油渍、炭火灼伤等难以恢复情况。' },
				{ title: '配件缺失', desc: '如地钉、营绳、防潮垫等配件缺失，将按配件采购价或门店公示价格赔付。' },
				{ title: '主体破损', desc: '帐篷杆断裂、面料撕裂、桌椅结构损坏等情况，按维修成本或折旧后重置成本赔付。' },
				{ title: '整件遗失', desc: '整件设备遗失或无法归还时，平台将按设备公示赔付标准进行扣款处理。' }
			]
		},
		privacy: {
			title: '隐私协议',
			desc: '说明平台如何收集、使用和保护你的个人信息。',
			items: [
				{ title: '信息收集', desc: '为完成登录、下单、配送和客服服务，平台会收集手机号、联系人、地址等必要信息。' },
				{ title: '信息使用', desc: '信息仅用于订单履约、账户安全、活动通知和客服回访，不会用于无关商业用途。' },
				{ title: '信息保护', desc: '平台采用加密传输与权限隔离机制保护用户数据，未经授权不会向第三方出售个人信息。' },
				{ title: '用户权利', desc: '你可在设置页查看账号信息，后续可联系客服申请更正、导出或注销相关数据。' }
			]
		}
	};

	const currentKey = ref('rental');
	const remoteHtml = ref('');
	const loading = ref(false);
	const tabActiveStyle = { color: '#59b50e', fontWeight: '700' };
	const tabInactiveStyle = { color: '#6b7280' };
	const currentTabIndex = computed(() => Math.max(tabs.findIndex((item) => item.key === currentKey.value), 0));
	const currentSection = computed(() => sectionMap[currentKey.value] || sectionMap.rental);

	const loadAgreementContent = async (type) => {
		loading.value = true;
		remoteHtml.value = '';
		try {
			const html = await fetchAgreementHtml(type);
			if (html && html.trim() && !/^<p>暂无协议内容<\/p>$/i.test(html.trim())) {
				remoteHtml.value = html;
			}
		} catch (error) {
			// 使用本地静态兜底
		} finally {
			loading.value = false;
		}
	};

	const handleTabChange = (item) => {
		currentKey.value = item.key;
	};

	watch(currentKey, (type) => {
		loadAgreementContent(type);
	}, { immediate: true });

	onLoad((options) => {
		if (options.type && sectionMap[options.type]) {
			currentKey.value = options.type;
		}
	});
</script>

<style scoped>
	.page { min-height: 100vh; padding: 24rpx; background: #f5f7ff; }
	.hero-card { padding: 32rpx; border-radius: 32rpx; background: linear-gradient(135deg, #8ee226, #9ce741); }
	.hero-title, .hero-desc, .section-title, .section-desc, .rule-title, .rule-desc, .tip-text, .loading-text { display: block; }
	.hero-title { font-size: 40rpx; font-weight: 700; color: #ffffff; }
	.hero-desc { margin-top: 14rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.92); }
	.tab-wrap { margin-top: 24rpx; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item) { padding: 0 28rpx; }
	.tab-wrap :deep(.u-tabs__wrapper__nav__item__text) { font-size: 24rpx; white-space: nowrap; }
	.card { margin-top: 24rpx; padding: 28rpx; border-radius: 28rpx; background: #ffffff; }
	.section-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.section-desc { margin-top: 12rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
	.rule-item + .rule-item { margin-top: 24rpx; padding-top: 24rpx; border-top: 1rpx solid #eef2f7; }
	.rule-title { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.rule-desc { margin-top: 10rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
	.tip-text { margin-top: 14rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
	.tip-card { margin-bottom: 24rpx; }
	.loading-text { font-size: 24rpx; color: #6b7280; text-align: center; }
	.rich-card :deep(p), .rich-card :deep(div) { font-size: 24rpx; line-height: 1.8; color: #374151; }
</style>
