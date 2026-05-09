import React from 'react';
import './fileeasy-shared.css';

type StatusTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

type StatusPillProps = {
  children: React.ReactNode;
  className?: string;
  tone?: StatusTone;
};

const StatusPill: React.FC<StatusPillProps> = ({ children, className, tone = 'neutral' }) => {
  const resolvedClassName = ['fe-status-pill', `fe-status-pill--${tone}`, className || '']
    .filter(Boolean)
    .join(' ');

  return <span className={resolvedClassName}>{children}</span>;
};

export default StatusPill;
