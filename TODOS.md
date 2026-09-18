# c001apk 重构 TODO

## UI 改造（用户提出）

### A. 底栏重排
- [x] **A1**: 底栏去掉「设置」tab
- [x] **A2**: 「消息」tab 改名「我的」
- [x] **A3**: nav_menu.xml + MainActivity.kt 改为 2 个 tab

### B. 设置入口迁移
- [x] **B1**: 我的页面右上角放「设置」图标
- [x] **B2**: 点击图标 → 打开 SettingsActivity（从 MainActivity 剥出来）

### C. 设置 → 一级菜单分组
| 一级菜单 | 包含项 |
|---|---|
| 高级 | 数字联盟id、机型参数、图片画质 |
| 外观 | 系统主题色、主题颜色、深色主题、纯色主题、字体调节、显示表情 |
| 隐私 | 用户黑名单 |
| 推荐流相关 | 话题黑名单、关键字屏蔽、关键字用户、关键字节点、外链打开、极简图标卡、记录历史、深色滤镜、检查更新 |
| 其他 | 关于、清理缓存 |

### D. 我的页面改造
- **D1**: 顶部用户资料区保留（头像/等级/经验值/菜单）
- **D2**: 九宫格 6 项：本地收藏、浏览历史、我的常去、我的赞、我的回复、我的装备
- **D3**: 我的装备 → 二级菜单（我的设备 / 修改设备 / 退出登录）
- **D4**: 下方保留现有消息/通知列表（关注/粉丝/动态/@我的/收到的赞 等）

## 分支（创建）
- [x] `beta`（基于当前 main）
- [x] `debug`
- [x] `update`

## 旧 P0 重构建议（用户已批准，未做）
- [ ] 拆 `feed/` 巨型目录 → 独立 `feedreply/` `feedvote/` `feedquestion/`
- [ ] 拆 `others/` 杂物抽屉 → `about/` `debug/` `webview/` `link/`
- [ ] 重命名 `messagedetail/MessageFragment` → `MessageDetailFragment`
- [ ] Adapter 改名：`MessageFirst/Second/Third` → 业务命名
- [ ] `feed/reply/attopic/` 的 `SearchAdapter` → `AtUserSearchAdapter`
- [ ] `coolpic/` 并入 `hometopic/` 或新建 `discover/`
- [ ] 审视 `base/` 是否需要 4 个 Fragment 基类

## 编译/验证
- [ ] 推送到 origin 触发 GitHub Actions 编译
- [ ] 下载 APK 到本地（签名后）通过 adb 安装测试