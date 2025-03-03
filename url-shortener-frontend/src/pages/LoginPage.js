import React from 'react';
import { Container, Row, Col, Card } from 'react-bootstrap';
import LoginButton from '../components/LoginButton';
import { FaLink } from 'react-icons/fa';

const LoginPage = () => {
  return (
    <Container className="py-5">
      <Row className="justify-content-center">
        <Col md={6} lg={5}>
          <Card className="shadow">
            <Card.Body className="p-5">
              <div className="text-center mb-4">
                <FaLink size={50} className="text-primary mb-3" />
                <h2 className="fw-bold">URL Shortener</h2>
                <p className="text-muted">Sign in to manage your links</p>
              </div>

              <div className="d-grid gap-3 mt-4">
                <LoginButton provider="google" className="w-100" />
                <LoginButton provider="github" className="w-100" />
              </div>
              
              <div className="mt-4 text-center">
                <p className="text-muted small">
                  By signing in, you agree to our Terms of Service and Privacy Policy.
                </p>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default LoginPage;