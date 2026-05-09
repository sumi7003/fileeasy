import React from 'react';
import type { ServiceStage, ServiceStep } from '../../types/apk';

type FileEasyApkServiceStripProps = {
  serviceStage: ServiceStage;
  serviceSteps: ServiceStep[];
  onSelect: (stage: ServiceStage) => void;
};

const FileEasyApkServiceStrip: React.FC<FileEasyApkServiceStripProps> = ({
  serviceStage,
  serviceSteps,
  onSelect,
}) => {
  return (
    <div className="fileeasy-service-strip">
      {serviceSteps.map((step, index) => {
        const currentIndex = serviceSteps.findIndex((item) => item.key === serviceStage);
        const isActive = step.key === serviceStage;
        const isDone = index < currentIndex;
        return (
          <button
            key={step.key}
            className={`fileeasy-service-step ${isActive ? 'active' : ''} ${isDone ? 'done' : ''}`}
            type="button"
            onClick={() => onSelect(step.key)}
          >
            <span className="fileeasy-service-step__dot">{index + 1}</span>
            <div>
              <strong>{step.title}</strong>
              <small>{step.detail}</small>
            </div>
          </button>
        );
      })}
    </div>
  );
};

export default FileEasyApkServiceStrip;
