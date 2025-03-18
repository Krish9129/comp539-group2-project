import React, { useState, useEffect, useRef } from 'react';
import { Card, Spinner } from 'react-bootstrap';
import { FaInfoCircle, FaExternalLinkAlt, FaTags } from 'react-icons/fa';
import urlService from '../services/urlService';
import './UrlPreview.css';

// Throttle helper function to prevent multiple API calls
const throttle = (callback, delay = 2000) => {
  let lastCall = 0;
  return function(...args) {
    const now = Date.now();
    if (now - lastCall >= delay) {
      lastCall = now;
      callback(...args);
    }
  };
};

// Refined color palette for keyword tags with elegant soft backgrounds
const KEYWORD_COLORS = [
  '#E0F2FE', // Soft blue
  '#FEE2E2', // Soft red
  '#D1FAE5', // Soft green
  '#FEF3C7', // Soft yellow
  '#E5E7EB', // Soft gray
  '#FCE7F3', // Soft pink
  '#DBEAFE', // Light blue
  '#ECFCCB', // Soft lime
  '#F3E8FF', // Soft purple
  '#FFEDD5', // Soft orange
  '#E0E7FF', // Soft indigo
  '#F5F5F4', // Soft stone
];

// Custom keyword tag component to replace Bootstrap Badge
const KeywordTag = ({ keyword, backgroundColor }) => {
  return (
    <span 
      className="custom-keyword-tag"
      style={{ backgroundColor }}
    >
      {keyword}
    </span>
  );
};

const UrlPreview = ({ shortId, show, position }) => {
  const [summary, setSummary] = useState(null);
  const [keywords, setKeywords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [summaryRequested, setSummaryRequested] = useState(false);
  const previewRef = useRef(null);
  
  // Fetch summary only once per shortId
  const fetchSummary = async () => {
    if (summaryRequested || loading || summary || error) return;
    
    setLoading(true);
    setSummaryRequested(true);
    
    try {
      const data = await urlService.getUrlSummary(shortId);
      
      // Process summary text to limit length
      let processedSummary = data.summary;
      if (processedSummary && processedSummary.length > 300) {
        processedSummary = processedSummary.substring(0, 297) + '...';
      }
      
      setSummary(processedSummary);
      
      // Process keywords if available
      if (data.keywords) {
        // Split keywords by comma and trim spaces
        const keywordArray = data.keywords.split(',').map(keyword => keyword.trim());
        setKeywords(keywordArray);
        console.log("Processed keywords:", keywordArray);
      }
      
      setLoading(false);
    } catch (err) {
      console.error('Error fetching URL summary:', err);
      setError('Unable to load preview');
      setLoading(false);
    }
  };
  
  // Throttled fetch to prevent multiple calls
  const throttledFetch = useRef(throttle(fetchSummary));
  
  // Fetch summary when component is shown
  useEffect(() => {
    let isMounted = true;
    
    if (show && !summaryRequested && !summary && !loading && !error) {
      throttledFetch.current();
    }
    
    return () => {
      isMounted = false;
    };
  }, [show, shortId, summary, loading, error, summaryRequested]);
  
  // Reset state when shortId changes
  useEffect(() => {
    setSummary(null);
    setKeywords([]);
    setError(null);
    setLoading(false);
    setSummaryRequested(false);
  }, [shortId]);
  
  // Don't render anything if not showing
  if (!show) return null;
  
  // Calculate dynamic position
  const getStyle = () => {
    if (!position) return {};
    
    // Calculate if preview would go off-screen
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const previewWidth = 350; // Max width of preview
    
    const style = {
      position: 'fixed', // Fixed positioning relative to viewport
      zIndex: 1000,
    };
    
    // Horizontal position adjustment
    const rightEdge = position.x + previewWidth + 20;
    const isOffScreenRight = rightEdge > viewportWidth;
    
    if (isOffScreenRight) {
      style.right = `${viewportWidth - position.x + 10}px`;
    } else {
      style.left = `${position.x + 20}px`; // Offset to the right of cursor
    }
    
    // Vertical position adjustment - position the preview above the mouse pointer
    style.top = `${position.y - 20}px`; // Slight upward offset to avoid being covered by cursor
    
    return style;
  };
  
  // Format summary text to handle multiple paragraphs
  const formatSummary = (text) => {
    if (!text) return "Loading summary data...";
    
    // Process text into paragraphs
    const paragraphs = text.split('\n').filter(p => p.trim() !== '');
    
    if (paragraphs.length <= 1) {
      // If no natural paragraphs found, return as is
      return text;
    } else {
      return paragraphs.map((paragraph, index) => (
        <p key={index}>{paragraph}</p>
      ));
    }
  };
  
  // Get a color from the predefined palette based on the keyword
  const getKeywordColor = (keyword) => {
    // Use a hash method to map keywords to colors
    // This ensures consistent colors for the same keyword
    const hash = keyword
      .split('')
      .reduce((acc, char, i) => {
        return acc + char.charCodeAt(0) * (i + 1);
      }, 0);
    
    // Select a color from our predefined palette
    const colorIndex = Math.abs(hash % KEYWORD_COLORS.length);
    const selectedColor = KEYWORD_COLORS[colorIndex];
    
    console.log(`Keyword: ${keyword}, Using color: ${selectedColor}`);
    return selectedColor;
  };
  
  return (
    <div className="url-preview-container" style={getStyle()} ref={previewRef}>
      <Card className="url-preview-card">
        <Card.Body>
          <div className="d-flex align-items-center mb-2">
            <FaInfoCircle className="me-2 text-primary" />
            <h6 className="mb-0">URL Preview</h6>
            <div className="ms-auto">
              <a 
                href={`http://localhost:8080/api/${shortId}`} 
                target="_blank" 
                rel="noopener noreferrer" 
                className="preview-link"
                onClick={(e) => e.stopPropagation()}
              >
                <FaExternalLinkAlt size={14} />
              </a>
            </div>
          </div>
          
          {loading ? (
            <div className="preview-loading-container">
              <div className="preview-loading-animation">
                <Spinner animation="border" variant="primary" size="sm" className="me-2" />
                <Spinner animation="grow" variant="primary" size="sm" className="me-2" />
                <Spinner animation="grow" variant="primary" size="sm" />
              </div>
              <p className="preview-loading-text">Generating summary...</p>
            </div>
          ) : error ? (
            <div className="preview-error">
              <p>{error}</p>
            </div>
          ) : (
            <>
              <div className="preview-summary">
                {formatSummary(summary)}
              </div>
              
              {keywords && keywords.length > 0 && (
                <div className="preview-keywords">
                  <div className="keywords-header">
                    <FaTags className="keywords-icon" />
                    <span>Keywords</span>
                  </div>
                  <div className="keywords-badges">
                    {keywords.map((keyword, index) => (
                      <KeywordTag 
                        key={index}
                        keyword={keyword}
                        backgroundColor={getKeywordColor(keyword)}
                      />
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default UrlPreview; 