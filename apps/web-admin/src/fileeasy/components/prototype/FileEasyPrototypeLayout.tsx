import React from 'react';
import type { FileEasyPrototypeHint, FileEasyPrototypeSceneMeta } from '../../types/prototype';
import FileEasyPrototypeSidebar from './FileEasyPrototypeSidebar';
import FileEasyPrototypeStage from './FileEasyPrototypeStage';

type FileEasyPrototypeLayoutProps = {
  activeStep: string;
  canvas: React.ReactNode;
  currentScene: string;
  currentSceneMeta: FileEasyPrototypeSceneMeta;
  hintItems: FileEasyPrototypeHint[];
  scenes: FileEasyPrototypeSceneMeta[];
  canGoNext: boolean;
  canGoPrev: boolean;
  onNext: () => void;
  onPrev: () => void;
  onSceneChange: (sceneId: string) => void;
};

const FileEasyPrototypeLayout: React.FC<FileEasyPrototypeLayoutProps> = ({
  activeStep,
  canvas,
  currentScene,
  currentSceneMeta,
  hintItems,
  scenes,
  canGoNext,
  canGoPrev,
  onNext,
  onPrev,
  onSceneChange,
}) => {
  return (
    <div className="fileeasy-prototype">
      <FileEasyPrototypeSidebar
        activeStep={activeStep}
        currentScene={currentScene}
        scenes={scenes}
        onSceneChange={onSceneChange}
      />
      <FileEasyPrototypeStage
        canvas={canvas}
        canGoNext={canGoNext}
        canGoPrev={canGoPrev}
        eyebrow={currentSceneMeta.eyebrow}
        hintItems={hintItems}
        summary={currentSceneMeta.summary}
        title={currentSceneMeta.label}
        onNext={onNext}
        onPrev={onPrev}
      />
    </div>
  );
};

export default FileEasyPrototypeLayout;
