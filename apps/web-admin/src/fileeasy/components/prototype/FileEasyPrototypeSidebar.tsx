import React from 'react';
import type { FileEasyPrototypeSceneMeta } from '../../types/prototype';

type FileEasyPrototypeSidebarProps = {
  activeStep: string;
  currentScene: string;
  scenes: FileEasyPrototypeSceneMeta[];
  onSceneChange: (sceneId: string) => void;
};

const FileEasyPrototypeSidebar: React.FC<FileEasyPrototypeSidebarProps> = ({
  activeStep,
  currentScene,
  scenes,
  onSceneChange,
}) => {
  return (
    <aside className="fileeasy-sidebar">
      <div className="fileeasy-sidebar__brand">
        <div className="fileeasy-sidebar__logo">F</div>
        <div>
          <div className="fileeasy-sidebar__title">FileEasy Demo</div>
          <div className="fileeasy-sidebar__subtitle">可点击全流程原型</div>
        </div>
      </div>

      <div className="fileeasy-sidebar__section">
        <div className="fileeasy-sidebar__label">当前阶段</div>
        <div className="fileeasy-stage-pill">{activeStep}</div>
      </div>

      <div className="fileeasy-scene-list">
        {scenes.map((item) => (
          <button
            key={item.id}
            type="button"
            className={`fileeasy-scene-item ${currentScene === item.id ? 'active' : ''}`}
            onClick={() => onSceneChange(item.id)}
          >
            <span>{item.eyebrow}</span>
            <strong>{item.label}</strong>
            <small>{item.summary}</small>
          </button>
        ))}
      </div>

      <div className="fileeasy-sidebar__footnote">
        当前版本是可复用原型页：
        <br />
        页面结构、状态卡、弹层和任务卡都已做成后续可直接拆分的 HTML/React 结构。
      </div>
    </aside>
  );
};

export default FileEasyPrototypeSidebar;
