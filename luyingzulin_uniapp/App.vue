<script setup>
	import {
		onLaunch,
		onShow,
		onHide
	} from '@dcloudio/uni-app';
	import fontUtil from '@/uni_modules/uview-plus/components/u-icon/util.js';
	import { refreshSession } from '@/common/auth';
	import { captureInviteFromLaunchOptions } from '@/common/inviteUtils';
	import { loadOaOAuthConfig } from '@/common/oaOAuthConfig';

	onLaunch(async (options) => {
		// H5 仅依赖下方 @font-face，避免 loadFontFace 与 CSS 字体双通道冲突
		// #ifndef H5
		fontUtil.loadFont();
		// #endif
		refreshSession();
		captureInviteFromLaunchOptions(options);
		await loadOaOAuthConfig();
	});
	onShow((options) => {
		captureInviteFromLaunchOptions(options);
	});
	onHide(() => {
		console.log('onHide')
	});
</script>

<style lang="scss">
	/*每个页面公共css */
	//uview-plus公共样式
	@import "@/uni_modules/uview-plus/index.scss";
	//自定义公共样式
	@import "@/static/css/main.css";
	@import "@/static/css/iconfont.css";

	page {
		background-color: #F5F7FF;
	}

	:root,
	page {
		--theme-page-bg: #f5f7ff;
		--theme-card-bg: #ffffff;
		--theme-primary: #7ed321;
		--theme-primary-strong: #59b50e;
		--theme-primary-deep: #5f8c00;
		--theme-primary-soft: #eefadb;
		--theme-primary-soft-2: #f2fde5;
		--theme-hero-start: #8ee226;
		--theme-hero-end: #9ce741;
		--theme-hero-deep-start: #74cf1f;
		--theme-hero-deep-end: #8cdd34;
		--theme-hero-soft-start: #a4e85f;
		--theme-hero-soft-end: #b7ef75;
		--theme-text-main: #1f2937;
		--theme-text-secondary: #6b7280;
		--theme-text-muted: #8f97a5;
		--theme-text-light: rgba(255, 255, 255, 0.92);
		--theme-border: #eef2f7;
		--native-tabbar-height: 100rpx;
		--native-tabbar-offset: calc(var(--native-tabbar-height) + env(safe-area-inset-bottom));
	}

	/* #ifdef H5 */
	uni-page-head,
	.uni-page-head {
		display: none !important;
	}
	/* #endif */
</style>
