<div align="center">

# **省钱兄 JAVA 露营装备租赁项目全套开源系统（JAVA + Uni-app + Vue3）**

**服务端 · 管理端 · 用户端**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-1.8-orange.svg)](docs/服务端-功能说明.md)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.11-6DB33F.svg)](docs/服务端-功能说明.md)
[![Vue](https://img.shields.io/badge/Vue-3-42b883.svg)](luyingzulin_admin/README.md)
[![uni-app](https://img.shields.io/badge/uni--app-用户端-2b9939.svg)](luyingzulin_uniapp/manifest.json)

八年沉淀，分批次开源由 AI 辅助开发的系统源码。  
开源为生态，服务才是根基。欢迎各位开发者一同交流。

</div>

---
⭐ 如果觉得项目不错，请点击右上角 Star 收藏支持！您的 Star 是我们持续开源迭代的最大动力！

## **1. 项目简介**

**省钱兄露营装备租赁项目全套开源系统**是一套面向同城/本地露营装备租赁业务的三端系统，采用前后端分离架构，服务端、管理端、用户端全套代码开源。

服务端基于 **Spring Boot 2.6.11**（**Java 8**）构建，负责接口、数据库，以及支付、短信、地图等外部能力。启动后控制台会打印「露营租赁项目启动成功」。
管理端基于 **Vue 3** 开发，供运营人员在电脑浏览器中维护分类、商品、订单、商户与营销配置。
用户端基于 **uni-app** 开发，覆盖 H5 与微信小程序。用户可浏览帐篷、天幕、桌椅等装备，按日租/小时租/过夜租下单；商户在同一应用内管理商品、库存、取货核销、归还验收与经营钱包。

本仓库为服务端、管理端、用户端全套源码。线上 H5 演示见 [https://luyingzl.xianmaxiong.com](https://luyingzl.xianmaxiong.com)（与仓库用户端同源）。

- **官方网站开源计划**：[https://www.xiansqx.com](https://www.xiansqx.com)
- **开源计划**：[https://opensource.xiansqx.cn](https://opensource.xiansqx.cn)
- **官方文档**：[功能说明与运行教程](docs/服务端-运行教程.md)
- **Gitee 仓库**：[https://gitee.com/maxiaodong1996/sqx-luying.git](https://gitee.com/maxiaodong1996/sqx-luying.git)
- **GitHub 仓库**：[https://github.com/yuyang7816/sqx-luying.git](https://github.com/yuyang7816/sqx-luying.git)

## **2. 移动端界面预览**

截图来自本项目线上 H5 演示站（与 `luyingzulin_uniapp` 构建产物一致），手机视口截取。

<div align="center">

| 首页 | 分类 | 装备详情 | 租物车 | 我的 |
| :---: | :---: | :---: | :---: | :---: |
| <img src="https://tu.mianyazuju.com/2026/11/03/abc9c3db-a1d5-47a5-9c45-e6b7ead05e9f.png" width="160" alt="首页" /> | <img src="https://tu.mianyazuju.com/2026/11/03/8c075368-8a6a-4063-a4ec-6e1bb97240de.png" width="160" alt="分类" /> | <img src="https://tu.mianyazuju.com/2026/11/03/793e44ba-0074-475e-b0c3-4d3b7323a4a5.png" width="160" alt="装备详情" /> | <img src="https://tu.mianyazuju.com/2026/11/03/0d2d1ea9-16e7-494e-9372-16801b1d689e.png" width="160" alt="租物车" /> | <img src="https://tu.mianyazuju.com/2026/11/03/6fcdefc4-0b8b-443e-be74-c12d810aa1c6.png" width="160" alt="我的" /> |

</div>

## **3. 开源说明**

> **【省钱兄科技｜开源计划启动】**
>
> 八年沉淀，我们将分批次开源全套由 AI 辅助开发的系统源码。  
> 本项目采用 **Apache 2.0** 许可协议，源码供学习、技术研究、商业使用、二次开发与参考使用。  
> **开源为生态，服务才是根基；** 欢迎各位开发者一同交流。

## **4. 快速开始**

建议顺序：**先服务端，再管理端，最后用户端。** 管理端和用户端都要等服务端已经启动，才能登录和加载数据。

**说明：** 仓库里**不包含** `node_modules`（体积大、随系统生成）。克隆或下载后，在 **`luyingzulin_admin`**、**`luyingzulin_uniapp`** 各自目录下**必须先执行一次** `npm install`，再启动开发命令。详细步骤见下方与 [docs](docs/管理端-运行教程.md) 中的运行教程。

### **4.1 服务端**

安装 **JDK 8**、**Maven**、**MySQL 8**。新建数据库 `luyingzulin`，导入 [`luyingzulin_server/db/lyzl.sql`](luyingzulin_server/db/lyzl.sql)。

本地开发时，将 [`luyingzulin_server/sqx-application/src/main/resources/application.yml`](luyingzulin_server/sqx-application/src/main/resources/application.yml) 中的 `spring.profiles.active` 改为 **`dev`**，并按本机环境修改 [`application-dev.yml`](luyingzulin_server/sqx-application/src/main/resources/application-dev.yml) 里的数据库账号密码（默认连接 `localhost:3306/luyingzulin`）。

在 `luyingzulin_server` 目录执行 Maven 启动 `sqx-application` 模块。默认 **端口 9188**，上下文路径 **`/sqx_fast`**。接口文档：`http://127.0.0.1:9188/sqx_fast/doc.html`。

当前配置下 **Redis 非必须**（`sqx.redis.open: false`）；若开启 Shiro/Redis 相关能力，需本机 Redis 可用并在配置中打开开关。

更完整的模块划分与接口约定见 [服务端功能说明](docs/服务端-功能说明.md)；在线接口文档启动后访问 `http://127.0.0.1:9188/sqx_fast/doc.html`。

### **4.2 管理端**

安装 **Node.js 18** 或更高版本。进入 `luyingzulin_admin` 目录，**首次运行前**安装依赖并启动：

```bash
cd luyingzulin_admin
npm install
npm run dev
```

`npm install` 会根据 `package.json` / `package-lock.json` 在本机生成 `node_modules`，只需在依赖变更或新克隆后执行。

开发站点默认端口 **5173**。API 地址在 `.env.development` 中配置，需指向已启动的服务端（如 `http://127.0.0.1:9188/sqx_fast/`）。

默认管理端账号可参考架构文档（常见为 `admin` / `123456`，以导入后的数据库为准）。

### **4.3 用户端**

**命令行（联调本机服务端，推荐）：** 进入 `luyingzulin_uniapp`，首次先 `npm install`，再 `npm run dev:integration`（H5 端口 **5573**，见 `manifest.json`）。

**HBuilderX：** 打开 `luyingzulin_uniapp` 目录运行到浏览器或微信开发者工具。若 uView 等组件样式异常，同样在该目录执行一次 `npm install` 后再运行。

开发时可通过 `.env` / `vite.config.js` 将接口代理到本地服务端；生产默认可对接线上 API。

地图、短信、微信、支付宝等密钥需自行申请，并在管理端「系统配置」中填写。

## **5. 文档**

| 想做的事 | 打开哪一份 |
|---|---|
| 把服务端跑起来 | [服务端运行教程](docs/服务端-运行教程.md) |
| 看服务端能做什么 | [服务端功能说明](docs/服务端-功能说明.md) |
| 把管理端跑起来 | [管理端运行教程](docs/管理端-运行教程.md) |
| 看管理端能做什么 | [管理端功能说明](docs/管理端-功能说明.md) |
| 把用户端跑起来 | [用户端运行教程](docs/用户端-运行教程.md) |
| 看用户端能做什么 | [用户端功能说明](docs/用户端-功能说明.md) |
| 了解服务端架构与接口规范 | [服务端功能说明](docs/服务端-功能说明.md) |

## **6. 目录结构**

仓库根目录只放三端代码、说明和协议。编译出来的 `target`、`dist`、`unpackage`、`node_modules` 是运行时生成的，不需要改。

### **6.1 仓库根目录**

| 路径 | 是什么 |
|---|---|
| `luyingzulin_server` | **服务端**。接口、数据库脚本、支付和短信等能力都在这里 |
| `luyingzulin_admin` | **管理端**。运营人员在电脑浏览器里打开的后台 |
| `luyingzulin_uniapp` | **用户端**。用户和商户用的手机应用，也可在浏览器 H5 预览 |
| `docs` | 开发约定、清单与 README 预览截图 |
| `LICENSE` | Apache License 2.0 全文 |

### **6.2 服务端 `luyingzulin_server`**

| 路径 | 是什么 |
|---|---|
| `sqx-application` | 启动入口。主程序在 `SqxApplication.java`，配置在 `src/main/resources` |
| `sqx-modules/sqx-app` | 露营租赁业务。商品、购物车、订单、取还货、续租、商户、钱包、邀请等 |
| `sqx-modules/sqx-system` | 管理员、角色、菜单、字典、系统配置 |
| `sqx-modules/sqx-generator` | 按数据表生成代码的工具，日常运行用不到 |
| `sqx-common/sqx-common-web` | 网页请求、统一异常提示 |
| `sqx-common/sqx-common-mybatis` | 访问 MySQL |
| `sqx-common/sqx-common-redis` | 访问 Redis（可选） |
| `sqx-common/sqx-common-shiro` | 管理端登录和权限 |
| `sqx-common/sqx-common-pay` | 微信支付、支付宝 |
| `sqx-common/sqx-common-sms` | 短信宝、阿里云短信、腾讯云短信 |
| `sqx-common/sqx-common-mail` | 发邮件 |
| `sqx-common/sqx-common-file` | 上传图片。可以存本机硬盘，也可以存阿里云 |
| `sqx-common/sqx-common-doc` | 接口文档页面 |
| `sqx-common/sqx-common-websocket` | 在线客服的实时消息 |
| `sqx-common/sqx-common-core` | 三端都会用到的公共工具 |
| `db/lyzl.sql` | 要导入的完整数据库，库名是 `luyingzulin` |
| `docs/服务端-功能说明.md` | 服务端职责与对接的外部能力说明 |

### **6.3 管理端 `luyingzulin_admin`**

| 路径 | 是什么 |
|---|---|
| `src/views/rental` | 租赁业务。分类、商品、库存、订单、定价、验收记录 |
| `src/views/merchant` | 商户入驻、商户列表、保证金退款 |
| `src/views/business` | 轮播、优惠券、提现、消息、平台客服、邀请海报等运营能力 |
| `src/views/system` | 系统页面。管理员、角色、菜单、权限、字典、操作日志、系统配置 |
| `src/views/user` | C 端用户列表 |
| `src/views/LoginView.vue` | 登录页 |
| `src/views/DashboardView.vue` | 登录后的数据统计 |
| `src/api` | 各页面调用服务端的接口封装 |
| `src/router` | 页面地址和菜单对应关系 |
| `.env.development` | 开发时服务端地址 |

### **6.4 用户端 `luyingzulin_uniapp`**

| 路径 | 是什么 |
|---|---|
| `pages` | 底部四个主入口：首页、分类、租物车、我的 |
| `packageGoods` | 装备列表、装备详情、搜索 |
| `packageOrder` | 确认订单、支付结果、订单列表与详情、归还、续租 |
| `packageUser` | 登录注册、地址、钱包、优惠券、会员、邀请、设置等 |
| `packageMerchant` | 商户入驻、商品与库存、订单核销、验收、经营钱包与消息 |
| `packageContent` | 消息、IM 客服、协议、城市选择、押金说明等 |
| `common` | 接口地址、登录、地图、上传、支付等公共逻辑 |
| `components` | 页面里重复使用的组件 |
| `static` | 图标与默认图片 |
| `pages.json` | 页面路由与底部导航 |
| `manifest.json` | 应用名称、小程序 AppId、H5 端口与地图密钥 |

界面组件使用 **uView Plus**。

## **7. 核心特性与技术栈**

### **7.1 核心特性**

· **露营装备按分类浏览，支持日租 / 小时租 / 过夜租多种计费**  
· **租物车加购、确认订单、在线支付与押金管理**  
· **到店/自提点取货核销、使用中、归还申请与续租**  
· **商户入驻、商品发布、库存调整、订单处理与验收**  
· **优惠券、会员、钱包充值提现、邀请分销**  
· **平台客服与用户-商户 IM 咨询**  
· **管理端数据统计、审核、上下架与权限配置**  
· **服务端统一处理登录、订单状态与支付回调**

业务细节与接口分组见 [服务端功能说明](docs/服务端-功能说明.md) 与 Swagger 文档 `doc.html`。

版本以仓库里的配置文件为准。没写死版本号的，按上文快速开始安装对应大版本即可。

### **7.2 服务端**

| 类别 | 技术 | 版本 | 用来做什么 |
|---|---|---|---|
| 语言 | **Java** | **1.8** | 整个服务端。不能换成 11 或 17 |
| 框架 | **Spring Boot** | **2.6.11** | 提供网页接口，端口 `9188`，路径 `/sqx_fast` |
| 构建 | **Maven** | 3.6 及以上 | 下载依赖、编译、启动 |
| 数据库 | **MySQL** | 8.0 及以上 | 业务数据。库名 `luyingzulin`，脚本是 `db/lyzl.sql` |
| 数据库访问 | **MyBatis-Plus** | 3.5.3.2 | 读写表 |
| 连接池 | **Druid** | 1.2.19 | 管理数据库连接 |
| 缓存 | **Redis**、**Redisson** | 可选 | 默认关闭；开启后用于登录态等 |
| 管理端登录 | **Apache Shiro** | 1.4.0 | 后台账号、角色、菜单权限 |
| 用户端登录 | **JWT**（jjwt） | 0.9.1 | 用户和商户登录后的凭证 |
| 接口文档 | **Knife4j** | 4.4.0 | 浏览器打开 `doc.html` 查看接口 |
| JSON | **Fastjson2** | 2.0.41 | 接口数据格式 |
| 工具库 | **Hutool** | 5.8.16 | 日期、字符串等常用处理 |
| 登录验证码 | **Kaptcha** | 0.0.9 | 管理端登录页上的图片验证码 |
| 对象存储 | **阿里云 OSS SDK** | 3.4.0 | 可选。不填密钥时图片可以存在本机硬盘 |
| 支付 | 微信支付 SDK、支付宝 SDK | 随支付模块 | 下单、退款。密钥要自己申请 |

### **7.3 管理端**

| 类别 | 技术 | 版本 | 用来做什么 |
|---|---|---|---|
| 运行环境 | **Node.js** | 18 及以上 | 安装依赖、启动开发网站 |
| 页面框架 | **Vue** | 3.5 | 后台每一个页面 |
| 构建 | **Vite** | 5.4 | 开发时热更新，默认端口 `5173` |
| 界面组件 | **Element Plus** | 2.11 | 表格、表单、弹窗、菜单 |
| 页面跳转 | **Vue Router** | 4.5 | 登录页、统计页和各业务页的地址 |
| 状态 | **Pinia** | 3.0 | 记住是否已登录、当前主题 |
| 请求 | **Axios** | 1.9 | 把操作发给服务端 |
| 图表 | **ECharts** | 6.1 | 数据统计页上的图 |
| 样式 | **Sass** | 1.89 | 后台样式 |

### **7.4 用户端**

| 类别 | 技术 | 版本 | 用来做什么 |
|---|---|---|---|
| 框架 | **uni-app** | 用 HBuilderX 打开 | 同一套页面可运行到浏览器、微信小程序 |
| 开发工具 | **HBuilderX** | 正式版 | 编译并运行到 Chrome 或微信开发者工具 |
| 界面组件 | **uView Plus** | 3.x | 按钮、输入框、轮播、弹窗等 |
| 省市区 | **element-china-area-data** | 6.1 | 地址里选择省、市、区 |
| 浏览器预览 | H5 | 默认端口 `5573` | 电脑上看用户端 |
| 微信小程序 | 微信开发者工具 | 可选 | 要自己的小程序 AppId 才能真机预览 |

## **8. 开源协议**

<div align="center">

本仓库使用 **Apache License 2.0**

全文见 [LICENSE](LICENSE)

</div>

## **9. 社区与支持**

运行、二次开发或商务合作，可以联系 **省钱兄科技**。

使用过程中如果接口起不来、页面打不开，先按本文 **4. 快速开始** 和 `docs` 里的运行教程排查。地图、短信、微信、支付宝等密钥需要自行申请，并在管理端系统配置中填写。仍解决不了时，可通过官方渠道说明你的环境和报错。

当前项目正在测试中，如遇到 BUG，可自行修复或者留言反馈，我们将第一时间进行版本迭代。  
欢迎一起交流学习。开源是把源码开放出来，具体落地、定制和长期维护仍由服务来承接。

<div align="center">

<img src="docs/images/community-qr.jpg" alt="省钱兄科技二维码" width="220" />

**省钱兄科技**

</div>
