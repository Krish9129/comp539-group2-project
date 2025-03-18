import React from 'react';
import { Navbar, Container, Nav, NavDropdown, Image } from 'react-bootstrap';
import { Link, useNavigate } from 'react-router-dom';
import { FaLink, FaUser } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';

const Header = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handleProfile = () => {
    navigate('/profile');
  };

  return (
    <Navbar bg="dark" variant="dark" expand="lg" className="mb-4">
      <Container>
        <Navbar.Brand as={Link} to="/">
          <FaLink className="me-2" />
          ZapLink
        </Navbar.Brand>
        
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <Nav.Link as={Link} to="/">Home</Nav.Link>
            <Nav.Link as={Link} to="/urls">My Links</Nav.Link>
          </Nav>
          
          <Nav>
            {isAuthenticated ? (
              <NavDropdown 
                title={
                  <span className="d-inline-flex align-items-center">
                    {user && user.pictureUrl ? (
                      <Image 
                        src={user.pictureUrl} 
                        roundedCircle 
                        width={24} 
                        height={24} 
                        className="me-1" 
                      />
                    ) : (
                      <FaUser className="me-1" />
                    )}
                    {user ? user.name : 'User'}
                  </span>
                } 
                id="basic-nav-dropdown"
              >
                <NavDropdown.Item onClick={handleProfile}>My Profile</NavDropdown.Item>
                <NavDropdown.Divider />
                <NavDropdown.Item onClick={handleLogout}>Logout</NavDropdown.Item>
              </NavDropdown>
            ) : (
              <Nav.Link as={Link} to="/login">Login</Nav.Link>
            )}
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default Header;