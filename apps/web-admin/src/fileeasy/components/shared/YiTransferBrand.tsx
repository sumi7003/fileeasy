import React from 'react';
import './yi-transfer-brand.css';

type YiTransferBrandProps = {
  caption?: string;
  className?: string;
  tone?: 'default' | 'inverse';
};

const YiTransferBrand: React.FC<YiTransferBrandProps> = ({ caption, className = '', tone = 'default' }) => (
  <div className={`yitransfer-brand yitransfer-brand--${tone} ${className}`.trim()}>
    <div className="yitransfer-brand__mark" aria-hidden="true">
      <span className="yitransfer-brand__corner yitransfer-brand__corner--tl" />
      <span className="yitransfer-brand__corner yitransfer-brand__corner--tr" />
      <span className="yitransfer-brand__corner yitransfer-brand__corner--bl" />
      <span className="yitransfer-brand__corner yitransfer-brand__corner--br" />
      <span className="yitransfer-brand__beam" />
      <span className="yitransfer-brand__beam-tip" />
    </div>
    <div className="yitransfer-brand__copy">
      <strong>易传输</strong>
      {caption ? <span>{caption}</span> : null}
    </div>
  </div>
);

export default YiTransferBrand;
