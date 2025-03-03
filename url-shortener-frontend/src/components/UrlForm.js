import React, { useState } from 'react';
import { Form, Button, Alert, Spinner } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import urlService from '../services/urlService';
import { useAuth } from '../context/AuthContext';
import UrlSuccessCard from './UrlSuccessCard'; // Import the new success component

const UrlForm = ({ onSuccess }) => {
  const [url, setUrl] = useState('');
  const [alias, setAlias] = useState('');
  const [tag, setTag] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const { isAuthenticated } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate URL
    if (!url) {
      setError('Please enter a URL');
      return;
    }
    
    // Reset states
    setLoading(true);
    setError('');
    setResult(null);
    
    try {
      // Call service to shorten URL
      const response = await urlService.shortenUrl(url, alias, tag);
      
      // Set result
      setResult(response);
      
      // Call success callback if provided
      if (onSuccess) {
        onSuccess(response);
      }
      
      // Reset form fields but keep the result displayed
      setUrl('');
      setAlias('');
      setTag('');
    } catch (error) {
      console.error('Error shortening URL:', error);
      setError(error.response?.data?.error || 'Failed to create short URL. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="url-form-container">
      <h2 className="mb-4">Create Short URL</h2>
      
      {error && <Alert variant="danger">{error}</Alert>}
      
      {/* Use the new UrlSuccessCard component for result display */}
      {result && <UrlSuccessCard result={result} />}
      
      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3">
          <Form.Label>Original URL *</Form.Label>
          <Form.Control
            type="url"
            placeholder="https://example.com"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            required
          />
        </Form.Group>
        
        <Form.Group className="mb-3">
          <Form.Label>Custom Alias (Optional)</Form.Label>
          <Form.Control
            type="text"
            placeholder="my-custom-url"
            value={alias}
            onChange={(e) => setAlias(e.target.value)}
          />
          <Form.Text className="text-muted">
            Leave blank to auto-generate. If specified, must be unique.
          </Form.Text>
        </Form.Group>
        
        <Form.Group className="mb-3">
          <Form.Label>Tag (Optional)</Form.Label>
          <Form.Control
            type="text"
            placeholder="e.g.: social, work, personal"
            value={tag}
            onChange={(e) => setTag(e.target.value)}
          />
          <Form.Text className="text-muted">
            Used to categorize and find your URLs
          </Form.Text>
        </Form.Group>
        
        <Button 
          variant="primary" 
          type="submit" 
          disabled={loading}
          className="w-100"
        >
          {loading ? (
            <>
              <Spinner
                as="span"
                animation="border"
                size="sm"
                role="status"
                aria-hidden="true"
                className="me-2"
              />
              Processing...
            </>
          ) : 'Generate Short URL'}
        </Button>
        
        {!isAuthenticated && (
          <div className="mt-3 text-center">
            <small className="text-muted">
              <Link to="/login">Sign in</Link> to save your short links and view click statistics.
            </small>
          </div>
        )}
      </Form>
    </div>
  );
};

export default UrlForm;