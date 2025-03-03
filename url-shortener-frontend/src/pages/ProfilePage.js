import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Alert, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import { FaUser, FaSignOutAlt } from 'react-icons/fa';

const ProfilePage = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true);
        
        // make sure user is authenticated
        if (!authService.isAuthenticated()) {
          navigate('/login');
          return;
        }
  
        // use the service to get user profile
        const userData = await authService.getUserProfile();
        console.log('Profile data received:', userData);
        setProfile(userData);
      } catch (error) {
        console.error('Error loading profile:', error);
        
        // check if response is available
        if (error.response) {
          console.error('Error status:', error.response.status);
          console.error('Error data:', error.response.data);
        }
        
        // check if error is due to unauthorized access
        if (error.response && error.response.status === 401) {
          authService.logout();
          navigate('/login');
        } else {
          setError('Failed to load profile. Please try again later.');
        }
      } finally {
        setLoading(false);
      }
    };
  
    loadProfile();
  }, [navigate]);

  const handleLogout = () => {
    authService.logout();
  };

  if (loading) {
    return (
      <Container className="text-center py-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3">Loading profile...</p>
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <Row className="justify-content-center">
        <Col md={8} lg={6}>
          <h2 className="mb-4">My Profile</h2>

          {error && <Alert variant="danger">{error}</Alert>}

          {profile && (
            <Card>
              <Card.Body>
                <div className="d-flex align-items-center mb-4">
                  {profile.pictureUrl ? (
                    <img 
                      src={profile.pictureUrl} 
                      alt="Profile" 
                      className="rounded-circle me-3" 
                      style={{ width: '64px', height: '64px' }}
                    />
                  ) : (
                    <div 
                      className="rounded-circle bg-secondary d-flex align-items-center justify-content-center me-3" 
                      style={{ width: '64px', height: '64px' }}
                    >
                      <FaUser size={24} color="white" />
                    </div>
                  )}
                  <div>
                    <h4 className="mb-0">{profile.name}</h4>
                    <p className="text-muted mb-0">{profile.email}</p>
                  </div>
                </div>

                <Row className="mb-3">
                  <Col sm={4}>
                    <p className="text-muted mb-0">Auth Provider:</p>
                  </Col>
                  <Col sm={8}>
                    <p className="mb-0 text-capitalize">{profile.provider}</p>
                  </Col>
                </Row>

                <Row className="mb-3">
                  <Col sm={4}>
                    <p className="text-muted mb-0">Role:</p>
                  </Col>
                  <Col sm={8}>
                    <p className="mb-0">{profile.role}</p>
                  </Col>
                </Row>

                <Row className="mb-3">
                  <Col sm={4}>
                    <p className="text-muted mb-0">Last Login:</p>
                  </Col>
                  <Col sm={8}>
                    <p className="mb-0">
                      {new Date(profile.lastLogin).toLocaleString()}
                    </p>
                  </Col>
                </Row>

                <div className="d-grid mt-4">
                  <Button variant="outline-danger" onClick={handleLogout}>
                    <FaSignOutAlt className="me-2" /> Sign Out
                  </Button>
                </div>
              </Card.Body>
            </Card>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default ProfilePage;