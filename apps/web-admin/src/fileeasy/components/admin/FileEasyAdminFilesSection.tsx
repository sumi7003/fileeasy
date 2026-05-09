import React from 'react';
import { FILEEASY_FOLDER_LABELS } from '../../constants/fileTypes';
import type { FileItem, FolderKey } from '../../types/file';
import Button from '../shared/Button';
import EmptyState from '../shared/EmptyState';

type FileEasyAdminFilesSectionProps = {
  files: FileItem[];
  filteredFiles: FileItem[];
  isBatchMode: boolean;
  isLoading: boolean;
  pageError: string;
  selectedFolder: FolderKey;
  selectedIds: string[];
  onRefresh: () => void;
  onToggleBatchSelection: (fileId: string) => void;
  renderFileActions: (file: FileItem) => React.ReactNode;
};

const FileEasyAdminFilesSection: React.FC<FileEasyAdminFilesSectionProps> = ({
  files,
  filteredFiles,
  isBatchMode,
  isLoading,
  pageError,
  selectedFolder,
  selectedIds,
  onRefresh,
  onToggleBatchSelection,
  renderFileActions,
}) => {
  const renderEmptyState = () => {
    if (pageError) {
      return (
        <EmptyState
          actions={
            <Button className="fileeasy-admin-button secondary" variant="secondary" onClick={onRefresh}>
              重新加载
            </Button>
          }
          className="fileeasy-admin-empty"
          description={pageError}
          icon={null}
          title="文件列表暂时不可用"
        />
      );
    }

    if (files.length === 0) {
      return (
        <EmptyState
          className="fileeasy-admin-empty"
          description="上传页完成文件上传后，文件会按最新时间显示在这里。"
          icon={null}
          title="当前暂无文件"
        />
      );
    }

      return (
        <EmptyState
          className="fileeasy-admin-empty"
          description="试试更换关键词，或切换文件类型筛选。"
          icon={null}
          title="未找到相关文件"
        />
      );
  };

  return (
    <section className="fileeasy-admin-card">
      <div className="fileeasy-admin-list-head">
        <div>
          <h2>{selectedFolder === 'all' ? '全部文件' : `${FILEEASY_FOLDER_LABELS[selectedFolder]}文件`}</h2>
          <p>{filteredFiles.length} 个文件，默认按最新上传时间排序。</p>
        </div>
        <span className="fileeasy-admin-inline-note">不支持预览的文件会直接提供下载入口。</span>
      </div>

      {isLoading ? (
        <EmptyState
          className="fileeasy-admin-empty"
          description="请稍候，当前设备上的文件记录正在同步到管理页。"
          icon={null}
          title="正在加载文件列表"
        />
      ) : filteredFiles.length === 0 ? (
        renderEmptyState()
      ) : (
        <>
          <div className="fileeasy-admin-table-wrap">
            <table className="fileeasy-admin-table">
              <thead>
                <tr>
                  {isBatchMode ? <th className="check-column">勾选</th> : null}
                  <th>文件名</th>
                  <th>类型</th>
                  <th>大小</th>
                  <th>时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {filteredFiles.map((file) => (
                  <tr key={file.id}>
                    {isBatchMode ? (
                      <td className="check-column">
                        <input
                          checked={selectedIds.includes(file.id)}
                          type="checkbox"
                          onChange={() => onToggleBatchSelection(file.id)}
                        />
                      </td>
                    ) : null}
                    <td>
                      <div className="fileeasy-admin-file-name">
                        <strong>{file.name}</strong>
                        {!file.previewable ? <span>仅支持下载</span> : null}
                      </div>
                    </td>
                    <td>{file.kind}</td>
                    <td>{file.size}</td>
                    <td>{file.time}</td>
                    <td>{renderFileActions(file)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="fileeasy-admin-mobile-list">
            {filteredFiles.map((file) => (
              <article className={`fileeasy-admin-mobile-card ${selectedIds.includes(file.id) ? 'selected' : ''}`} key={file.id}>
                <div className="fileeasy-admin-mobile-card-top">
                  {isBatchMode ? (
                    <input
                      checked={selectedIds.includes(file.id)}
                      type="checkbox"
                      onChange={() => onToggleBatchSelection(file.id)}
                    />
                  ) : null}
                  <div>
                    <strong>{file.name}</strong>
                    <p>
                      {file.kind} | {file.size} | {file.time}
                    </p>
                  </div>
                </div>
                {renderFileActions(file)}
              </article>
            ))}
          </div>
        </>
      )}
    </section>
  );
};

export default FileEasyAdminFilesSection;
