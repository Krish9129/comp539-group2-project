import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Form, Button, Alert, Spinner, Card, Badge } from 'react-bootstrap';
import UrlCard from '../components/UrlCard';
import urlService from '../services/urlService';
import { FaChevronDown, FaChevronRight, FaTag } from 'react-icons/fa';
import './UrlListPage.css'; 

const UrlListPage = () => {
  const [tag, setTag] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [urls, setUrls] = useState([]);
  const [expandedTags, setExpandedTags] = useState({});
  
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
        
        // Initialize expanded state for all tags
        const tagsState = {};
        const uniqueTags = [...new Set(data.map(url => url.tag || 'default'))];
        uniqueTags.forEach(tag => {
          tagsState[tag] = true; // All groups start expanded
        });
        setExpandedTags(tagsState);
        
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
  
  // Toggle expanded state for a tag group
  const toggleTagGroup = (tagName) => {
    setExpandedTags(prevState => ({
      ...prevState,
      [tagName]: !prevState[tagName]
    }));
  };
  
  // Group URLs by tag
  const getUrlsByTag = () => {
    const groupedUrls = {};
    
    urls.forEach(url => {
      const tagName = url.tag || 'default';
      if (!groupedUrls[tagName]) {
        groupedUrls[tagName] = [];
      }
      groupedUrls[tagName].push(url);
    });
    
    return groupedUrls;
  };

  // Get color for tag badge
  const getTagColor = (tagName) => {
    const colors = ['primary', 'success', 'danger', 'warning', 'info'];
    let sum = 0;
    for (let i = 0; i < tagName.length; i++) {
      sum += tagName.charCodeAt(i);
    }
    return colors[sum % colors.length];
  };

  return (
    <Container>
      <Row>
        <Col md={10} lg={8} className="mx-auto">
          <div className="page-header my-4">
            <h2>My Link Collection</h2>
            <div className="header-underline"></div>
          </div>
          
          <Form onSubmit={handleSubmit} className="mb-4 search-form">
            <Form.Group className="mb-3">
              <Form.Label><strong>Search by Tag</strong></Form.Label>
              <div className="d-flex">
                <Form.Control
                  type="text"
                  placeholder="Enter tag name (leave empty for all URLs)"
                  value={tag}
                  onChange={handleTagChange}
                  className="me-2 search-input"
                />
                <Button 
                  type="submit" 
                  variant="primary"
                  disabled={loading}
                  className="search-button"
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
              <p className="mt-2">Loading your links...</p>
            </div>
          ) : (
            <>
              {urls.length > 0 ? (
                <div className="tag-groups-container">
                  {Object.entries(getUrlsByTag()).map(([tagName, tagUrls]) => (
                    <Card 
                      className={`mb-3 tag-group-card ${expandedTags[tagName] ? 'expanded' : ''}`} 
                      key={tagName}
                    >
                      <Card.Header 
                        onClick={() => toggleTagGroup(tagName)}
                        className="tag-header d-flex justify-content-between align-items-center"
                      >
                        <div className="d-flex align-items-center">
                          <FaTag className="me-2 tag-icon" />
                          <span className="tag-name">
                            {tagName === 'None' ? 'Default Group' : tagName}
                          </span>
                          <Badge 
                            bg={getTagColor(tagName)} 
                            className="ms-2 tag-count"
                          >
                            {tagUrls.length}
                          </Badge>
                        </div>
                        <div className="tag-toggle">
                          {expandedTags[tagName] ? <FaChevronDown /> : <FaChevronRight />}
                        </div>
                      </Card.Header>
                      
                      <div className={`collapse-container ${expandedTags[tagName] ? 'show' : ''}`}>
                        <Card.Body className="tag-body">
                          {tagUrls.map(url => (
                            <UrlCard 
                              key={url.id} 
                              url={url} 
                              onDelete={handleUrlDelete}
                            />
                          ))}
                        </Card.Body>
                      </div>
                    </Card>
                  ))}
                </div>
              ) : (
                !error && <Alert variant="light" className="no-results">No URLs found. Try clearing the search to view all your URLs.</Alert>
              )}
            </>
          )}
        </Col>
      </Row>
    </Container>
  );
};

export default UrlListPage;