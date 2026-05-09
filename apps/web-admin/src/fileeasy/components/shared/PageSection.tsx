import React from 'react';
import './fileeasy-shared.css';

type PageSectionProps = {
  actions?: React.ReactNode;
  children?: React.ReactNode;
  className?: string;
  description?: string;
  eyebrow?: string;
  title?: React.ReactNode;
};

const PageSection: React.FC<PageSectionProps> = ({
  actions,
  children,
  className,
  description,
  eyebrow,
  title,
}) => {
  const wrapperClassName = ['fe-page-section', className || ''].filter(Boolean).join(' ');

  return (
    <section className={wrapperClassName}>
      {eyebrow || title || description || actions ? (
        <div className="fe-page-section__header">
          <div className="fe-page-section__copy">
            {eyebrow ? <span className="fe-page-section__eyebrow">{eyebrow}</span> : null}
            {title ? <h2>{title}</h2> : null}
            {description ? <p>{description}</p> : null}
          </div>
          {actions}
        </div>
      ) : null}
      {children}
    </section>
  );
};

export default PageSection;
