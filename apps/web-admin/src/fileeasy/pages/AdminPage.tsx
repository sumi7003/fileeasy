import axios from 'axios';
import React, { useEffect, useMemo, useState } from 'react';
import { getFileEasyHomeSummary } from '../../api/fileeasy';
import {
  FileEasyAdminControlsCard,
  FileEasyAdminDeleteDialog,
  type FileEasyAdminDeleteDialogState,
  FileEasyAdminFilesSection,
  FileEasyAdminHero,
  FileEasyAdminLoginGate,
  FileEasyAdminPreviewDialog,
  FileEasyAdminRenameDialog,
} from '../components/admin';
import Button from '../components/shared/Button';
import { useAuthState } from '../hooks/useAuthState';
import { fileEasyFilesService, type RenameOutcome } from '../services/files';
import type { FileItem, FolderKey } from '../types/file';
import './admin-page.css';

type FeedbackState = {
  tone: 'success' | 'error' | 'warning';
  message: string;
};

const folderOrder: FolderKey[] = ['all', 'document', 'video', 'image', 'audio', 'archive'];
const INVALID_FILE_NAME_CHARACTERS = /[\\/:*?"<>|]/;

const getRequestErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data as
      | { message?: string; error?: string; detail?: string }
      | undefined;
    if (typeof responseData?.message === 'string' && responseData.message.trim()) {
      return responseData.message;
    }
    if (typeof responseData?.error === 'string' && responseData.error.trim()) {
      return responseData.error;
    }
    if (typeof responseData?.detail === 'string' && responseData.detail.trim()) {
      return responseData.detail;
    }
    if (error.response?.status === 401) {
      return '登录已失效，请重新输入密码。';
    }
    if (error.code === 'ERR_NETWORK' || !error.response) {
      return '当前无法连接到设备，请确认局域网服务已开启。';
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }

  return fallback;
};

const isUnauthorizedError = (error: unknown) =>
  axios.isAxiosError(error) && error.response?.status === 401;

const previewTitles: Partial<Record<FolderKey, string>> = {
  document: 'PDF 预览',
  image: '图片预览',
  video: '视频预览',
  audio: '音频预览',
};

const getTimestampValue = (value?: string) => {
  if (!value) return 0;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime();
};

const formatRelativeTime = (value?: string) => {
  const timestamp = getTimestampValue(value);
  if (!timestamp) return '刚刚上传';

  const delta = Date.now() - timestamp;
  if (delta < 60_000) return '刚刚上传';

  const minutes = Math.floor(delta / 60_000);
  if (minutes < 60) return `${minutes} 分钟前`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} 小时前`;

  const days = Math.floor(hours / 24);
  return `${days} 天前`;
};

const AdminPage: React.FC = () => {
  const {
    isAuthenticated,
    isSubmitting: isAuthSubmitting,
    error: authError,
    setError: setAuthError,
    login,
    logout,
    clearSession,
  } = useAuthState();

  const [password, setPassword] = useState('');
  const [isAuthBootstrapReady, setIsAuthBootstrapReady] = useState(false);
  const [files, setFiles] = useState<FileItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [pageError, setPageError] = useState('');
  const [query, setQuery] = useState('');
  const [selectedFolder, setSelectedFolder] = useState<FolderKey>('all');
  const [isBatchMode, setIsBatchMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [previewFile, setPreviewFile] = useState<FileItem | null>(null);
  const [previewResourceUrl, setPreviewResourceUrl] = useState('');
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState('');
  const [renameFile, setRenameFile] = useState<FileItem | null>(null);
  const [renameBaseName, setRenameBaseName] = useState('');
  const [renameError, setRenameError] = useState('');
  const [isRenaming, setIsRenaming] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState<FileEasyAdminDeleteDialogState | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [actionMenuFileId, setActionMenuFileId] = useState<string | null>(null);

  const closePreview = () => {
    if (previewResourceUrl.startsWith('blob:')) {
      URL.revokeObjectURL(previewResourceUrl);
    }
    setPreviewFile(null);
    setPreviewResourceUrl('');
    setPreviewError('');
    setIsPreviewLoading(false);
  };

  const handleSessionExpired = () => {
    closePreview();
    setRenameFile(null);
    setDeleteDialog(null);
    clearSession();
    setFiles([]);
    setIsBatchMode(false);
    setSelectedIds([]);
    setActionMenuFileId(null);
    setFeedback({
      tone: 'warning',
      message: '登录已失效，请重新输入密码后继续管理文件。',
    });
  };

  const loadFiles = async () => {
    setIsLoading(true);
    setPageError('');
    try {
      const nextFiles = await fileEasyFilesService.listFiles();
      setFiles(nextFiles);
    } catch (error) {
      if (isUnauthorizedError(error)) {
        handleSessionExpired();
        return;
      }
      setPageError(getRequestErrorMessage(error, '文件列表加载失败，请稍后重试。'));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!feedback) return undefined;
    const timer = window.setTimeout(() => {
      setFeedback(null);
    }, 3600);
    return () => {
      window.clearTimeout(timer);
    };
  }, [feedback]);

  useEffect(() => {
    return () => {
      if (previewResourceUrl.startsWith('blob:')) {
        URL.revokeObjectURL(previewResourceUrl);
      }
    };
  }, [previewResourceUrl]);

  useEffect(() => {
    let active = true;

    const bootstrapAuthMode = async () => {
      try {
        const summary = await getFileEasyHomeSummary();
        if (!active) return;
        if (!summary.passwordRequired && !isAuthenticated) {
          await login({ password: '' });
        }
      } catch (error) {
        console.warn('Failed to bootstrap FileEasy admin auth mode', error);
      } finally {
        if (active) {
          setIsAuthBootstrapReady(true);
        }
      }
    };

    void bootstrapAuthMode();

    return () => {
      active = false;
    };
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      setFiles([]);
      setSelectedIds([]);
      setIsBatchMode(false);
      setPageError('');
      setActionMenuFileId(null);
      closePreview();
      return;
    }

    void loadFiles();
  }, [isAuthenticated]);

  useEffect(() => {
    setSelectedIds((current) => current.filter((id) => files.some((file) => file.id === id)));
  }, [files]);

  useEffect(() => {
    setActionMenuFileId(null);
  }, [isBatchMode, query, selectedFolder]);

  const sortedFiles = useMemo(
    () =>
      [...files].sort((left, right) => getTimestampValue(right.createdAt) - getTimestampValue(left.createdAt)),
    [files],
  );

  const filteredFiles = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return sortedFiles.filter((file) => {
      const matchesFolder = selectedFolder === 'all' || file.folder === selectedFolder;
      const matchesKeyword = !keyword || file.name.toLowerCase().includes(keyword);
      return matchesFolder && matchesKeyword;
    });
  }, [query, selectedFolder, sortedFiles]);

  const recentFiles = useMemo(() => sortedFiles.slice(0, 4), [sortedFiles]);

  const folderCounts = useMemo(
    () =>
      files.reduce<Record<FolderKey, number>>(
        (result, file) => {
          result.all += 1;
          result[file.folder] += 1;
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
      ),
    [files],
  );

  const allVisibleSelected =
    filteredFiles.length > 0 && filteredFiles.every((file) => selectedIds.includes(file.id));

  const handleLogin = async () => {
    if (!password.trim()) {
      setAuthError('请输入访问密码。');
      return;
    }

    const success = await login({ password: password.trim() });
    if (success) {
      setPassword('');
      setFeedback({
        tone: 'success',
        message: '身份验证成功，已进入文件管理。',
      });
    }
  };

  const handleLogout = async () => {
    await logout();
    closePreview();
    setFeedback({
      tone: 'success',
      message: '已退出文件管理登录态。',
    });
  };

  const toggleBatchSelection = (fileId: string) => {
    setSelectedIds((current) =>
      current.includes(fileId) ? current.filter((id) => id !== fileId) : [...current, fileId],
    );
  };

  const toggleSelectVisible = () => {
    const visibleIds = filteredFiles.map((file) => file.id);
    if (visibleIds.length === 0) return;

    setSelectedIds((current) => {
      if (visibleIds.every((id) => current.includes(id))) {
        return current.filter((id) => !visibleIds.includes(id));
      }
      const next = new Set(current);
      visibleIds.forEach((id) => next.add(id));
      return Array.from(next);
    });
  };

  const openRenameDialog = (file: FileItem) => {
    setActionMenuFileId(null);
    setRenameFile(file);
    setRenameBaseName(file.baseName);
    setRenameError('');
  };

  const handlePreview = async (file: FileItem) => {
    if (!file.previewable) return;

    setActionMenuFileId(null);
    closePreview();
    setPreviewFile(file);
    setPreviewError('');
    setIsPreviewLoading(true);

    try {
      const resourceUrl = await fileEasyFilesService.createPreviewUrl(file);
      setPreviewResourceUrl(resourceUrl);
    } catch (error) {
      if (isUnauthorizedError(error)) {
        handleSessionExpired();
        return;
      }
      setPreviewError(getRequestErrorMessage(error, '预览加载失败，请稍后重试。'));
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleRenameSubmit = async () => {
    if (!renameFile) return;

    const nextBaseName = renameBaseName.trim();
    if (!nextBaseName) {
      setRenameError('主文件名不能为空。');
      return;
    }
    if (INVALID_FILE_NAME_CHARACTERS.test(nextBaseName)) {
      setRenameError('主文件名不能包含 \\ / : * ? " < > |。');
      return;
    }

    setIsRenaming(true);
    setRenameError('');
    try {
      const result: RenameOutcome = await fileEasyFilesService.renameFile({
        file: renameFile,
        nextBaseName,
      });
      await loadFiles();
      setRenameFile(null);
      setFeedback({
        tone: 'success',
        message: `已将文件重命名为 ${result.finalName}`,
      });
    } catch (error) {
      if (isUnauthorizedError(error)) {
        handleSessionExpired();
        return;
      }
      setRenameError(getRequestErrorMessage(error, '重命名失败，请稍后重试。'));
    } finally {
      setIsRenaming(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deleteDialog) return;

    setIsDeleting(true);
    try {
      if (deleteDialog.mode === 'single') {
        await fileEasyFilesService.deleteFile(deleteDialog.file);
        setSelectedIds((current) => current.filter((id) => id !== deleteDialog.file.id));
        setFeedback({
          tone: 'success',
          message: `已删除 ${deleteDialog.file.name}`,
        });
      } else {
        const deletedIds = deleteDialog.files.map((file) => file.id);
        await fileEasyFilesService.batchDeleteFiles(deleteDialog.files);
        setSelectedIds((current) => current.filter((id) => !deletedIds.includes(id)));
        setIsBatchMode(false);
        setFeedback({
          tone: 'success',
          message: `已删除所选 ${deleteDialog.files.length} 个文件`,
        });
      }

      setDeleteDialog(null);
      setActionMenuFileId(null);
      await loadFiles();
    } catch (error) {
      if (isUnauthorizedError(error)) {
        handleSessionExpired();
        return;
      }
      setFeedback({
        tone: 'error',
        message: getRequestErrorMessage(error, '删除失败，请稍后重试。'),
      });
    } finally {
      setIsDeleting(false);
    }
  };

  const renderPreviewContent = () => {
    if (!previewFile) return null;
    if (isPreviewLoading) {
      return <div className="fileeasy-admin-preview-placeholder">预览内容加载中...</div>;
    }
    if (previewError) {
      return <div className="fileeasy-admin-feedback error">{previewError}</div>;
    }
    if (!previewResourceUrl) {
      return <div className="fileeasy-admin-preview-placeholder">暂未获取到预览内容。</div>;
    }
    if (previewFile.folder === 'image') {
      return <img alt={previewFile.name} className="fileeasy-admin-preview-image" src={previewResourceUrl} />;
    }
    if (previewFile.folder === 'video') {
      return <video className="fileeasy-admin-preview-media" controls src={previewResourceUrl} />;
    }
    if (previewFile.folder === 'audio') {
      return <audio className="fileeasy-admin-preview-audio" controls src={previewResourceUrl} />;
    }
    return <iframe className="fileeasy-admin-preview-frame" src={previewResourceUrl} title={previewFile.name} />;
  };

  const renderFileActions = (file: FileItem) => {
    if (isBatchMode) {
      return null;
    }

    return (
      <div className="fileeasy-admin-actions">
        {file.previewable ? (
          <Button
            className="fileeasy-admin-button ghost compact"
            variant="ghost"
            onClick={() => void handlePreview(file)}
          >
            预览
          </Button>
        ) : null}
        <a className="fileeasy-admin-button ghost compact" download={file.name} href={file.downloadUrl}>
          下载
        </a>
        <div className="fileeasy-admin-more">
          <Button
            className="fileeasy-admin-button ghost compact"
            variant="ghost"
            onClick={() =>
              setActionMenuFileId((current) => (current === file.id ? null : file.id))
            }
          >
            更多
          </Button>
          {actionMenuFileId === file.id ? (
            <div className="fileeasy-admin-more-menu">
              <button className="fileeasy-admin-more-item" type="button" onClick={() => openRenameDialog(file)}>
                重命名
              </button>
              <button
                className="fileeasy-admin-more-item danger"
                type="button"
                onClick={() => {
                  setActionMenuFileId(null);
                  setDeleteDialog({ mode: 'single', file });
                }}
              >
                删除
              </button>
            </div>
          ) : null}
        </div>
      </div>
    );
  };

  if (!isAuthBootstrapReady) {
    return null;
  }

  if (!isAuthenticated) {
    return (
      <FileEasyAdminLoginGate
        authError={authError}
        isSubmitting={isAuthSubmitting}
        password={password}
        onLogin={() => void handleLogin()}
        onPasswordChange={setPassword}
        onPasswordKeyDown={(event) => {
          if (event.key === 'Enter') {
            void handleLogin();
          }
        }}
      />
    );
  }

  return (
    <div className="fileeasy-admin-page">
      <div className="fileeasy-admin-shell">
        <FileEasyAdminHero
          feedback={feedback}
          isBatchMode={isBatchMode}
          onEnterBatchMode={() => {
            setActionMenuFileId(null);
            setIsBatchMode(true);
          }}
          onExitBatchMode={() => setIsBatchMode(false)}
          onLogout={() => void handleLogout()}
        />

        <FileEasyAdminControlsCard
          allVisibleSelected={allVisibleSelected}
          folderCounts={folderCounts}
          folderOrder={folderOrder}
          isBatchMode={isBatchMode}
          isLoading={isLoading}
          query={query}
          selectedFolder={selectedFolder}
          onQueryChange={setQuery}
          onRefresh={() => void loadFiles()}
          onSelectFolder={setSelectedFolder}
          onToggleSelectVisible={toggleSelectVisible}
        />

        {!isBatchMode && recentFiles.length > 0 ? (
          <section className="fileeasy-admin-card fileeasy-admin-recent">
            <div className="fileeasy-admin-list-head">
              <div>
                <h2>最近上传</h2>
                <p>先确认刚上传到当前设备的文件是否已经到位。</p>
              </div>
            </div>
            <div className="fileeasy-admin-recent-grid">
              {recentFiles.map((file) => (
                <article className="fileeasy-admin-recent-card" key={file.id}>
                  <div className="fileeasy-admin-recent-top">
                    <strong>{file.name}</strong>
                    <span>{formatRelativeTime(file.createdAt)}</span>
                  </div>
                  <p>
                    {file.kind} · {file.size}
                  </p>
                  <div className="fileeasy-admin-recent-time">{file.time}</div>
                  {renderFileActions(file)}
                </article>
              ))}
            </div>
          </section>
        ) : null}

        {isBatchMode ? (
          <section className="fileeasy-admin-batch-bar">
            <div>
              <strong>已选择 {selectedIds.length} 个文件</strong>
              <p>当前页已切换为选择态，单文件操作已暂时收起。</p>
            </div>
            <button
              className="fileeasy-admin-button danger"
              type="button"
              disabled={selectedIds.length === 0}
              onClick={() =>
                setDeleteDialog({
                  mode: 'batch',
                  files: files.filter((file) => selectedIds.includes(file.id)),
                })
              }
            >
              删除已选文件
            </button>
          </section>
        ) : null}

        <FileEasyAdminFilesSection
          files={files}
          filteredFiles={filteredFiles}
          isBatchMode={isBatchMode}
          isLoading={isLoading}
          pageError={pageError}
          selectedFolder={selectedFolder}
          selectedIds={selectedIds}
          onRefresh={() => void loadFiles()}
          onToggleBatchSelection={toggleBatchSelection}
          renderFileActions={renderFileActions}
        />
      </div>

      {previewFile ? (
        <FileEasyAdminPreviewDialog
          content={renderPreviewContent()}
          previewFile={previewFile}
          previewTitles={previewTitles}
          onClose={closePreview}
        />
      ) : null}

      {renameFile ? (
        <FileEasyAdminRenameDialog
          error={renameError}
          file={renameFile}
          isSubmitting={isRenaming}
          value={renameBaseName}
          onChange={(value) => {
            setRenameBaseName(value);
            if (renameError) {
              setRenameError('');
            }
          }}
          onClose={() => setRenameFile(null)}
          onSubmit={() => void handleRenameSubmit()}
        />
      ) : null}

      {deleteDialog ? (
        <FileEasyAdminDeleteDialog
          dialog={deleteDialog}
          isDeleting={isDeleting}
          onClose={() => setDeleteDialog(null)}
          onConfirm={() => void handleDeleteConfirm()}
        />
      ) : null}
    </div>
  );
};

export default AdminPage;
