import React from 'react';
import Button from '../shared/Button';
import type { FileEasyPrototypeHint } from '../../types/prototype';

type FileEasyPrototypeStageProps = {
  canvas: React.ReactNode;
  canGoNext: boolean;
  canGoPrev: boolean;
  eyebrow: string;
  hintItems: FileEasyPrototypeHint[];
  summary: string;
  title: string;
  onNext: () => void;
  onPrev: () => void;
};

const FileEasyPrototypeStage: React.FC<FileEasyPrototypeStageProps> = ({
  canvas,
  canGoNext,
  canGoPrev,
  eyebrow,
  hintItems,
  summary,
  title,
  onNext,
  onPrev,
}) => {
  return (
    <main className="fileeasy-stage">
      <div className="fileeasy-stage__hero">
        <div>
          <div className="fileeasy-stage__eyebrow">{eyebrow}</div>
          <h1>{title}</h1>
          <p>{summary}</p>
        </div>
        <div className="fileeasy-stage__actions">
          <Button variant="ghost" disabled={!canGoPrev} onClick={onPrev}>
            上一步
          </Button>
          <Button variant="primary" disabled={!canGoNext} onClick={onNext}>
            下一步
          </Button>
        </div>
      </div>

      <div className="fileeasy-stage__canvas">{canvas}</div>
      <div className="fileeasy-stage__hint">
        可点击区域提示：
        {hintItems.map((item, index) => (
          <React.Fragment key={`${item.label}-${item.detail}`}>
            {index > 0 ? '，' : ''}
            <code>{item.label}</code>
            {item.detail}
          </React.Fragment>
        ))}
        。
      </div>
    </main>
  );
};

export default FileEasyPrototypeStage;
