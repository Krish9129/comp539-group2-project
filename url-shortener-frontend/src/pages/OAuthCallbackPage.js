import React, { useEffect, useState } from 'react';
import { Container, Spinner, Alert } from 'react-bootstrap';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext'; // import AuthContext

const OAuthCallbackPage = () => {
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  useEffect(() => {
    const processCallback = async () => {
      try {
        // extract token from URL
        const searchParams = new URLSearchParams(location.search);
        const token = searchParams.get('token');
        
        if (token) {
          console.log('Token received:', token);
          
          // save token to local storage
          localStorage.setItem('auth_token', token);
          
          // login user
          login(token);
          
          // redirect to profile page
          setTimeout(() => {
            navigate('/profile');
          }, 1000);
        } else {
          console.error('No token found in URL');
          setError('No authentication token received. Authentication failed.');
        }
      } catch (error) {
        console.error('Error processing OAuth callback:', error);
        setError('Authentication failed. Please try again.');
      }
    };

    processCallback();
  }, [navigate, location, login]);

  return (
    <Container className="text-center py-5">
      {!error ? (
        <div>
          <Spinner animation="border" variant="primary" />
          <h4 className="mt-3">Authentication successful!</h4>
          <p className="text-muted">You will be redirected to your profile shortly.</p>
        </div>
      ) : (
        <Alert variant="danger">
          <h4>Authentication Error</h4>
          <p>{error}</p>
          <Alert.Link onClick={() => navigate('/login')}>
            Go back to login
          </Alert.Link>
        </Alert>
      )}
    </Container>
  );
};

export default OAuthCallbackPage;