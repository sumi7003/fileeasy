import React, { useEffect, useMemo, useState } from 'react';
import {
  FILEEASY_PROTOTYPE_SCENE_LIST,
  FILEEASY_PROTOTYPE_SCENE_ORDER,
  getFileEasyPrototypeSceneMeta,
  getFileEasyPrototypeStage,
} from '../fileeasy/constants/prototype';
import {
  FILEEASY_PROTOTYPE_DEMO_FILES,
  FILEEASY_PROTOTYPE_FILE_KIND_GLYPHS,
  FILEEASY_PROTOTYPE_FOLDER_GLYPHS,
  FILEEASY_PROTOTYPE_FOLDER_OPTIONS,
  FILEEASY_PROTOTYPE_INITIAL_TASKS,
  FILEEASY_PROTOTYPE_UPLOAD_FEATURE_BADGES,
} from '../fileeasy/constants/prototypeData';
import {
  FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS,
  FILEEASY_PROTOTYPE_STATUS_COPY,
  FILEEASY_PROTOTYPE_UPLOAD_STATUS_TONE_MAP,
  readFileEasyPrototypeOnboardingDone,
  readFileEasyPrototypePassword,
  writeFileEasyPrototypeOnboardingDone,
  writeFileEasyPrototypePassword,
} from '../fileeasy/constants/prototypeRuntime';
import {
  FileEasyAdminPrototypeBatchScene,
  FileEasyAdminPrototypeDeleteDialog,
  FileEasyAdminPrototypeFileActions,
  FileEasyAdminPrototypeFileGrid,
  FileEasyAdminPrototypeFolderSidebar,
  FileEasyAdminPrototypePreviewDialog,
  FileEasyAdminPrototypeRenameDialog,
  FileEasyAdminPrototypeTopbar,
} from '../fileeasy/components/admin';
import {
  FileEasyApkDialog,
  FileEasyApkHomeScene,
} from '../fileeasy/components/apk';
import {
  FileEasyUploadPrototypeActiveScene,
  FileEasyUploadPrototypeAlertsScene,
  FileEasyUploadPrototypeLoginScene,
} from '../fileeasy/components/upload';
import { FileEasyPrototypeLayout } from '../fileeasy/components/prototype';
import EmptyState from '../fileeasy/components/shared/EmptyState';
import type { ApkDialog, ApkMode, ServiceStage, ServiceStep } from '../fileeasy/types/apk';
import type {
  FileEasyPrototypeDemoFile,
  FileEasyPrototypeFolderOption,
  FileEasyPrototypeSceneId,
  FileEasyPrototypeUploadTask,
} from '../fileeasy/types/prototype';
import './fileeasy-prototype.css';

const FileEasyPrototypePage: React.FC = () => {
  const [scene, setScene] = useState<FileEasyPrototypeSceneId>('apk-home');
  const [apkMode, setApkMode] = useState<ApkMode>(() =>
    readFileEasyPrototypeOnboardingDone(FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS)
      ? 'normal'
      : 'first-install',
  );
  const [apkDialog, setApkDialog] = useState<ApkDialog>(() =>
    readFileEasyPrototypeOnboardingDone(FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS)
      ? 'none'
      : 'welcome',
  );
  const [apkPasswordDraft, setApkPasswordDraft] = useState(() =>
    readFileEasyPrototypePassword(FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS),
  );
  const [apkPasswordCurrentInput, setApkPasswordCurrentInput] = useState(() =>
    readFileEasyPrototypePassword(FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS),
  );
  const [apkPasswordNext, setApkPasswordNext] = useState('');
  const [apkPasswordConfirm, setApkPasswordConfirm] = useState('');
  const [serviceStage, setServiceStage] = useState<ServiceStage>('booting');
  const [passwordValue, setPasswordValue] = useState('');
  const [passwordError, setPasswordError] = useState(false);
  const [uploadTasks, setUploadTasks] = useState<FileEasyPrototypeUploadTask[]>(
    FILEEASY_PROTOTYPE_INITIAL_TASKS,
  );
  const [alertMode, setAlertMode] = useState<'none' | 'space' | 'network' | 'expired'>('none');
  const [searchValue, setSearchValue] = useState('');
  const [selectedFolder, setSelectedFolder] = useState<FileEasyPrototypeFolderOption['key']>('all');
  const [previewFile, setPreviewFile] = useState<FileEasyPrototypeDemoFile | null>(null);
  const [renameFile, setRenameFile] = useState<FileEasyPrototypeDemoFile | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<FileEasyPrototypeDemoFile | 'batch' | null>(null);
  const [selectedFiles, setSelectedFiles] = useState<string[]>(['f1', 'f2']);
  const [savedToast, setSavedToast] = useState('');

  const currentSceneIndex = FILEEASY_PROTOTYPE_SCENE_ORDER.indexOf(scene);
  const currentSceneMeta = getFileEasyPrototypeSceneMeta(scene);

  useEffect(() => {
    writeFileEasyPrototypePassword(
      FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS,
      apkPasswordDraft,
    );
  }, [apkPasswordDraft]);

  useEffect(() => {
    writeFileEasyPrototypeOnboardingDone(
      FILEEASY_PROTOTYPE_DEMO_STORAGE_KEYS,
      apkMode !== 'first-install',
    );
  }, [apkMode]);

  const filteredFiles = useMemo(() => {
    const keyword = searchValue.trim().toLowerCase();
    return FILEEASY_PROTOTYPE_DEMO_FILES.filter((file) => {
      const matchesFolder = selectedFolder === 'all' || file.folder === selectedFolder;
      const matchesKeyword = !keyword || file.name.toLowerCase().includes(keyword);
      return matchesFolder && matchesKeyword;
    });
  }, [searchValue, selectedFolder]);

  const folderStats = useMemo(() => {
    const counts = FILEEASY_PROTOTYPE_DEMO_FILES.reduce<
      Record<FileEasyPrototypeFolderOption['key'], number>
    >(
      (result, file) => {
        result[file.folder] += 1;
        result.all += 1;
        return result;
      },
      {
        all: 0,
        document: 0,
        video: 0,
        image: 0,
        audio: 0,
        archive: 0,
      },
    );

    return counts;
  }, []);

  const selectedFolderMeta = useMemo(
    () =>
      FILEEASY_PROTOTYPE_FOLDER_OPTIONS.find((folder) => folder.key === selectedFolder) ??
      FILEEASY_PROTOTYPE_FOLDER_OPTIONS[0],
    [selectedFolder],
  );

  const activeStep = useMemo(() => getFileEasyPrototypeStage(scene), [scene]);

  const serviceSteps = useMemo<ServiceStep[]>(
    () => [
      {
        key: 'booting',
        title: '服务启动中',
        detail: '本地服务与首页资源正在初始化',
      },
      {
        key: 'foreground',
        title: '前台服务运行中',
        detail: 'FileEasy 已常驻，支持开机自启与后台存活',
      },
      {
        key: 'ready',
        title: '局域网访问已就绪',
        detail: '二维码与上传地址已可提供给其他设备',
      },
    ],
    [],
  );

  const jumpToScene = (nextScene: FileEasyPrototypeSceneId) => {
    setScene(nextScene);
    setSavedToast('');
    setAlertMode(nextScene === 'upload-alerts' ? 'space' : 'none');

    if (nextScene === 'upload-login') {
      setPasswordError(false);
    }
  };

  const goToNextScene = () => {
    const nextIndex = Math.min(currentSceneIndex + 1, FILEEASY_PROTOTYPE_SCENE_ORDER.length - 1);
    jumpToScene(FILEEASY_PROTOTYPE_SCENE_ORDER[nextIndex]);
  };

  const goToPrevScene = () => {
    const prevIndex = Math.max(currentSceneIndex - 1, 0);
    jumpToScene(FILEEASY_PROTOTYPE_SCENE_ORDER[prevIndex]);
  };

  const handleLogin = () => {
    if (passwordValue.trim() !== apkPasswordDraft) {
      setPasswordError(true);
      return;
    }

    setPasswordError(false);
    jumpToScene('upload-active');
  };

  const simulateUpload = () => {
    setUploadTasks((tasks) =>
      tasks.map((task) => {
        if (task.status === 'queued') {
          return { ...task, progress: 22, status: 'uploading' };
        }

        if (task.status === 'uploading') {
          const nextProgress = Math.min(task.progress + 28, 100);
          return {
            ...task,
            progress: nextProgress,
            status: nextProgress >= 100 ? 'done' : 'uploading',
          };
        }

        if (task.status === 'restoring') {
          return { ...task, progress: 100, status: 'done', note: undefined };
        }

        return task;
      }),
    );
  };

  const cycleTaskStatus = (taskId: string) => {
    setUploadTasks((tasks) =>
      tasks.map((task) => {
        if (task.id !== taskId) return task;

        if (task.status === 'uploading') {
          return {
            ...task,
            status: 'failed',
            note: FILEEASY_PROTOTYPE_STATUS_COPY.failed.note,
          };
        }

        if (task.status === 'failed') {
          return {
            ...task,
            status: 'restoring',
            note: FILEEASY_PROTOTYPE_STATUS_COPY.restoring.note,
          };
        }

        if (task.status === 'restoring') {
          return { ...task, status: 'done', progress: 100, note: undefined };
        }

        if (task.status === 'done') {
          return { ...task, status: 'queued', progress: 0 };
        }

        return { ...task, status: 'uploading', progress: 18, note: undefined };
      }),
    );
  };

  const toggleBatchSelection = (fileId: string) => {
    setSelectedFiles((current) =>
      current.includes(fileId) ? current.filter((id) => id !== fileId) : [...current, fileId],
    );
  };

  const openRename = (file: FileEasyPrototypeDemoFile) => {
    setRenameFile(file);
    setSavedToast('');
  };

  const openApkDialog = (dialog: ApkDialog) => {
    if (dialog === 'password') {
      setApkPasswordCurrentInput(apkPasswordDraft);
      setApkPasswordNext('');
      setApkPasswordConfirm('');
    }
    setApkDialog(dialog);
  };

  const applyApkMode = (mode: ApkMode) => {
    setApkMode(mode);
    if (mode === 'first-install') {
      setApkDialog('welcome');
      setServiceStage('booting');
      return;
    }

    if (mode === 'network-missing') {
      setApkDialog('network');
      setServiceStage('foreground');
      return;
    }

    setApkDialog('none');
    setServiceStage('ready');
  };

  const saveApkPassword = () => {
    if (apkPasswordCurrentInput !== apkPasswordDraft) {
      setSavedToast('当前密码不正确');
      return;
    }

    setSavedToast('系统密码已更新，后续访问请使用新密码');
    setApkPasswordDraft(apkPasswordNext || apkPasswordDraft);
    setApkPasswordCurrentInput(apkPasswordNext || apkPasswordDraft);
    setApkPasswordNext('');
    setApkPasswordConfirm('');
    setApkDialog('none');
    setApkMode('normal');
    setServiceStage('ready');
  };

  const openFolder = (folder: FileEasyPrototypeFolderOption['key']) => {
    setSelectedFolder(folder);
    setSearchValue('');
  };

  const saveRename = () => {
    if (!renameFile) return;
    setSavedToast('文件名已更新');
    setRenameFile(null);
  };

  const confirmDelete = () => {
    setSavedToast(deleteTarget === 'batch' ? '已删除所选文件' : '文件已删除');
    setDeleteTarget(null);
  };

  const markOnboardingDone = () => {
    setApkMode('normal');
    setServiceStage('ready');
    setApkDialog('none');
    setSavedToast('首次安装引导已完成');
  };

  const cycleServiceStage = () => {
    setServiceStage((current) => {
      if (current === 'booting') return 'foreground';
      if (current === 'foreground') return 'ready';
      return 'booting';
    });
  };

  const renderStageByScene = () => {
    if (scene === 'apk-home') {
      return renderApkHome();
    }

    if (scene === 'upload-login') {
      return renderUploadLogin();
    }

    if (scene === 'upload-active') {
      return renderUploadActive();
    }

    if (scene === 'upload-alerts') {
      return renderUploadAlerts();
    }

    if (scene === 'admin-batch') {
      return renderAdminBatch();
    }

    return renderAdminList();
  };

  const renderEmptyState = (title: string, detail: string) => (
    <EmptyState
      className="fileeasy-empty-state"
      description={detail}
      iconClassName="fileeasy-empty-state__icon"
      title={title}
    />
  );

  const renderApkHome = () => (
    <FileEasyApkHomeScene
      apkMode={apkMode}
      apkPassword={apkPasswordDraft}
      homeSummary={{
        activeUploads: uploadTasks
          .filter((task) => task.status === 'uploading' || task.status === 'restoring' || task.status === 'queued')
          .slice(0, 3)
          .map((task) => ({
            createdAt: Date.now(),
            fileName: task.name,
            progress: task.progress,
            status:
              task.status === 'restoring'
                ? 'uploading'
                : task.status === 'queued'
                  ? 'initialized'
                  : 'uploading',
            totalChunks: 10,
            updatedAt: Date.now(),
            uploadId: task.id,
            uploadedChunks: Math.max(0, Math.round((task.progress / 100) * 10)),
          })),
        recentFiles: FILEEASY_PROTOTYPE_DEMO_FILES.slice(0, 4).map((file) => ({
          category: file.folder,
          createdAt: Date.now(),
          fileName: file.name,
          id: file.id,
          size: 12 * 1024 * 1024,
        })),
        uploadUrl: 'http://192.168.1.23:3000/',
      }}
      isHomeSummaryLoading={false}
      serviceStage={serviceStage}
      serviceSteps={serviceSteps}
      onCompleteOnboarding={markOnboardingDone}
      onCycleServiceStage={cycleServiceStage}
      onJumpToAdmin={() => jumpToScene('admin-list')}
      onJumpToUpload={() => jumpToScene('upload-login')}
      onOpenInfoDialog={() => openApkDialog(apkMode === 'network-missing' ? 'network' : 'welcome')}
      onOpenNetworkDialog={() => openApkDialog('network')}
      onOpenPasswordDialog={() => openApkDialog('password')}
      onSelectMode={applyApkMode}
      onSelectServiceStage={setServiceStage}
      onSetReady={() => setServiceStage('ready')}
    />
  );

  const renderUploadLogin = () => (
    <FileEasyUploadPrototypeLoginScene
      featureBadges={FILEEASY_PROTOTYPE_UPLOAD_FEATURE_BADGES}
      passwordError={passwordError}
      passwordValue={passwordValue}
      onPasswordChange={setPasswordValue}
      onSubmit={handleLogin}
    />
  );

  const renderUploadActive = () => (
    <FileEasyUploadPrototypeActiveScene
      featureBadges={FILEEASY_PROTOTYPE_UPLOAD_FEATURE_BADGES}
      statusCopy={FILEEASY_PROTOTYPE_STATUS_COPY}
      statusToneMap={FILEEASY_PROTOTYPE_UPLOAD_STATUS_TONE_MAP}
      tasks={uploadTasks}
      onJumpToAdmin={() => jumpToScene('admin-list')}
      onJumpToAlerts={() => jumpToScene('upload-alerts')}
      onJumpToApkHome={() => jumpToScene('apk-home')}
      onSimulateUpload={simulateUpload}
      onToggleTaskStatus={cycleTaskStatus}
    />
  );

  const renderUploadAlerts = () => (
    <FileEasyUploadPrototypeAlertsScene
      alertMode={alertMode}
      statusCopy={FILEEASY_PROTOTYPE_STATUS_COPY}
      statusToneMap={FILEEASY_PROTOTYPE_UPLOAD_STATUS_TONE_MAP}
      tasks={uploadTasks.slice(0, 2).map((task) =>
        alertMode === 'network'
          ? {
              ...task,
              status: 'uploading',
              note: '网络中断，正在等待恢复',
            }
          : alertMode === 'expired'
            ? {
                ...task,
                status: 'failed',
                note: '上传任务已过期，请重新上传',
              }
            : task,
      )}
      onAlertModeChange={setAlertMode}
      onToggleTaskStatus={cycleTaskStatus}
    />
  );

  const renderFileActions = (file: FileEasyPrototypeDemoFile) => (
    <FileEasyAdminPrototypeFileActions
      fileName={file.name}
      previewable={file.previewable}
      onDelete={() => setDeleteTarget(file)}
      onDownload={() => setSavedToast(`${file.name} 已开始下载`)}
      onPreview={() => (file.previewable ? setPreviewFile(file) : setSavedToast('当前类型不支持在线预览'))}
      onRename={() => openRename(file)}
    />
  );

  const renderAdminList = () => (
    <div className="fileeasy-device fileeasy-device--desktop">
      <div className="fileeasy-screen fileeasy-screen--admin">
        <FileEasyAdminPrototypeTopbar
          fileCount={FILEEASY_PROTOTYPE_DEMO_FILES.length}
          previewableCount={FILEEASY_PROTOTYPE_DEMO_FILES.filter((file) => file.previewable).length}
          searchPlaceholder={`在${selectedFolderMeta.label}中搜索文件名`}
          searchValue={searchValue}
          selectedFolderLabel={selectedFolderMeta.label}
          onEnterBatchMode={() => jumpToScene('admin-batch')}
          onExitDemoLogin={() => setSavedToast('已退出登录示意')}
          onJumpToApkHome={() => jumpToScene('apk-home')}
          onSearchChange={setSearchValue}
        />

        <div className="fileeasy-admin-layout">
          <FileEasyAdminPrototypeFolderSidebar
            folderGlyphs={FILEEASY_PROTOTYPE_FOLDER_GLYPHS}
            folderOptions={FILEEASY_PROTOTYPE_FOLDER_OPTIONS}
            folderStats={folderStats}
            selectedFolder={selectedFolder}
            onOpenFolder={openFolder}
          />

          <FileEasyAdminPrototypeFileGrid
            emptyState={renderEmptyState('当前文件夹下暂无匹配文件', '可以切换类型文件夹，或调整搜索关键词后再查看')}
            fileKindGlyphs={FILEEASY_PROTOTYPE_FILE_KIND_GLYPHS}
            files={filteredFiles}
            selectedFolderLabel={selectedFolderMeta.label}
            onRenderActions={renderFileActions}
          />
        </div>
      </div>
    </div>
  );

  const renderAdminBatch = () => (
    <FileEasyAdminPrototypeBatchScene
      emptyState={renderEmptyState('当前文件夹下没有可批量处理的文件', '选择其他类型文件夹，或先回到普通模式查看全部文件')}
      files={filteredFiles}
      folderGlyphs={FILEEASY_PROTOTYPE_FOLDER_GLYPHS}
      folderOptions={FILEEASY_PROTOTYPE_FOLDER_OPTIONS}
      folderStats={folderStats}
      selectedFileIds={selectedFiles}
      selectedFolder={selectedFolder}
      selectedFolderLabel={selectedFolderMeta.label}
      onDeleteSelected={() => setDeleteTarget('batch')}
      onExitBatch={() => jumpToScene('admin-list')}
      onOpenFolder={openFolder}
      onToggleSelection={toggleBatchSelection}
    />
  );

  return (
    <>
      <FileEasyPrototypeLayout
        activeStep={activeStep}
        canGoNext={currentSceneIndex !== FILEEASY_PROTOTYPE_SCENE_ORDER.length - 1}
        canGoPrev={currentSceneIndex !== 0}
        canvas={renderStageByScene()}
        currentScene={scene}
        currentSceneMeta={currentSceneMeta}
        hintItems={currentSceneMeta.interactionHints}
        scenes={FILEEASY_PROTOTYPE_SCENE_LIST}
        onNext={goToNextScene}
        onPrev={goToPrevScene}
        onSceneChange={(sceneId) => jumpToScene(sceneId as FileEasyPrototypeSceneId)}
      />

      {previewFile ? (
        <FileEasyAdminPrototypePreviewDialog
          fileKind={previewFile.kind}
          title={`${previewFile.kind} 预览`}
          onClose={() => setPreviewFile(null)}
        />
      ) : null}

      {renameFile ? (
        <FileEasyAdminPrototypeRenameDialog
          extension={`.${renameFile.name.split('.').pop()}`}
          value={renameFile.name.replace(/\.[^.]+$/, '')}
          onClose={() => setRenameFile(null)}
          onSave={saveRename}
        />
      ) : null}

      {deleteTarget ? (
        <FileEasyAdminPrototypeDeleteDialog
          description={
            deleteTarget === 'batch'
              ? `是否删除已选 ${selectedFiles.length} 个文件`
              : `是否删除“${deleteTarget.name}”`
          }
          title={deleteTarget === 'batch' ? '确认批量删除' : '确认删除'}
          onClose={() => setDeleteTarget(null)}
          onConfirm={confirmDelete}
        />
      ) : null}

      {apkDialog !== 'none' ? (
        <FileEasyApkDialog
          confirmPassword={apkPasswordConfirm}
          currentPassword={apkPasswordCurrentInput}
          displayPassword={apkPasswordDraft}
          dialog={apkDialog}
          nextPassword={apkPasswordNext}
          onClose={() => setApkDialog('none')}
          onCompleteOnboarding={markOnboardingDone}
          onConfirmPasswordChange={saveApkPassword}
          onConfirmPasswordInputChange={setApkPasswordConfirm}
          onCurrentPasswordChange={setApkPasswordCurrentInput}
          onGoToPasswordDialog={() => setApkDialog('password')}
          onNextPasswordChange={setApkPasswordNext}
          onRecoverNetwork={() => applyApkMode('normal')}
        />
      ) : null}

      {savedToast && <div className="fileeasy-toast">{savedToast}</div>}
    </>
  );
};

export default FileEasyPrototypePage;
