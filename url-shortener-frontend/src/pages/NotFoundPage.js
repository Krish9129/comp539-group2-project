import React from 'react';
import { Container, Row, Col, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';

const NotFoundPage = () => {
  return (
    <Container className="text-center py-5">
      <Row className="justify-content-center">
        <Col md={6}>
          <h1 className="display-1">404</h1>
          <h2 className="mb-4">Page Not Found</h2>
          <p className="lead mb-4">
            The page you requested does not exist or has been removed.
          </p>
          <Button as={Link} to="/" variant="primary" size="lg">
            Return to Home
          </Button>
        </Col>
      </Row>
    </Container>
  );
};

export default NotFoundPage;