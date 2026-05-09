import React from 'react';
import './fileeasy-shared.css';

type EmptyStateProps = {
  actions?: React.ReactNode;
  className?: string;
  description?: string;
  icon?: React.ReactNode;
  iconClassName?: string;
  title: string;
};

const EmptyState: React.FC<EmptyStateProps> = ({
  actions,
  className,
  description,
  icon = '○',
  iconClassName,
  title,
}) => {
  const wrapperClassName = ['fe-empty-state', className || ''].filter(Boolean).join(' ');
  const resolvedIconClassName = ['fe-empty-state__icon', iconClassName || ''].filter(Boolean).join(' ');

  return (
    <div className={wrapperClassName}>
      {icon ? <div className={resolvedIconClassName}>{icon}</div> : null}
      <strong>{title}</strong>
      {description ? <span>{description}</span> : null}
      {actions}
    </div>
  );
};

export default EmptyState;
