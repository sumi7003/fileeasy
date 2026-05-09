import React from 'react';
import './fileeasy-shared.css';

type ProgressTone = 'default' | 'success' | 'warning' | 'danger';

type ProgressBarProps = React.HTMLAttributes<HTMLDivElement> & {
  fillClassName?: string;
  tone?: ProgressTone;
  value: number;
};

const ProgressBar: React.FC<ProgressBarProps> = ({
  className,
  fillClassName,
  tone = 'default',
  value,
  ...props
}) => {
  const normalizedValue = Math.max(0, Math.min(100, Math.round(value)));
  const resolvedTrackClassName = ['fe-progress', className || ''].filter(Boolean).join(' ');
  const resolvedFillClassName = [
    'fe-progress__fill',
    tone !== 'default' ? `fe-progress__fill--${tone}` : '',
    fillClassName || '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div
      aria-label={`progress-${normalizedValue}`}
      aria-valuemax={100}
      aria-valuemin={0}
      aria-valuenow={normalizedValue}
      className={resolvedTrackClassName}
      role="progressbar"
      {...props}
    >
      <div className={resolvedFillClassName} style={{ width: `${normalizedValue}%` }} />
    </div>
  );
};

export default ProgressBar;
