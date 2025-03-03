import React, { useState } from 'react';
import { Container, Row, Col, Card } from 'react-bootstrap';
import UrlForm from '../components/UrlForm';
import UrlCard from '../components/UrlCard';

const HomePage = () => {
  const [recentUrl, setRecentUrl] = useState(null);

  // Callback after successful URL creation
  const handleUrlCreated = (data) => {
    // Construct URL object
    const urlEntity = {
      id: data.shortId,
      originalUrl: document.querySelector('input[type="url"]').value,
      tag: document.querySelector('input[placeholder*="e.g.:"]').value || 'None',
      clickCount: 0,
      lastAccess: new Date().toString()
    };
    
    // Update recently created URL
    setRecentUrl(urlEntity);
  };

  return (
    <Container>
      <Row className="justify-content-center">
        <Col md={8} lg={6}>
          <Card className="shadow-sm mb-4">
            <Card.Body>
              <UrlForm onSuccess={handleUrlCreated} />
            </Card.Body>
          </Card>

          {recentUrl && (
            <div className="mt-4">
              <h4>Recently Created Link</h4>
              <UrlCard 
                url={recentUrl} 
                onDelete={() => setRecentUrl(null)} 
              />
            </div>
          )}
          
          <div className="mt-5">
            <h3 className="text-center mb-4">Why Use Our URL Shortening Service?</h3>
            
            <Row className="g-4">
              <Col md={4}>
                <Card className="h-100 text-center p-3">
                  <Card.Body>
                    <h5>Simple to Use</h5>
                    <p>Just paste your long URL and get a concise short link</p>
                  </Card.Body>
                </Card>
              </Col>
              
              <Col md={4}>
                <Card className="h-100 text-center p-3">
                  <Card.Body>
                    <h5>Custom Aliases</h5>
                    <p>Create meaningful, easy-to-remember links</p>
                  </Card.Body>
                </Card>
              </Col>
              
              <Col md={4}>
                <Card className="h-100 text-center p-3">
                  <Card.Body>
                    <h5>Click Analytics</h5>
                    <p>Track how many times your links are clicked and when they were last accessed</p>
                  </Card.Body>
                </Card>
              </Col>
            </Row>
          </div>
        </Col>
      </Row>
    </Container>
  );
};

export default HomePage;