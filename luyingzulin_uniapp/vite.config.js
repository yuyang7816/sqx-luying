import {
	defineConfig,
	loadEnv
} from 'vite';
import uni from '@dcloudio/vite-plugin-uni';
import path from 'path';

export default defineConfig(({
	mode
}) => {
	const env = loadEnv(mode, __dirname);
	const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:9188';
	return {
		plugins: [uni()],
		transpileDependencies: ['uview-plus'],
		resolve: {
			alias: {
				'@': path.resolve(__dirname),
				'uni_modules': path.resolve(__dirname, 'uni_modules')
			}
		},
		define: {
			'import.meta.env.VITE_USE_MOCK': JSON.stringify(env.VITE_USE_MOCK ?? 'false'),
			'import.meta.env.VITE_API_BASE_URL': JSON.stringify(env.VITE_API_BASE_URL ?? '/sqx_fast/'),
			'import.meta.env.VITE_H5_BASE_URL': JSON.stringify(env.VITE_H5_BASE_URL ?? 'https://luyingzl.xianmaxiong.com')
		},
		server: {
			host: '0.0.0.0',
			port: Number(env.VITE_DEV_PORT || 5173),
			proxy: {
				'/sqx_fast': {
					target: apiProxyTarget,
					changeOrigin: true
				},
				'/upload': {
					target: apiProxyTarget,
					changeOrigin: true,
					rewrite: (path) => `/sqx_fast${path}`
				},
				'/media-proxy': {
					target: 'https://tu.mianyazuju.com',
					changeOrigin: true,
					rewrite: (path) => path.replace(/^\/media-proxy/, '')
				}
			}
		}
	};
});