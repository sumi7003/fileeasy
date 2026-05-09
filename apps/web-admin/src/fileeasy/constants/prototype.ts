import type {
  FileEasyPrototypeSceneId,
  FileEasyPrototypeSceneMeta,
  FileEasyPrototypeStage,
} from '../types/prototype';

export const FILEEASY_PROTOTYPE_SCENE_ORDER: FileEasyPrototypeSceneId[] = [
  'apk-home',
  'upload-login',
  'upload-active',
  'upload-alerts',
  'admin-list',
  'admin-batch',
];

export const FILEEASY_PROTOTYPE_SCENES: Record<
  FileEasyPrototypeSceneId,
  FileEasyPrototypeSceneMeta
> = {
  'apk-home': {
    id: 'apk-home',
    label: 'APK 首页',
    eyebrow: 'Step 1',
    stage: 'APK 端',
    summary: '展示服务状态、二维码、地址和管理入口。',
    interactionHints: [
      { label: '二维码', detail: '可直接跳转到扫码上传页' },
      { label: '模式切换', detail: '可演示首次安装、正常运行和无网络状态' },
      { label: '服务状态', detail: '可切换启动中、前台运行中和已就绪阶段' },
    ],
  },
  'upload-login': {
    id: 'upload-login',
    label: '扫码登录',
    eyebrow: 'Step 2',
    stage: '扫码端',
    summary: '先看到上传页，再输入密码进入上传。',
    interactionHints: [
      { label: '登录', detail: '密码与首页修改密码联动' },
      { label: '返回首页', detail: '可回看 APK 首页的二维码与状态' },
    ],
  },
  'upload-active': {
    id: 'upload-active',
    label: '上传过程',
    eyebrow: 'Step 3',
    stage: '扫码端',
    summary: '单文件、多文件、断点恢复与完成态都可点击演示。',
    interactionHints: [
      { label: '选择文件', detail: '可推进上传任务状态' },
      { label: '任务卡', detail: '可切换上传中、失败、恢复和完成状态' },
      { label: '进入管理页', detail: '可联动跳转到文件管理工作台' },
    ],
  },
  'upload-alerts': {
    id: 'upload-alerts',
    label: '异常提示',
    eyebrow: 'Step 4',
    stage: '扫码端',
    summary: '空间不足、网络中断、续传过期都就地提示。',
    interactionHints: [
      { label: '异常切换', detail: '可演示空间不足、网络中断和任务过期' },
      { label: '任务卡', detail: '可结合异常状态继续切换演示结果' },
    ],
  },
  'admin-list': {
    id: 'admin-list',
    label: '管理页',
    eyebrow: 'Step 5',
    stage: '管理端',
    summary: '搜索、预览、下载、重命名、删除的主工作台。',
    interactionHints: [
      { label: '类型文件夹', detail: '可按文档、视频、图片、音频、压缩包切换' },
      { label: '文件操作', detail: '预览、重命名、删除和下载都可点击演示' },
      { label: '批量模式', detail: '可进入专注批量删除场景' },
    ],
  },
  'admin-batch': {
    id: 'admin-batch',
    label: '批量删除',
    eyebrow: 'Step 6',
    stage: '管理端',
    summary: '批量模式切换后的专注态页面。',
    interactionHints: [
      { label: '文件勾选', detail: '可切换文件选择状态' },
      { label: '批量删除', detail: '可演示删除确认弹层' },
      { label: '退出批量模式', detail: '可回到普通文件管理列表' },
    ],
  },
};

export const FILEEASY_PROTOTYPE_SCENE_LIST: FileEasyPrototypeSceneMeta[] =
  FILEEASY_PROTOTYPE_SCENE_ORDER.map((sceneId) => FILEEASY_PROTOTYPE_SCENES[sceneId]);

export const getFileEasyPrototypeSceneMeta = (
  sceneId: FileEasyPrototypeSceneId,
): FileEasyPrototypeSceneMeta => FILEEASY_PROTOTYPE_SCENES[sceneId];

export const getFileEasyPrototypeStage = (
  sceneId: FileEasyPrototypeSceneId,
): FileEasyPrototypeStage => FILEEASY_PROTOTYPE_SCENES[sceneId].stage;
