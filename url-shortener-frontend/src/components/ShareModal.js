import React, { useState, useRef, useEffect } from 'react';
import { Modal, Button, InputGroup, Form } from 'react-bootstrap';
import { FaShareAlt, FaLinkedin, FaReddit, FaTwitter, FaCheck } from 'react-icons/fa';
import { FaSquareXTwitter } from "react-icons/fa6";
import urlService from '../services/urlService';
import './UrlSuccessCard.css'; // Using the same CSS file for styling

/**
 * ShareModal component for sharing URLs to social media platforms
 * 
 * @param {Object} props Component props
 * @param {boolean} props.show Whether to show the modal
 * @param {Function} props.onHide Callback when modal is closed
 * @param {string} props.shortId The short ID of the URL
 * @param {string} props.shortUrl The full short URL to share
 */
const ShareModal = ({ show, onHide, shortId, shortUrl }) => {
  // State for clipboard functionality
  const [copied, setCopied] = useState(false);
  const urlRef = useRef(null);
  
  // State for URL summary
  const [urlSummary, setUrlSummary] = useState(null);
  const [isSummaryLoading, setIsSummaryLoading] = useState(false);
  
  // Fetch URL summary for sharing
  const fetchUrlSummary = async () => {
    if (!urlSummary && !isSummaryLoading && shortId) {
      setIsSummaryLoading(true);
      try {
        const summary = await urlService.getUrlSummary(shortId);
        setUrlSummary(summary);
      } catch (error) {
        console.error('Error fetching URL summary:', error);
      } finally {
        setIsSummaryLoading(false);
      }
    }
  };
  
  // Handle copy to clipboard
  const handleCopy = () => {
    if (urlRef.current) {
      urlRef.current.select();
    }
    navigator.clipboard.writeText(shortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Handle social media sharing
  const handleShare = (platform) => {
    const defaultTitle = "Check out this link";
    const shareTitle = urlSummary?.keywords || defaultTitle;
    const encodedUrl = encodeURIComponent(shortUrl);
    const encodedTitle = encodeURIComponent(shareTitle);
    
    let shareUrl = '';
    
    switch (platform) {
      case 'twitter':
        shareUrl = `https://x.com/intent/post?url=${encodedUrl}&text=${encodedTitle}`;
        break;
      case 'linkedin':
        shareUrl = `https://www.linkedin.com/feed/?linkOrigin=LI_BADGE&shareActive=true&shareUrl=${encodedUrl}&text=${encodedTitle}`;
        break;
      case 'reddit':
        shareUrl = `https://www.reddit.com/submit?url=${encodedUrl}&title=${encodedTitle}&type=LINK`;
        break;
      default:
        shareUrl = `https://wa.me/?text=${encodeURIComponent(`${shareTitle}: ${shortUrl}`)}`;
    }
    
    window.open(shareUrl, '_blank', 'noopener,noreferrer');
  };

  // Fetch URL summary when modal is shown
  useEffect(() => {
    if (show) {
      fetchUrlSummary();
    }
  }, [show, shortId]);

  return (
    <Modal
      show={show}
      onHide={onHide}
      centered
      size="md"
      className="share-modal"
    >
      <Modal.Header closeButton className="border-0 pb-0">
        <Modal.Title className="text-success">
          <FaShareAlt className="me-2" />
          Share Your ZapLink
        </Modal.Title>
      </Modal.Header>
      <Modal.Body className="px-4 pt-2 pb-3">
        <p className="text-muted mb-3">Share this link with your network</p>
        
        <div className="url-share-container mb-4">
          <InputGroup className="mb-2 shadow-sm">
            <Form.Control
              ref={urlRef}
              type="text"
              value={shortUrl}
              readOnly
              className="bg-light border"
            />
            <Button 
              variant={copied ? "success" : "outline-primary"}
              onClick={handleCopy}
              className="d-flex align-items-center justify-content-center"
            >
              {copied ? "Copied!" : "Copy link"}
            </Button>
          </InputGroup>
        </div>
        
        <div className="share-buttons-container">
          <div className="social-platforms-title mb-2">
            <span className="text-muted">Share via</span>
          </div>
          <div className="share-buttons">
            <Button 
              variant="light" 
              className="share-platform-btn linkedin-btn"
              onClick={() => handleShare('linkedin')}
            >
              <div className="share-platform-icon">
                <FaLinkedin />
              </div>
              <span>LinkedIn</span>
            </Button>
            
            <Button 
              variant="light" 
              className="share-platform-btn reddit-btn"
              onClick={() => handleShare('reddit')}
            >
              <div className="share-platform-icon">
                <FaReddit />
              </div>
              <span>Reddit</span>
            </Button>
            
            <Button 
              variant="light" 
              className="share-platform-btn twitter-btn"
              onClick={() => handleShare('twitter')}
            >
              <div className="share-platform-icon">
                <FaSquareXTwitter />
              </div>
              <span>X</span>
            </Button>
          </div>
        </div>
      </Modal.Body>
    </Modal>
  );
};

export default ShareModal; 