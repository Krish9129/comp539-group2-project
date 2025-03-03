import React from 'react';
import { Button } from 'react-bootstrap';
import { FaGoogle, FaGithub } from 'react-icons/fa';
import authService from '../services/authService';

const LoginButton = ({ provider, className }) => {
  const handleLogin = () => {
    // Use the provider parameter to determine the authentication URL
    authService.login(provider);
  };

  const getButtonStyle = () => {
    switch (provider) {
      case 'google':
        return { 
          variant: 'light', 
          icon: <FaGoogle className="me-2" />,
          text: 'Sign in with Google'
        };
      case 'github':
        return { 
          variant: 'dark', 
          icon: <FaGithub className="me-2" />,
          text: 'Sign in with GitHub'
        };
      default:
        return { 
          variant: 'primary', 
          icon: null,
          text: 'Sign in'
        };
    }
  };

  const { variant, icon, text } = getButtonStyle();

  return (
    <Button 
      variant={variant} 
      onClick={handleLogin}
      className={className}
    >
      {icon} {text}
    </Button>
  );
};

export default LoginButton;