import React from 'react';
import { Container } from 'react-bootstrap';

const Footer = () => {
  return (
    <footer className="bg-light py-3 mt-auto">
      <Container className="text-center text-muted">
        <p className="mb-0">© {new Date().getFullYear()} URL Shortener. All rights reserved.</p>
      </Container>
    </footer>
  );
};

export default Footer;