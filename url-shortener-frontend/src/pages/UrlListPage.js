import React, { useState} from 'react';
import { Container, Row, Col, Form, Button, Alert, Spinner } from 'react-bootstrap';
import UrlCard from '../components/UrlCard';
import urlService from '../services/urlService';

const UrlListPage = () => {
  const [tag, setTag] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [urls, setUrls] = useState([]);
  
  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!tag) {
      setError('Please enter a tag');
      return;
    }
    
    await fetchUrlsByTag(tag);
  };
  
  // Fetch URLs by tag
  const fetchUrlsByTag = async (tagValue) => {
    setLoading(true);
    setError('');
    
    try {
      const data = await urlService.getUrlsByTag(tagValue);
      
      if (Array.isArray(data)) {
        setUrls(data);
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
                  placeholder="Enter tag name"
                  value={tag}
                  onChange={(e) => setTag(e.target.value)}
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
                !error && <Alert variant="light">No URLs found. Try searching with a different tag.</Alert>
              )}
            </>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default UrlListPage;