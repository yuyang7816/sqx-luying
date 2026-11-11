<template>
	<view class="page">
		<view class="hero-card">
			<text class="hero-title">租赁指南</text>
			<text class="hero-desc">从挑选装备、提交订单到归还退押，一次看清整套租赁流程。</text>
		</view>
		<view class="card">
			<text class="section-title">租赁流程</text>
			<view v-for="(item, index) in flowList" :key="item.title" class="flow-row">
				<view class="flow-index">{{ index + 1 }}</view>
				<view class="flow-main"><text class="flow-title">{{ item.title }}</text><text class="flow-desc">{{ item.desc }}</text></view>
			</view>
		</view>
		<view class="card">
			<text class="section-title">常见问题</text>
			<view v-for="item in faqList" :key="item.q" class="faq-row"><text class="faq-q">{{ item.q }}</text><text class="faq-a">{{ item.a }}</text></view>
		</view>
	</view>
</template>

<script setup>
	import { ref } from 'vue';
	import { onShow } from '@dcloudio/uni-app';
	import { fetchHelpContent } from '@/common/api/content';

	const defaultFlow = [
		{ title: '挑选装备', desc: '在首页或分类页选择租赁套餐、小时租、整日租或过夜租装备。' },
		{ title: '提交订单', desc: '选择门店自提或同城送达，确认租期、地址、优惠券和押金。' },
		{ title: '领取装备', desc: '按预约时间到店核销或等待配送，领取前请确认配件齐全。' },
		{ title: '按时归还', desc: '归还后平台验收，设备完好则押金按规则退回。' }
	];
	const defaultFaq = [
		{ q: '押金什么时候退回？', a: '通常在归还验收完成后 1-3 个工作日原路退回。' },
		{ q: '可以续租吗？', a: '可以，库存允许时可在订单详情中发起续租。' },
		{ q: '支持同城配送吗？', a: '支持，确认订单页选择同城送达即可查看配送费用。' }
	];

	const flowList = ref(defaultFlow);
	const faqList = ref(defaultFaq);

	onShow(async () => {
		try {
			const remote = await fetchHelpContent();
			if (remote?.flowList?.length) flowList.value = remote.flowList;
			if (remote?.faqList?.length) faqList.value = remote.faqList;
		} catch (error) {
			// 使用默认静态内容
		}
	});
</script>

<style scoped>
	.page { box-sizing: border-box; min-height: 100vh; background: #f5f7ff; overflow-x: hidden; }
	.hero-card { box-sizing: border-box; margin: 24rpx; padding: 30rpx; border-radius: 28rpx; background: linear-gradient(135deg, #8ee226, #9be83d); }
	.hero-title, .hero-desc, .section-title, .flow-title, .flow-desc, .faq-q, .faq-a { display: block; }
	.hero-title { font-size: 38rpx; font-weight: 700; color: #fff; }
	.hero-desc { margin-top: 12rpx; font-size: 24rpx; line-height: 1.7; color: rgba(255,255,255,0.92); }
	.card { box-sizing: border-box; margin: 0 24rpx 24rpx; padding: 28rpx; background: #fff; border-radius: 28rpx; overflow: hidden; }
	.section-title { font-size: 30rpx; font-weight: 700; color: #1f2937; }
	.flow-row { display: flex; margin-top: 24rpx; }
	.flow-index { width: 44rpx; height: 44rpx; line-height: 44rpx; text-align: center; border-radius: 50%; font-size: 24rpx; font-weight: 700; color: #fff; background: #7ed321; }
	.flow-main { flex: 1; min-width: 0; margin-left: 16rpx; }
	.flow-title { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.flow-desc { margin-top: 8rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
	.faq-row { margin-top: 26rpx; }
	.faq-q { font-size: 26rpx; font-weight: 700; color: #1f2937; }
	.faq-a { margin-top: 8rpx; font-size: 22rpx; line-height: 1.7; color: #6b7280; }
</style>
