import React from 'react';
import './fileeasy-shared.css';

type InputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  invalid?: boolean;
};

const Input: React.FC<InputProps> = ({ className, invalid = false, ...props }) => {
  const ariaInvalidProp = props['aria-invalid'];
  const isAriaInvalid = invalid || ariaInvalidProp === true || ariaInvalidProp === 'true';
  const resolvedClassName = ['fe-input', invalid ? 'fe-input--invalid' : '', className || '']
    .filter(Boolean)
    .join(' ');

  return <input {...props} aria-invalid={isAriaInvalid} className={resolvedClassName} />;
};

export default Input;
