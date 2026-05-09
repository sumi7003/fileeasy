import React from 'react';
import './fileeasy-shared.css';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  block?: boolean;
  leadingIcon?: React.ReactNode;
  trailingIcon?: React.ReactNode;
  size?: ButtonSize;
  variant?: ButtonVariant;
};

const Button: React.FC<ButtonProps> = ({
  block = false,
  children,
  className,
  leadingIcon,
  size = 'md',
  trailingIcon,
  type = 'button',
  variant = 'primary',
  ...props
}) => {
  const classes = [
    'fe-button',
    `fe-button--${variant}`,
    `fe-button--${size}`,
    variant,
    block ? 'fe-button--block' : '',
    className || '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button className={classes} type={type} {...props}>
      {leadingIcon ? <span className="fe-button__icon">{leadingIcon}</span> : null}
      <span>{children}</span>
      {trailingIcon ? <span className="fe-button__icon">{trailingIcon}</span> : null}
    </button>
  );
};

export default Button;
