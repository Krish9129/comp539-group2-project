import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Form, Button, Alert, Spinner } from 'react-bootstrap';
import UrlCard from '../components/UrlCard';
import urlService from '../services/urlService';

const UrlListPage = () => {
  const [tag, setTag] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [urls, setUrls] = useState([]);
  
  // Load all URLs when the component mounts
  useEffect(() => {
    fetchUrlsByTag('');
  }, []);
  
  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    await fetchUrlsByTag(tag);
  };
  
  // Fetch URLs by tag (if tag is empty, fetch all URLs)
  const fetchUrlsByTag = async (tagValue) => {
    setLoading(true);
    setError('');
    
    try {
      const data = await urlService.getUrlsByTag(tagValue);
      
      if (Array.isArray(data)) {
        setUrls(data);
        if (data.length === 0) {
          setError('No URLs found for this tag.');
        }
      } else {
        // If response is a message rather than an array
        setUrls([]);
        if (data.message) {
          setError(data.message);
        }
      }
    } catch (error) {
      console.error('Error fetching URLs:', error);
      setError('Failed to retrieve URL list. Please try again later.');
      setUrls([]);
    } finally {
      setLoading(false);
    }
  };
  
  // Handle URL deletion
  const handleUrlDelete = (deletedId) => {
    setUrls(urls.filter(url => url.id !== deletedId));
  };

  // Handle tag input change
  const handleTagChange = (e) => {
    const newTag = e.target.value;
    setTag(newTag);
    
    // If the tag is cleared, fetch all URLs
    if (!newTag.trim()) {
      fetchUrlsByTag('');
    }
  };

  return (
    <Container>
      <Row>
        <Col md={8} className="mx-auto">
          <h2 className="mb-4">My Links</h2>
          
          <Form onSubmit={handleSubmit} className="mb-4">
            <Form.Group className="mb-3">
              <Form.Label>Search by Tag</Form.Label>
              <div className="d-flex">
                <Form.Control
                  type="text"
                  placeholder="Enter tag name (leave empty for all URLs)"
                  value={tag}
                  onChange={handleTagChange}
                  className="me-2"
                />
                <Button 
                  type="submit" 
                  variant="primary"
                  disabled={loading}
                >
                  {loading ? <Spinner animation="border" size="sm" /> : 'Search'}
                </Button>
              </div>
            </Form.Group>
          </Form>
          
          {error && <Alert variant="info">{error}</Alert>}
          
          {loading ? (
            <div className="text-center my-5">
              <Spinner animation="border" />
              <p className="mt-2">Loading...</p>
            </div>
          ) : (
            <>
              {urls.length > 0 ? (
                <div>
                  {urls.map(url => (
                    <UrlCard 
                      key={url.id} 
                      url={url} 
                      onDelete={handleUrlDelete}
                    />
                  ))}
                </div>
              ) : (
                !error && <Alert variant="light">No URLs found. Try clearing the search to view all your URLs.</Alert>
              )}
            </>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default UrlListPage;