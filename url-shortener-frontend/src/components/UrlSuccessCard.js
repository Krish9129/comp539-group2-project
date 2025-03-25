import React, { useState, useRef} from 'react';
import { Card, Button, InputGroup, Form, OverlayTrigger, Tooltip, Modal, Spinner, Badge } from 'react-bootstrap';
import { FaCopy, FaQrcode, FaCheck, FaExternalLinkAlt, FaDownload, FaLink, FaShareAlt, FaTimes } from 'react-icons/fa';
import urlService from '../services/urlService';
import UrlPreview from './UrlPreview';

const UrlSuccessCard = ({ result, onClose }) => {
  // State for clipboard functionality
  const [copied, setCopied] = useState(false);
  const urlRef = useRef(null);
  
  // State for QR code modal
  const [showQrModal, setShowQrModal] = useState(false);
  const [isQrLoading, setIsQrLoading] = useState(true);
  const [qrCodeUrl, setQrCodeUrl] = useState('');
  
  // State for URL preview
  const [showPreview, setShowPreview] = useState(false);
  const [previewPosition, setPreviewPosition] = useState({ x: 0, y: 0 });
  
  const fullShortUrl = `${result.shortUrl}`;
  
  // Handle copy to clipboard
  const handleCopy = () => {
    if (urlRef.current) {
      urlRef.current.select();
    }
    navigator.clipboard.writeText(fullShortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Handle QR code view
  const handleViewQrCode = () => {
    setIsQrLoading(true);
    setShowQrModal(true);
    
    // Get the QR code URL with proper authentication
    if (urlService.getQrCodeUrl) {
      setQrCodeUrl(urlService.getQrCodeUrl(result.shortId));
    } else {
      // Fallback if method doesn't exist
      setQrCodeUrl(`/api/${result.shortId}/qr`);
    }
  };
  
  // Handle QR code download
  const downloadQrCode = () => {
    const link = document.createElement('a');
    link.href = qrCodeUrl;
    link.download = `zaplink-${result.shortId}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Handle mouse enter for preview
  const handleMouseEnter = (e) => {
    // Get mouse position relative to viewport
    setPreviewPosition({ 
      x: e.clientX,
      y: e.clientY
    });
    setShowPreview(true);
  };
  
  // Handle mouse move for updating position
  const handleMouseMove = (e) => {
    if (showPreview) {
      // Update position based on current mouse coordinates
      setPreviewPosition({ 
        x: e.clientX,
        y: e.clientY
      });
    }
  };
  
  // Handle mouse leave for preview
  const handleMouseLeave = () => {
    setShowPreview(false);
  };

  // Handle close of success card
  const handleClose = () => {
    if (onClose) {
      onClose();
    }
  };

  return (
    <>
      <Card className="border-0 shadow-lg recently-created-card success-card">
        <Card.Header className="bg-success text-white py-3 d-flex align-items-center">
          <div className="success-icon-circle me-2">
            <FaCheck />
          </div>
          <h5 className="mb-0 flex-grow-1">Success! Your ZapLink is Ready</h5>
          <Badge bg="light" text="success" className="py-2 px-3 me-2">New</Badge>
          {onClose && (
            <Button 
              variant="link" 
              className="p-0 text-white close-btn" 
              onClick={handleClose}
              aria-label="Close"
            >
              <FaTimes />
            </Button>
          )}
        </Card.Header>
        
        <Card.Body className="p-4">
          {/* Link display and actions */}
          <div className="mb-4">
            <label className="form-label text-muted mb-2">Your short link:</label>
            <InputGroup className="mb-3 shadow-sm">
              <InputGroup.Text className="bg-light border-end-0">
                <FaLink className="text-primary" />
              </InputGroup.Text>
              <Form.Control
                ref={urlRef}
                type="text"
                value={fullShortUrl}
                readOnly
                className="bg-light border-start-0"
                onMouseEnter={handleMouseEnter}
                onMouseMove={handleMouseMove}
                onMouseLeave={handleMouseLeave}
              />
              <OverlayTrigger
                placement="top"
                overlay={<Tooltip>{copied ? "Copied!" : "Copy to clipboard"}</Tooltip>}
              >
                <Button 
                  variant={copied ? "success" : "outline-primary"}
                  onClick={handleCopy}
                  className="d-flex align-items-center justify-content-center"
                  style={{ width: '50px' }}
                >
                  {copied ? <FaCheck /> : <FaCopy />}
                </Button>
              </OverlayTrigger>
              <OverlayTrigger
                placement="top"
                overlay={<Tooltip>Open in new tab</Tooltip>}
              >
                <Button 
                  variant="outline-primary"
                  as="a" 
                  href={fullShortUrl} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="d-flex align-items-center justify-content-center"
                  style={{ width: '50px' }}
                >
                  <FaExternalLinkAlt />
                </Button>
              </OverlayTrigger>
            </InputGroup>
          </div>
          
          {/* Link details and additional options */}
          <div className="d-flex flex-wrap justify-content-between align-items-center bg-light p-3 rounded">
            <div className="mb-2 mb-md-0">
              <span className="text-muted">Short ID: </span>
              <span 
                className="fw-semibold"
                onMouseEnter={handleMouseEnter}
                onMouseMove={handleMouseMove}
                onMouseLeave={handleMouseLeave}
              >{result.shortId}</span>
            </div>
            <div className="d-flex gap-2 flex-wrap">
              <Button 
                variant="outline-primary" 
                size="sm"
                onClick={handleViewQrCode}
                className="d-flex align-items-center"
              >
                <FaQrcode className="me-2" /> 
                <span>QR Code</span>
              </Button>
              <Button 
                variant="outline-success" 
                size="sm"
                as="a"
                href={`https://wa.me/?text=${encodeURIComponent(`Check out this link: ${fullShortUrl}`)}`}
                target="_blank"
                rel="noopener noreferrer"
                className="d-flex align-items-center"
              >
                <FaShareAlt className="me-2" /> 
                <span>Share</span>
              </Button>
            </div>
          </div>
          
          {/* URL Preview */}
          <UrlPreview 
            shortId={result.shortId} 
            show={showPreview} 
            position={previewPosition} 
          />
        </Card.Body>
      </Card>
      
      {/* QR Code Modal */}
      <Modal 
        show={showQrModal} 
        onHide={() => setShowQrModal(false)}
        centered
        size="md"
        className="qr-modal"
      >
        <Modal.Header closeButton className="border-0 pb-0">
          <Modal.Title className="text-primary">
            <FaQrcode className="me-2" />
            QR Code for Your ZapLink
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="text-center px-4 pt-2 pb-4">
          <p className="text-muted mb-4">Scan this code to open your shortened link</p>
          
          <div className="qr-container p-3 mb-3 mx-auto bg-light rounded shadow-sm" style={{ maxWidth: '280px' }}>
            <img 
              src={qrCodeUrl} 
              alt="QR Code" 
              className="img-fluid rounded" 
              onLoad={() => setIsQrLoading(false)}
              onError={() => setIsQrLoading(false)}
              style={{ 
                display: isQrLoading ? 'none' : 'block',
                width: '250px',
                height: '250px'
              }}
            />
            {isQrLoading && (
              <div className="text-center p-4 my-5">
                <Spinner animation="border" variant="primary" />
                <p className="mt-3">Loading QR code...</p>
              </div>
            )}
          </div>
          
          <p className="mb-0 text-muted small">Link: {fullShortUrl}</p>
        </Modal.Body>
        <Modal.Footer className="border-0 pt-0">
          <Button 
            variant="primary" 
            onClick={downloadQrCode}
            className="px-4"
          >
            <FaDownload className="me-2" /> Download QR Code
          </Button>
          <Button 
            variant="light" 
            onClick={() => setShowQrModal(false)}
          >
            Close
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
};

export default UrlSuccessCard;