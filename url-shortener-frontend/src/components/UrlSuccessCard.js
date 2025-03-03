import React, { useState, useRef} from 'react';
import { Card, Button, InputGroup, Form, OverlayTrigger, Tooltip, Modal, Spinner } from 'react-bootstrap';
import { FaCopy, FaQrcode, FaCheck, FaExternalLinkAlt, FaDownload } from 'react-icons/fa';
import urlService from '../services/urlService';

const UrlSuccessCard = ({ result }) => {
  // Import urlService for API calls with authentication
  const [copied, setCopied] = useState(false);
  const urlRef = useRef(null);
  
  // State for QR code modal
  const [showQrModal, setShowQrModal] = useState(false);
  const [isQrLoading, setIsQrLoading] = useState(true);
  const [qrCodeUrl, setQrCodeUrl] = useState('');
  
  const fullShortUrl = `http://${result.shortUrl}`;
  
  const handleCopy = () => {
    if (urlRef.current) {
      urlRef.current.select();
    }
    navigator.clipboard.writeText(fullShortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

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
  
  const downloadQrCode = () => {
    const link = document.createElement('a');
    link.href = qrCodeUrl;
    link.download = `qrcode-${result.shortId}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <>
      <Card className="mb-4 border-success">
        <Card.Header className="bg-success text-white">
          <div className="d-flex align-items-center">
            <FaCheck className="me-2" />
            <span className="fw-bold">URL Shortened Successfully!</span>
          </div>
        </Card.Header>
        
        <Card.Body>
          <Card.Title className="text-muted fs-6 mb-2">Your short link is ready:</Card.Title>
          
          <InputGroup className="mb-3">
            <Form.Control
              ref={urlRef}
              type="text"
              value={fullShortUrl}
              readOnly
              className="bg-light border-primary"
            />
            <OverlayTrigger
              placement="top"
              overlay={<Tooltip>{copied ? "Copied!" : "Copy to clipboard"}</Tooltip>}
            >
              <Button 
                variant={copied ? "success" : "outline-primary"}
                onClick={handleCopy}
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
              >
                <FaExternalLinkAlt />
              </Button>
            </OverlayTrigger>
          </InputGroup>
          
          <div className="d-flex justify-content-between mt-3">
            <div>
              <span className="text-muted small">Short ID: </span>
              <span className="fw-semibold small">{result.shortId}</span>
            </div>
            <Button 
              variant="outline-info" 
              size="sm"
              onClick={handleViewQrCode}
              className="d-flex align-items-center"
            >
              <FaQrcode className="me-2" /> 
              <span>QR Code</span>
            </Button>
          </div>
        </Card.Body>
      </Card>
      
      {/* QR Code Modal */}
      <Modal 
        show={showQrModal} 
        onHide={() => setShowQrModal(false)}
        centered
        size="md"
      >
        <Modal.Header closeButton>
          <Modal.Title>QR Code</Modal.Title>
        </Modal.Header>
        <Modal.Body className="text-center p-4">
          <img 
            src={qrCodeUrl} 
            alt="QR Code" 
            className="img-fluid" 
            onLoad={() => setIsQrLoading(false)}
            onError={() => setIsQrLoading(false)}
            style={{ 
              display: isQrLoading ? 'none' : 'inline-block',
              width: '250px',
              height: '250px'
            }}
          />
          {isQrLoading && (
            <div className="text-center p-4">
              <Spinner animation="border" variant="primary" />
              <p className="mt-2">Loading QR code...</p>
            </div>
          )}
          <p className="mt-3 mb-0 text-muted small">Scan to open {result.shortUrl}</p>
        </Modal.Body>
        <Modal.Footer>
          <Button 
            variant="primary" 
            size="sm"
            onClick={downloadQrCode}
          >
            <FaDownload className="me-1" /> Download
          </Button>
          <Button 
            variant="secondary" 
            size="sm"
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