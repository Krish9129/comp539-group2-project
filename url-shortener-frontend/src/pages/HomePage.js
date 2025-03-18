import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Button, Badge } from 'react-bootstrap';
import { motion, AnimatePresence } from 'framer-motion';
import { FaLink, FaRocket, FaChartLine, FaTag, FaUserFriends, FaBrain, FaMagic, FaHistory } from 'react-icons/fa';
import UrlForm from '../components/UrlForm';
import UrlCard from '../components/UrlCard';
import UrlSuccessCard from '../components/UrlSuccessCard';

const HomePage = () => {
  const [recentUrl, setRecentUrl] = useState(null);
  // Animation loading state
  const [isLoaded, setIsLoaded] = useState(false);
  const [urlCreationResult, setUrlCreationResult] = useState(null);
  
  useEffect(() => {
    // Set animation state after page loads
    setIsLoaded(true);
  }, []);

  // Callback after successful URL creation
  const handleUrlCreated = (data) => {
    // Store creation result for success card
    setUrlCreationResult(data);
    
    // Construct URL object for display in history
    const urlEntity = {
      id: data.shortId,
      originalUrl: document.querySelector('input[type="url"]').value,
      tag: document.querySelector('input[placeholder*="e.g.:"]').value || 'None',
      clickCount: 0,
      lastAccess: new Date().toString()
    };
    
    // Update recently created URL
    setRecentUrl(urlEntity);
    
    // Scroll to success card with smooth animation
    // setTimeout(() => {
    //   const successElement = document.getElementById('success-section');
    //   if (successElement) {
    //     successElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    //   }
    // }, 100);
  };

  // Handle close of success card
  const handleCloseSuccess = () => {
    setUrlCreationResult(null);
  };

  // Fade-in animation configuration
  const fadeInVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.6 } }
  };

  return (
    <div className="homepage-wrapper py-5" style={{ background: 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)' }}>
      <Container>
        {/* Title Section */}
        <motion.div 
          initial={{ opacity: 0, y: -30 }}
          animate={isLoaded ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.8 }}
          className="text-center mb-5"
        >
          <h1 className="display-4 fw-bold text-primary mb-2">
            <FaLink className="me-2" style={{ verticalAlign: 'middle' }} />
            ZapLink
          </h1>
          <p className="lead text-muted">Create short links quickly, track clicks easily</p>
        </motion.div>
        
        <Row className="justify-content-center">
          <Col lg={7} md={10}>
            {/* Success Card Section - Shown only after URL creation */}
            <AnimatePresence>
              {urlCreationResult && (
                <motion.div 
                  id="success-section"
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -20 }}
                  transition={{ duration: 0.4 }}
                  className="mb-4"
                >
                  <UrlSuccessCard 
                    result={urlCreationResult} 
                    onClose={handleCloseSuccess} 
                  />
                </motion.div>
              )}
            </AnimatePresence>
            
            {/* URL Shortening Form */}
            <motion.div
              initial="hidden"
              animate={isLoaded ? "visible" : "hidden"}
              variants={fadeInVariants}
            >
              <Card className="shadow border-0 mb-4 overflow-hidden">
                <Card.Header className="bg-primary text-white py-3">
                  <h4 className="mb-0"><FaMagic className="me-2" />Shorten Your URL</h4>
                </Card.Header>
                <Card.Body className="p-4">
                  <UrlForm onSuccess={handleUrlCreated} />
                </Card.Body>
              </Card>
            </motion.div>

            {/* Recently Created Link History */}
            {recentUrl && (
              <motion.div 
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.4, delay: 0.2 }}
                className="mt-4 mb-5"
              >
                <Card className="border-0 shadow-sm link-history-card">
                  <Card.Header className="bg-light text-dark py-3 d-flex align-items-center">
                    <FaHistory className="me-2 text-primary" />
                    <h5 className="mb-0 flex-grow-1">Link History</h5>
                    <Badge bg="secondary" className="py-1 px-2">{recentUrl ? 1 : 0} link</Badge>
                  </Card.Header>
                  <Card.Body className="p-4">
                    <UrlCard 
                      url={recentUrl} 
                      onDelete={() => setRecentUrl(null)} 
                    />
                  </Card.Body>
                </Card>
              </motion.div>
            )}
          </Col>
        </Row>
        
        {/* Service Features Section */}
        <motion.div 
          initial="hidden"
          animate={isLoaded ? "visible" : "hidden"}
          variants={{
            hidden: { opacity: 0 },
            visible: { opacity: 1, transition: { staggerChildren: 0.1, delayChildren: 0.3 } }
          }}
          className="mt-5 pt-4"
        >
          <h2 className="text-center fw-bold mb-4">Why Choose Our ZapLink Service?</h2>
          <div className="text-center mb-5">
            <div className="d-inline-block mx-auto" style={{ width: '80px', height: '4px', background: 'linear-gradient(90deg, #4e73df, #36b9cc)' }}></div>
          </div>
          
          <Row className="g-4 justify-content-center">
            <Col lg={3} md={6}>
              <motion.div variants={fadeInVariants}>
                <Card className="h-100 text-center border-0 shadow-sm hover-card ai-feature-card">
                  <Card.Body className="p-4">
                    <div className="icon-wrapper mb-3">
                      <div className="icon-circle bg-primary bg-opacity-10 text-primary">
                        <FaBrain size={24} />
                      </div>
                    </div>
                    <h4 className="mb-3">AI Summary</h4>
                    <p className="text-muted">Get intelligent summaries of your shared content powered by advanced AI technology</p>
                  </Card.Body>
                </Card>
              </motion.div>
            </Col>
            
            <Col lg={3} md={6}>
              <motion.div variants={fadeInVariants}>
                <Card className="h-100 text-center border-0 shadow-sm hover-card">
                  <Card.Body className="p-4">
                    <div className="icon-wrapper mb-3">
                      <div className="icon-circle bg-success bg-opacity-10 text-success">
                        <FaTag size={24} />
                      </div>
                    </div>
                    <h4 className="mb-3">Custom Aliases</h4>
                    <p className="text-muted">Create meaningful, easy-to-remember links to enhance brand recognition</p>
                  </Card.Body>
                </Card>
              </motion.div>
            </Col>
            
            <Col lg={3} md={6}>
              <motion.div variants={fadeInVariants}>
                <Card className="h-100 text-center border-0 shadow-sm hover-card">
                  <Card.Body className="p-4">
                    <div className="icon-wrapper mb-3">
                      <div className="icon-circle bg-info bg-opacity-10 text-info">
                        <FaChartLine size={24} />
                      </div>
                    </div>
                    <h4 className="mb-3">Click Analytics</h4>
                    <p className="text-muted">Track how many times your links are clicked and when they were last accessed</p>
                  </Card.Body>
                </Card>
              </motion.div>
            </Col>

            <Col lg={3} md={6}>
              <motion.div variants={fadeInVariants}>
                <Card className="h-100 text-center border-0 shadow-sm hover-card">
                  <Card.Body className="p-4">
                    <div className="icon-wrapper mb-3">
                      <div className="icon-circle bg-warning bg-opacity-10 text-warning">
                        <FaUserFriends size={24} />
                      </div>
                    </div>
                    <h4 className="mb-3">Safe & Reliable</h4>
                    <p className="text-muted">High-availability design ensures your links are always accessible with data security</p>
                  </Card.Body>
                </Card>
              </motion.div>
            </Col>
          </Row>
        </motion.div>

        {/* CTA Section */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={isLoaded ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.8, delay: 0.6 }}
          className="text-center mt-5 pt-4 cta-section"
        >
          <Card className="border-0 shadow" style={{ background: '#2c3e50' }}>
            <Card.Body className="py-5 px-md-5">
              <h2 className="text-white fw-bold mb-4">Ready to start using ZapLink?</h2>
              <p className="text-white lead mb-4">Sign up for an account to access more advanced features and detailed click analytics</p>
              <Button 
                variant="light" 
                size="lg" 
                className="px-4 shadow-sm fw-semibold" 
                href="/login"
              >
                Get Started
              </Button>
            </Card.Body>
          </Card>
        </motion.div>
      </Container>
    </div>
  );
};

export default HomePage;