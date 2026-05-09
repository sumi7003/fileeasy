import React, { useEffect } from 'react';
import Button from './Button';
import './fileeasy-shared.css';

type DialogShellProps = {
  backdropClassName?: string;
  bodyClassName?: string;
  children: React.ReactNode;
  closeLabel?: string;
  compact?: boolean;
  description?: string;
  footer?: React.ReactNode;
  footerClassName?: string;
  headerClassName?: string;
  onClose?: () => void;
  panelClassName?: string;
  title?: React.ReactNode;
};

const DialogShell: React.FC<DialogShellProps> = ({
  backdropClassName,
  bodyClassName,
  children,
  closeLabel = '关闭',
  compact = false,
  description,
  footer,
  footerClassName,
  headerClassName,
  onClose,
  panelClassName,
  title,
}) => {
  useEffect(() => {
    if (!onClose) {
      return undefined;
    }

    const handleKeydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeydown);
    return () => window.removeEventListener('keydown', handleKeydown);
  }, [onClose]);

  const backdropClasses = ['fe-dialog-backdrop', backdropClassName || ''].filter(Boolean).join(' ');
  const panelClasses = ['fe-dialog', compact ? 'fe-dialog--compact' : '', panelClassName || '']
    .filter(Boolean)
    .join(' ');
  const resolvedHeaderClassName = ['fe-dialog__header', headerClassName || ''].filter(Boolean).join(' ');
  const resolvedBodyClassName = ['fe-dialog__body', bodyClassName || ''].filter(Boolean).join(' ');
  const resolvedFooterClassName = ['fe-dialog__footer', footerClassName || ''].filter(Boolean).join(' ');

  return (
    <div
      className={backdropClasses}
      role="presentation"
      onClick={() => {
        if (onClose) {
          onClose();
        }
      }}
    >
      <div
        aria-modal="true"
        className={panelClasses}
        role="dialog"
        onClick={(event) => event.stopPropagation()}
      >
        {title || onClose ? (
          <div className={resolvedHeaderClassName}>
            <div className="fe-dialog__header-copy">
              {title ? <strong>{title}</strong> : null}
              {description ? <span>{description}</span> : null}
            </div>
            {onClose ? (
              <Button onClick={onClose} size="sm" variant="ghost">
                {closeLabel}
              </Button>
            ) : null}
          </div>
        ) : null}
        <div className={resolvedBodyClassName}>{children}</div>
        {footer ? <div className={resolvedFooterClassName}>{footer}</div> : null}
      </div>
    </div>
  );
};

export default DialogShell;
